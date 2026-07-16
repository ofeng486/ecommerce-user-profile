-- Hive DWD 层：完成去重、有效性校验、字段标准化和业务关联。
CREATE DATABASE IF NOT EXISTS ecommerce_profile_dwd
COMMENT '电商用户画像 DWD 明细数据层';

CREATE TABLE IF NOT EXISTS ecommerce_profile_dwd.dwd_ecommerce_user (
  user_id BIGINT,
  user_code STRING,
  gender STRING,
  age INT,
  province STRING,
  city STRING,
  register_channel STRING,
  membership_level STRING,
  registered_at TIMESTAMP,
  status INT
)
STORED AS PARQUET;

INSERT OVERWRITE TABLE ecommerce_profile_dwd.dwd_ecommerce_user
SELECT id, user_code, gender, age, province, city, register_channel,
       membership_level, registered_at, status
FROM (
  SELECT u.*, ROW_NUMBER() OVER (PARTITION BY id ORDER BY registered_at DESC) AS rn
  FROM ecommerce_profile_ods.ods_ecommerce_user u
  WHERE id IS NOT NULL
    AND user_code IS NOT NULL
    AND age BETWEEN 1 AND 120
    AND registered_at IS NOT NULL
    AND status IN (0, 1)
) t
WHERE rn = 1;

CREATE TABLE IF NOT EXISTS ecommerce_profile_dwd.dwd_product (
  product_id BIGINT,
  product_code STRING,
  category_id BIGINT,
  category_name STRING,
  product_name STRING,
  brand_name STRING,
  unit_price DECIMAL(18,2),
  status INT
)
STORED AS PARQUET;

INSERT OVERWRITE TABLE ecommerce_profile_dwd.dwd_product
SELECT p.id, p.product_code, p.category_id, c.category_name,
       p.product_name, p.brand_name, p.unit_price, p.status
FROM (
  SELECT p.*, ROW_NUMBER() OVER (PARTITION BY id ORDER BY id) AS rn
  FROM ecommerce_profile_ods.ods_product p
  WHERE id IS NOT NULL AND category_id IS NOT NULL AND unit_price >= 0
) p
LEFT JOIN ecommerce_profile_ods.ods_product_category c ON p.category_id = c.id
WHERE p.rn = 1;

CREATE TABLE IF NOT EXISTS ecommerce_profile_dwd.dwd_user_behavior (
  behavior_id BIGINT,
  user_id BIGINT,
  product_id BIGINT,
  category_id BIGINT,
  behavior_type STRING,
  session_id STRING,
  device_type STRING,
  channel STRING,
  behavior_at TIMESTAMP,
  behavior_date DATE
)
STORED AS PARQUET;

INSERT OVERWRITE TABLE ecommerce_profile_dwd.dwd_user_behavior
SELECT b.id, b.user_id, b.product_id, p.category_id, b.behavior_type,
       b.session_id, b.device_type, b.channel, b.behavior_at, TO_DATE(b.behavior_at)
FROM (
  SELECT b.*, ROW_NUMBER() OVER (PARTITION BY id ORDER BY behavior_at DESC) AS rn
  FROM ecommerce_profile_ods.ods_user_browse_behavior b
  WHERE id IS NOT NULL
    AND user_id IS NOT NULL
    AND product_id IS NOT NULL
    AND behavior_type IN ('View', 'Click', 'Favorite', 'Cart')
    AND behavior_at IS NOT NULL
) b
JOIN ecommerce_profile_dwd.dwd_ecommerce_user u ON b.user_id = u.user_id
JOIN ecommerce_profile_dwd.dwd_product p ON b.product_id = p.product_id
WHERE b.rn = 1 AND b.behavior_at >= u.registered_at;

CREATE TABLE IF NOT EXISTS ecommerce_profile_dwd.dwd_user_login (
  login_id BIGINT,
  user_id BIGINT,
  session_id STRING,
  device_type STRING,
  login_channel STRING,
  login_at TIMESTAMP,
  logout_at TIMESTAMP,
  duration_seconds INT,
  login_date DATE
)
STORED AS PARQUET;

