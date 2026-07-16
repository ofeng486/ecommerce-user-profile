package com.oufeng.ecommerceuserprofilev2.domain.dto.user;

import java.time.LocalDateTime;

/** 登录日志响应。 */
public record LoginLogResponse(
        Long id, Long sysUserId, String username, String loginIp,
        String userAgent, Byte loginResult, String failureReason, LocalDateTime loginAt
) {}
