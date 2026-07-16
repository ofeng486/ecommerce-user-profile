-- 基于 Spark 的电商平台用户画像分析系统 MySQL 初始化脚本
-- 目标：使用通用现代 MySQL 语法，字符集 utf8mb4，存储引擎 InnoDB。

CREATE DATABASE IF NOT EXISTS ecommerce_user_profile
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE ecommerce_user_profile;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '系统用户主键',
    username VARCHAR(50) NOT NULL COMMENT '登录用户名',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要',
    display_name VARCHAR(50) NOT NULL COMMENT '显示名称',
    role ENUM('User', 'Admin') NOT NULL DEFAULT 'User' COMMENT '系统角色',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    last_login_at DATETIME(3) NULL COMMENT '最后登录时间',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    CONSTRAINT chk_sys_user_status CHECK (status IN (0, 1))
) ENGINE=InnoDB COMMENT='系统登录用户表';

CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志主键',
    sys_user_id BIGINT UNSIGNED NULL COMMENT '系统用户主键，登录失败时可为空',
    username VARCHAR(50) NOT NULL COMMENT '本次登录用户名',
    login_ip VARCHAR(45) NULL COMMENT '登录 IP，兼容 IPv6',
    user_agent VARCHAR(500) NULL COMMENT '客户端 User-Agent',
    login_result TINYINT UNSIGNED NOT NULL COMMENT '登录结果：0失败，1成功',
    failure_reason VARCHAR(200) NULL COMMENT '失败原因',
    login_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '登录时间',
    PRIMARY KEY (id),
    KEY idx_sys_login_user_time (sys_user_id, login_at),
    KEY idx_sys_login_username_time (username, login_at),
    CONSTRAINT fk_sys_login_user FOREIGN KEY (sys_user_id) REFERENCES sys_user (id) ON DELETE SET NULL,
    CONSTRAINT chk_sys_login_result CHECK (login_result IN (0, 1))
) ENGINE=InnoDB COMMENT='系统登录审计日志表';

CREATE TABLE IF NOT EXISTS ecommerce_user (
    id BIGINT UNSIGNED NOT NULL COMMENT '电商用户主键',
    user_code VARCHAR(32) NOT NULL COMMENT '脱敏业务用户编码',
    gender ENUM('Unknown', 'Male', 'Female') NOT NULL DEFAULT 'Unknown' COMMENT '性别',
    age SMALLINT UNSIGNED NULL COMMENT '年龄',
    province VARCHAR(50) NULL COMMENT '省级地区',
    city VARCHAR(50) NULL COMMENT '城市',
    register_channel VARCHAR(30) NOT NULL COMMENT '注册渠道',
    membership_level VARCHAR(20) NOT NULL DEFAULT 'Normal' COMMENT '会员等级',
    registered_at DATETIME(3) NOT NULL COMMENT '注册时间',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0停用，1正常',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecommerce_user_code (user_code),
    KEY idx_ecommerce_user_region (province, city),
    KEY idx_ecommerce_user_registered (registered_at),
    CONSTRAINT chk_ecommerce_user_age CHECK (age IS NULL OR age BETWEEN 1 AND 120),
    CONSTRAINT chk_ecommerce_user_status CHECK (status IN (0, 1))
) ENGINE=InnoDB COMMENT='电商画像分析用户表';

