package com.oufeng.ecommerceuserprofilev2.interfaces;

import com.oufeng.ecommerceuserprofilev2.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 公开数据接口——无需登录即可访问，供大屏展示使用。
 */
@Tag(name = "公开数据")
@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    private final JdbcTemplate jdbc;

    public PublicController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Operation(summary = "画像概览（无需登录）")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        String sql = """
                SELECT COUNT(u.id) AS totalUsers, COUNT(s.user_id) AS profiledUsers,
                       SUM(CASE WHEN seg.segment_code IN ('HIGH_VALUE','POTENTIAL') THEN 1 ELSE 0 END) AS highValueUsers,
                       COALESCE(SUM(s.total_payment_amount), 0) AS totalPaymentAmount
                FROM ecommerce_user u
                LEFT JOIN user_profile_summary s ON u.id = s.user_id
                LEFT JOIN user_segment seg ON u.id = seg.user_id
                """;
        return Result.success(jdbc.queryForList(sql).stream().findFirst()
                .orElse(Map.of("totalUsers", 0, "profiledUsers", 0, "highValueUsers", 0, "totalPaymentAmount", 0D)));
    }

    @Operation(summary = "用户分层分布（无需登录）")
    @GetMapping("/segments")
    public Result<List<Map<String, Object>>> segments() {
        return Result.success(jdbc.queryForList("""
                SELECT segment_code, segment_name, COUNT(*) AS userCount
                FROM user_segment WHERE segment_name IS NOT NULL
                GROUP BY segment_code, segment_name ORDER BY userCount DESC
                """));
    }

    @Operation(summary = "省份分布 TOP10（无需登录）")
    @GetMapping("/provinces")
    public Result<List<Map<String, Object>>> provinces() {
        return Result.success(jdbc.queryForList("""
                SELECT u.province, COUNT(*) AS userCount
                FROM ecommerce_user u INNER JOIN user_profile_summary s ON u.id = s.user_id
                WHERE u.province IS NOT NULL AND u.province != ''
                GROUP BY u.province ORDER BY userCount DESC LIMIT 10
                """));
    }

    @Operation(summary = "标签分布（无需登录）")
    @GetMapping("/tags")
    public Result<List<Map<String, Object>>> tags(@RequestParam(defaultValue = "0") int tagId) {
        return Result.success(jdbc.queryForList("""
                SELECT t.tag_value, COUNT(*) AS userCount
                FROM user_profile_tag t WHERE t.tag_id = ? GROUP BY t.tag_value ORDER BY userCount DESC
                """, tagId));
    }
}
