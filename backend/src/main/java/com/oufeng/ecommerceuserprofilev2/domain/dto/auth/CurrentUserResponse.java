package com.oufeng.ecommerceuserprofilev2.domain.dto.auth;

import com.oufeng.ecommerceuserprofilev2.common.UserRole;

/**
 * 当前登录用户信息。
 */
public record CurrentUserResponse(
        Long userId,
        String username,
        String displayName,
        UserRole role
) {}
