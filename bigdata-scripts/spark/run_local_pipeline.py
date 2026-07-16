"""PySpark 本地模式端到端用户画像计算管线。

无集群依赖，直接读取 Python 生成的 CSV 文件，在本地完成数据清洗、
指标汇总、RFM 8 分类打分和标签构建，最终 JDBC 写入 MySQL 画像结果表。

运行方式：
  spark-submit run_local_pipeline.py \
    --input ../generated-data/demo \
    --jdbc-url "jdbc:mysql://localhost:23307/ecommerce_user_profile?..." \
    --mysql-user root

或者直接 python 执行（自动创建本地 SparkSession）：
  python run_local_pipeline.py --input ../generated-data/demo
"""

from __future__ import annotations

import argparse
import os
from datetime import datetime
from pathlib import Path
from typing import Optional

from pyspark.sql import SparkSession, DataFrame, functions as F
from pyspark.sql.window import Window
from pyspark.sql.types import DecimalType


# ═══════════════════════════════════════════════════════════════
# Phase 0 — Spark 初始化
# ═══════════════════════════════════════════════════════════════

def create_spark() -> SparkSession:
    """创建本地模式 SparkSession，不依赖 Hive 和 HDFS。"""
    import os as _os
    jar_dir = _os.path.join(_os.path.dirname(_os.path.abspath(__file__)), "jars")
    mysql_jar = _os.path.join(jar_dir, "mysql-connector-j-9.7.0.jar")
    return SparkSession.builder \
        .appName("EcommerceProfileLocal") \
        .master("local[*]") \
         \
        .config("spark.driver.extraClassPath", mysql_jar) \
        .config("spark.sql.adaptive.enabled", "true") \
        .config("spark.sql.adaptive.coalescePartitions.enabled", "true") \
        .config("spark.sql.legacy.timeParserPolicy", "LEGACY") \
        .config("spark.driver.memory", "2g") \
        .getOrCreate()


# ═══════════════════════════════════════════════════════════════
# Phase 1 — 读取 CSV 原始数据
# ═══════════════════════════════════════════════════════════════

def read_csv(spark: SparkSession, data_dir: str, file: str,
             schema: Optional[str] = None) -> DataFrame:
    """读取 CSV 文件，跳过表头行，自动推断类型。"""
    path = str(Path(data_dir) / file)
    reader = spark.read.option("header", "true").option("inferSchema", "true")
    if schema:
        reader = reader.schema(schema)
    return reader.csv(path)


def load_raw_data(spark: SparkSession, data_dir: str) -> dict[str, DataFrame]:
    """加载全部 7 张原始 CSV 表。"""
    print("=== Phase 1：读取 CSV 原始数据 ===")

    df = {}
    df["category"]  = read_csv(spark, data_dir, "product_category.csv")
    df["product"]   = read_csv(spark, data_dir, "product.csv")
    df["user"]      = read_csv(spark, data_dir, "ecommerce_user.csv")
    df["behavior"]  = read_csv(spark, data_dir, "user_browse_behavior.csv")
    df["login"]     = read_csv(spark, data_dir, "user_login_behavior.csv")
    df["order"]     = read_csv(spark, data_dir, "sales_order.csv")
    df["order_item"]= read_csv(spark, data_dir, "sales_order_item.csv")

    for name, d in df.items():
        print(f"  {name}: {d.count()} 行")
    return df


# ═══════════════════════════════════════════════════════════════
# Phase 2 — DWD 级清洗与维度关联
# ═══════════════════════════════════════════════════════════════

