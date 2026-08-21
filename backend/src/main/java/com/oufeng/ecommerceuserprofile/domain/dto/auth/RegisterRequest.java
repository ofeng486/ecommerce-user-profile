package com.oufeng.ecommerceuserprofile.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 普通用户注册请求。
 * 公开注册只能创建 User 角色，不能由客户端指定角色。
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,50}$", message = "用户名只能包含字母、数字和下划线，长度为4到50位")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度必须为8到64位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$", message = "密码必须同时包含字母和数字")
        String password,

        @NotBlank(message = "显示名称不能为空")
        @Size(max = 50, message = "显示名称不能超过50个字符")
        String displayName
) {}
