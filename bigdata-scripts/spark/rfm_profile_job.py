"""PySpark 用户画像、标签和 RFM 分层任务。

输入：Hive DWS 用户指标表。
输出：Hive ADS 画像汇总、用户价值分层、用户标签及分布表。
"""

from __future__ import annotations

import argparse
from datetime import datetime

from pyspark.sql import DataFrame, SparkSession, functions as F
from pyspark.sql.window import Window

DWS_METRICS = "ecommerce_profile_dws.dws_user_profile_metrics"
ADS_SUMMARY = "ecommerce_profile_ads.ads_user_profile_summary"
ADS_SEGMENT = "ecommerce_profile_ads.ads_user_value_segment"
ADS_USER_TAG = "ecommerce_profile_ads.ads_user_tag"
ADS_TAG_DISTRIBUTION = "ecommerce_profile_ads.ads_tag_distribution"
ADS_SEGMENT_DISTRIBUTION = "ecommerce_profile_ads.ads_segment_distribution"


def quantile_score(column: str, ascending: bool = True):
    """使用五分位生成 1～5 分；数值越优得分越高。"""
    order = F.col(column).asc_nulls_last() if ascending else F.col(column).desc_nulls_last()
    return F.ntile(5).over(Window.orderBy(order))


def build_profile_summary(metrics: DataFrame, data_version: str) -> DataFrame:
    """构建供后端高频查询的用户画像汇总。"""
    return metrics.select(
        "user_id", "total_order_count", "total_payment_amount", "average_order_amount",
        "browse_count_30d", "login_count_30d", "last_active_at", "favorite_category_id",
    ).withColumn("data_version", F.lit(data_version)).withColumn("calculated_at", F.current_timestamp())


def build_segments(metrics: DataFrame, data_version: str) -> DataFrame:
    """计算 RFM 得分、综合得分和五类用户价值分层。"""
    scored = (
        metrics
        .withColumn("r_score", F.when(F.col("total_order_count") == 0, F.lit(1)).otherwise(6 - quantile_score("recency_days", True)))
        .withColumn("f_score", quantile_score("total_order_count", True))
        .withColumn("m_score", quantile_score("total_payment_amount", True))
        .withColumn("segment_score", (F.col("r_score") * 0.4 + F.col("f_score") * 0.3 + F.col("m_score") * 0.3).cast("decimal(10,4)"))
    )
    code = (
        F.when((F.col("r_score") >= 4) & (F.col("f_score") >= 4) & (F.col("m_score") >= 4), "HIGH_VALUE")
        .when((F.col("r_score") >= 4) & ((F.col("f_score") >= 3) | (F.col("m_score") >= 3)), "POTENTIAL")
        .when((F.col("r_score") <= 2) & ((F.col("f_score") >= 4) | (F.col("m_score") >= 4)), "AT_RISK")
        .when((F.col("f_score") <= 2) & (F.col("m_score") <= 2), "LOW_VALUE")
        .otherwise("GENERAL")
    )
    name = (
        F.when(code == "HIGH_VALUE", "高价值用户")
        .when(code == "POTENTIAL", "潜力用户")
        .when(code == "AT_RISK", "流失风险用户")
        .when(code == "LOW_VALUE", "低价值用户")
        .otherwise("一般用户")
    )
    return scored.withColumn("segment_code", code).withColumn("segment_name", name).select(
        "user_id", "r_score", "f_score", "m_score", "segment_code", "segment_name", "segment_score"
    ).withColumn("data_version", F.lit(data_version)).withColumn("calculated_at", F.current_timestamp())


