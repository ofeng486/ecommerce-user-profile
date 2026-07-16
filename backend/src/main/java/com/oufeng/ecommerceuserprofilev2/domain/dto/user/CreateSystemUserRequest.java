package com.oufeng.ecommerceuserprofilev2.domain.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理员创建系统用户请求。
 */
public record CreateSystemUserRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,50}$", message = "用户名只能包含字母、数字和下划线，长度为4到50位")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度必须为8到64位")
        String password,

        @NotBlank(message = "显示名称不能为空")
        @Size(max = 50, message = "显示名称不能超过50个字符")
        String displayName,

        @NotBlank(message = "角色不能为空")
        @Pattern(regexp = "^(User|Admin)$", message = "角色只能是 User 或 Admin")
        String role
) {}
