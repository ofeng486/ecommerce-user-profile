"""Spark SQL 版 RFM 8 分类用户价值分析。
基于 Hive 明细表 dwd_order_detail 计算每个用户的 RFM 指标，
使用 NTILE(5) 五等分打分，以中位数 3 为阈值划分为 8 类客户，
最终通过 JDBC 写入 MySQL ads_user_rfm 表。

运行方式：
  spark-submit rfm_sql_8seg.py --stat-date 2026-01-01 --data-version 20260101120000
"""

from __future__ import annotations

import argparse
import os
from datetime import datetime

from pyspark.sql import SparkSession

# ──────────────────────────── JDBC 配置 ────────────────────────────

def jdbc_options(jdbc_url: str, mysql_user: str) -> dict[str, str]:
    """构建 JDBC 连接参数，密码从环境变量 MYSQL_PASSWORD 读取。"""
    password = os.environ.get("MYSQL_PASSWORD")
    if not password:
        raise RuntimeError("缺少 MYSQL_PASSWORD 环境变量，请设置后重试")
    return {
        "url": jdbc_url,
        "user": mysql_user,
        "password": password,
        "driver": "com.mysql.cj.jdbc.Driver",
    }


# ──────────────────────────── Spark SQL RFM 计算 ────────────────────────────

def run(spark: SparkSession, stat_date: str, data_version: str,
        jdbc_url: str, mysql_user: str) -> None:
    """执行 RFM 8 分类计算并写入 MySQL。"""

    # =====================================================================
    # Step 1：从 Hive 明细表汇总每个用户的 R / F / M 原始值。
    # R = 统计日 - 最近一次订单日期（天），越小越好。
    # F = 有效订单数，越大越好。
    # M = 累计实付金额，越大越好。
    # COALESCE 确保无订单用户也有默认值 0，避免空值导致 NTILE 异常。
    # =====================================================================
    spark.sql(f"""
        CREATE OR REPLACE TEMP VIEW user_rfm_base AS
        SELECT
            user_id,
            DATEDIFF('{stat_date}', MAX(order_time))     AS recency_days,
            COUNT(DISTINCT order_id)                     AS frequency,
            COALESCE(SUM(pay_amount), CAST(0 AS DECIMAL(18,2))) AS monetary
        FROM ecommerce_profile_dwd.dwd_valid_order
        WHERE order_date <= DATE('{stat_date}')
          AND order_status IN ('Paid', 'Shipped', 'Completed')
        GROUP BY user_id
    """)

    # =====================================================================
    # Step 2：NTILE(5) 五等分打分。
    # R：按 recency_days ASC 分桶，ntile=1 最近（最好），反向得 R=5。
    # F/M：按值 ASC 分桶，ntile=1 最小（最差），ntile=5 最大（最好）。
    # 结果：R/F/M 得分范围均为 1～5。
    # =====================================================================
    spark.sql("""
        CREATE OR REPLACE TEMP VIEW user_rfm_scored AS
        SELECT
            user_id,
            recency_days,
            frequency,
            monetary,
            6 - NTILE(5) OVER (ORDER BY recency_days ASC) AS r_score,
                NTILE(5) OVER (ORDER BY frequency ASC)    AS f_score,
                NTILE(5) OVER (ORDER BY monetary ASC)     AS m_score
        FROM user_rfm_base
    """)

    # =====================================================================
    # Step 3：以中位数 3 为阈值，CASE WHEN 划分为 8 类用户。
    #
    #  ┌─────┬─────┬─────┬──────────────────┐
    #  │  R  │  F  │  M  │       分类       │
    #  ├─────┼─────┼─────┼──────────────────┤
    #  │ ≥3  │ ≥3  │ ≥3  │ 重要价值客户     │
    #  │ ≥3  │ ≥3  │ <3  │ 重要发展客户     │
    #  │ ≥3  │ <3  │ ≥3  │ 重要保持客户     │
    #  │ <3  │ ≥3  │ ≥3  │ 重要挽留客户     │
    #  │ ≥3  │ <3  │ <3  │ 一般价值客户     │
    #  │ <3  │ ≥3  │ <3  │ 一般发展客户     │
    #  │ <3  │ <3  │ ≥3  │ 一般保持客户     │
    #  │ <3  │ <3  │ <3  │ 流失客户         │
    #  └─────┴─────┴─────┴──────────────────┘
    # =====================================================================
    rfm_result = spark.sql(f"""
        SELECT
            user_id,
            recency_days                                    AS r_value,
            frequency                                       AS f_value,
            monetary                                        AS m_value,
            r_score,
            f_score,
            m_score,
            CASE
                WHEN r_score >= 3 AND f_score >= 3 AND m_score >= 3 THEN 'HIGH_VALUE'
                WHEN r_score >= 3 AND f_score >= 3 AND m_score <  3 THEN 'HIGH_DEVELOP'
                WHEN r_score >= 3 AND f_score <  3 AND m_score >= 3 THEN 'HIGH_RETAIN'
                WHEN r_score <  3 AND f_score >= 3 AND m_score >= 3 THEN 'LOST_RETAIN'
                WHEN r_score >= 3 AND f_score <  3 AND m_score <  3 THEN 'GEN_VALUE'
                WHEN r_score <  3 AND f_score >= 3 AND m_score <  3 THEN 'GEN_DEVELOP'
                WHEN r_score <  3 AND f_score <  3 AND m_score >= 3 THEN 'GEN_RETAIN'
                ELSE 'LOST'
            END                                             AS rfm_group,
            CASE
                WHEN r_score >= 3 AND f_score >= 3 AND m_score >= 3 THEN '重要价值客户'
                WHEN r_score >= 3 AND f_score >= 3 AND m_score <  3 THEN '重要发展客户'
                WHEN r_score >= 3 AND f_score <  3 AND m_score >= 3 THEN '重要保持客户'
                WHEN r_score <  3 AND f_score >= 3 AND m_score >= 3 THEN '重要挽留客户'
                WHEN r_score >= 3 AND f_score <  3 AND m_score <  3 THEN '一般价值客户'
                WHEN r_score <  3 AND f_score >= 3 AND m_score <  3 THEN '一般发展客户'
                WHEN r_score <  3 AND f_score <  3 AND m_score >= 3 THEN '一般保持客户'
                ELSE '流失客户'
            END                                             AS rfm_group_name,
            '{data_version}'                                AS data_version,
            CURRENT_TIMESTAMP()                             AS calculated_at
        FROM user_rfm_scored
    """)

    # =====================================================================
    # Step 4：JDBC 写入 MySQL ads_user_rfm 表。
    # 使用 truncate + overwrite 模式，写入前先创建表（幂等）。
    # =====================================================================
    options = jdbc_options(jdbc_url, mysql_user)

    # 确保目标表存在（幂等建表，不存在则创建）
    _ensure_table(spark, options)

    # truncate 清空当前数据，overwrite 写入新结果
    rfm_result.write \
        .format("jdbc") \
        .options(**options) \
        .option("dbtable", "ads_user_rfm") \
        .option("truncate", "true") \
        .mode("overwrite") \
        .save()

    count = rfm_result.count()
    print(f"RFM 8 分类完成，共 {count} 条记录已写入 ads_user_rfm（版本 {data_version}）")


