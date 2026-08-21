"""用户聚类独立重算脚本（CLUSTER_RECALC）。

仅重算 K-Means 聚类，不重跑画像管线：
  读取 MySQL 画像结果表 user_profile_summary 的四维特征
  （消费金额/订单数/近30日浏览/近30日登录）→ 标准化 → KMeans(k)
  → truncate 覆盖写回 user_cluster 表。

适用场景：调整簇数 K 后重算聚类，速度快且不影响画像/标签/分层数据。

运行方式：
  python run_cluster_only.py --k 6 --jdbc-url "jdbc:mysql://localhost:3306/ecommerce_user_profile" --mysql-user root
"""

from __future__ import annotations

import argparse
import os
from collections import defaultdict
from datetime import datetime
from pathlib import Path

from pyspark.sql import SparkSession, functions as F

# 重复簇判定：标准化空间（withMean/withStd 后各维同量纲）中簇中心欧氏距离
# 低于该阈值视为"几乎同一簇"并合并（解决 K 偏大时出现特征相近簇的问题）。
# 说明：不能用余弦相似度——K-Means 各簇中心常为"方向一致、幅度不同"，
# 余弦只比方向会误判全部相似；欧氏距离能区分幅度差异。
# 校准：真实数据中重复对（零浏览零登录低消费）距离约 1.1，非重复对 >3.1，故取 1.5。
MERGE_DIST_THRESHOLD = 1.5


def create_spark() -> SparkSession:
    """创建本地模式 SparkSession（与主管线一致，加载 MySQL 驱动）。"""
    jar_dir = Path(__file__).resolve().parent / "jars"
    mysql_jar = jar_dir / "mysql-connector-j-9.7.0.jar"
    return SparkSession.builder \
        .appName("EcommerceClusterOnly") \
        .master("local[*]") \
        .config("spark.driver.extraClassPath", str(mysql_jar)) \
        .config("spark.sql.adaptive.enabled", "true") \
        .config("spark.driver.memory", "2g") \
        .getOrCreate()


def jdbc_options(args: argparse.Namespace) -> dict[str, str]:
    """构建 JDBC 连接参数（密码取环境变量 MYSQL_PASSWORD）。"""
    password = os.environ.get("MYSQL_PASSWORD")
    if not password:
        raise RuntimeError("缺少 MYSQL_PASSWORD 环境变量")
    return {
        "url": args.jdbc_url,
        "user": args.mysql_user,
        "password": password,
        "driver": "com.mysql.cj.jdbc.Driver",
    }


def merge_similar_clusters(centers: list) -> dict:
    """基于簇中心欧氏距离合并重复簇，返回 {旧簇ID: 合并后簇ID} 映射。

    - centers: KMeans 簇中心向量列表（标准化后，长度=K）
    - 返回: 字典 old_id -> new_id，new_id 为 0..M-1 连续重编号（按原编号序）

    实现：两两中心计算欧氏距离，低于阈值用并查集合并（并查集保证传递闭包，
    即 A~B、B~C 时三簇合并为一组），再按稳定顺序重编号。
    """
    k = len(centers)

    def dist(a, b):
        # centers 是 numpy.ndarray（clusterCenters 返回），直接逐元素运算
        return float(sum((float(x) - float(y)) ** 2 for x, y in zip(a, b)) ** 0.5)

    # 并查集
    parent = list(range(k))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def union(a, b):
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[ra] = rb

    for i in range(k):
        for j in range(i + 1, k):
            if dist(centers[i], centers[j]) <= MERGE_DIST_THRESHOLD:
                union(i, j)

    # 根 → 新编号（连续 0..M-1）
    roots = sorted({find(i) for i in range(k)})
    root_to_id = {r: idx for idx, r in enumerate(roots)}
    return {i: root_to_id[find(i)] for i in range(k)}


