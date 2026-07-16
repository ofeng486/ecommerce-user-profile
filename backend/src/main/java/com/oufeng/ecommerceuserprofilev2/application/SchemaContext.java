package com.oufeng.ecommerceuserprofilev2.application;

/**
 * 数据库表结构上下文，注入到 LLM system prompt 中，
 * 帮助大模型理解数据模型并生成正确的 SQL 查询。
 */
public final class SchemaContext {

    private SchemaContext() {}

    /** 业务表结构摘要（精简版，控制 token 消耗）。 */
    public static String build() {
        return """
                ## 数据库表结构
                只列出与用户分析相关的业务表，系统管理表（sys_user/sys_login_log 等）不应出现在分析中。

                ### ecommerce_user — 电商用户基础信息
                id BIGINT PK, user_code VARCHAR, gender ENUM('Male','Female'), age INT, province VARCHAR, city VARCHAR

                ### product_category — 商品分类
                id BIGINT PK, name VARCHAR, parent_id BIGINT

                ### product — 商品
                id BIGINT PK, product_name VARCHAR, category_id BIGINT, price DECIMAL

                ### user_browse_behavior — 用户浏览行为
                id BIGINT PK, user_id BIGINT FK→ecommerce_user, product_id BIGINT, action_type ENUM('view','click','favorite','cart'), browse_time DATETIME

                ### user_login_behavior — 用户登录行为
                id BIGINT PK, user_id BIGINT FK→ecommerce_user, login_time DATETIME, duration_seconds INT

                ### sales_order — 销售订单
                id BIGINT PK, user_id BIGINT FK→ecommerce_user, order_status ENUM('pending','paid','shipped','completed','cancelled'), payment_amount DECIMAL, created_at DATETIME

                ### sales_order_item — 订单明细
                id BIGINT PK, order_id BIGINT FK→sales_order, product_id BIGINT FK→product, quantity INT, unit_price DECIMAL

                ### user_profile_summary — 用户画像汇总
                user_id BIGINT PK FK→ecommerce_user, total_order_count INT, total_payment_amount DECIMAL, average_order_amount DECIMAL, browse_count_30d INT, login_count_30d INT, last_active_at DATETIME, favorite_category_id BIGINT

                ### user_segment — 用户价值分层
                user_id BIGINT PK FK→ecommerce_user, segment_code VARCHAR( HIGH_VALUE | POTENTIAL | AT_RISK | GENERAL | LOW_VALUE ), segment_name VARCHAR( 高价值用户 | 潜力用户 | 流失风险用户 | 一般用户 | 低价值用户 ), r_score INT, f_score INT, m_score INT, segment_score DECIMAL

                ### user_profile_tag — 用户标签
                tag_id BIGINT FK→profile_tag_definition, user_id BIGINT FK→ecommerce_user, tag_code VARCHAR, tag_value VARCHAR, tag_score DECIMAL

                ## 分析规则
                - 只生成 SELECT 查询，禁止 DELETE/UPDATE/INSERT/DROP/ALTER/TRUNCATE。
                - 涉及金额的字段为 DECIMAL 类型，比较时直接使用数字。
                - gender 字段值为 'Male'/'Female'，中文提问的"男"/"女"请映射为对应值。
                - segment_code 值见 user_segment 表说明，中文分层名称映射为对应英文 code。
                - 省份字段 province 存储的是完整名称如"广东省""浙江省"。
                - 回答使用中文，简洁明了，200字以内。如包含数据结果请用表格或列表呈现。
                """;
    }
}
