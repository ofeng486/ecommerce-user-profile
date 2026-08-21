package com.oufeng.ecommerceuserprofile.domain.dto.user;

import jakarta.validation.constraints.NotNull;

/** 更新系统用户状态请求。 */
public record UpdateSystemUserStatusRequest(@NotNull(message = "状态不能为空") Boolean enabled) {}
