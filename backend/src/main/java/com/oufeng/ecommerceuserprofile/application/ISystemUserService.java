package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.domain.dto.user.CreateSystemUserRequest;
import com.oufeng.ecommerceuserprofile.domain.dto.user.LoginLogResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.user.SystemUserResponse;

/**
 * 管理员系统用户与登录日志管理接口。
 */
public interface ISystemUserService {

    /** 分页查询系统用户列表。 */
    Page<SystemUserResponse> listUsers(int page, int size);

    /** 创建系统用户。 */
    SystemUserResponse createUser(CreateSystemUserRequest request);

    /** 更新用户启用/禁用状态。 */
    SystemUserResponse updateStatus(Long userId, boolean enabled);

    /** 更新用户信息（显示名、角色）。 */
    SystemUserResponse updateUser(Long userId, String displayName, String role);

    /** 重置用户密码。 */
    SystemUserResponse resetPassword(Long userId, String newPassword);

    /** 删除系统用户。 */
    void deleteUser(Long userId);

    /** 分页查询登录审计日志。 */
    Page<LoginLogResponse> listLoginLogs(int page, int size);
}
