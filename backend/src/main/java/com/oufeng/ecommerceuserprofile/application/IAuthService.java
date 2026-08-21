package com.oufeng.ecommerceuserprofile.application;

import com.oufeng.ecommerceuserprofile.domain.dto.auth.ChangePasswordRequest;
import com.oufeng.ecommerceuserprofile.domain.dto.auth.CurrentUserResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.auth.LoginRequest;
import com.oufeng.ecommerceuserprofile.domain.dto.auth.LoginResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.auth.RegisterRequest;
import com.oufeng.ecommerceuserprofile.domain.dto.user.LoginLogResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.user.SystemUserResponse;
import com.oufeng.ecommerceuserprofile.infrastructure.security.AuthenticatedUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 系统认证业务接口。
 */
public interface IAuthService {

    /** 公开注册普通用户，客户端不能指定角色。 */
    SystemUserResponse register(RegisterRequest request);

    /** 校验账号和密码，成功后签发 JWT。 */
    LoginResponse login(LoginRequest request, String loginIp, String userAgent);

    /** 获取当前登录用户信息。 */
    CurrentUserResponse getCurrentUser(AuthenticatedUser user);

    /** 当前用户修改自己的密码（校验原密码）。 */
    void changeOwnPassword(AuthenticatedUser user, ChangePasswordRequest request);

    /** 当前用户修改自己的显示名称。 */
    void changeOwnDisplayName(AuthenticatedUser user, String displayName);

    /** 查询当前用户的登录历史（分页）。 */
    Page<LoginLogResponse> listOwnLoginLogs(AuthenticatedUser user, int page, int size);
}
