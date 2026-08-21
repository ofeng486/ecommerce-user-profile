package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 公开数据接口——无需登录即可访问，供用户端/管理端共用（省份、城市、活跃趋势等基础数据）。
 */
@Tag(name = "公开数据")
@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    private final JdbcTemplate jdbc;

    public PublicController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

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

    @Operation(summary = "全量省份列表（无需登录，供圈选下拉等）")
    @GetMapping("/all-provinces")
    public Result<List<String>> allProvinces() {
        return Result.success(jdbc.queryForList("""
                SELECT DISTINCT u.province
                FROM ecommerce_user u
                WHERE u.province IS NOT NULL AND u.province != ''
                ORDER BY u.province
                """, String.class));
    }

    @Operation(summary = "城市列表（无需登录，可按省份过滤，返回城市+所属省份供分组下拉）")
    @GetMapping("/cities")
    public Result<List<Map<String, Object>>> cities(@RequestParam(required = false) String province) {
        String sql = """
                SELECT DISTINCT u.city AS city, u.province AS province
                FROM ecommerce_user u
                WHERE u.city IS NOT NULL AND u.city != ''
                """ + (province != null && !province.isBlank() ? " AND u.province = ? " : "") + """
                ORDER BY u.province, u.city
                """;
        return Result.success(province != null && !province.isBlank()
                ? jdbc.queryForList(sql, province)
                : jdbc.queryForList(sql));
    }

    @Operation(summary = "分层×省份交叉分布（无需登录，供概览页堆叠柱图）")
    @GetMapping("/segment-provinces")
    public Result<List<Map<String, Object>>> segmentProvinces() {
        // 取用户量 TOP10 省份为列，每个分层为一行，交叉人数（洞察"高价值集中在哪些省"）
        return Result.success(jdbc.queryForList("""
                SELECT sg.segment_code AS segmentCode, sg.segment_name AS segmentName,
                       u.province AS province, COUNT(*) AS userCount
                FROM user_segment sg
                JOIN ecommerce_user u ON u.id = sg.user_id
                WHERE u.province IS NOT NULL AND u.province != ''
                GROUP BY sg.segment_code, sg.segment_name, u.province
                ORDER BY sg.segment_code, userCount DESC
                """));
    }

    @Operation(summary = "活跃趋势（无需登录，支持时间范围）")
    @GetMapping("/active-trend")
    public Result<List<Map<String, Object>>> activeTrend(@RequestParam(defaultValue = "30") int days) {
        // 统计基准日取业务数据最大登录日期（数据可能为历史固定日期生成，不能依赖系统当天）；
        // days 经白名单范围收敛后拼接，无注入风险
        int window = Math.max(7, Math.min(days, 365));
        // 生成完整日期序列并 LEFT JOIN 行为表：缺失天 activeCount=0，保证返回 window 条数据点
        // （避免数据稀疏时 X 轴只显示部分天，用户看不出"90 天"效果）
        // 同时统计每日活跃中的高价值用户（JOIN user_segment），供工作台叠加"高价值对比线"
        return Result.success(jdbc.queryForList("""
                WITH RECURSIVE date_series AS (
                    SELECT DATE_SUB((SELECT DATE(MAX(login_at)) FROM user_login_behavior), INTERVAL %1$d DAY) AS day
                    UNION ALL
                    SELECT DATE_ADD(day, INTERVAL 1 DAY)
                    FROM date_series
                    WHERE day < (SELECT DATE(MAX(login_at)) FROM user_login_behavior)
                )
                SELECT ds.day AS day,
                       COALESCE(t.cnt, 0) AS activeCount,
                       COALESCE(h.cnt, 0) AS highValueCount
                FROM date_series ds
                LEFT JOIN (
                    SELECT DATE(b.login_at) AS d, COUNT(DISTINCT b.user_id) AS cnt
                    FROM user_login_behavior b
                    WHERE b.login_at >= DATE_SUB((SELECT DATE(MAX(login_at)) FROM user_login_behavior), INTERVAL %1$d DAY)
                    GROUP BY DATE(b.login_at)
                ) t ON ds.day = t.d
                LEFT JOIN (
                    SELECT DATE(b.login_at) AS d, COUNT(DISTINCT b.user_id) AS cnt
                    FROM user_login_behavior b
                    JOIN user_segment sg ON sg.user_id = b.user_id AND sg.segment_code = 'HIGH_VALUE'
                    WHERE b.login_at >= DATE_SUB((SELECT DATE(MAX(login_at)) FROM user_login_behavior), INTERVAL %1$d DAY)
                    GROUP BY DATE(b.login_at)
                ) h ON ds.day = h.d
                ORDER BY ds.day
                """.formatted(window - 1)));
    }
}
