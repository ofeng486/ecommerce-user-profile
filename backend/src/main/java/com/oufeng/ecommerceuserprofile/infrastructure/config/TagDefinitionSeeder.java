package com.oufeng.ecommerceuserprofile.infrastructure.config;

import com.oufeng.ecommerceuserprofile.domain.entity.ProfileTagDefinition;
import com.oufeng.ecommerceuserprofile.domain.mapper.ProfileTagDefinitionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 标签定义种子数据初始化器。
 * 应用启动时若 profile_tag_definition 表为空，则插入系统预设标签定义，
 * 保证标签体系管理页与画像计算有可用的标签基础（幂等，重复启动不重复插入）。
 */
@Component
@Order(2)
public class TagDefinitionSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TagDefinitionSeeder.class);

    private final ProfileTagDefinitionMapper mapper;

    public TagDefinitionSeeder(ProfileTagDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) {
        try {
            Long count = mapper.selectCount(null);
            if (count != null && count > 0) {
                backfillRules(); // 已有数据：仅补齐缺失的计算规则（幂等）
                return;
            }
            seed("ACTIVE_LEVEL", "用户活跃等级", "行为特征", "String", "根据近30日登录和浏览次数分级",
                    "user_profile_summary",
                    "CASE WHEN login_count_30d + browse_count_30d >= 50 THEN 'High' WHEN login_count_30d + browse_count_30d >= 15 THEN 'Medium' ELSE 'Low' END");
            seed("CONSUMPTION_LEVEL", "消费能力等级", "消费特征", "String", "根据累计消费金额和平均客单价分级",
                    "user_profile_summary",
                    "CASE WHEN total_payment_amount >= 10000 THEN 'High' WHEN total_payment_amount >= 3000 THEN 'Medium' ELSE 'Low' END");
            seed("FAVORITE_CATEGORY", "偏好商品分类", "兴趣偏好", "String", "根据浏览、加购和购买行为综合计算",
                    "user_profile_summary",
                    "CASE WHEN favorite_category_id IS NOT NULL THEN CAST(favorite_category_id AS CHAR) ELSE 'Unknown' END");
            seed("RFM_SEGMENT", "RFM用户分层", "用户价值", "String", "根据最近消费、消费频次和消费金额计算",
                    "ads_user_rfm",
                    "CASE WHEN r_score >= 4 AND f_score >= 4 AND m_score >= 4 THEN 'HIGH_VALUE' WHEN r_score >= 4 AND (f_score >= 3 OR m_score >= 3) THEN 'POTENTIAL' WHEN r_score <= 2 AND (f_score >= 4 OR m_score >= 4) THEN 'AT_RISK' WHEN f_score <= 2 AND m_score <= 2 THEN 'LOW_VALUE' ELSE 'GENERAL' END");
            log.info("标签定义种子数据初始化完成：4 个预设标签");
        } catch (Exception e) {
            log.warn("标签定义种子数据初始化失败（不影响系统启动）: {}", e.getMessage());
        }
    }

    /** 补种：预设标签若已存在但缺少计算配置（升级场景），补齐 source_table/rule_expression */
    private void backfillRules() {
        backfill("ACTIVE_LEVEL", "user_profile_summary",
                "CASE WHEN login_count_30d + browse_count_30d >= 50 THEN 'High' WHEN login_count_30d + browse_count_30d >= 15 THEN 'Medium' ELSE 'Low' END");
        backfill("CONSUMPTION_LEVEL", "user_profile_summary",
                "CASE WHEN total_payment_amount >= 10000 THEN 'High' WHEN total_payment_amount >= 3000 THEN 'Medium' ELSE 'Low' END");
        backfill("FAVORITE_CATEGORY", "user_profile_summary",
                "CASE WHEN favorite_category_id IS NOT NULL THEN CAST(favorite_category_id AS CHAR) ELSE 'Unknown' END");
        backfill("RFM_SEGMENT", "ads_user_rfm",
                "CASE WHEN r_score >= 4 AND f_score >= 4 AND m_score >= 4 THEN 'HIGH_VALUE' WHEN r_score >= 4 AND (f_score >= 3 OR m_score >= 3) THEN 'POTENTIAL' WHEN r_score <= 2 AND (f_score >= 4 OR m_score >= 4) THEN 'AT_RISK' WHEN f_score <= 2 AND m_score <= 2 THEN 'LOW_VALUE' ELSE 'GENERAL' END");
    }

    private void backfill(String code, String sourceTable, String ruleExpression) {
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProfileTagDefinition> uw =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        uw.eq(ProfileTagDefinition::getTagCode, code)
                .and(w -> w.isNull(ProfileTagDefinition::getRuleExpression).or()
                        .eq(ProfileTagDefinition::getRuleExpression, ""))
                .set(ProfileTagDefinition::getSourceTable, sourceTable)
                .set(ProfileTagDefinition::getRuleExpression, ruleExpression);
        int n = mapper.update(null, uw);
        if (n > 0) log.info("标签 [{}] 计算规则已补种", code);
    }

    private void seed(String code, String name, String category, String valueType, String rule,
                      String sourceTable, String ruleExpression) {
        ProfileTagDefinition tag = new ProfileTagDefinition(code, name, category, valueType, rule, 1L);
        tag.setSourceTable(sourceTable);
        tag.setRuleExpression(ruleExpression);
        mapper.insert(tag);
    }
}
