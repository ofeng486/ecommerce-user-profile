package com.oufeng.ecommerceuserprofilev2.application;

import com.oufeng.ecommerceuserprofilev2.domain.dto.auth.CurrentUserResponse;
import com.oufeng.ecommerceuserprofilev2.domain.dto.auth.LoginRequest;
import com.oufeng.ecommerceuserprofilev2.domain.dto.auth.LoginResponse;
import com.oufeng.ecommerceuserprofilev2.domain.dto.auth.RegisterRequest;
import com.oufeng.ecommerceuserprofilev2.domain.dto.user.SystemUserResponse;
import com.oufeng.ecommerceuserprofilev2.infrastructure.security.AuthenticatedUser;

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
}