def cleanse_dwd(raw: dict[str, DataFrame], stat_date: str) -> dict[str, DataFrame]:
    """完成去重、有效性校验和维度关联，返回清洗后的宽表。"""
    print("=== Phase 2：DWD 清洗与维度关联 ===")

    # 有效电商用户（状态正常，年龄合法）
    user = raw["user"].filter(
        (F.col("status") == 1) &
        F.col("age").between(1, 120) &
        F.col("registered_at").isNotNull()
    ).dropDuplicates(["id"])

    # 商品关联分类
    product = raw["product"].filter(
        F.col("unit_price") >= 0
    ).dropDuplicates(["id"]).join(
        raw["category"].select(F.col("id").alias("cat_id"), F.col("category_name")),
        F.col("category_id") == F.col("cat_id"), "left"
    )

    # 有效订单（Paid/Shipped/Completed，校验金额一致性）
    valid_order = raw["order"].filter(
        F.col("order_status").isin("Paid", "Shipped", "Completed") &
        (F.col("total_amount") >= 0) &
        (F.col("discount_amount") >= 0) &
        (F.col("payment_amount") == F.col("total_amount") - F.col("discount_amount")) &
        F.col("paid_at").isNotNull() &
        (F.col("paid_at") >= F.col("ordered_at"))
    ).join(user.select("id", "registered_at"), F.col("user_id") == user["id"]) \
     .filter(F.col("ordered_at") >= F.col("registered_at")) \
     .select(raw["order"]["*"])

    # 浏览行为去重，行为时间 ≥ 注册时间
    behavior = raw["behavior"].filter(
        F.col("behavior_type").isin("View", "Click", "Favorite", "Cart") &
        F.col("behavior_at").isNotNull()
    ).dropDuplicates(["id"]).join(
        product.select(F.col("id").alias("p_id"), F.col("category_id").alias("beh_cat_id")),
        F.col("product_id") == F.col("p_id"), "left"
    ).join(user.select("id", "registered_at"), F.col("user_id") == user["id"]) \
     .filter(F.col("behavior_at") >= F.col("registered_at"))

    # 登录行为去重
    login = raw["login"].filter(
        F.col("login_at").isNotNull()
    ).dropDuplicates(["id"]).join(
        user.select("id", "registered_at"), F.col("user_id") == user["id"]
    ).filter(F.col("login_at") >= F.col("registered_at"))

    # 订单明细关联商品
    order_item = raw["order_item"].filter(
        (F.col("quantity") > 0) & (F.col("unit_price") >= 0)
    ).join(valid_order.select("id", "user_id", "ordered_at"),
           F.col("order_id") == valid_order["id"])

    print(f"  有效用户={user.count()}  有效订单={valid_order.count()}  有效行为={behavior.count()}")
    return {"user": user, "product": product, "order": valid_order,
            "behavior": behavior, "login": login, "order_item": order_item}


# ═══════════════════════════════════════════════════════════════
# Phase 3 — DWS 级用户指标汇总
# ═══════════════════════════════════════════════════════════════

