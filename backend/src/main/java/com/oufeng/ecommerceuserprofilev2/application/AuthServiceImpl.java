package com.oufeng.ecommerceuserprofilev2.application;

import com.oufeng.ecommerceuserprofilev2.common.ResultCode;
import com.oufeng.ecommerceuserprofilev2.common.UserRole;
import com.oufeng.ecommerceuserprofilev2.domain.converter.SystemUserConverter;
import com.oufeng.ecommerceuserprofilev2.domain.dto.auth.CurrentUserResponse;
import com.oufeng.ecommerceuserprofilev2.domain.dto.auth.LoginRequest;
import com.oufeng.ecommerceuserprofilev2.domain.dto.auth.LoginResponse;
import com.oufeng.ecommerceuserprofilev2.domain.dto.auth.RegisterRequest;
import com.oufeng.ecommerceuserprofilev2.domain.dto.user.SystemUserResponse;
import com.oufeng.ecommerceuserprofilev2.domain.entity.SystemLoginLog;
import com.oufeng.ecommerceuserprofilev2.domain.entity.SystemUser;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.SystemLoginLogMapper;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.SystemUserMapper;
import com.oufeng.ecommerceuserprofilev2.common.BusinessException;
import com.oufeng.ecommerceuserprofilev2.infrastructure.security.AuthenticatedUser;
import com.oufeng.ecommerceuserprofilev2.infrastructure.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 系统认证业务服务。
 */
@Service
public class AuthServiceImpl implements IAuthService {

    private final SystemUserMapper systemUserMapper;
    private final SystemLoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(
            SystemUserMapper systemUserMapper,
            SystemLoginLogMapper loginLogMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.systemUserMapper = systemUserMapper;
        this.loginLogMapper = loginLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** 公开注册普通用户，客户端不能指定角色。 */
    @Transactional
    public SystemUserResponse register(RegisterRequest request) {
        String normalizedUsername = request.username().trim();
        if (systemUserMapper.existsByUsername(normalizedUsername)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
        }
        SystemUser user = new SystemUser(
                normalizedUsername,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                UserRole.USER
        );
        systemUserMapper.insert(user);
        return SystemUserConverter.INSTANCE.toResponse(user);
    }

    /** 校验账号和密码，成功后签发 JWT。 */
    @Transactional
    public LoginResponse login(LoginRequest request, String loginIp, String userAgent) {
        SystemUser user = systemUserMapper.findByUsername(request.username()).orElse(null);
        if (user == null || !user.isEnabled()
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginLogMapper.insert(new SystemLoginLog(
                    user != null ? user.getId() : null, request.username(),
                    loginIp, userAgent, false, "用户名或密码错误"
            ));
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        user.updateLastLoginAt(LocalDateTime.now());
        systemUserMapper.updateById(user);

        loginLogMapper.insert(new SystemLoginLog(
                user.getId(), request.username(), loginIp, userAgent, true, null
        ));

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getRoleEnum());
        String token = jwtTokenProvider.generateToken(authenticatedUser);

        return new LoginResponse(
                token, "Bearer", jwtTokenProvider.getExpirationSeconds(),
                user.getId(), user.getUsername(), user.getDisplayName(), user.getRoleEnum()
        );
    }

    /** 获取当前登录用户信息。 */
    public CurrentUserResponse getCurrentUser(AuthenticatedUser user) {
        return new CurrentUserResponse(user.userId(), user.username(), user.displayName(), user.role());
    }
}
