package com.oufeng.ecommerceuserprofile.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 系统登录审计日志实体（MyBatis-Plus）。
 */
@TableName("sys_login_log")
public class SystemLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("sys_user_id")
    private Long sysUserId;

    @TableField("username")
    private String username;

    @TableField("login_ip")
    private String loginIp;

    @TableField("user_agent")
    private String userAgent;

    @TableField("login_result")
    private Byte loginResult;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("login_at")
    private LocalDateTime loginAt;

    public SystemLoginLog() {}

    public SystemLoginLog(Long sysUserId, String username, String loginIp,
                           String userAgent, boolean success, String failureReason) {
        this.sysUserId = sysUserId;
        this.username = username;
        this.loginIp = loginIp;
        this.userAgent = userAgent;
        this.loginResult = success ? (byte) 1 : (byte) 0;
        this.failureReason = failureReason;
        this.loginAt = LocalDateTime.now();
    }

    // ─── getters / setters ───
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSysUserId() { return sysUserId; }
    public void setSysUserId(Long sysUserId) { this.sysUserId = sysUserId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getLoginIp() { return loginIp; }
    public void setLoginIp(String loginIp) { this.loginIp = loginIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Byte getLoginResult() { return loginResult; }
    public void setLoginResult(Byte loginResult) { this.loginResult = loginResult; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getLoginAt() { return loginAt; }
    public void setLoginAt(LocalDateTime loginAt) { this.loginAt = loginAt; }
}
