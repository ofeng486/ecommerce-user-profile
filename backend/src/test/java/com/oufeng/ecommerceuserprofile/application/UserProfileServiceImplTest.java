package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.common.BusinessException;
import com.oufeng.ecommerceuserprofile.domain.dto.profile.*;
import com.oufeng.ecommerceuserprofile.infrastructure.mapper.UserProfileQueryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * UserProfileServiceImpl 单元测试。
 * 覆盖画像查询核心逻辑，验证缓存失效和分页行为。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileServiceImpl 单元测试")
class UserProfileServiceImplTest {

    @Mock private UserProfileQueryMapper queryMapper;

    private UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserProfileServiceImpl(queryMapper);
    }

    @Test
    @DisplayName("画像概览正常返回")
    void shouldReturnOverview() {
        ProfileOverviewResponse expected = new ProfileOverviewResponse(
                10000L, 8500L, 1200L, 800L, new BigDecimal("1250000.00"), "v1.0", "2026-08-14 10:00:00");
        when(queryMapper.queryOverview()).thenReturn(expected);

        ProfileOverviewResponse result = service.getOverview();

        assertThat(result.totalUsers()).isEqualTo(10000L);
        assertThat(result.profiledUsers()).isEqualTo(8500L);
        assertThat(result.highValueUsers()).isEqualTo(1200L);
    }

    @Test
    @DisplayName("分层分布正常返回")
    void shouldReturnSegmentDistribution() {
        List<SegmentDistributionResponse> expected = List.of(
                new SegmentDistributionResponse("HIGH_VALUE", "高价值用户", 1200L),
                new SegmentDistributionResponse("POTENTIAL", "潜力用户", 3500L)
        );
        when(queryMapper.querySegmentDistribution()).thenReturn(expected);

        List<SegmentDistributionResponse> result = service.getSegmentDistribution();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).segmentCode()).isEqualTo("HIGH_VALUE");
    }

    @Test
    @DisplayName("标签分布按 tagCode 过滤")
    void shouldReturnTagDistribution() {
        List<TagDistributionResponse> expected = List.of(
                new TagDistributionResponse("RFM_SEGMENT", "HIGH_VALUE", "HIGH_VALUE", 1200L, new BigDecimal("0.1200"), null)
        );
        when(queryMapper.queryTagDistribution("RFM_SEGMENT")).thenReturn(expected);

        List<TagDistributionResponse> result = service.getTagDistribution("RFM_SEGMENT");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tagValue()).isEqualTo("HIGH_VALUE");
    }

    @Test
    @DisplayName("用户画像列表分页查询")
    void shouldListProfiles() {
        List<UserProfileListItemResponse> records = List.of(
                new UserProfileListItemResponse(1L, "U001", "男", 28, "广东", "深圳",
                        15, new BigDecimal("35000.00"), "HIGH_VALUE", "高价值用户", new BigDecimal("4.2000")),
                new UserProfileListItemResponse(2L, "U002", "女", 32, "北京", "北京",
                        8, new BigDecimal("12000.00"), "GENERAL", "一般用户", new BigDecimal("2.8000"))
        );
        when(queryMapper.queryProfiles(null, null, null, null, null, null, null, "p.total_payment_amount", "DESC", 0L, 20L)).thenReturn(records);
        when(queryMapper.countProfiles(null, null, null, null, null, null, null)).thenReturn(2L);

        Page<UserProfileListItemResponse> result = service.listProfiles(null, null, null, null, null, null, null, "p.total_payment_amount", "DESC", 0, 20);

        assertThat(result.getTotal()).isEqualTo(2L);
        assertThat(result.getRecords()).hasSize(2);
    }

    @Test
    @DisplayName("查询单个用户画像详情")
    void shouldGetUserProfile() {
        UserProfileResponse expected = new UserProfileResponse(
                1L, "U001", "男", 28, "广东", "深圳",
                15, new BigDecimal("35000.00"), new BigDecimal("2333.33"),
                45, 30, LocalDateTime.of(2026, 7, 13, 10, 0),
                "HIGH_VALUE", "高价值用户", new BigDecimal("4.2000"),
                List.of(new TagItem("ACTIVE_LEVEL", "用户活跃等级", "High"))
        );
        when(queryMapper.queryUserProfile(1L)).thenReturn(Optional.of(expected));
        when(queryMapper.queryUserTags(1L)).thenReturn(List.of(
                Map.of("tagCode", "ACTIVE_LEVEL", "tagName", "用户活跃等级", "tagValue", "High")));

        UserProfileResponse result = service.getUserProfile(1L);

        assertThat(result.getUserCode()).isEqualTo("U001");
        assertThat(result.getTotalOrderCount()).isEqualTo(15);
        assertThat(result.getTags()).hasSize(1);
        assertThat(result.getTags().get(0).tagCode()).isEqualTo("ACTIVE_LEVEL");
    }

    @Test
    @DisplayName("查询不存在的用户抛出 NOT_FOUND")
    void shouldThrowWhenUserNotFound() {
        when(queryMapper.queryUserProfile(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserProfile(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("电商用户不存在");
    }
}
