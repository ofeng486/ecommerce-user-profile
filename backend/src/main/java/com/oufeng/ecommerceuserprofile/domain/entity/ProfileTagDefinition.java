package com.oufeng.ecommerceuserprofile.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.oufeng.ecommerceuserprofile.common.BaseEntity;

import java.time.Instant;

/**
 * 用户画像标签定义实体（MyBatis-Plus）。
 */
@TableName("profile_tag_definition")
public class ProfileTagDefinition extends BaseEntity {

    @TableField("tag_code")
    private String tagCode;

    @TableField("tag_name")
    private String tagName;

    @TableField("tag_category")
    private String tagCategory;

    @TableField("value_type")
    private String valueType;

    @TableField("calculation_rule")
    private String calculationRule;

    @TableField("source_table")
    private String sourceTable;

    @TableField("rule_expression")
    private String ruleExpression;

    @TableField("status")
    private Byte status = 1;

    @TableField("created_by")
    private Long createdBy;

    public ProfileTagDefinition() {}

    public ProfileTagDefinition(String tagCode, String tagName, String tagCategory,
                                 String valueType, String calculationRule, Long createdBy) {
        this.tagCode = tagCode;
        this.tagName = tagName;
        this.tagCategory = tagCategory;
        this.valueType = valueType;
        this.calculationRule = calculationRule;
        this.createdBy = createdBy;
    }

    public void update(String tagName, String tagCategory, String valueType, String calculationRule) {
        this.tagName = tagName;
        this.tagCategory = tagCategory;
        this.valueType = valueType;
        this.calculationRule = calculationRule;
    }

    public void updateStatus(boolean enabled) { this.status = enabled ? (byte) 1 : (byte) 0; }

    public boolean isEnabled() { return Byte.valueOf((byte) 1).equals(status); }

    // ─── getters / setters ───
    public String getTagCode() { return tagCode; }
    public void setTagCode(String tagCode) { this.tagCode = tagCode; }
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
    public String getTagCategory() { return tagCategory; }
    public void setTagCategory(String tagCategory) { this.tagCategory = tagCategory; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public String getCalculationRule() { return calculationRule; }
    public void setCalculationRule(String calculationRule) { this.calculationRule = calculationRule; }
    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
    public String getRuleExpression() { return ruleExpression; }
    public void setRuleExpression(String ruleExpression) { this.ruleExpression = ruleExpression; }
    public Byte getStatus() { return status; }
    public void setStatus(Byte status) { this.status = status; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
