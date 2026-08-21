package com.oufeng.ecommerceuserprofile.infrastructure.security;

import com.oufeng.ecommerceuserprofile.common.UserRole;

/**
 * JWT 认证后保存到 Spring Security 上下文中的用户身份。
 */
public record AuthenticatedUser(
        Long userId,
        String username,
        String displayName,
        UserRole role
) {}
