package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.domain.entity.SysNotification;
import com.oufeng.ecommerceuserprofile.domain.mapper.SysNotificationMapper;
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
 * NotificationService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 单元测试")
class NotificationServiceTest {

    @Mock
    private SysNotificationMapper mapper;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("发送通知成功")
    void shouldSendNotification() {
        notificationService.send(1L, "SYSTEM", "测试通知", "这是测试内容", null, null);
        verify(mapper).insert(any(SysNotification.class));
    }

    @Test
    @DisplayName("分页查询用户通知")
    void shouldListByUser() {
        Page<SysNotification> mockPage = new Page<>();
        mockPage.setRecords(java.util.List.of());
        mockPage.setTotal(0);
        when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        Page<SysNotification> result = notificationService.listByUser(1L, 0, 20, null, null);
        assertThat(result).isNotNull();
        verify(mapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询未读通知数")
    void shouldCountUnread() {
        when(mapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        long count = notificationService.unreadCount(1L);
        assertThat(count).isEqualTo(3);
    }
}
