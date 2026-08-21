package com.oufeng.ecommerceuserprofile.domain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 当前用户修改自己的密码请求。 */
public record ChangePasswordRequest(
        @NotBlank(message = "原密码不能为空")
        String oldPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 50, message = "新密码长度需在 8-50 位之间")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,50}$", message = "新密码必须同时包含字母和数字")
        String newPassword) {}