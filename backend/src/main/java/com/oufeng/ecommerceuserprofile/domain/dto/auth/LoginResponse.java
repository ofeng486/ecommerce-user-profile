package com.oufeng.ecommerceuserprofile.domain.dto.auth;

import com.oufeng.ecommerceuserprofile.common.UserRole;

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
