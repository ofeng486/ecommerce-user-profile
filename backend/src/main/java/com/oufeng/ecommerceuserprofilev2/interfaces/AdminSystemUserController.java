package com.oufeng.ecommerceuserprofilev2.interfaces;

import com.oufeng.ecommerceuserprofilev2.common.Result;
import com.oufeng.ecommerceuserprofilev2.domain.dto.user.*;
import com.oufeng.ecommerceuserprofilev2.application.ISystemUserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员系统用户与登录审计 RESTful API。
 */
@Tag(name = "系统用户管理")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminSystemUserController {

    private final ISystemUserService systemUserService;

    public AdminSystemUserController(ISystemUserService systemUserService) {
        this.systemUserService = systemUserService;
    }

    @Operation(summary = "分页查询系统用户")
    @GetMapping("/users")
    public Result<Page<SystemUserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(systemUserService.listUsers(page, Math.min(size, 100)));
    }

    @Operation(summary = "创建系统用户")
    @PostMapping("/users")
    public Result<SystemUserResponse> createUser(@Valid @RequestBody CreateSystemUserRequest request) {
        return Result.success(systemUserService.createUser(request));
    }

    @Operation(summary = "更新系统用户信息")
    @PutMapping("/users/{userId}")
    public Result<SystemUserResponse> updateUser(
            @PathVariable Long userId, @RequestBody Map<String, String> body) {
        return Result.success(systemUserService.updateUser(userId, body.get("displayName"), body.get("role")));
    }

    @Operation(summary = "删除系统用户")
    @DeleteMapping("/users/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        systemUserService.deleteUser(userId);
        return Result.success(null);
    }

    @Operation(summary = "更新用户启用状态")
    @PatchMapping("/users/{userId}/status")
    public Result<SystemUserResponse> updateStatus(
            @PathVariable Long userId, @Valid @RequestBody UpdateSystemUserStatusRequest request) {
        return Result.success(systemUserService.updateStatus(userId, request.enabled()));
    }

    @Operation(summary = "查询登录审计日志")
    @GetMapping("/login-logs")
    public Result<Page<LoginLogResponse>> listLoginLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(systemUserService.listLoginLogs(page, Math.min(size, 100)));
    }
}
