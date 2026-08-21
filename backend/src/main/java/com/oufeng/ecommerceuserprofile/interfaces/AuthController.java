package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.common.BusinessException;
import com.oufeng.ecommerceuserprofile.common.Result;
import com.oufeng.ecommerceuserprofile.common.ResultCode;
import com.oufeng.ecommerceuserprofile.domain.dto.auth.*;
import com.oufeng.ecommerceuserprofile.domain.dto.user.LoginLogResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.user.SystemUserResponse;
import com.oufeng.ecommerceuserprofile.infrastructure.security.AuthenticatedUser;
import com.oufeng.ecommerceuserprofile.application.IAuthService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 系统认证 RESTful API。
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "公开注册普通用户")
    @PostMapping("/register")
    public Result<SystemUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "用户名密码登录获取 JWT")
    @PostMapping("/login")
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, resolveClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")));
    }

    @Operation(summary = "获取当前 JWT 对应的用户信息")
    @GetMapping("/me")
    public Result<CurrentUserResponse> currentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(authService.getCurrentUser(user));
    }

    @Operation(summary = "退出登录（客户端删除令牌）")
    @PostMapping("/logout")
    public Result<Void> logout() { return Result.success(); }

    @Operation(summary = "当前用户修改自己的密码")
    @PatchMapping("/me/password")
    public Result<Void> changeOwnPassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changeOwnPassword(user, request);
        return Result.success();
    }

    @Operation(summary = "当前用户修改自己的显示名称")
    @PatchMapping("/me/display-name")
    public Result<Void> changeOwnDisplayName(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> body) {
        String name = body.getOrDefault("displayName", "").trim();
        if (name.isEmpty()) throw new BusinessException(ResultCode.BAD_REQUEST, "显示名称不能为空");
        if (name.length() > 50) throw new BusinessException(ResultCode.BAD_REQUEST, "显示名称不能超过 50 字符");
        authService.changeOwnDisplayName(user, name);
        return Result.success();
    }

    @Operation(summary = "查询当前用户的登录历史")
    @GetMapping("/me/login-logs")
    public Result<Page<LoginLogResponse>> listOwnLoginLogs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(authService.listOwnLoginLogs(user, page, size));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
