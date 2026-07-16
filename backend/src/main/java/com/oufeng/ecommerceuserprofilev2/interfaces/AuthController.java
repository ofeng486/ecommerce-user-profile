package com.oufeng.ecommerceuserprofilev2.interfaces;

import com.oufeng.ecommerceuserprofilev2.common.Result;
import com.oufeng.ecommerceuserprofilev2.domain.dto.auth.*;
import com.oufeng.ecommerceuserprofilev2.domain.dto.user.SystemUserResponse;
import com.oufeng.ecommerceuserprofilev2.infrastructure.security.AuthenticatedUser;
import com.oufeng.ecommerceuserprofilev2.application.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
