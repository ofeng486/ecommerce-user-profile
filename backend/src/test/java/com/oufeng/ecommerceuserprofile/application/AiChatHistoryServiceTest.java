package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oufeng.ecommerceuserprofile.domain.entity.AiChatHistory;
import com.oufeng.ecommerceuserprofile.domain.mapper.AiChatHistoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AiChatHistoryService 单元测试：保存兜底、越权删除校验。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatHistoryService 单元测试")
class AiChatHistoryServiceTest {

    @Mock
    private AiChatHistoryMapper mapper;

    @InjectMocks
    private AiChatHistoryService service;

    @Test
    @DisplayName("空 userId 不保存")
    void shouldSkipWhenUserIdNull() {
        service.save(null, "问题", "回答", null);
        verify(mapper, never()).insert(any(AiChatHistory.class));
    }

    @Test
    @DisplayName("空白问题不保存")
    void shouldSkipWhenQuestionBlank() {
        service.save(1L, "  ", "回答", null);
        verify(mapper, never()).insert(any(AiChatHistory.class));
    }

    @Test
    @DisplayName("正常保存，null answer/dataJson 兜底为空串/null")
    void shouldSaveWithFallback() {
        service.save(1L, "问题", null, null);
        verify(mapper).insert(any(AiChatHistory.class));
    }

    @Test
    @DisplayName("删除时按 id+userId 双条件校验归属")
    void shouldDeleteWithOwnerCheck() {
        when(mapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        assertThat(service.deleteById(10L, 2L)).isTrue();
        verify(mapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("他人记录（归属不匹配）删除返回 false")
    void shouldNotDeleteOthers() {
        when(mapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
        assertThat(service.deleteById(10L, 99L)).isFalse();
    }

    @Test
    @DisplayName("清空返回受影响行数")
    void shouldClearByUser() {
        when(mapper.delete(any(LambdaQueryWrapper.class))).thenReturn(7);
        assertThat(service.clearByUser(1L)).isEqualTo(7);
    }
}
