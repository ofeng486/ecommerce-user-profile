package com.oufeng.ecommerceuserprofilev2.domain.dto.segmentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 人群圈选条件 DTO。
 * 前端传递的单个圈选条件，支持 AND/OR 组合。
 *
 * @param field    字段名（如 gender、age、province、segment_code、tag_value、
 *                 total_payment_amount、browse_count_30d 等）
 * @param operator 运算符（eq、neq、gt、gte、lt、lte、between、in、contains、not_contains）
 * @param value    条件值（String / Number / List，由 Service 按 field 类型解析）
 */
public record ConditionDTO(
        @NotBlank(message = "圈选字段不能为空") String field,
        @NotBlank(message = "运算符不能为空") String operator,
        @NotNull(message = "条件值不能为空") Object value
) {}
