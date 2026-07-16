package com.oufeng.ecommerceuserprofilev2.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.oufeng.ecommerceuserprofilev2.common.UserRole;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 系统登录用户实体（MyBatis-Plus）。
 * 本实体只表示后台系统账号，不表示画像分析中的电商业务用户。
 */
@TableName("sys_user")
public class SystemUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("display_name")
    private String displayName;

    /**
     * 角色枚举，数据库存储 'User'/'Admin'，
     * 读取时通过自定义 TypeHandler 或显式转换。
     */
    @TableField("role")
    private String role;

    @TableField("status")
    private Byte status;

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    /** MyBatis-Plus 要求的无参构造。 */
    public SystemUser() {}

    public SystemUser(String username, String passwordHash, String displayName, UserRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = convertRole(role);
        this.status = 1;
    }

    /** 将 UserRole 枚举转为数据库存储值 */
    public static String convertRole(UserRole role) {
        return role == UserRole.ADMIN ? "Admin" : "User";
    }

    /** 从数据库值还原为 UserRole 枚举 */
    public UserRole getRoleEnum() {
        return "Admin".equals(role) ? UserRole.ADMIN : UserRole.USER;
    }

    public boolean isEnabled() { return Byte.valueOf((byte) 1).equals(status); }

    public void updateEnabled(boolean enabled) { this.status = enabled ? (byte) 1 : (byte) 0; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public void setRole(String role) { this.role = role; }

    public void setRoleEnum(UserRole role) { this.role = convertRole(role); }

    public void updatePasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public void updateLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    // ─── getters / setters ───

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public Byte getStatus() { return status; }
    public void setStatus(Byte status) { this.status = status; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
