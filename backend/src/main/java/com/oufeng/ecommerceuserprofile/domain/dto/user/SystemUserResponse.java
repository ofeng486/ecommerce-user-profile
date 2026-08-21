package com.oufeng.ecommerceuserprofile.domain.dto.user;

import java.time.LocalDateTime;

/** 系统用户响应。 */
public record SystemUserResponse(
        Long id, String username, String displayName,
        String role, boolean enabled, LocalDateTime lastLoginAt
) {}
