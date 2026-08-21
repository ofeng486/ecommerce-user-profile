package com.oufeng.ecommerceuserprofile.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oufeng.ecommerceuserprofile.application.IAuthService;
import com.oufeng.ecommerceuserprofile.common.ResultCode;
import com.oufeng.ecommerceuserprofile.common.UserRole;
import com.oufeng.ecommerceuserprofile.domain.dto.auth.LoginRequest;
import com.oufeng.ecommerceuserprofile.domain.dto.auth.LoginResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 单元测试（standalone MockMvc，不加载 Spring 上下文）。
 * 验证登录成功返回 JWT 结构、失败返回错误码。
 */
@DisplayName("AuthController 单元测试")
class AuthControllerTest {

    private final IAuthService authService = mock(IAuthService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthController(authService))
            .setControllerAdvice(new com.oufeng.ecommerceuserprofile.common.GlobalExceptionHandler())
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("登录成功 — 返回 200 与 JWT 结构")
    void shouldLoginSuccess() throws Exception {
        LoginResponse resp = new LoginResponse(
                "jwt-token-abc", "Bearer", 7200L,
                1L, "admin", "管理员", UserRole.ADMIN);
        when(authService.login(any(LoginRequest.class), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(new LoginRequest("admin", "Admin@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token-abc"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("登录失败 — 返回业务错误码而非 200")
    void shouldReturnBusinessErrorOnBadCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class), any(), any()))
                .thenThrow(new com.oufeng.ecommerceuserprofile.common.BusinessException(
                        ResultCode.UNAUTHORIZED, "用户名或密码错误"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(new LoginRequest("admin", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ResultCode.UNAUTHORIZED.getCode()));
    }
}
