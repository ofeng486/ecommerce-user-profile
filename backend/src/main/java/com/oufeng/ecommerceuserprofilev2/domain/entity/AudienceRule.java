package com.oufeng.ecommerceuserprofilev2.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 人群包圈选规则实体。
 * 每个人群包可有多条规则，支持 AND/OR 组合。
 */
@Data
@TableName("audience_rule")
public class AudienceRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("package_id")
    private Long packageId;

    @TableField("rule_group")
    private String ruleGroup;

    @TableField("field_name")
    private String fieldName;

    @TableField("operator")
    private String operator;

    @TableField("value")
    private String value;

    @TableField("logic_op")
    private String logicOp;

    @TableField("sort_order")
    private Integer sortOrder;
}