def aggregate_dws(dwd: dict[str, DataFrame], stat_date: str) -> DataFrame:
    """按用户汇总消费、行为、登录指标和偏好分类。"""
    print("=== Phase 3：DWS 用户指标汇总 ===")

    stat_date_col = F.lit(stat_date).cast("date")

    # 消费指标
    order_metrics = dwd["order"].filter(
        F.col("ordered_at") <= stat_date_col
    ).groupBy("user_id").agg(
        F.countDistinct("id").alias("total_order_count"),
        F.coalesce(F.sum("payment_amount"), F.lit(0)).cast(DecimalType(18, 2)).alias("total_payment_amount"),
        F.coalesce(F.avg("payment_amount"), F.lit(0)).cast(DecimalType(18, 2)).alias("average_order_amount"),
        F.max("ordered_at").alias("last_order_at"),
        F.datediff(stat_date_col, F.to_date(F.max("ordered_at"))).alias("recency_days")
    )

    # 近 30 日行为指标
    behavior_30d = dwd["behavior"].filter(
        (F.col("behavior_at") >= F.date_sub(stat_date_col, 29)) &
        (F.col("behavior_at") <= stat_date_col)
    ).groupBy("user_id").agg(
        F.sum(F.when(F.col("behavior_type") == "View", 1).otherwise(0)).alias("browse_count_30d"),
        F.sum(F.when(F.col("behavior_type") == "Click", 1).otherwise(0)).alias("click_count_30d"),
        F.sum(F.when(F.col("behavior_type") == "Favorite", 1).otherwise(0)).alias("favorite_count_30d"),
        F.sum(F.when(F.col("behavior_type") == "Cart", 1).otherwise(0)).alias("cart_count_30d"),
        F.max("behavior_at").alias("last_behavior_at")
    )

    # 近 30 日登录指标
    login_30d = dwd["login"].filter(
        (F.col("login_at") >= F.date_sub(stat_date_col, 29)) &
        (F.col("login_at") <= stat_date_col)
    ).groupBy("user_id").agg(
        F.count("*").alias("login_count_30d"),
        F.coalesce(F.sum("duration_seconds"), F.lit(0)).alias("login_duration_seconds_30d"),
        F.max("login_at").alias("last_login_at")
    )

    # 偏好分类——加权行为评分
    behavior_all = dwd["behavior"].filter(F.col("behavior_at") <= stat_date_col)
    pref_score = (
        F.when(F.col("behavior_type") == "View", 1)
        .when(F.col("behavior_type") == "Click", 2)
        .when(F.col("behavior_type") == "Favorite", 4)
        .when(F.col("behavior_type") == "Cart", 5).otherwise(0)
    )
    category_scores = behavior_all.withColumn("pref", pref_score) \
        .groupBy("user_id", "beh_cat_id").agg(F.sum("pref").alias("score"))
    win_cat = Window.partitionBy("user_id").orderBy(F.col("score").desc(), F.col("beh_cat_id").asc())
    favorite_category = category_scores.withColumn("rn", F.row_number().over(win_cat)) \
        .filter("rn = 1").select("user_id", F.col("beh_cat_id").alias("favorite_category_id"))

    # 合并为一张用户指标宽表
    metrics = dwd["user"].select(
        "id", "user_code", "gender", "age", "province", "city",
        "register_channel", "membership_level"
    ).withColumnRenamed("id", "user_id") \
        .join(order_metrics, "user_id", "left") \
        .join(behavior_30d, "user_id", "left") \
        .join(login_30d, "user_id", "left") \
        .join(favorite_category, "user_id", "left") \
        .withColumn("total_order_count", F.coalesce(F.col("total_order_count"), F.lit(0))) \
        .withColumn("total_payment_amount", F.coalesce(F.col("total_payment_amount"), F.lit(0))) \
        .withColumn("average_order_amount", F.coalesce(F.col("average_order_amount"), F.lit(0))) \
        .withColumn("browse_count_30d", F.coalesce(F.col("browse_count_30d"), F.lit(0))) \
        .withColumn("login_count_30d", F.coalesce(F.col("login_count_30d"), F.lit(0))) \
        .withColumn("recency_days", F.coalesce(F.col("recency_days"), F.lit(9999))) \
        .withColumn("last_active_at",
            F.greatest(F.coalesce("last_behavior_at", F.lit("1970-01-01")),
                       F.coalesce("last_login_at", F.lit("1970-01-01"))))

    print(f"  用户指标汇总：{metrics.count()} 条")
    return metrics.cache()


# ═══════════════════════════════════════════════════════════════
# Phase 4 — RFM 打分与 8 分类
# ═══════════════════════════════════════════════════════════════

