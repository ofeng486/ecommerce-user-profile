package com.oufeng.ecommerceuserprofilev2.common;

/**
 * 系统角色枚举。
 * 角色值与 Spring Security 的 ROLE_ 权限前缀保持一致。
 */
public enum UserRole {

    /** 普通用户：可以查看公开数据和本人可访问的画像分析结果。 */
    USER,

    /** 管理员：可以管理用户、标签、分析任务和系统配置。 */
    ADMIN
}