def build_user_tags(metrics: DataFrame, segments: DataFrame, data_version: str) -> DataFrame:
    """构建活跃度、消费能力、偏好分类和 RFM 分层标签。"""
    base = metrics.select("user_id", "login_count_30d", "browse_count_30d", "total_payment_amount", "favorite_category_id")
    active = base.select(
        "user_id", F.lit("ACTIVE_LEVEL").alias("tag_code"),
        F.when(F.col("login_count_30d") + F.col("browse_count_30d") >= 50, "High")
        .when(F.col("login_count_30d") + F.col("browse_count_30d") >= 15, "Medium").otherwise("Low").alias("tag_value"),
        (F.col("login_count_30d") + F.col("browse_count_30d")).cast("decimal(10,4)").alias("tag_score"),
    )
    consumption = base.select(
        "user_id", F.lit("CONSUMPTION_LEVEL").alias("tag_code"),
        F.when(F.col("total_payment_amount") >= 10000, "High")
        .when(F.col("total_payment_amount") >= 3000, "Medium").otherwise("Low").alias("tag_value"),
        F.col("total_payment_amount").cast("decimal(10,4)").alias("tag_score"),
    )
    favorite = base.select(
        "user_id", F.lit("FAVORITE_CATEGORY").alias("tag_code"),
        F.coalesce(F.col("favorite_category_id").cast("string"), F.lit("Unknown")).alias("tag_value"),
        F.lit(1).cast("decimal(10,4)").alias("tag_score"),
    )
    rfm = segments.select(
        "user_id", F.lit("RFM_SEGMENT").alias("tag_code"),
        F.col("segment_code").alias("tag_value"), F.col("segment_score").alias("tag_score"),
    )
    return active.unionByName(consumption).unionByName(favorite).unionByName(rfm) \
        .withColumn("data_version", F.lit(data_version)).withColumn("calculated_at", F.current_timestamp())


def build_distribution(tags: DataFrame, data_version: str) -> DataFrame:
    """按标签统计用户数和占比。"""
    counts = tags.groupBy("tag_code", "tag_value").agg(F.countDistinct("user_id").alias("user_count"))
    totals = counts.groupBy("tag_code").agg(F.sum("user_count").alias("total_count"))
    return counts.join(totals, "tag_code").select(
        "tag_code", "tag_value", "user_count",
        (F.col("user_count") / F.col("total_count")).cast("decimal(10,4)").alias("user_ratio"),
        F.lit(data_version).alias("data_version"), F.current_timestamp().alias("calculated_at"),
    )


def build_segment_distribution(segments: DataFrame, data_version: str) -> DataFrame:
    """统计用户分层分布。"""
    counts = segments.groupBy("segment_code", "segment_name").agg(F.count("*").alias("user_count"))
    total = segments.count()
    return counts.select(
        "segment_code", "segment_name", "user_count",
        (F.col("user_count") / F.lit(total)).cast("decimal(10,4)").alias("user_ratio"),
        F.lit(data_version).alias("data_version"), F.current_timestamp().alias("calculated_at"),
    )


def run(spark: SparkSession, data_version: str) -> None:
    """执行完整画像计算并覆盖当前 ADS 结果。"""
    metrics = spark.table(DWS_METRICS).cache()
    if metrics.rdd.isEmpty():
        raise RuntimeError("DWS 用户指标表为空，无法执行画像计算")
    summary = build_profile_summary(metrics, data_version)
    segments = build_segments(metrics, data_version).cache()
    tags = build_user_tags(metrics, segments, data_version).cache()
    summary.write.mode("overwrite").insertInto(ADS_SUMMARY)
    segments.write.mode("overwrite").insertInto(ADS_SEGMENT)
    tags.write.mode("overwrite").insertInto(ADS_USER_TAG)
    build_distribution(tags, data_version).write.mode("overwrite").insertInto(ADS_TAG_DISTRIBUTION)
    build_segment_distribution(segments, data_version).write.mode("overwrite").insertInto(ADS_SEGMENT_DISTRIBUTION)


def main() -> None:
    parser = argparse.ArgumentParser(description="计算电商用户画像标签与 RFM 分层")
    parser.add_argument("--data-version", default=datetime.now().strftime("%Y%m%d%H%M%S"), help="分析批次版本")
    args = parser.parse_args()
    spark = SparkSession.builder.appName("EcommerceUserProfileJob").enableHiveSupport().getOrCreate()
    try:
        run(spark, args.data_version)
    finally:
        spark.stop()


if __name__ == "__main__":
    main()
