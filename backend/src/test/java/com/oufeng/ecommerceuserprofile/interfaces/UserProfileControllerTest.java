package com.oufeng.ecommerceuserprofile.interfaces;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oufeng.ecommerceuserprofile.application.IAuthService;
import com.oufeng.ecommerceuserprofile.application.IUserProfileService;
import com.oufeng.ecommerceuserprofile.common.ResultCode;
import com.oufeng.ecommerceuserprofile.domain.dto.profile.ProfileOverviewResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.profile.UserProfileListItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserProfileController 单元测试（standalone MockMvc）。
 * 验证画像概览与分页列表的响应结构。
 */
@DisplayName("UserProfileController 单元测试")
class UserProfileControllerTest {

    private final IUserProfileService profileService = mock(IUserProfileService.class);
    private final IAuthService authService = mock(IAuthService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new UserProfileController(profileService))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("画像概览 — 返回统计字段")
    void shouldReturnOverview() throws Exception {
        ProfileOverviewResponse overview = new ProfileOverviewResponse(100L, 80L, 12L, 5L, new BigDecimal("350000.00"), "v1.0", "2026-08-14 10:00:00");
        when(profileService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/v1/profiles/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.totalUsers").value(100))
                .andExpect(jsonPath("$.data.highValueUsers").value(12));
    }

    @Test
    @DisplayName("画像列表 — 返回分页 records/total")
    void shouldReturnProfileList() throws Exception {
        Page<UserProfileListItemResponse> page = new Page<>(0, 20, 1);
        page.setRecords(List.of(new UserProfileListItemResponse(
                1L, "U001", "男", 28, "广东", "深圳", 15,
                new BigDecimal("35000.00"), "HIGH_VALUE", "高价值用户", new BigDecimal("4.2000"))));
        when(profileService.listProfiles(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/profiles/users").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].userCode").value("U001"));
    }
}
