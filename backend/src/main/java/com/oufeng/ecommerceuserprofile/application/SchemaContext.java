package com.oufeng.ecommerceuserprofile.application;

/**
 * 数据库表结构上下文，注入到 LLM system prompt 中，
 * 帮助大模型理解数据模型并生成正确的 SQL 查询。
 *
 * 设计要点：
 * - 只列精选常用字段，并附中文注释（减少模型猜测字段语义）
 * - 明确 SQL 生成硬性规则（对应后端黑名单校验，先告知模型避免生成即被拒）
 * - 提供 3 个「问题 → SQL」Few-shot 示例，引导模型输出风格
 */
public final class SchemaContext {

    private SchemaContext() {}

    /** 业务表结构摘要（精选字段 + 中文注释，控制 token 消耗）。 */
    public static String build() {
        return """
                ## 数据库表结构（精选字段，附中文注释）
                只列出与用户分析相关的业务表，系统管理表（sys_user 等）不应出现在分析中。

                ### ecommerce_user — 用户基础信息
                - id 主键
                - user_code 用户编码
                - gender 性别（'Male' 男 / 'Female' 女）
                - age 年龄（整数）
                - province 省份（完整名称，如"广东省"）
                - city 城市

                ### user_profile_summary — 用户画像汇总（最常用）
                - user_id 关联 ecommerce_user.id
                - total_order_count 累计订单数
                - total_payment_amount 累计消费金额（DECIMAL）
                - average_order_amount 平均客单价
                - browse_count_30d 近30天浏览次数
                - login_count_30d 近30天登录次数
                - last_active_at 最近活跃时间
                - favorite_category_id 偏好品类 id（关联 product_category.id）

                ### user_segment — 用户价值分层
                - user_id 关联 ecommerce_user.id
                - segment_code 分层编码（HIGH_VALUE / POTENTIAL / AT_RISK / GENERAL / LOW_VALUE）
                - segment_name 分层名称（高价值用户 / 潜力用户 / 流失风险用户 / 一般用户 / 低价值用户）
                - segment_score 分层评分（DECIMAL）

                ### sales_order — 销售订单
                - id 订单号
                - user_id 关联 ecommerce_user.id
                - order_status 订单状态（pending / paid / shipped / completed / cancelled）
                - payment_amount 实付金额（DECIMAL）
                - created_at 下单时间（DATETIME）

                ### user_login_behavior — 用户登录行为
                - id 主键
                - user_id 关联 ecommerce_user.id
                - login_time 登录时间（DATETIME）

                ### user_browse_behavior — 用户浏览行为
                - id 主键
                - user_id 关联 ecommerce_user.id
                - product_id 关联 product.id
                - action_type 行为类型（view / click / favorite / cart）
                - browse_time 浏览时间（DATETIME）

                ### product — 商品
                - id 商品号
                - product_name 商品名称
                - category_id 关联 product_category.id
                - price 售价（DECIMAL）

                ### product_category — 商品分类
                - id 分类号
                - name 分类名称
                - parent_id 父分类（0 为顶级）

                ## SQL 生成硬性规则（违反任一规则，查询会被系统拒绝执行）
                1. 禁止子查询：FROM / JOIN 之后不得出现括号 (SELECT ...
                2. 禁止反引号 `，禁止注释（--、#、/* */）
                3. 所有查询必须以 LIMIT 结尾（最多 100 条）
                4. 只能使用上面列出的字段名，禁止凭空猜测列名
                5. 只允许 SELECT 查询，禁止 DELETE / UPDATE / INSERT / DROP / ALTER / TRUNCATE
                6. gender 条件用 'Male' / 'Female'；中文"男/女"必须映射为对应值
                7. 中文分层名称须映射为 segment_code（高价值用户→HIGH_VALUE 等）

                ## 参考示例（问题 → SQL）
                Q: 各分层用户占比
                ```sql
                SELECT segment_name, COUNT(*) AS cnt FROM user_segment GROUP BY segment_name ORDER BY cnt DESC LIMIT 10
                ```

                Q: 近30天活跃趋势
                ```sql
                SELECT DATE(login_time) AS 日期, COUNT(DISTINCT user_id) AS 活跃人数 FROM user_login_behavior WHERE login_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY DATE(login_time) ORDER BY 日期 LIMIT 100
                ```

                Q: 高价值用户的省份分布
                ```sql
                SELECT u.province, COUNT(DISTINCT u.id) AS cnt FROM ecommerce_user u JOIN user_segment s ON s.user_id = u.id WHERE s.segment_code = 'HIGH_VALUE' GROUP BY u.province ORDER BY cnt DESC LIMIT 10
                ```

                ## 回答规范
                - 回答使用中文，结论先行，数据用**加粗**突出，200 字以内
                - 需要查询数据时，在回答末尾用 ```sql ... ``` 包含 SQL
                """;
    }
}