# ──────────────────────────── 建表 ────────────────────────────

def _ensure_table(spark: SparkSession, options: dict[str, str]) -> None:
    """在 MySQL 中幂等创建 ads_user_rfm 表，已存在则跳过。"""
    ddl = """
        CREATE TABLE IF NOT EXISTS ads_user_rfm (
            user_id          BIGINT UNSIGNED NOT NULL COMMENT '电商用户主键',
            r_value          INT             NULL     COMMENT '最近消费距统计日天数',
            f_value          BIGINT          NULL     COMMENT '累计有效订单数',
            m_value          DECIMAL(18,2)   NULL     COMMENT '累计实付金额',
            r_score          TINYINT UNSIGNED NULL    COMMENT 'R最近消费得分(1-5)',
            f_score          TINYINT UNSIGNED NULL    COMMENT 'F消费频次得分(1-5)',
            m_score          TINYINT UNSIGNED NULL    COMMENT 'M消费金额得分(1-5)',
            rfm_group        VARCHAR(20)     NOT NULL COMMENT 'RFM分组编码',
            rfm_group_name   VARCHAR(20)     NOT NULL COMMENT 'RFM分组中文名称',
            data_version     VARCHAR(32)     NOT NULL COMMENT '分析数据版本',
            calculated_at    DATETIME(3)     NOT NULL COMMENT '计算时间',
            PRIMARY KEY (user_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RFM 8分类用户价值分析结果表'
    """
    spark.read.format("jdbc").options(**options) \
        .option("query", "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'ads_user_rfm'") \
        .load()

    # 用 JDBC statement 执行建表
    import jaydebeapi  # noqa: F811 — 仅在集群环境可用
    try:
        import java.sql
        props = java.util.Properties()
        props.setProperty("user", options["user"])
        props.setProperty("password", options["password"])
        conn = java.sql.DriverManager.getConnection(options["url"], props)
        stmt = conn.createStatement()
        stmt.executeUpdate(ddl)
        stmt.close()
        conn.close()
        print("ads_user_rfm 表已就绪")
    except Exception:
        # 如果 JDBC 直连不可用（非集群环境），表应已由 mysql-schema.sql 创建
        print("ads_user_rfm 表需由 mysql-schema.sql 提前创建（见 docs/sql/mysql-schema.sql）")


# ──────────────────────────── 入口 ────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Spark SQL RFM 8 分类用户价值分析"
    )
    parser.add_argument(
        "--stat-date",
        default=datetime.now().strftime("%Y-%m-%d"),
        help="统计截止日期，格式 YYYY-MM-DD",
    )
    parser.add_argument(
        "--data-version",
        default=datetime.now().strftime("%Y%m%d%H%M%S"),
        help="分析批次版本号",
    )
    parser.add_argument(
        "--jdbc-url",
        default="jdbc:mysql://localhost:3306/ecommerce_user_profile?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai",
        help="MySQL JDBC 连接地址",
    )
    parser.add_argument(
        "--mysql-user",
        default="root",
        help="MySQL 用户名",
    )
    args = parser.parse_args()

    spark = SparkSession.builder \
        .appName("RFM_8Segment_SQL") \
        .enableHiveSupport() \
        .config("spark.sql.adaptive.enabled", "true") \
        .config("spark.sql.adaptive.coalescePartitions.enabled", "true") \
        .getOrCreate()

    try:
        run(spark, args.stat_date, args.data_version, args.jdbc_url, args.mysql_user)
    finally:
        spark.stop()


if __name__ == "__main__":
    main()
