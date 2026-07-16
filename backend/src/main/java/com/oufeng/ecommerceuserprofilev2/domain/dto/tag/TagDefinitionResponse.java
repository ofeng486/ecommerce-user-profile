package com.oufeng.ecommerceuserprofilev2.domain.dto.tag;

/** 标签定义响应。 */
public record TagDefinitionResponse(
        Long id, String tagCode, String tagName, String tagCategory,
        String valueType, String calculationRule, boolean enabled,
        Long createdBy, java.time.Instant createdAt, java.time.Instant updatedAt
) {}
