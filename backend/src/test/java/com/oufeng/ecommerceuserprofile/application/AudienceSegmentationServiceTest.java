package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.domain.dto.profile.UserProfileListItemResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.segmentation.ConditionDTO;
import com.oufeng.ecommerceuserprofile.infrastructure.mapper.UserProfileQueryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AudienceSegmentationService 单元测试：空条件走全量、between/in 条件构建、逻辑归一化。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AudienceSegmentationService 单元测试")
class AudienceSegmentationServiceTest {

    @Mock
    private UserProfileQueryMapper queryMapper;

    @InjectMocks
    private AudienceSegmentationService service;

    private static UserProfileListItemResponse sampleUser(long id, String code) {
        return new UserProfileListItemResponse(id, code, "男", 28, "广东", "深圳",
                15, new BigDecimal("35000.00"), "HIGH_VALUE", "高价值用户", new BigDecimal("4.2"));
    }

    @Test
    @DisplayName("空条件：走全量分页，不触发圈选查询")
    void shouldUseAllUsersWhenNoConditions() {
        Page<UserProfileListItemResponse> p = new Page<>(0, 20, 3L);
        p.setRecords(List.of(sampleUser(1L, "U1")));
        when(queryMapper.countAllUsers()).thenReturn(3L);
        when(queryMapper.queryAllProfilesPaged(0L, 20)).thenReturn(p.getRecords());

        Page<UserProfileListItemResponse> result = service.segmentUsers(null, "AND", 0, 20);

        assertThat(result.getTotal()).isEqualTo(3L);
        verify(queryMapper, never()).queryAudience(anyList(), anyString(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("between 条件：valueFrom/valueTo 正确构建")
    void shouldBuildBetweenCondition() {
        List<ConditionDTO> conds = List.of(new ConditionDTO("age", "between", List.of(18, 30), "AND"));
        lenient().when(queryMapper.countAudience(anyList(), anyString())).thenReturn(2L);
        when(queryMapper.queryAudience(anyList(), anyString(), anyLong(), anyLong())).thenReturn(List.of());

        service.segmentUsers(conds, "AND", 0, 20);

        verify(queryMapper).queryAudience(argThat(list -> {
            Map<?, ?> m = list.get(0);
            return Integer.valueOf(18).equals(m.get("valueFrom"))
                    && Integer.valueOf(30).equals(m.get("valueTo"));
        }), eq("AND"), eq(0L), eq(20L));
    }

    @Test
    @DisplayName("in 条件 JSON 字符串：解析为 valueList")
    void shouldParseJsonInCondition() {
        List<ConditionDTO> conds = List.of(new ConditionDTO("province", "in", "[\"广东\",\"浙江\"]", "AND"));
        lenient().when(queryMapper.countAudience(anyList(), anyString())).thenReturn(0L);
        when(queryMapper.queryAudience(anyList(), anyString(), anyLong(), anyLong())).thenReturn(List.of());

        service.segmentUsers(conds, "AND", 0, 20);

        verify(queryMapper).queryAudience(argThat(list -> {
            Object vl = list.get(0).get("valueList");
            return vl instanceof List<?> l && l.size() == 2;
        }), eq("AND"), eq(0L), eq(20L));
    }

    @Test
    @DisplayName("resolveLogic：OR 归一化，其他一律 AND")
    void shouldNormalizeLogic() {
        List<ConditionDTO> conds = List.of(new ConditionDTO("age", "gt", "25", ""));
        lenient().when(queryMapper.countAudience(anyList(), anyString())).thenReturn(1L);
        when(queryMapper.queryAudience(anyList(), anyString(), anyLong(), anyLong())).thenReturn(List.of());

        service.segmentUsers(conds, "or", 0, 20);   // 小写 or 也应归一化

        verify(queryMapper).queryAudience(anyList(), eq("OR"), eq(0L), eq(20L));
    }

    @Test
    @DisplayName("estimateCount：空条件走全量计数")
    void shouldEstimateAllWhenEmpty() {
        when(queryMapper.countAllUsers()).thenReturn(88L);
        assertThat(service.estimateCount(null, "AND")).isEqualTo(88L);
    }
}
