package com.oufeng.ecommerceuserprofile.infrastructure.llm;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * 本地模拟 LLM 提供者，用于无 API Key 时演示。
 * 支持基础的 NL→SQL 规则匹配，不依赖外部 LLM 服务。
 *
 * 回答约定：与真实 LLM 输出风格一致（结论先行 + 分点 + 末尾 ```sql 块），
 * 调用方（AIChatStreamController）会提取 SQL 执行并把结构化结果返回前端渲染图表。
 */
public class MockLLMProvider implements LLMProvider {

    private final JdbcTemplate jdbc;

    public MockLLMProvider(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        String q = userMessage.toLowerCase().trim();

        if (q.contains("用户") && q.contains("总") && (q.contains("多少") || q.contains("数量"))) {
            long total = jdbc.queryForObject("SELECT COUNT(*) FROM ecommerce_user", Long.class);
            long profiled = jdbc.queryForObject("SELECT COUNT(*) FROM user_profile_summary", Long.class);
            return "当前共有 **" + total + "** 位电商用户，其中 **" + profiled + "** 位已生成画像。\n"
                    + "```sql\nSELECT COUNT(*) AS total FROM ecommerce_user\n```";
        }

        if (q.contains("高价值") && (q.contains("多少") || q.contains("数量"))) {
            long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM user_segment WHERE segment_code='HIGH_VALUE'", Long.class);
            return "当前共有 **" + count + "** 位高价值用户。\n"
                    + "```sql\nSELECT COUNT(*) AS cnt FROM user_segment WHERE segment_code='HIGH_VALUE'\n```";
        }

        if (q.contains("分层") || q.contains("分布") || q.contains("概览")) {
            var rows = jdbc.queryForList(
                    "SELECT segment_name, COUNT(*) AS cnt FROM user_segment GROUP BY segment_name ORDER BY cnt DESC");
            if (rows.isEmpty()) return "暂无分层数据，请先生成画像数据。";
            StringBuilder sb = new StringBuilder("当前用户分层分布如下：\n");
            for (Map<String, Object> row : rows) {
                sb.append("  • ").append(row.get("segment_name")).append("：**")
                  .append(row.get("cnt")).append("** 人\n");
            }
            sb.append("```sql\nSELECT segment_name, COUNT(*) AS cnt FROM user_segment GROUP BY segment_name ORDER BY cnt DESC\n```");
            return sb.toString();
        }

        if (q.contains("广东") || q.contains("省份") || q.contains("地区")) {
            var rows = jdbc.queryForList(
                    "SELECT province, COUNT(*) AS cnt FROM ecommerce_user WHERE province IS NOT NULL AND province != '' GROUP BY province ORDER BY cnt DESC LIMIT 5");
            if (rows.isEmpty()) return "暂无地区数据。";
            StringBuilder sb = new StringBuilder("用户数量最多的 5 个省份：\n");
            for (Map<String, Object> row : rows) {
                sb.append("  • ").append(row.get("province")).append("：**")
                  .append(row.get("cnt")).append("** 人\n");
            }
            sb.append("```sql\nSELECT province, COUNT(*) AS cnt FROM ecommerce_user WHERE province IS NOT NULL AND province != '' GROUP BY province ORDER BY cnt DESC LIMIT 5\n```");
            return sb.toString();
        }

        if (q.contains("性别") || q.contains("男女")) {
            var rows = jdbc.queryForList(
                    "SELECT gender, COUNT(*) AS cnt FROM ecommerce_user GROUP BY gender");
            if (rows.isEmpty()) return "暂无性别数据。";
            StringBuilder sb = new StringBuilder("用户性别分布：\n");
            for (Map<String, Object> row : rows) {
                String g = "Male".equals(row.get("gender")) ? "男" : "女";
                sb.append("  • ").append(g).append("：**").append(row.get("cnt")).append("** 人\n");
            }
            sb.append("```sql\nSELECT gender, COUNT(*) AS cnt FROM ecommerce_user GROUP BY gender\n```");
            return sb.toString();
        }

        if (q.contains("活跃") && (q.contains("趋势") || q.contains("近") || q.contains("30"))) {
            var rows = jdbc.queryForList(
                    "SELECT DATE(login_time) AS 日期, COUNT(DISTINCT user_id) AS 活跃人数 FROM user_login_behavior "
                    + "WHERE login_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY DATE(login_time) ORDER BY 日期 LIMIT 100");
            if (rows.isEmpty()) return "近30天暂无登录行为数据。";
            StringBuilder sb = new StringBuilder("近 30 天活跃趋势（每日登录人数）：\n");
            for (Map<String, Object> row : rows) {
                sb.append("  • ").append(row.get("日期")).append("：**")
                  .append(row.get("活跃人数")).append("** 人\n");
            }
            sb.append("```sql\nSELECT DATE(login_time) AS 日期, COUNT(DISTINCT user_id) AS 活跃人数 FROM user_login_behavior WHERE login_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY DATE(login_time) ORDER BY 日期 LIMIT 100\n```");
            return sb.toString();
        }

        if (q.contains("消费") || q.contains("订单") || q.contains("金额")) {
            var rows = jdbc.queryForList(
                    "SELECT segment_name, ROUND(SUM(o.payment_amount), 2) AS 消费金额 FROM sales_order o "
                    + "JOIN user_segment s ON s.user_id = o.user_id WHERE o.order_status != 'cancelled' "
                    + "GROUP BY segment_name ORDER BY 消费金额 DESC LIMIT 10");
            if (rows.isEmpty()) return "暂无消费数据。";
            StringBuilder sb = new StringBuilder("各分层用户累计消费金额：\n");
            for (Map<String, Object> row : rows) {
                sb.append("  • ").append(row.get("segment_name")).append("：**")
                  .append(row.get("消费金额")).append("** 元\n");
            }
            sb.append("```sql\nSELECT segment_name, ROUND(SUM(o.payment_amount), 2) AS 消费金额 FROM sales_order o JOIN user_segment s ON s.user_id = o.user_id WHERE o.order_status != 'cancelled' GROUP BY segment_name ORDER BY 消费金额 DESC LIMIT 10\n```");
            return sb.toString();
        }

        if (q.contains("商品") || q.contains("销量") || q.contains("热销")) {
            var rows = jdbc.queryForList(
                    "SELECT p.product_name, SUM(oi.quantity) AS 销量 FROM sales_order_item oi "
                    + "JOIN product p ON p.id = oi.product_id GROUP BY p.product_name ORDER BY 销量 DESC LIMIT 10");
            if (rows.isEmpty()) return "暂无商品销量数据。";
            StringBuilder sb = new StringBuilder("销量最高的商品 Top10：\n");
            for (Map<String, Object> row : rows) {
                sb.append("  • ").append(row.get("product_name")).append("：**")
                  .append(row.get("销量")).append("** 件\n");
            }
            sb.append("```sql\nSELECT p.product_name, SUM(oi.quantity) AS 销量 FROM sales_order_item oi JOIN product p ON p.id = oi.product_id GROUP BY p.product_name ORDER BY 销量 DESC LIMIT 10\n```");
            return sb.toString();
        }

        return "我是本地模拟 AI 助手。你可以问我：\n"
                + "  • \"用户总数多少\"\n"
                + "  • \"高价值用户数量\"\n"
                + "  • \"用户分层分布\"\n"
                + "  • \"省份分布\"\n"
                + "  • \"性别分布\"\n"
                + "  • \"近30天活跃趋势\"\n"
                + "  • \"各分层消费金额\"\n"
                + "  • \"热销商品 Top10\"\n"
                + "配置 AI_API_KEY 环境变量接入大模型后可解锁完整智能分析能力。";
    }
}
