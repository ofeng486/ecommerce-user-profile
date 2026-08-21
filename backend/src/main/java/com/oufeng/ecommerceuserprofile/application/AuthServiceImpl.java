package com.oufeng.ecommerceuserprofile.application;

import com.oufeng.ecommerceuserprofile.common.ResultCode;
import com.oufeng.ecommerceuserprofile.common.UserRole;
import com.oufeng.ecommerceuserprofile.domain.converter.LoginLogConverter;
import com.oufeng.ecommerceuserprofile.domain.converter.SystemUserConverter;
import com.oufeng.ecommerceuserprofile.domain.dto.auth.ChangePasswordRequest;
import com.oufeng.ecommerceuserprofile.domain.dto.auth.CurrentUserResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.auth.LoginRequest;
import com.oufeng.ecommerceuserprofile.domain.dto.auth.LoginResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.auth.RegisterRequest;
import com.oufeng.ecommerceuserprofile.domain.dto.user.LoginLogResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.user.SystemUserResponse;
import com.oufeng.ecommerceuserprofile.domain.entity.SystemLoginLog;
import com.oufeng.ecommerceuserprofile.domain.entity.SystemUser;
import com.oufeng.ecommerceuserprofile.domain.mapper.SystemLoginLogMapper;
import com.oufeng.ecommerceuserprofile.domain.mapper.SystemUserMapper;
import com.oufeng.ecommerceuserprofile.common.BusinessException;
import com.oufeng.ecommerceuserprofile.infrastructure.security.AuthenticatedUser;
import com.oufeng.ecommerceuserprofile.infrastructure.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    /** 当前用户修改自己的密码（校验原密码）。 */
    @Transactional
    public void changeOwnPassword(AuthenticatedUser user, ChangePasswordRequest request) {
        SystemUser dbUser = systemUserMapper.selectById(user.userId());
        if (dbUser == null) throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        if (!passwordEncoder.matches(request.oldPassword(), dbUser.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码不正确");
        }
        dbUser.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
        systemUserMapper.updateById(dbUser);
    }

    /** 当前用户修改自己的显示名称 */
    @Transactional
    public void changeOwnDisplayName(AuthenticatedUser user, String displayName) {
        SystemUser dbUser = systemUserMapper.selectById(user.userId());
        if (dbUser == null) throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        dbUser.setDisplayName(displayName);
        systemUserMapper.updateById(dbUser);
    }

    /** 查询当前用户的登录历史（分页）。 */
    public Page<LoginLogResponse> listOwnLoginLogs(AuthenticatedUser user, int page, int size) {
        IPage<SystemLoginLog> entityPage = loginLogMapper.selectPage(
                new Page<>(page, Math.min(size, 50)),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemLoginLog>()
                        .eq(SystemLoginLog::getSysUserId, user.userId())
                        .orderByDesc(SystemLoginLog::getLoginAt)
        );
        List<LoginLogResponse> records = entityPage.getRecords().stream()
                .map(LoginLogConverter.INSTANCE::toResponse).toList();
        Page<LoginLogResponse> result = new Page<>(page, size, entityPage.getTotal());
        result.setRecords(records);
        return result;
    }
}