CREATE TABLE IF NOT EXISTS product_category (
    id BIGINT UNSIGNED NOT NULL COMMENT '分类主键',
    parent_id BIGINT UNSIGNED NULL COMMENT '父分类主键',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    category_level TINYINT UNSIGNED NOT NULL COMMENT '分类层级',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_category_parent (parent_id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES product_category (id) ON DELETE SET NULL,
    CONSTRAINT chk_category_status CHECK (status IN (0, 1))
) ENGINE=InnoDB COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS product (
    id BIGINT UNSIGNED NOT NULL COMMENT '商品主键',
    product_code VARCHAR(32) NOT NULL COMMENT '商品编码',
    category_id BIGINT UNSIGNED NOT NULL COMMENT '商品分类主键',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    brand_name VARCHAR(100) NULL COMMENT '品牌名称',
    unit_price DECIMAL(18,2) NOT NULL COMMENT '商品单价',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0下架，1上架',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_product_category_status (category_id, status),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category (id),
    CONSTRAINT chk_product_price CHECK (unit_price >= 0),
    CONSTRAINT chk_product_status CHECK (status IN (0, 1))
) ENGINE=InnoDB COMMENT='商品信息表';

CREATE TABLE IF NOT EXISTS user_browse_behavior (
    id BIGINT UNSIGNED NOT NULL COMMENT '行为主键',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '电商用户主键',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '商品主键',
    behavior_type ENUM('View', 'Click', 'Favorite', 'Cart') NOT NULL COMMENT '行为类型',
    session_id VARCHAR(64) NOT NULL COMMENT '会话编码',
    device_type VARCHAR(20) NULL COMMENT '设备类型',
    channel VARCHAR(30) NULL COMMENT '访问渠道',
    behavior_at DATETIME(3) NOT NULL COMMENT '行为发生时间',
    PRIMARY KEY (id),
    KEY idx_browse_user_time (user_id, behavior_at),
    KEY idx_browse_product_time (product_id, behavior_at),
    KEY idx_browse_type_time (behavior_type, behavior_at)
) ENGINE=InnoDB COMMENT='用户浏览与互动行为表';

CREATE TABLE IF NOT EXISTS user_login_behavior (
    id BIGINT UNSIGNED NOT NULL COMMENT '登录行为主键',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '电商用户主键',
    session_id VARCHAR(64) NOT NULL COMMENT '会话编码',
    device_type VARCHAR(20) NULL COMMENT '设备类型',
    login_channel VARCHAR(30) NULL COMMENT '登录渠道',
    login_at DATETIME(3) NOT NULL COMMENT '登录时间',
    logout_at DATETIME(3) NULL COMMENT '退出时间',
    duration_seconds INT UNSIGNED NULL COMMENT '在线时长（秒）',
    PRIMARY KEY (id),
    KEY idx_login_behavior_user_time (user_id, login_at),
    CONSTRAINT chk_login_time CHECK (logout_at IS NULL OR logout_at >= login_at)
) ENGINE=InnoDB COMMENT='电商用户登录行为表';

CREATE TABLE IF NOT EXISTS sales_order (
    id BIGINT UNSIGNED NOT NULL COMMENT '订单主键',
    order_no VARCHAR(40) NOT NULL COMMENT '订单编号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '电商用户主键',
    order_status ENUM('Pending', 'Paid', 'Shipped', 'Completed', 'Cancelled', 'Refunded') NOT NULL COMMENT '订单状态',
    total_amount DECIMAL(18,2) NOT NULL COMMENT '商品总金额',
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    payment_amount DECIMAL(18,2) NOT NULL COMMENT '实付金额',
    payment_method VARCHAR(30) NULL COMMENT '支付方式',
    ordered_at DATETIME(3) NOT NULL COMMENT '下单时间',
    paid_at DATETIME(3) NULL COMMENT '支付时间',
    completed_at DATETIME(3) NULL COMMENT '完成时间',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sales_order_no (order_no),
    KEY idx_sales_order_user_time (user_id, ordered_at),
    KEY idx_sales_order_status_time (order_status, ordered_at),
    CONSTRAINT chk_order_amount CHECK (total_amount >= 0 AND discount_amount >= 0 AND payment_amount >= 0),
    CONSTRAINT chk_order_payment CHECK (payment_amount = total_amount - discount_amount),
    CONSTRAINT chk_order_paid_time CHECK (paid_at IS NULL OR paid_at >= ordered_at),
    CONSTRAINT chk_order_completed_time CHECK (completed_at IS NULL OR completed_at >= ordered_at)
) ENGINE=InnoDB COMMENT='销售订单表';

CREATE TABLE IF NOT EXISTS sales_order_item (
    id BIGINT UNSIGNED NOT NULL COMMENT '订单明细主键',
    order_id BIGINT UNSIGNED NOT NULL COMMENT '订单主键',
    product_id BIGINT UNSIGNED NOT NULL COMMENT '商品主键',
    product_name_snapshot VARCHAR(200) NOT NULL COMMENT '下单时商品名称快照',
    unit_price DECIMAL(18,2) NOT NULL COMMENT '成交单价',
    quantity INT UNSIGNED NOT NULL COMMENT '购买数量',
    item_amount DECIMAL(18,2) NOT NULL COMMENT '明细金额',
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id),
    KEY idx_order_item_product (product_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES sales_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_item_amount CHECK (unit_price >= 0 AND item_amount = unit_price * quantity)
) ENGINE=InnoDB COMMENT='销售订单明细表';

CREATE TABLE IF NOT EXISTS profile_tag_definition (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签定义主键',
    tag_code VARCHAR(50) NOT NULL COMMENT '标签编码',
    tag_name VARCHAR(100) NOT NULL COMMENT '标签名称',
    tag_category VARCHAR(50) NOT NULL COMMENT '标签分类',
    value_type ENUM('String', 'Number', 'Boolean', 'Date') NOT NULL COMMENT '标签值类型',
    calculation_rule TEXT NULL COMMENT '标签计算规则说明',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    created_by BIGINT UNSIGNED NULL COMMENT '创建系统用户主键',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_profile_tag_code (tag_code),
    KEY idx_profile_tag_category (tag_category, status),
    CONSTRAINT fk_profile_tag_creator FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL,
    CONSTRAINT chk_profile_tag_status CHECK (status IN (0, 1))
) ENGINE=InnoDB COMMENT='用户画像标签定义表';

CREATE TABLE IF NOT EXISTS user_profile_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户标签主键',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '电商用户主键',
    tag_id BIGINT UNSIGNED NOT NULL COMMENT '标签定义主键',
    tag_value VARCHAR(500) NOT NULL COMMENT '统一序列化后的标签值',
    score DECIMAL(10,4) NULL COMMENT '标签置信度或得分',
    data_version VARCHAR(32) NOT NULL COMMENT '分析数据版本',
    calculated_at DATETIME(3) NOT NULL COMMENT '计算时间',
    expires_at DATETIME(3) NULL COMMENT '标签失效时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_profile_tag_version (user_id, tag_id, data_version),
    KEY idx_user_profile_tag_tag_value (tag_id, tag_value(100)),
    KEY idx_user_profile_tag_calculated (calculated_at),
    CONSTRAINT fk_user_profile_tag_user FOREIGN KEY (user_id) REFERENCES ecommerce_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_profile_tag_definition FOREIGN KEY (tag_id) REFERENCES profile_tag_definition (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_profile_tag_score CHECK (score IS NULL OR score >= 0),
    CONSTRAINT chk_user_profile_tag_expiry CHECK (expires_at IS NULL OR expires_at >= calculated_at)
) ENGINE=InnoDB COMMENT='用户画像标签结果表';

CREATE TABLE IF NOT EXISTS user_profile_summary (
    user_id BIGINT UNSIGNED NOT NULL COMMENT '电商用户主键',
    total_order_count BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计有效订单数',
    total_payment_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计实付金额',
    average_order_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '平均客单价',
    browse_count_30d BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '近30日浏览次数',
    login_count_30d BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '近30日登录次数',
    last_active_at DATETIME(3) NULL COMMENT '最近活跃时间',
    favorite_category_id BIGINT UNSIGNED NULL COMMENT '偏好商品分类主键',
    data_version VARCHAR(32) NOT NULL COMMENT '分析数据版本',
    calculated_at DATETIME(3) NOT NULL COMMENT '计算时间',
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (user_id),
    KEY idx_profile_summary_value (total_payment_amount, total_order_count),
    KEY idx_profile_summary_active (last_active_at),
    CONSTRAINT fk_profile_summary_user FOREIGN KEY (user_id) REFERENCES ecommerce_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_profile_summary_category FOREIGN KEY (favorite_category_id) REFERENCES product_category (id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='用户画像汇总结果表';

CREATE TABLE IF NOT EXISTS user_segment (
    user_id BIGINT UNSIGNED NOT NULL COMMENT '电商用户主键',
    segment_code VARCHAR(50) NOT NULL COMMENT '分层编码',
    segment_name VARCHAR(100) NOT NULL COMMENT '分层名称',
    r_score TINYINT UNSIGNED NULL COMMENT 'RFM最近消费得分',
    f_score TINYINT UNSIGNED NULL COMMENT 'RFM消费频次得分',
    m_score TINYINT UNSIGNED NULL COMMENT 'RFM消费金额得分',
    segment_score DECIMAL(10,4) NULL COMMENT '综合分层得分',
    data_version VARCHAR(32) NOT NULL COMMENT '分析数据版本',
    calculated_at DATETIME(3) NOT NULL COMMENT '计算时间',
    PRIMARY KEY (user_id),
    KEY idx_user_segment_code_score (segment_code, segment_score),
    CONSTRAINT fk_user_segment_user FOREIGN KEY (user_id) REFERENCES ecommerce_user (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_segment_rfm CHECK ((r_score IS NULL OR r_score BETWEEN 1 AND 5) AND (f_score IS NULL OR f_score BETWEEN 1 AND 5) AND (m_score IS NULL OR m_score BETWEEN 1 AND 5))
) ENGINE=InnoDB COMMENT='用户价值分层结果表';

CREATE TABLE IF NOT EXISTS spark_analysis_task (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务主键',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型',
    task_status ENUM('Pending', 'Running', 'Succeeded', 'Failed', 'Cancelled') NOT NULL DEFAULT 'Pending' COMMENT '任务状态',
    data_version VARCHAR(32) NOT NULL COMMENT '目标数据版本',
    submitter_id BIGINT UNSIGNED NULL COMMENT '提交系统用户主键',
    spark_application_id VARCHAR(100) NULL COMMENT 'Spark Application ID',
    started_at DATETIME(3) NULL COMMENT '开始时间',
    finished_at DATETIME(3) NULL COMMENT '结束时间',
    error_message TEXT NULL COMMENT '失败信息',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_spark_task_status_time (task_status, created_at),
    KEY idx_spark_task_version (data_version),
    CONSTRAINT fk_spark_task_submitter FOREIGN KEY (submitter_id) REFERENCES sys_user (id) ON DELETE SET NULL,
    CONSTRAINT chk_spark_task_time CHECK (finished_at IS NULL OR started_at IS NULL OR finished_at >= started_at)
) ENGINE=InnoDB COMMENT='Spark 用户画像分析任务表';

CREATE TABLE IF NOT EXISTS ads_user_rfm (
    user_id BIGINT UNSIGNED NOT NULL COMMENT '电商用户主键',
    r_value INT NULL COMMENT '最近消费距统计日天数',
    f_value BIGINT NULL COMMENT '累计有效订单数',
    m_value DECIMAL(18,2) NULL COMMENT '累计实付金额',
    r_score TINYINT UNSIGNED NULL COMMENT 'R得分(1-5)',
    f_score TINYINT UNSIGNED NULL COMMENT 'F得分(1-5)',
    m_score TINYINT UNSIGNED NULL COMMENT 'M得分(1-5)',
    rfm_group VARCHAR(20) NOT NULL COMMENT 'RFM分组编码',
    rfm_group_name VARCHAR(20) NOT NULL COMMENT 'RFM分组中文名称',
    data_version VARCHAR(32) NOT NULL COMMENT '分析数据版本',
    calculated_at DATETIME(3) NOT NULL COMMENT '计算时间',
    PRIMARY KEY (user_id)
) ENGINE=InnoDB COMMENT='RFM 8分类用户价值分析结果表';

-- 仅初始化标签定义，不提供默认密码，管理员账号应由后端使用 BCrypt 安全创建。
INSERT INTO profile_tag_definition (tag_code, tag_name, tag_category, value_type, calculation_rule)
VALUES
    ('ACTIVE_LEVEL', '用户活跃等级', '行为特征', 'String', '根据近30日登录和浏览次数分级'),
    ('CONSUMPTION_LEVEL', '消费能力等级', '消费特征', 'String', '根据累计消费金额和平均客单价分级'),
    ('FAVORITE_CATEGORY', '偏好商品分类', '兴趣偏好', 'String', '根据浏览、加购和购买行为综合计算'),
    ('RFM_SEGMENT', 'RFM用户分层', '用户价值', 'String', '根据最近消费、消费频次和消费金额计算')
ON DUPLICATE KEY UPDATE tag_name = VALUES(tag_name), calculation_rule = VALUES(calculation_rule);

-- ============================================================
-- 11. 人群包管理表
-- ============================================================
CREATE TABLE IF NOT EXISTS audience_package
(
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    package_name VARCHAR(100)  NOT NULL COMMENT '人群包名称',
    description  VARCHAR(500)  NULL COMMENT '描述',
    total_count  INT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '实际圈选人数',
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1=已保存 0=已删除',
    created_by   BIGINT UNSIGNED NULL COMMENT '创建人（关联 sys_user.id）',
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_created_by (created_by),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人群包管理表';

-- ============================================================
-- 11-2. 人群包圈选规则表
-- ============================================================
CREATE TABLE IF NOT EXISTS audience_rule
(
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    package_id BIGINT UNSIGNED NOT NULL COMMENT '关联 audience_package.id',
    rule_group VARCHAR(50)     NOT NULL DEFAULT 'root' COMMENT '规则组名（支持嵌套 AND/OR 分组）',
    field_name VARCHAR(50)     NOT NULL COMMENT '规则字段（如 gender、age、segment_code、tag_value）',
    operator   VARCHAR(20)     NOT NULL COMMENT '运算符：EQ/NEQ/IN/BETWEEN/GT/LT/CONTAINS',
    value      VARCHAR(500)    NOT NULL COMMENT '条件值（支持 JSON 格式，如 "女"、[20,30]、["高消费"]）',
    logic_op   VARCHAR(5)      NOT NULL DEFAULT 'AND' COMMENT '与下一规则的逻辑关系：AND / OR',
    sort_order INT             NOT NULL DEFAULT 0 COMMENT '排序',
    UNIQUE KEY uk_package_rule (package_id, rule_group, field_name, sort_order),
    KEY idx_package_id (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人群包圈选规则表';

-- ============================================================
-- 12. 画像对比任务表
-- ============================================================
CREATE TABLE IF NOT EXISTS comparison_task
(
    id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    task_name            VARCHAR(100)  NOT NULL COMMENT '对比任务名称',
    package_id_a         BIGINT UNSIGNED NOT NULL COMMENT '人群包 A（关联 audience_package.id）',
    package_id_b         BIGINT UNSIGNED NOT NULL COMMENT '人群包 B（关联 audience_package.id）',
    comparison_dimensions VARCHAR(500)  NULL COMMENT '对比维度（JSON 数组，如 ["gender","age","consumption"]）',
    status               VARCHAR(20)   NOT NULL DEFAULT 'Pending' COMMENT '任务状态：Pending/Running/Succeeded/Failed',
    created_by           BIGINT UNSIGNED NULL COMMENT '创建人（关联 sys_user.id）',
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    started_at           DATETIME      NULL COMMENT '开始执行时间',
    finished_at          DATETIME      NULL COMMENT '完成时间',
    KEY idx_created_by (created_by),
    KEY idx_status (status),
    KEY idx_package_a (package_id_a),
    KEY idx_package_b (package_id_b)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='画像对比任务表';

-- ============================================================
-- 13. 画像对比结果表
-- ============================================================
CREATE TABLE IF NOT EXISTS comparison_result
(
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    task_id         BIGINT UNSIGNED NOT NULL COMMENT '关联 comparison_task.id',
    dimension       VARCHAR(50)     NOT NULL COMMENT '对比维度',
    dimension_value VARCHAR(100)    NOT NULL COMMENT '维度值（如 "男"、"25-35"、"高消费"）',
    count_a         INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '人群 A 该维度的人数',
    count_b         INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '人群 B 该维度的人数',
    ratio_a         DECIMAL(10,4)   NOT NULL DEFAULT 0 COMMENT '人群 A 占比',
    ratio_b         DECIMAL(10,4)   NOT NULL DEFAULT 0 COMMENT '人群 B 占比',
    diff_ratio      DECIMAL(10,4)   NOT NULL DEFAULT 0 COMMENT '差异率（ratio_a - ratio_b）',
    UNIQUE KEY uk_task_dimension (task_id, dimension, dimension_value),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='画像对比结果表';

-- ─── 系统通知表 ───
CREATE TABLE IF NOT EXISTS sys_notification (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED NOT NULL,
    type        VARCHAR(20)     NOT NULL COMMENT 'TASK/DATA/SYSTEM/AI',
    title       VARCHAR(200)    NOT NULL,
    content     VARCHAR(500)    NOT NULL,
    is_read     TINYINT(1)      NOT NULL DEFAULT 0,
    ref_type    VARCHAR(50)     NULL COMMENT '关联业务类型',
    ref_id      BIGINT          NULL COMMENT '关联业务ID',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知';
