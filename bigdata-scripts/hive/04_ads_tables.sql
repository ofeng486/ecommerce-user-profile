-- Hive ADS 层：面向后端同步和 ECharts 可视化的画像结果。
-- data_version 建议传入 yyyyMMddHHmmss，stat_date 格式 yyyy-MM-dd。
CREATE DATABASE IF NOT EXISTS ecommerce_profile_ads
COMMENT '电商用户画像 ADS 应用数据层';

CREATE TABLE IF NOT EXISTS ecommerce_profile_ads.ads_user_profile_summary (
  user_id BIGINT,
  total_order_count BIGINT,
  total_payment_amount DECIMAL(18,2),
  average_order_amount DECIMAL(18,2),
  browse_count_30d BIGINT,
  login_count_30d BIGINT,
  last_active_at TIMESTAMP,
  favorite_category_id BIGINT,
  data_version STRING,
  calculated_at TIMESTAMP
)
STORED AS PARQUET;

INSERT OVERWRITE TABLE ecommerce_profile_ads.ads_user_profile_summary
SELECT user_id, total_order_count, total_payment_amount, average_order_amount,
       browse_count_30d, login_count_30d, last_active_at, favorite_category_id,
       '${hivevar:data_version}', CURRENT_TIMESTAMP
FROM ecommerce_profile_dws.dws_user_profile_metrics;

CREATE TABLE IF NOT EXISTS ecommerce_profile_ads.ads_user_value_segment (
  user_id BIGINT,
  r_score INT,
  f_score INT,
  m_score INT,
  segment_code STRING,
  segment_name STRING,
  segment_score DECIMAL(10,4),
  data_version STRING,
  calculated_at TIMESTAMP
)
STORED AS PARQUET;

WITH scored AS (
  SELECT user_id,
         CASE
           WHEN total_order_count = 0 THEN 1
           ELSE 6 - NTILE(5) OVER (ORDER BY recency_days ASC NULLS LAST)
         END AS r_score,
         NTILE(5) OVER (ORDER BY total_order_count ASC) AS f_score,
         NTILE(5) OVER (ORDER BY total_payment_amount ASC) AS m_score
  FROM ecommerce_profile_dws.dws_user_profile_metrics
),
segmented AS (
  SELECT user_id, r_score, f_score, m_score,
         CAST((r_score * 0.4 + f_score * 0.3 + m_score * 0.3) AS DECIMAL(10,4)) AS segment_score
  FROM scored
)
INSERT OVERWRITE TABLE ecommerce_profile_ads.ads_user_value_segment
SELECT user_id, r_score, f_score, m_score,
       CASE
         WHEN r_score >= 4 AND f_score >= 4 AND m_score >= 4 THEN 'HIGH_VALUE'
         WHEN r_score >= 4 AND (f_score >= 3 OR m_score >= 3) THEN 'POTENTIAL'
         WHEN r_score <= 2 AND (f_score >= 4 OR m_score >= 4) THEN 'AT_RISK'
         WHEN f_score <= 2 AND m_score <= 2 THEN 'LOW_VALUE'
         ELSE 'GENERAL'
       END AS segment_code,
       CASE
         WHEN r_score >= 4 AND f_score >= 4 AND m_score >= 4 THEN '高价值用户'
         WHEN r_score >= 4 AND (f_score >= 3 OR m_score >= 3) THEN '潜力用户'
         WHEN r_score <= 2 AND (f_score >= 4 OR m_score >= 4) THEN '流失风险用户'
         WHEN f_score <= 2 AND m_score <= 2 THEN '低价值用户'
         ELSE '一般用户'
       END AS segment_name,
       segment_score,
       '${hivevar:data_version}',
       CURRENT_TIMESTAMP
FROM segmented;

