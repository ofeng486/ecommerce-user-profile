package com.oufeng.ecommerceuserprofile.domain.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 重置系统用户密码请求。 */
public record UpdateSystemUserPasswordRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 50, message = "密码长度需在 8-50 位之间")
        String newPassword) {}
