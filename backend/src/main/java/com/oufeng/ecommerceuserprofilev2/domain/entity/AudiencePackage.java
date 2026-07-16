package com.oufeng.ecommerceuserprofilev2.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人群包实体。
 * 管理员基于圈选条件保存的用户分组。
 */
@Data
@TableName("audience_package")
public class AudiencePackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("package_name")
    private String packageName;

    @TableField("description")
    private String description;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("status")
    private Integer status;

    @TableField("created_by")
    private Long createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
