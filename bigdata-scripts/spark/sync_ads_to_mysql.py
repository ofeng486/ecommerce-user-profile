"""将 Hive ADS 当前画像结果通过 JDBC 同步到 MySQL。

密码只从环境变量 MYSQL_PASSWORD 读取，不接受命令行明文参数。
"""

from __future__ import annotations

import argparse
import os
from datetime import datetime

from pyspark.sql import DataFrame, SparkSession, functions as F


def jdbc_options(args: argparse.Namespace) -> dict[str, str]:
    """构建 JDBC 连接参数并校验密码环境变量。"""
    password = os.environ.get("MYSQL_PASSWORD")
    if not password:
        raise RuntimeError("缺少 MYSQL_PASSWORD 环境变量")
    return {
        "url": args.jdbc_url,
        "user": args.mysql_user,
        "password": password,
        "driver": "com.mysql.cj.jdbc.Driver",
    }


def write_table(data: DataFrame, table: str, options: dict[str, str]) -> None:
    """清空后覆盖 MySQL 当前结果数据，并要求 JDBC 保留既有表结构和约束。"""
    data.write.format("jdbc").options(**options).option("dbtable", table) \
        .option("truncate", "true").mode("overwrite").save()


def run(spark: SparkSession, args: argparse.Namespace) -> None:
    """同步画像汇总、分层和用户标签结果。"""
    options = jdbc_options(args)
    summary = spark.table("ecommerce_profile_ads.ads_user_profile_summary").where(F.col("data_version") == args.data_version)
    segments = spark.table("ecommerce_profile_ads.ads_user_value_segment").where(F.col("data_version") == args.data_version)
    tags = spark.table("ecommerce_profile_ads.ads_user_tag").where(F.col("data_version") == args.data_version)
    if summary.rdd.isEmpty() or segments.rdd.isEmpty():
        raise RuntimeError(f"ADS 中不存在数据版本 {args.data_version} 的画像结果")

    write_table(summary.select(
        "user_id", "total_order_count", "total_payment_amount", "average_order_amount",
        "browse_count_30d", "login_count_30d", "last_active_at", "favorite_category_id",
        "data_version", "calculated_at",
    ), "user_profile_summary", options)
    write_table(segments.select(
        "user_id", "segment_code", "segment_name", "r_score", "f_score", "m_score",
        "segment_score", "data_version", "calculated_at",
    ), "user_segment", options)

    # 将标签编码关联到 MySQL 标签定义 ID，统一写入字符串标签值。
    definitions = spark.read.format("jdbc").options(**options).option("dbtable", "profile_tag_definition").load() \
        .select(F.col("id").alias("tag_id"), "tag_code")
    mysql_tags = tags.join(definitions, "tag_code").select(
        "user_id", "tag_id", F.col("tag_value"), F.col("tag_score").alias("score"),
        "data_version", "calculated_at",
    ).withColumn("expires_at", F.lit(None).cast("timestamp"))
    write_table(mysql_tags, "user_profile_tag", options)


def main() -> None:
    parser = argparse.ArgumentParser(description="将 Hive ADS 用户画像同步到 MySQL")
    parser.add_argument("--data-version", default=datetime.now().strftime("%Y%m%d%H%M%S"))
    parser.add_argument("--jdbc-url", default="jdbc:mysql://localhost:23307/ecommerce_user_profile?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai")
    parser.add_argument("--mysql-user", default="root")
    args = parser.parse_args()
    spark = SparkSession.builder.appName("SyncUserProfileToMySQL").enableHiveSupport().getOrCreate()
    try:
        run(spark, args)
    finally:
        spark.stop()


if __name__ == "__main__":
    main()
