package com.oufeng.ecommerceuserprofilev2.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofilev2.common.ResultCode;
import com.oufeng.ecommerceuserprofilev2.common.UserRole;
import com.oufeng.ecommerceuserprofilev2.domain.converter.LoginLogConverter;
import com.oufeng.ecommerceuserprofilev2.domain.converter.SystemUserConverter;
import com.oufeng.ecommerceuserprofilev2.domain.dto.user.CreateSystemUserRequest;
import com.oufeng.ecommerceuserprofilev2.domain.dto.user.LoginLogResponse;
import com.oufeng.ecommerceuserprofilev2.domain.dto.user.SystemUserResponse;
import com.oufeng.ecommerceuserprofilev2.domain.entity.SystemUser;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.SystemLoginLogMapper;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.SystemUserMapper;
import com.oufeng.ecommerceuserprofilev2.common.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管理员系统用户与登录日志管理服务。
 */
@Service
public class SystemUserServiceImpl implements ISystemUserService {

    private final SystemUserMapper systemUserMapper;
    private final SystemLoginLogMapper loginLogMapper;
    private final PasswordEncoder passwordEncoder;

    public SystemUserServiceImpl(
            SystemUserMapper systemUserMapper,
            SystemLoginLogMapper loginLogMapper,
            PasswordEncoder passwordEncoder) {
        this.systemUserMapper = systemUserMapper;
        this.loginLogMapper = loginLogMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<SystemUserResponse> listUsers(int page, int size) {
        Page<SystemUser> entityPage = systemUserMapper.selectPage(
                new Page<>(page, Math.min(size, 100)),
                new LambdaQueryWrapper<SystemUser>().orderByDesc(SystemUser::getCreatedAt));
        List<SystemUserResponse> records = entityPage.getRecords().stream()
                .map(SystemUserConverter.INSTANCE::toResponse).toList();
        Page<SystemUserResponse> result = new Page<>(page, size, entityPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Transactional
    public SystemUserResponse createUser(CreateSystemUserRequest request) {
        if (systemUserMapper.existsByUsername(request.username())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
        }
        SystemUser user = new SystemUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                UserRole.valueOf(request.role())
        );
        systemUserMapper.insert(user);
        return SystemUserConverter.INSTANCE.toResponse(user);
    }

    @Transactional
    public SystemUserResponse updateStatus(Long userId, boolean enabled) {
        SystemUser user = systemUserMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND, "系统用户不存在");
        user.updateEnabled(enabled);
        systemUserMapper.updateById(user);
        return SystemUserConverter.INSTANCE.toResponse(user);
    }

    @Transactional
    public SystemUserResponse updateUser(Long userId, String displayName, String role) {
        SystemUser user = systemUserMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND, "系统用户不存在");
        if (displayName != null && !displayName.isBlank()) user.setDisplayName(displayName.trim());
        if (role != null && !role.isBlank()) user.setRoleEnum(UserRole.valueOf(role));
        systemUserMapper.updateById(user);
        return SystemUserConverter.INSTANCE.toResponse(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (systemUserMapper.selectById(userId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "系统用户不存在");
        }
        systemUserMapper.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public Page<LoginLogResponse> listLoginLogs(int page, int size) {
        var entityPage = loginLogMapper.selectPage(
                new Page<>(page, Math.min(size, 100)),
                new LambdaQueryWrapper<com.oufeng.ecommerceuserprofilev2.domain.entity.SystemLoginLog>()
                        .orderByDesc(com.oufeng.ecommerceuserprofilev2.domain.entity.SystemLoginLog::getLoginAt));
        List<LoginLogResponse> records = entityPage.getRecords().stream()
                .map(LoginLogConverter.INSTANCE::toResponse).toList();
        Page<LoginLogResponse> result = new Page<>(page, size, entityPage.getTotal());
        result.setRecords(records);
        return result;
    }
}
