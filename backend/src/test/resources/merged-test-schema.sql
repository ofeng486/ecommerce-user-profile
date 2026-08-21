DROP TABLE IF EXISTS sys_notification;
DROP TABLE IF EXISTS spark_analysis_task;
DROP TABLE IF EXISTS sys_user;
-- 每个测试方法前重建（@Sql BEFORE_TEST_METHOD）：先 DROP 再 CREATE，保证从空库开始
DROP TABLE IF EXISTS user_browse_behavior;
DROP TABLE IF EXISTS user_login_behavior;
DROP TABLE IF EXISTS sales_order_item;
DROP TABLE IF EXISTS sales_order;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS product_category;
DROP TABLE IF EXISTS ecommerce_user;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    role ENUM('User', 'Admin') NOT NULL DEFAULT 'User',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    last_login_at DATETIME(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    CONSTRAINT chk_sys_user_status CHECK (status IN (0, 1))
) ;


CREATE TABLE IF NOT EXISTS ecommerce_user (
    id BIGINT NOT NULL,
    user_code VARCHAR(32) NOT NULL,
    gender ENUM('Unknown', 'Male', 'Female') NOT NULL DEFAULT 'Unknown',
    age SMALLINT UNSIGNED NULL,
    province VARCHAR(50) NULL,
    city VARCHAR(50) NULL,
    register_channel VARCHAR(30) NOT NULL,
    membership_level VARCHAR(20) NOT NULL DEFAULT 'Normal',
    registered_at DATETIME(3) NOT NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ecommerce_user_code (user_code),
    KEY idx_ecommerce_user_region (province, city),
    KEY idx_ecommerce_user_registered (registered_at),
    CONSTRAINT chk_ecommerce_user_age CHECK (age IS NULL OR age BETWEEN 1 AND 120),
    CONSTRAINT chk_ecommerce_user_status CHECK (status IN (0, 1))
) ;

CREATE TABLE IF NOT EXISTS product_category (
    id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    category_name VARCHAR(100) NOT NULL,
    category_level TINYINT UNSIGNED NOT NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_category_parent (parent_id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES product_category (id) ON DELETE SET NULL,
    CONSTRAINT chk_category_status CHECK (status IN (0, 1))
) ;

CREATE TABLE IF NOT EXISTS product (
    id BIGINT NOT NULL,
    product_code VARCHAR(32) NOT NULL,
    category_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    brand_name VARCHAR(100) NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_product_category_status (category_id, status),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category (id),
    CONSTRAINT chk_product_price CHECK (unit_price >= 0),
    CONSTRAINT chk_product_status CHECK (status IN (0, 1))
) ;

CREATE TABLE IF NOT EXISTS sales_order (
    id BIGINT NOT NULL,
    order_no VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status ENUM('Pending', 'Paid', 'Shipped', 'Completed', 'Cancelled', 'Refunded') NOT NULL,
    total_amount DECIMAL(18,2) NOT NULL,
    discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    payment_amount DECIMAL(18,2) NOT NULL,
    payment_method VARCHAR(30) NULL,
    ordered_at DATETIME(3) NOT NULL,
    paid_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sales_order_no (order_no),
    KEY idx_sales_order_user_time (user_id, ordered_at),
    KEY idx_sales_order_status_time (order_status, ordered_at),
    CONSTRAINT chk_order_amount CHECK (total_amount >= 0 AND discount_amount >= 0 AND payment_amount >= 0),
    CONSTRAINT chk_order_payment CHECK (payment_amount = total_amount - discount_amount),
    CONSTRAINT chk_order_paid_time CHECK (paid_at IS NULL OR paid_at >= ordered_at),
    CONSTRAINT chk_order_completed_time CHECK (completed_at IS NULL OR completed_at >= ordered_at)
) ;

CREATE TABLE IF NOT EXISTS sales_order_item (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name_snapshot VARCHAR(200) NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    quantity INT UNSIGNED NOT NULL,
    item_amount DECIMAL(18,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id),
    KEY idx_order_item_product (product_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES sales_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_item_amount CHECK (unit_price >= 0 AND item_amount = unit_price * quantity)
) ;

CREATE TABLE IF NOT EXISTS user_browse_behavior (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    behavior_type ENUM('View', 'Click', 'Favorite', 'Cart', 'Purchase') NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    device_type VARCHAR(20) NULL,
    channel VARCHAR(30) NULL,
    behavior_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_browse_user_time (user_id, behavior_at),
    KEY idx_browse_product_time (product_id, behavior_at),
    KEY idx_browse_type_time (behavior_type, behavior_at)
) ;

CREATE TABLE IF NOT EXISTS user_login_behavior (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    device_type VARCHAR(20) NULL,
    login_channel VARCHAR(30) NULL,
    login_at DATETIME(3) NOT NULL,
    logout_at DATETIME(3) NULL,
    duration_seconds INT UNSIGNED NULL,
    PRIMARY KEY (id),
    KEY idx_login_behavior_user_time (user_id, login_at),
    KEY idx_login_behavior_time (login_at),
    CONSTRAINT chk_login_time CHECK (logout_at IS NULL OR logout_at >= login_at)
) ;

CREATE TABLE IF NOT EXISTS spark_analysis_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_name VARCHAR(100) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    task_status ENUM('Pending', 'Running', 'Succeeded', 'Failed', 'Cancelled') NOT NULL DEFAULT 'Pending',
    data_version VARCHAR(32) NOT NULL,
    submitter_id BIGINT NULL,
    spark_application_id VARCHAR(100) NULL,
    started_at DATETIME(3) NULL,
    finished_at DATETIME(3) NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_spark_task_status_time (task_status, created_at),
    KEY idx_spark_task_version (data_version),
    CONSTRAINT fk_spark_task_submitter FOREIGN KEY (submitter_id) REFERENCES sys_user (id) ON DELETE SET NULL,
    CONSTRAINT chk_spark_task_time CHECK (finished_at IS NULL OR started_at IS NULL OR finished_at >= started_at)
) ;


CREATE TABLE IF NOT EXISTS sys_notification (
    id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    type        VARCHAR(20)     NOT NULL,
    title       VARCHAR(200)    NOT NULL,
    content     VARCHAR(500)    NOT NULL,
    is_read     TINYINT      NOT NULL DEFAULT 0,
    ref_type    VARCHAR(50)     NULL,
    ref_id      BIGINT          NULL,
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_created (created_at)
)  DEFAULT CHARSET=utf8mb4 COMMENT='系统通知';