CREATE TABLE IF NOT EXISTS ecommerce_profile_ads.ads_user_tag (
  user_id BIGINT,
  tag_code STRING,
  tag_value STRING,
  tag_score DECIMAL(10,4),
  data_version STRING,
  calculated_at TIMESTAMP
)
STORED AS PARQUET;

INSERT OVERWRITE TABLE ecommerce_profile_ads.ads_user_tag
SELECT user_id, tag_code, tag_value, tag_score,
       '${hivevar:data_version}', CURRENT_TIMESTAMP
FROM (
  SELECT user_id,
         'ACTIVE_LEVEL' AS tag_code,
         CASE
           WHEN login_count_30d + browse_count_30d >= 50 THEN 'High'
           WHEN login_count_30d + browse_count_30d >= 15 THEN 'Medium'
           ELSE 'Low'
         END AS tag_value,
         CAST(login_count_30d + browse_count_30d AS DECIMAL(10,4)) AS tag_score
  FROM ecommerce_profile_dws.dws_user_profile_metrics

  UNION ALL

  SELECT user_id,
         'CONSUMPTION_LEVEL',
         CASE
           WHEN total_payment_amount >= 10000 THEN 'High'
           WHEN total_payment_amount >= 3000 THEN 'Medium'
           ELSE 'Low'
         END,
         CAST(total_payment_amount AS DECIMAL(10,4))
  FROM ecommerce_profile_dws.dws_user_profile_metrics

  UNION ALL

  SELECT user_id,
         'FAVORITE_CATEGORY',
         COALESCE(CAST(favorite_category_id AS STRING), 'Unknown'),
         CAST(1 AS DECIMAL(10,4))
  FROM ecommerce_profile_dws.dws_user_profile_metrics

  UNION ALL

  SELECT user_id,
         'RFM_SEGMENT',
         segment_code,
         segment_score
  FROM ecommerce_profile_ads.ads_user_value_segment
) tags;

CREATE TABLE IF NOT EXISTS ecommerce_profile_ads.ads_tag_distribution (
  tag_code STRING,
  tag_value STRING,
  user_count BIGINT,
  user_ratio DECIMAL(10,4),
  data_version STRING,
  calculated_at TIMESTAMP
)
STORED AS PARQUET;

WITH tag_counts AS (
  SELECT tag_code, tag_value, COUNT(DISTINCT user_id) AS user_count
  FROM ecommerce_profile_ads.ads_user_tag
  GROUP BY tag_code, tag_value
),
tag_totals AS (
  SELECT tag_code, SUM(user_count) AS total_count
  FROM tag_counts
  GROUP BY tag_code
)
INSERT OVERWRITE TABLE ecommerce_profile_ads.ads_tag_distribution
SELECT c.tag_code, c.tag_value, c.user_count,
       CAST(c.user_count / t.total_count AS DECIMAL(10,4)) AS user_ratio,
       '${hivevar:data_version}', CURRENT_TIMESTAMP
FROM tag_counts c
JOIN tag_totals t ON c.tag_code = t.tag_code;

CREATE TABLE IF NOT EXISTS ecommerce_profile_ads.ads_segment_distribution (
  segment_code STRING,
  segment_name STRING,
  user_count BIGINT,
  user_ratio DECIMAL(10,4),
  data_version STRING,
  calculated_at TIMESTAMP
)
STORED AS PARQUET;

WITH segment_counts AS (
  SELECT segment_code, segment_name, COUNT(1) AS user_count
  FROM ecommerce_profile_ads.ads_user_value_segment
  GROUP BY segment_code, segment_name
),
total AS (
  SELECT SUM(user_count) AS total_count FROM segment_counts
)
INSERT OVERWRITE TABLE ecommerce_profile_ads.ads_segment_distribution
SELECT s.segment_code, s.segment_name, s.user_count,
       CAST(s.user_count / t.total_count AS DECIMAL(10,4)),
       '${hivevar:data_version}', CURRENT_TIMESTAMP
FROM segment_counts s CROSS JOIN total t;
