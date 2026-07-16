package com.oufeng.ecommerceuserprofilev2.application;

import com.oufeng.ecommerceuserprofilev2.common.BusinessException;
import com.oufeng.ecommerceuserprofilev2.common.UserRole;
import com.oufeng.ecommerceuserprofilev2.domain.dto.auth.LoginRequest;
import com.oufeng.ecommerceuserprofilev2.domain.dto.auth.RegisterRequest;
import com.oufeng.ecommerceuserprofilev2.domain.entity.SystemUser;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.SystemLoginLogMapper;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.SystemUserMapper;
import com.oufeng.ecommerceuserprofilev2.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthServiceImpl 单元测试。
 * 覆盖注册、登录成功/失败、密码验证等核心逻辑。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl 单元测试")
class AuthServiceImplTest {

    @Mock private SystemUserMapper systemUserMapper;
    @Mock private SystemLoginLogMapper loginLogMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @Nested
    @DisplayName("注册")
    class Register {

        @Test
        @DisplayName("正常注册成功")
        void shouldRegisterSuccessfully() {
            RegisterRequest request = new RegisterRequest("testuser", "password123", "测试用户");
            when(systemUserMapper.existsByUsername("testuser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");

            var response = authService.register(request);

            assertThat(response.username()).isEqualTo("testuser");
            assertThat(response.displayName()).isEqualTo("测试用户");
            assertThat(response.role()).isEqualTo("USER");
        }

        @Test
        @DisplayName("用户名已存在则抛出异常")
        void shouldThrowWhenUsernameExists() {
            RegisterRequest request = new RegisterRequest("existing", "password123", "重复用户");
            when(systemUserMapper.existsByUsername("existing")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名已存在");
        }

        @Test
        @DisplayName("用户名前后空格被 trim")
        void shouldTrimUsername() {
            RegisterRequest request = new RegisterRequest("  spaced  ", "password123", "空格用户");
            when(systemUserMapper.existsByUsername("spaced")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            var response = authService.register(request);
            assertThat(response.username()).isEqualTo("spaced");
        }
    }

    @Nested
    @DisplayName("登录")
    class Login {

        private static final String IP = "127.0.0.1";
        private static final String UA = "Mozilla/5.0";

        @Test
        @DisplayName("正常登录成功返回 JWT")
        void shouldLoginSuccessfully() {
            LoginRequest request = new LoginRequest("admin", "correct");
            SystemUser user = new SystemUser("admin", "$2a$10$hash", "管理员", UserRole.ADMIN);
            user.setId(1L);

            when(systemUserMapper.findByUsername("admin")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("correct", "$2a$10$hash")).thenReturn(true);
            when(jwtTokenProvider.getExpirationSeconds()).thenReturn(7200L);
            when(jwtTokenProvider.generateToken(any())).thenReturn("jwt.token.here");

            var response = authService.login(request, IP, UA);

            assertThat(response.accessToken()).isEqualTo("jwt.token.here");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.role()).isEqualTo(UserRole.ADMIN);
            assertThat(response.username()).isEqualTo("admin");
        }

        @Test
        @DisplayName("密码错误抛出 UNAUTHORIZED")
        void shouldThrowWhenPasswordWrong() {
            LoginRequest request = new LoginRequest("user", "wrong");
            SystemUser user = new SystemUser("user", "$2a$10$hash", "用户", UserRole.USER);
            user.setId(2L);

            when(systemUserMapper.findByUsername("user")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request, IP, UA))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名或密码错误");
        }

        @Test
        @DisplayName("用户不存在抛出 UNAUTHORIZED")
        void shouldThrowWhenUserNotFound() {
            LoginRequest request = new LoginRequest("nobody", "any");
            when(systemUserMapper.findByUsername("nobody")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request, IP, UA))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名或密码错误");
        }

        @Test
        @DisplayName("用户被禁用抛出 UNAUTHORIZED")
        void shouldThrowWhenUserDisabled() {
            LoginRequest request = new LoginRequest("disabled", "pass");
            SystemUser user = new SystemUser("disabled", "hash", "已禁用", UserRole.USER);
            user.setId(3L);
            user.updateEnabled(false);

            when(systemUserMapper.findByUsername("disabled")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(request, IP, UA))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名或密码错误");
        }
    }
}
