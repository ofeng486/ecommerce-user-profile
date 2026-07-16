-- Hive DWS 层：以 ${hivevar:stat_date} 为统计日按用户汇总画像指标。
-- stat_date 格式：yyyy-MM-dd，例如 2026-01-01。
CREATE DATABASE IF NOT EXISTS ecommerce_profile_dws
COMMENT '电商用户画像 DWS 汇总数据层';

CREATE TABLE IF NOT EXISTS ecommerce_profile_dws.dws_user_profile_metrics (
  user_id BIGINT,
  user_code STRING,
  gender STRING,
  age INT,
  province STRING,
  city STRING,
  register_channel STRING,
  membership_level STRING,
  total_order_count BIGINT,
  total_payment_amount DECIMAL(18,2),
  average_order_amount DECIMAL(18,2),
  last_order_at TIMESTAMP,
  recency_days INT,
  browse_count_30d BIGINT,
  click_count_30d BIGINT,
  favorite_count_30d BIGINT,
  cart_count_30d BIGINT,
  login_count_30d BIGINT,
  login_duration_seconds_30d BIGINT,
  last_behavior_at TIMESTAMP,
  last_login_at TIMESTAMP,
  last_active_at TIMESTAMP,
  favorite_category_id BIGINT,
  stat_date DATE
)
STORED AS PARQUET;

WITH order_metrics AS (
  SELECT user_id,
         COUNT(DISTINCT order_id) AS total_order_count,
         CAST(SUM(payment_amount) AS DECIMAL(18,2)) AS total_payment_amount,
         CAST(AVG(payment_amount) AS DECIMAL(18,2)) AS average_order_amount,
         MAX(ordered_at) AS last_order_at,
         DATEDIFF(CAST('${hivevar:stat_date}' AS DATE), TO_DATE(MAX(ordered_at))) AS recency_days
  FROM ecommerce_profile_dwd.dwd_valid_order
  WHERE order_date <= CAST('${hivevar:stat_date}' AS DATE)
  GROUP BY user_id
),
behavior_metrics AS (
  SELECT user_id,
         SUM(CASE WHEN behavior_type = 'View' THEN 1 ELSE 0 END) AS browse_count_30d,
         SUM(CASE WHEN behavior_type = 'Click' THEN 1 ELSE 0 END) AS click_count_30d,
         SUM(CASE WHEN behavior_type = 'Favorite' THEN 1 ELSE 0 END) AS favorite_count_30d,
         SUM(CASE WHEN behavior_type = 'Cart' THEN 1 ELSE 0 END) AS cart_count_30d,
         MAX(behavior_at) AS last_behavior_at
  FROM ecommerce_profile_dwd.dwd_user_behavior
  WHERE behavior_date BETWEEN DATE_SUB(CAST('${hivevar:stat_date}' AS DATE), 29)
                          AND CAST('${hivevar:stat_date}' AS DATE)
  GROUP BY user_id
),
login_metrics AS (
  SELECT user_id,
         COUNT(1) AS login_count_30d,
         SUM(COALESCE(duration_seconds, 0)) AS login_duration_seconds_30d,
         MAX(login_at) AS last_login_at
  FROM ecommerce_profile_dwd.dwd_user_login
  WHERE login_date BETWEEN DATE_SUB(CAST('${hivevar:stat_date}' AS DATE), 29)
                       AND CAST('${hivevar:stat_date}' AS DATE)
  GROUP BY user_id
),
category_scores AS (
  SELECT user_id, category_id,
         SUM(CASE behavior_type
               WHEN 'View' THEN 1
               WHEN 'Click' THEN 2
               WHEN 'Favorite' THEN 4
               WHEN 'Cart' THEN 5
               ELSE 0
             END) AS preference_score
  FROM ecommerce_profile_dwd.dwd_user_behavior
  WHERE behavior_date <= CAST('${hivevar:stat_date}' AS DATE)
  GROUP BY user_id, category_id
),
favorite_category AS (
  SELECT user_id, category_id
  FROM (
    SELECT user_id, category_id,
           ROW_NUMBER() OVER (
             PARTITION BY user_id
             ORDER BY preference_score DESC, category_id ASC
           ) AS rn
    FROM category_scores
  ) t
  WHERE rn = 1
)
INSERT OVERWRITE TABLE ecommerce_profile_dws.dws_user_profile_metrics
SELECT u.user_id,
       u.user_code,
       u.gender,
       u.age,
       u.province,
       u.city,
       u.register_channel,
       u.membership_level,
       COALESCE(o.total_order_count, 0),
       COALESCE(o.total_payment_amount, CAST(0 AS DECIMAL(18,2))),
       COALESCE(o.average_order_amount, CAST(0 AS DECIMAL(18,2))),
       o.last_order_at,
       o.recency_days,
       COALESCE(b.browse_count_30d, 0),
       COALESCE(b.click_count_30d, 0),
       COALESCE(b.favorite_count_30d, 0),
       COALESCE(b.cart_count_30d, 0),
       COALESCE(l.login_count_30d, 0),
       COALESCE(l.login_duration_seconds_30d, 0),
       b.last_behavior_at,
       l.last_login_at,
       CASE
         WHEN b.last_behavior_at IS NULL THEN l.last_login_at
         WHEN l.last_login_at IS NULL THEN b.last_behavior_at
         WHEN b.last_behavior_at >= l.last_login_at THEN b.last_behavior_at
         ELSE l.last_login_at
       END AS last_active_at,
       f.category_id AS favorite_category_id,
       CAST('${hivevar:stat_date}' AS DATE)
FROM ecommerce_profile_dwd.dwd_ecommerce_user u
LEFT JOIN order_metrics o ON u.user_id = o.user_id
LEFT JOIN behavior_metrics b ON u.user_id = b.user_id
LEFT JOIN login_metrics l ON u.user_id = l.user_id
LEFT JOIN favorite_category f ON u.user_id = f.user_id
WHERE u.status = 1;
