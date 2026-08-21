package com.oufeng.ecommerceuserprofile.domain.dto.tag;

/** 标签定义响应。 */
public record TagDefinitionResponse(
        Long id, String tagCode, String tagName, String tagCategory,
        String valueType, String calculationRule, String sourceTable, String ruleExpression, boolean enabled,
        Long createdBy, java.time.Instant createdAt, java.time.Instant updatedAt
) {}
