-- Hive ODS 层：按原始 CSV 结构保存模拟业务数据。
-- 运行前将 CSV 上传到 ${hivevar:raw_base_path} 下对应子目录。

CREATE DATABASE IF NOT EXISTS ecommerce_profile_ods
COMMENT '电商用户画像 ODS 原始数据层';

CREATE EXTERNAL TABLE IF NOT EXISTS ecommerce_profile_ods.ods_product_category (
  id BIGINT COMMENT '分类主键',
  parent_id BIGINT COMMENT '父分类主键',
  category_name STRING COMMENT '分类名称',
  category_level INT COMMENT '分类层级',
  status INT COMMENT '状态'
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
STORED AS TEXTFILE
LOCATION '${hivevar:raw_base_path}/product_category'
TBLPROPERTIES ('skip.header.line.count'='1');

CREATE EXTERNAL TABLE IF NOT EXISTS ecommerce_profile_ods.ods_product (
  id BIGINT COMMENT '商品主键',
  product_code STRING COMMENT '商品编码',
  category_id BIGINT COMMENT '分类主键',
  product_name STRING COMMENT '商品名称',
  brand_name STRING COMMENT '品牌名称',
  unit_price DECIMAL(18,2) COMMENT '商品单价',
  status INT COMMENT '状态'
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
STORED AS TEXTFILE
LOCATION '${hivevar:raw_base_path}/product'
TBLPROPERTIES ('skip.header.line.count'='1');

CREATE EXTERNAL TABLE IF NOT EXISTS ecommerce_profile_ods.ods_ecommerce_user (
  id BIGINT COMMENT '电商用户主键',
  user_code STRING COMMENT '脱敏用户编码',
  gender STRING COMMENT '性别',
  age INT COMMENT '年龄',
  province STRING COMMENT '省份',
  city STRING COMMENT '城市',
  register_channel STRING COMMENT '注册渠道',
  membership_level STRING COMMENT '会员等级',
  registered_at TIMESTAMP COMMENT '注册时间',
  status INT COMMENT '状态'
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
STORED AS TEXTFILE
LOCATION '${hivevar:raw_base_path}/ecommerce_user'
TBLPROPERTIES ('skip.header.line.count'='1');

CREATE EXTERNAL TABLE IF NOT EXISTS ecommerce_profile_ods.ods_user_browse_behavior (
  id BIGINT COMMENT '行为主键',
  user_id BIGINT COMMENT '用户主键',
  product_id BIGINT COMMENT '商品主键',
  behavior_type STRING COMMENT '行为类型',
  session_id STRING COMMENT '会话编码',
  device_type STRING COMMENT '设备类型',
  channel STRING COMMENT '访问渠道',
  behavior_at TIMESTAMP COMMENT '行为时间'
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
STORED AS TEXTFILE
LOCATION '${hivevar:raw_base_path}/user_browse_behavior'
TBLPROPERTIES ('skip.header.line.count'='1');

CREATE EXTERNAL TABLE IF NOT EXISTS ecommerce_profile_ods.ods_user_login_behavior (
  id BIGINT COMMENT '登录行为主键',
  user_id BIGINT COMMENT '用户主键',
  session_id STRING COMMENT '会话编码',
  device_type STRING COMMENT '设备类型',
  login_channel STRING COMMENT '登录渠道',
  login_at TIMESTAMP COMMENT '登录时间',
  logout_at TIMESTAMP COMMENT '退出时间',
  duration_seconds INT COMMENT '在线秒数'
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
STORED AS TEXTFILE
LOCATION '${hivevar:raw_base_path}/user_login_behavior'
TBLPROPERTIES ('skip.header.line.count'='1');

CREATE EXTERNAL TABLE IF NOT EXISTS ecommerce_profile_ods.ods_sales_order (
  id BIGINT COMMENT '订单主键',
  order_no STRING COMMENT '订单编号',
  user_id BIGINT COMMENT '用户主键',
  order_status STRING COMMENT '订单状态',
  total_amount DECIMAL(18,2) COMMENT '商品总金额',
  discount_amount DECIMAL(18,2) COMMENT '优惠金额',
  payment_amount DECIMAL(18,2) COMMENT '实付金额',
  payment_method STRING COMMENT '支付方式',
  ordered_at TIMESTAMP COMMENT '下单时间',
  paid_at TIMESTAMP COMMENT '支付时间',
  completed_at TIMESTAMP COMMENT '完成时间'
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
STORED AS TEXTFILE
LOCATION '${hivevar:raw_base_path}/sales_order'
TBLPROPERTIES ('skip.header.line.count'='1');

CREATE EXTERNAL TABLE IF NOT EXISTS ecommerce_profile_ods.ods_sales_order_item (
  id BIGINT COMMENT '订单明细主键',
  order_id BIGINT COMMENT '订单主键',
  product_id BIGINT COMMENT '商品主键',
  product_name_snapshot STRING COMMENT '商品名称快照',
  unit_price DECIMAL(18,2) COMMENT '成交单价',
  quantity INT COMMENT '购买数量',
  item_amount DECIMAL(18,2) COMMENT '明细金额'
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
STORED AS TEXTFILE
LOCATION '${hivevar:raw_base_path}/sales_order_item'
TBLPROPERTIES ('skip.header.line.count'='1');
