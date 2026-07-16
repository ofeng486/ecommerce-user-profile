package com.oufeng.ecommerceuserprofilev2.domain.dto.auth;

import com.oufeng.ecommerceuserprofilev2.common.UserRole;

/**
 * 登录成功响应。
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String username,
        String displayName,
        UserRole role
) {}
