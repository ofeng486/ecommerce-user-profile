package com.oufeng.ecommerceuserprofilev2.infrastructure.llm;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

/**
 * 本地模拟 LLM 提供者，用于无 API Key 时演示。
 * 支持基础的 NL→SQL 规则匹配，不依赖外部 LLM 服务。
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
            return "当前共有 " + total + " 位电商用户，其中 " + profiled + " 位已生成画像。";
        }

        if (q.contains("高价值") && (q.contains("多少") || q.contains("数量"))) {
            long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM user_segment WHERE segment_code='HIGH_VALUE'", Long.class);
            return "当前共有 " + count + " 位高价值用户。";
        }

        if (q.contains("分层") || q.contains("分布") || q.contains("概览")) {
            var rows = jdbc.queryForList(
                    "SELECT segment_name, COUNT(*) AS cnt FROM user_segment GROUP BY segment_name ORDER BY cnt DESC");
            if (rows.isEmpty()) return "暂无分层数据，请先生成画像数据。";
            StringBuilder sb = new StringBuilder("当前用户分层分布如下：\n");
            for (Map<String, Object> row : rows) {
                sb.append("  • ").append(row.get("segment_name")).append("：")
                  .append(row.get("cnt")).append(" 人\n");
            }
            return sb.toString();
        }

        if (q.contains("广东") || q.contains("省份") || q.contains("地区")) {
            var rows = jdbc.queryForList(
                    "SELECT province, COUNT(*) AS cnt FROM ecommerce_user WHERE province IS NOT NULL AND province != '' GROUP BY province ORDER BY cnt DESC LIMIT 5");
            if (rows.isEmpty()) return "暂无地区数据。";
            StringBuilder sb = new StringBuilder("用户数量最多的 5 个省份：\n");
            for (Map<String, Object> row : rows) {
                sb.append("  • ").append(row.get("province")).append("：")
                  .append(row.get("cnt")).append(" 人\n");
            }
            return sb.toString();
        }

        if (q.contains("性别") || q.contains("男女")) {
            var rows = jdbc.queryForList(
                    "SELECT gender, COUNT(*) AS cnt FROM ecommerce_user GROUP BY gender");
            if (rows.isEmpty()) return "暂无性别数据。";
            StringBuilder sb = new StringBuilder("用户性别分布：\n");
            for (Map<String, Object> row : rows) {
                String g = "Male".equals(row.get("gender")) ? "男" : "女";
                sb.append("  • ").append(g).append("：").append(row.get("cnt")).append(" 人\n");
            }
            return sb.toString();
        }

        return "我是本地模拟 AI 助手。你可以问我：\n"
                + "  • \"用户总数多少\"\n"
                + "  • \"高价值用户数量\"\n"
                + "  • \"用户分层分布\"\n"
                + "  • \"省份分布\"\n"
                + "  • \"性别分布\"\n"
                + "配置 AI_API_KEY 环境变量接入大模型后可解锁完整智能分析能力。";
    }
}