def score_rfm(metrics: DataFrame, data_version: str) -> tuple[DataFrame, DataFrame]:
    """NTILE(5) RFM 打分 + 8 分类 + 画像汇总。"""
    print("=== Phase 4：RFM 打分与 8 分类 ===")

    # ---------- 画像汇总 ----------
    summary = metrics.select(
        "user_id", "total_order_count", "total_payment_amount", "average_order_amount",
        "browse_count_30d", "login_count_30d", "last_active_at", "favorite_category_id"
    ).withColumn("data_version", F.lit(data_version)) \
     .withColumn("calculated_at", F.current_timestamp())

    # ---------- NTILE(5) RFM 打分 ----------
    scored = metrics.select("user_id", "total_order_count", "total_payment_amount", "recency_days") \
        .withColumn("r_ntile", F.ntile(5).over(Window.orderBy(F.col("recency_days").asc()))) \
        .withColumn("f_ntile", F.ntile(5).over(Window.orderBy(F.col("total_order_count").asc()))) \
        .withColumn("m_ntile", F.ntile(5).over(Window.orderBy(F.col("total_payment_amount").asc()))) \
        .withColumn("r_score", F.when(F.col("total_order_count") == 0, 1).otherwise(6 - F.col("r_ntile"))) \
        .withColumn("f_score", F.col("f_ntile")) \
        .withColumn("m_score", F.col("m_ntile")) \
        .drop("r_ntile", "f_ntile", "m_ntile")

    # ---------- 8 分类 ----------
    rfm = scored.withColumn("rfm_group",
        F.when((F.col("r_score") >= 3) & (F.col("f_score") >= 3) & (F.col("m_score") >= 3), "HIGH_VALUE")
         .when((F.col("r_score") >= 3) & (F.col("f_score") >= 3) & (F.col("m_score") <  3), "HIGH_DEVELOP")
         .when((F.col("r_score") >= 3) & (F.col("f_score") <  3) & (F.col("m_score") >= 3), "HIGH_RETAIN")
         .when((F.col("r_score") <  3) & (F.col("f_score") >= 3) & (F.col("m_score") >= 3), "LOST_RETAIN")
         .when((F.col("r_score") >= 3) & (F.col("f_score") <  3) & (F.col("m_score") <  3), "GEN_VALUE")
         .when((F.col("r_score") <  3) & (F.col("f_score") >= 3) & (F.col("m_score") <  3), "GEN_DEVELOP")
         .when((F.col("r_score") <  3) & (F.col("f_score") <  3) & (F.col("m_score") >= 3), "GEN_RETAIN")
         .otherwise("LOST")
    ).withColumn("rfm_group_name",
        F.when(F.col("rfm_group") == "HIGH_VALUE",    "重要价值客户")
         .when(F.col("rfm_group") == "HIGH_DEVELOP",  "重要发展客户")
         .when(F.col("rfm_group") == "HIGH_RETAIN",   "重要保持客户")
         .when(F.col("rfm_group") == "LOST_RETAIN",   "重要挽留客户")
         .when(F.col("rfm_group") == "GEN_VALUE",     "一般价值客户")
         .when(F.col("rfm_group") == "GEN_DEVELOP",   "一般发展客户")
         .when(F.col("rfm_group") == "GEN_RETAIN",    "一般保持客户")
         .otherwise("流失客户")
    ).withColumn("data_version", F.lit(data_version)) \
     .withColumn("calculated_at", F.current_timestamp())

    # ---------- 用户分层（8 分类合并为 5 类，与集群 rfm_profile_job.py 对齐） ----------
    # 映射规则：
    #   HIGH_VALUE                      → HIGH_VALUE  高价值用户
    #   HIGH_DEVELOP / HIGH_RETAIN      → POTENTIAL   潜力用户（R≥3 且至少一个指标高）
    #   LOST_RETAIN / GEN_DEVELOP / GEN_RETAIN → AT_RISK  流失风险用户（R<3 但至少一个指标高）
    #   GEN_VALUE                       → GENERAL     一般用户（R≥3 但两个指标都低）
    #   LOST                            → LOW_VALUE   低价值用户
    segment = rfm.withColumn("segment_code",
        F.when(F.col("rfm_group") == "HIGH_VALUE", "HIGH_VALUE")
         .when(F.col("rfm_group").isin("HIGH_DEVELOP", "HIGH_RETAIN"), "POTENTIAL")
         .when(F.col("rfm_group").isin("LOST_RETAIN", "GEN_DEVELOP", "GEN_RETAIN"), "AT_RISK")
         .when(F.col("rfm_group") == "GEN_VALUE", "GENERAL")
         .otherwise("LOW_VALUE")
    ).withColumn("segment_name",
        F.when(F.col("rfm_group") == "HIGH_VALUE", "高价值用户")
         .when(F.col("rfm_group").isin("HIGH_DEVELOP", "HIGH_RETAIN"), "潜力用户")
         .when(F.col("rfm_group").isin("LOST_RETAIN", "GEN_DEVELOP", "GEN_RETAIN"), "流失风险用户")
         .when(F.col("rfm_group") == "GEN_VALUE", "一般用户")
         .otherwise("低价值用户")
    ).withColumn("segment_score",
        (F.col("r_score") * 0.4 + F.col("f_score") * 0.3 + F.col("m_score") * 0.3).cast(DecimalType(10, 4))
    ).select(
        "user_id", "segment_code", "segment_name",
        "r_score", "f_score", "m_score", "segment_score",
        "data_version", "calculated_at"
    )

    # ---------- 用户标签 ----------
    tags = _build_tags(metrics, segment, data_version)

    print(f"  RFM 8 分类：{rfm.count()} 条   分层：{segment.count()} 条   标签：{tags.count()} 条")
    return summary, segment, tags, rfm


