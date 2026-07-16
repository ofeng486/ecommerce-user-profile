package com.oufeng.ecommerceuserprofilev2.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求参数。
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 50, message = "用户名长度不能超过50个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(max = 100, message = "密码长度不能超过100个字符")
        String password
) {}
