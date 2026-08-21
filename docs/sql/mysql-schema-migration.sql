-- ═══════════════════════════════════════════════════════════
-- 增量迁移脚本（已有数据库执行用；全新部署直接使用 mysql-schema.sql）
-- 适用版本：2026-08 补充优化（F6 AI 对话历史 + P1 大表索引）
-- 执行方式：mysql -u root -p ecommerce_user_profile < mysql-schema-migration.sql
-- ═══════════════════════════════════════════════════════════

-- 1. AI 对话历史表（F6）
CREATE TABLE IF NOT EXISTS ai_chat_history (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '提问用户',
    question    VARCHAR(500)    NOT NULL COMMENT '用户提问',
    answer      TEXT            NOT NULL COMMENT 'AI 回答文本',
    data_json   TEXT            NULL COMMENT 'SQL 查询结果（JSON 数组，供表格/图表回看）',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话历史';

-- 2. 登录行为表时间索引（P1：活跃趋势查询按 login_at 范围裁剪）
ALTER TABLE user_login_behavior
    ADD KEY idx_login_behavior_time (login_at);

-- 3. 浏览行为表 ENUM 补充 Purchase（天池 buy 行为映射，既有校验器已支持但 ENUM 缺失）
ALTER TABLE user_browse_behavior
    MODIFY COLUMN behavior_type ENUM('View', 'Click', 'Favorite', 'Cart', 'Purchase') NOT NULL COMMENT '行为类型';

-- 用户聚类结果表（Spark K-Means 分配）
CREATE TABLE IF NOT EXISTS user_cluster (
  user_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  cluster_id INT NOT NULL,
  data_version VARCHAR(32),
  calculated_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 标签体系可配置化：画像标签定义表新增计算配置字段（2026-08-17）
ALTER TABLE profile_tag_definition
    ADD COLUMN source_table VARCHAR(50) NULL COMMENT '标签计算数据源表（白名单：user_profile_summary/user_segment/ads_user_rfm）' AFTER calculation_rule,
    ADD COLUMN rule_expression VARCHAR(2000) NULL COMMENT '标签计算规则表达式（SQL CASE WHEN，经安全校验）' AFTER source_table;