def _build_tags(metrics: DataFrame, segment: DataFrame, data_version: str) -> DataFrame:
    """构建 4 类用户标签：活跃等级 / 消费能力 / 偏好分类 / RFM 分层。"""
    active = metrics.select(
        "user_id",
        F.lit(1).cast("bigint").alias("tag_id"),
        F.when(F.col("login_count_30d") + F.col("browse_count_30d") >= 50, "High")
         .when(F.col("login_count_30d") + F.col("browse_count_30d") >= 15, "Medium")
         .otherwise("Low").alias("tag_value"),
        (F.col("login_count_30d") + F.col("browse_count_30d")).cast(DecimalType(10, 4)).alias("score")
    )
    consumption = metrics.select(
        "user_id",
        F.lit(2).cast("bigint").alias("tag_id"),
        F.when(F.col("total_payment_amount") >= 10000, "High")
         .when(F.col("total_payment_amount") >= 3000, "Medium")
         .otherwise("Low").alias("tag_value"),
        F.col("total_payment_amount").cast(DecimalType(10, 4)).alias("score")
    )
    favorite = metrics.select(
        "user_id",
        F.lit(3).cast("bigint").alias("tag_id"),
        F.coalesce(F.col("favorite_category_id").cast("string"), F.lit("Unknown")).alias("tag_value"),
        F.lit(1).cast(DecimalType(10, 4)).alias("score")
    )
    rfm_tag = segment.select(
        "user_id",
        F.lit(4).cast("bigint").alias("tag_id"),
        F.col("segment_code").alias("tag_value"),
        F.col("segment_score").alias("score")
    )
    return active.unionByName(consumption).unionByName(favorite).unionByName(rfm_tag) \
        .withColumn("data_version", F.lit(data_version)) \
        .withColumn("calculated_at", F.current_timestamp())


# ═══════════════════════════════════════════════════════════════
# Phase 5 — 写入 MySQL
# ═══════════════════════════════════════════════════════════════

def write_to_mysql(summary: DataFrame, segment: DataFrame, tags: DataFrame,
                   rfm: DataFrame, options: dict[str, str]) -> None:
    """清空后覆盖写入 4 张画像结果表。"""
    print("=== Phase 5：写入 MySQL ===")

    tables = [
        (summary, "user_profile_summary"),
        (segment, "user_segment"),
        (tags,     "user_profile_tag"),
        (rfm,      "ads_user_rfm"),
    ]

    for df, table in tables:
        df.write.format("jdbc") \
            .options(**options) \
            .option("dbtable", table) \
            .option("truncate", "true") \
            .mode("overwrite").save()
        print(f"  [OK] {table}")


def jdbc_options(args: argparse.Namespace) -> dict[str, str]:
    """构建 JDBC 连接参数。"""
    password = os.environ.get("MYSQL_PASSWORD")
    if not password:
        raise RuntimeError("缺少 MYSQL_PASSWORD 环境变量")
    return {
        "url": args.jdbc_url,
        "user": args.mysql_user,
        "password": password,
        "driver": "com.mysql.cj.jdbc.Driver",
    }


# ═══════════════════════════════════════════════════════════════
# 入口
# ═══════════════════════════════════════════════════════════════

def main() -> None:
    parser = argparse.ArgumentParser(
        description="PySpark 本地模式端到端用户画像计算管线"
    )
    parser.add_argument("--input", default="../generated-data/demo",
                        help="CSV 数据目录路径")
    parser.add_argument("--stat-date",
                        default=datetime.now().strftime("%Y-%m-%d"),
                        help="统计截止日期 YYYY-MM-DD")
    parser.add_argument("--data-version",
                        default=datetime.now().strftime("%Y%m%d%H%M%S"),
                        help="分析批次版本号")
    parser.add_argument("--jdbc-url",
                        default="jdbc:mysql://localhost:3306/ecommerce_user_profile",
                        help="MySQL JDBC 连接地址")
    parser.add_argument("--mysql-user", default="root",
                        help="MySQL 用户名")
    args = parser.parse_args()

    spark = create_spark()
    try:
        # Phase 1：读取 CSV
        raw = load_raw_data(spark, args.input)

        # Phase 2：DWD 清洗
        dwd = cleanse_dwd(raw, args.stat_date)

        # Phase 3：DWS 汇总
        metrics = aggregate_dws(dwd, args.stat_date)

        # Phase 4：RFM 打分 + 8 分类 + 标签
        summary, segment, tags, rfm = score_rfm(metrics, args.data_version)

        # Phase 5：写入 MySQL
        opts = jdbc_options(args)
        write_to_mysql(summary, segment, tags, rfm, opts)

        print(f"\n画像管线执行完毕。版本：{args.data_version}  统计日：{args.stat_date}")
    finally:
        spark.stop()


if __name__ == "__main__":
    main()
