package com.oufeng.ecommerceuserprofile.domain.dto.audience;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 人群包列表项（带圈选规则）。
 */
public record PackageWithRulesVO(
        Long id,
        String packageName,
        String description,
        Integer totalCount,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<RuleVO> rules
) {
    /** 单条圈选规则（与 audience_rule 表字段对应）。 */
    public record RuleVO(
            String field,
            String operator,
            String value,
            String logicOp
    ) {}
}
