package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oufeng.ecommerceuserprofile.common.BusinessException;
import com.oufeng.ecommerceuserprofile.common.ResultCode;
import com.oufeng.ecommerceuserprofile.domain.entity.ProfileTagDefinition;
import com.oufeng.ecommerceuserprofile.domain.mapper.ProfileTagDefinitionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;

/**
 * 标签规则计算服务。
 * 依据标签定义中的 source_table 与 rule_expression，通过 JDBC 对画像基础表执行
 * 表达式聚合，生成或更新 user_profile_tag 标签结果；提供"重算全部"与"预览"能力。
 * 表达式经安全校验（白名单表 + 黑名单关键字），防止 SQL 注入。
 */
@Service
public class TagRuleComputeService {

    private static final Logger log = LoggerFactory.getLogger(TagRuleComputeService.class);

    /** 数据源白名单：仅允许对系统画像基础表计算标签 */
    private static final Set<String> ALLOWED_TABLES = Set.of(
            "user_profile_summary", "user_segment", "ads_user_rfm");

    /** 表达式黑名单：禁止多语句、子查询、注释与危险关键字 */
    private static final List<String> BLOCKED = List.of(";", "select", "from", "insert", "update",
            "delete", "drop", "alter", "create", "union", "join", "--", "/*", "*/", "#", "sleep", "load_file");

    private final ProfileTagDefinitionMapper tagMapper;
    private final JdbcTemplate jdbc;
    private final NotificationService notificationService;

    public TagRuleComputeService(ProfileTagDefinitionMapper tagMapper, JdbcTemplate jdbc,
                                 NotificationService notificationService) {
        this.tagMapper = tagMapper;
        this.jdbc = jdbc;
        this.notificationService = notificationService;
    }

    /** 校验数据源表与规则表达式（不合法直接抛异常，防止注入） */
    public void validateRule(String sourceTable, String ruleExpression) {
        if (sourceTable == null || sourceTable.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择计算依据（数据源）");
        }
        if (!ALLOWED_TABLES.contains(sourceTable.trim())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不允许的数据源: " + sourceTable);
        }
        if (ruleExpression == null || ruleExpression.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写分档规则");
        }
        String lower = ruleExpression.toLowerCase(Locale.ROOT);
        for (String kw : BLOCKED) {
            if (lower.contains(kw)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "规则表达式包含不允许的内容: " + kw);
            }
        }
    }

    /** 预览：按表达式统计各标签值人数（不落库） */
    public List<Map<String, Object>> preview(String sourceTable, String ruleExpression) {
        validateRule(sourceTable, ruleExpression);
        String sql = "SELECT (" + ruleExpression + ") AS tag_value, COUNT(*) AS user_count "
                + "FROM " + sourceTable + " GROUP BY (" + ruleExpression + ") ORDER BY user_count DESC LIMIT 20";
        return jdbc.queryForList(sql);
    }

    /** 重算单个标签定义 */
    @Transactional
    public int recalculateOne(ProfileTagDefinition tag) {
        if (tag.getSourceTable() == null || tag.getRuleExpression() == null
                || tag.getSourceTable().isBlank() || tag.getRuleExpression().isBlank()) {
            return 0; // 未配置计算规则的标签跳过
        }
        validateRule(tag.getSourceTable(), tag.getRuleExpression());
        String table = tag.getSourceTable();
        String expr = tag.getRuleExpression();
        String version = "recalc-" + new Timestamp(System.currentTimeMillis()).toLocalDateTime()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 1. 清理该标签旧结果（单版本语义，避免重复展示）
        jdbc.update("DELETE FROM user_profile_tag WHERE tag_id = ?", tag.getId());
        // 2. 按表达式为每个用户生成标签结果
        String insertSql = "INSERT INTO user_profile_tag (user_id, tag_id, tag_value, score, data_version, calculated_at) "
                + "SELECT u.user_id, " + tag.getId() + ", (" + expr + "), 1, ?, NOW() "
                + "FROM " + table + " u WHERE (" + expr + ") IS NOT NULL";
        return jdbc.update(insertSql, version);
    }

    /** 重算全部启用的标签定义，返回汇总 */
    public Map<String, Object> recalculateAll() {
        List<ProfileTagDefinition> tags = tagMapper.selectList(
                new LambdaQueryWrapper<ProfileTagDefinition>().eq(ProfileTagDefinition::getStatus, (byte) 1));
        int ok = 0, skipped = 0;
        List<String> failed = new ArrayList<>();
        Map<String, Integer> perTag = new LinkedHashMap<>();
        for (ProfileTagDefinition tag : tags) {
            try {
                int n = recalculateOne(tag);
                perTag.put(tag.getTagName(), n);
                if (tag.getSourceTable() == null || tag.getRuleExpression() == null
                        || tag.getSourceTable().isBlank() || tag.getRuleExpression().isBlank()) {
                    skipped++;
                } else {
                    ok++;
                }
            } catch (Exception e) {
                failed.add(tag.getTagName() + "(" + e.getMessage() + ")");
                log.warn("标签重算失败 [{}]: {}", tag.getTagName(), e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", ok);
        result.put("skipped", skipped);
        result.put("failed", failed);
        result.put("detail", perTag);
        // 重算影响全体用户画像标签数据，广播通知让运营及时感知数据刷新
        try {
            String content = "标签重算完成：成功 " + ok + " 个，跳过 " + skipped + " 个";
            if (!failed.isEmpty()) {
                content += "，失败 " + failed.size() + " 个（" + String.join("；", failed) + "）";
            }
            notificationService.broadcast("TAG_RECALC", "标签重算完成", content, "TAG", null);
        } catch (Exception e) {
            log.warn("标签重算通知发送失败: {}", e.getMessage());
        }
        return result;
    }
}