INSERT OVERWRITE TABLE ecommerce_profile_dwd.dwd_user_login
SELECT l.id, l.user_id, l.session_id, l.device_type, l.login_channel,
       l.login_at, l.logout_at, l.duration_seconds, TO_DATE(l.login_at)
FROM (
  SELECT l.*, ROW_NUMBER() OVER (PARTITION BY id ORDER BY login_at DESC) AS rn
  FROM ecommerce_profile_ods.ods_user_login_behavior l
  WHERE id IS NOT NULL
    AND user_id IS NOT NULL
    AND login_at IS NOT NULL
    AND (logout_at IS NULL OR logout_at >= login_at)
    AND (duration_seconds IS NULL OR duration_seconds >= 0)
) l
JOIN ecommerce_profile_dwd.dwd_ecommerce_user u ON l.user_id = u.user_id
WHERE l.rn = 1 AND l.login_at >= u.registered_at;

CREATE TABLE IF NOT EXISTS ecommerce_profile_dwd.dwd_valid_order (
  order_id BIGINT,
  order_no STRING,
  user_id BIGINT,
  order_status STRING,
  total_amount DECIMAL(18,2),
  discount_amount DECIMAL(18,2),
  payment_amount DECIMAL(18,2),
  payment_method STRING,
  ordered_at TIMESTAMP,
  paid_at TIMESTAMP,
  completed_at TIMESTAMP,
  order_date DATE
)
STORED AS PARQUET;

INSERT OVERWRITE TABLE ecommerce_profile_dwd.dwd_valid_order
SELECT o.id, o.order_no, o.user_id, o.order_status, o.total_amount,
       o.discount_amount, o.payment_amount, o.payment_method,
       o.ordered_at, o.paid_at, o.completed_at, TO_DATE(o.ordered_at)
FROM (
  SELECT o.*, ROW_NUMBER() OVER (PARTITION BY id ORDER BY ordered_at DESC) AS rn
  FROM ecommerce_profile_ods.ods_sales_order o
  WHERE id IS NOT NULL
    AND user_id IS NOT NULL
    AND order_status IN ('Paid', 'Shipped', 'Completed')
    AND total_amount >= 0
    AND discount_amount >= 0
    AND payment_amount = total_amount - discount_amount
    AND ordered_at IS NOT NULL
    AND paid_at IS NOT NULL
    AND paid_at >= ordered_at
    AND (completed_at IS NULL OR completed_at >= paid_at)
) o
JOIN ecommerce_profile_dwd.dwd_ecommerce_user u ON o.user_id = u.user_id
WHERE o.rn = 1 AND o.ordered_at >= u.registered_at;

CREATE TABLE IF NOT EXISTS ecommerce_profile_dwd.dwd_valid_order_item (
  item_id BIGINT,
  order_id BIGINT,
  user_id BIGINT,
  product_id BIGINT,
  category_id BIGINT,
  product_name_snapshot STRING,
  unit_price DECIMAL(18,2),
  quantity INT,
  item_amount DECIMAL(18,2),
  ordered_at TIMESTAMP
)
STORED AS PARQUET;

INSERT OVERWRITE TABLE ecommerce_profile_dwd.dwd_valid_order_item
SELECT i.id, i.order_id, o.user_id, i.product_id, p.category_id,
       i.product_name_snapshot, i.unit_price, i.quantity, i.item_amount, o.ordered_at
FROM (
  SELECT i.*, ROW_NUMBER() OVER (PARTITION BY id ORDER BY id) AS rn
  FROM ecommerce_profile_ods.ods_sales_order_item i
  WHERE id IS NOT NULL
    AND order_id IS NOT NULL
    AND product_id IS NOT NULL
    AND unit_price >= 0
    AND quantity > 0
    AND item_amount = unit_price * quantity
) i
JOIN ecommerce_profile_dwd.dwd_valid_order o ON i.order_id = o.order_id
JOIN ecommerce_profile_dwd.dwd_product p ON i.product_id = p.product_id
WHERE i.rn = 1;
