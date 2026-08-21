package com.oufeng.ecommerceuserprofile.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 人群包指定用户明细：画像列表批量选人创建的人群包成员。 */
@Data
@TableName("audience_package_users")
public class AudiencePackageUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("package_id")
    private Long packageId;

    @TableField("user_id")
    private Long userId;

    @TableField(value = "created_at", insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
