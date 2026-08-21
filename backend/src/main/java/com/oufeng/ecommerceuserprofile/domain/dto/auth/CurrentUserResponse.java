package com.oufeng.ecommerceuserprofile.domain.dto.auth;

import com.oufeng.ecommerceuserprofile.common.UserRole;

/**
 * 当前登录用户信息。
 */
public record CurrentUserResponse(
        Long userId,
        String username,
        String displayName,
        UserRole role
) {}
