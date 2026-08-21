package com.oufeng.ecommerceuserprofile.domain.dto.user;

import java.time.LocalDateTime;

/** 登录日志响应。<br>
 * loginResult: 1=成功, 0=失败（参考 sys_login_log 表定义） */
public record LoginLogResponse(
        Long id, Long sysUserId, String username, String loginIp,
        String userAgent, Byte loginResult, String failureReason, LocalDateTime loginAt
) {}