def main() -> None:
    parser = argparse.ArgumentParser(description="用户聚类独立重算（K 可配置，自动合并相似簇）")
    parser.add_argument("--k", type=int, default=5, help="K-Means 簇数（3-10，默认 5）")
    parser.add_argument("--data-version",
                        default=datetime.now().strftime("%Y%m%d%H%M%S"),
                        help="聚类数据版本号")
    parser.add_argument("--jdbc-url",
                        default="jdbc:mysql://localhost:3306/ecommerce_user_profile",
                        help="MySQL JDBC 连接地址")
    parser.add_argument("--mysql-user", default="root", help="MySQL 用户名")
    parser.add_argument("--no-merge", action="store_true",
                        help="关闭相似簇自动合并（严格按 K 输出原始 K-Means 结果）")
    args = parser.parse_args()

    if not (2 <= args.k <= 20):
        raise ValueError(f"K 值超出合理范围: {args.k}（允许 2-20）")

    spark = create_spark()
    try:
        jdbc = jdbc_options(args)
        merge_note = "已禁用相似簇合并" if args.no_merge else f"相似簇合并阈值 {MERGE_DIST_THRESHOLD}"
        print(f"=== 用户聚类重算（k={args.k}，版本 {args.data_version}，{merge_note}）===")

        # 读取画像结果表特征（与主管线 Phase 6 完全一致的四维特征）
        sql = ("SELECT user_id, total_payment_amount, total_order_count, "
               "browse_count_30d, login_count_30d FROM user_profile_summary")
        metrics = spark.read.jdbc(url=jdbc["url"], table=f"({sql}) t", properties=jdbc)
        print(f"  读取画像特征：{metrics.count()} 个用户")

        from pyspark.ml.feature import VectorAssembler, StandardScaler
        from pyspark.ml.clustering import KMeans

        feat_cols = ["total_payment_amount", "total_order_count", "browse_count_30d", "login_count_30d"]
        df = metrics.select("user_id", *feat_cols).fillna(0)

        vec = VectorAssembler(inputCols=feat_cols, outputCol="features_raw").transform(df)
        scaled = StandardScaler(inputCol="features_raw", outputCol="features",
                                withStd=True, withMean=True).fit(vec).transform(vec)
        kmodel = KMeans(featuresCol="features", k=args.k, seed=42, maxIter=30).fit(scaled)
        assigned = kmodel.transform(scaled).select("user_id", F.col("prediction").alias("cluster_id"))

        # ── 相似簇合并（治本：K 偏大时消除特征相近的重复簇；--no-merge 可关闭）──
        # 中心向量只有 K 个，在 driver 侧用并查集算好 old->new 映射，
        # 再用 when/otherwise 表达式链做纯 JVM 侧重映射——不经过 Python 数据回传，
        # 规避 Windows 下 Spark collect/createDataFrame 的 Connection reset 不稳定。
        centers = kmodel.clusterCenters()
        if args.no_merge:
            # 严格按 K 输出：簇号保持 0..K-1
            merged_k = kmodel.getK()
            assigned_df = assigned.withColumn("data_version", F.lit(args.data_version))
            print(f"  [--no-merge] 已关闭相似簇合并，严格按 K={merged_k} 输出原始聚类结果")
        else:
            # 调试：打印标准化空间中簇中心两两欧氏距离，用于校准 MERGE_DIST_THRESHOLD
            k = len(centers)
            dists = []
            for i in range(k):
                for j in range(i + 1, k):
                    d = float(sum((float(centers[i][m]) - float(centers[j][m])) ** 2 for m in range(len(centers[i]))) ** 0.5)
                    dists.append((i, j, d))
            dists.sort(key=lambda t: t[2])
            print(f"  [调试] 簇中心最近 3 对欧氏距离: " +
                  ", ".join(f"簇{i}-簇{j}={d:.3f}" for i, j, d in dists[:3]))
            merge_map = merge_similar_clusters(centers)
            merged_k = len(set(merge_map.values()))

            from functools import reduce
            merge_expr = reduce(
                lambda acc, kv: acc.when(F.col("cluster_id") == kv[0], F.lit(kv[1])),
                merge_map.items(),
                F.when(F.col("cluster_id") == -1, F.lit(-1))  # 占位（永不命中）
            )
            assigned_df = assigned.withColumn("cluster_id", merge_expr) \
                .withColumn("data_version", F.lit(args.data_version))

            print(f"  原始 K={kmodel.getK()}，合并后实际簇数 {merged_k}")
            if merged_k < kmodel.getK():
                print(f"  合并了 {kmodel.getK() - merged_k} 个相似簇（欧氏距离 ≤ {MERGE_DIST_THRESHOLD}）")
        print(f"  聚类完成：{assigned_df.count()} 个用户分配到 {merged_k} 个有效簇")

        # 覆盖写回 user_cluster 表（truncate 保留表结构）
        assigned_df.write.format("jdbc") \
            .options(**jdbc) \
            .option("dbtable", "user_cluster") \
            .option("truncate", "true") \
            .mode("overwrite").save()
        print("  [OK] user_cluster 已更新")
        print(f"\n聚类重算完毕。K={args.k}（{merged_k} 簇）  版本：{args.data_version}")
    finally:
        spark.stop()


if __name__ == "__main__":
    main()
