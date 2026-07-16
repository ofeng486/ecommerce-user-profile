package com.oufeng.ecommerceuserprofilev2.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofilev2.domain.entity.SysNotification;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.SysNotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final SysNotificationMapper mapper;

    public NotificationService(SysNotificationMapper mapper) { this.mapper = mapper; }

    /** 发送通知 */
    @Transactional
    public void send(Long userId, String type, String title, String content, String refType, Long refId) {
        mapper.insert(new SysNotification(userId, type, title, content, refType, refId));
    }

    /** 分页查当前用户通知 */
    public Page<SysNotification> listByUser(Long userId, int page, int size) {
        return mapper.selectPage(new Page<>(page + 1, size),
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .orderByDesc(SysNotification::getCreatedAt));
    }

    /** 未读数量 */
    public long unreadCount(Long userId) {
        return mapper.selectCount(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, false));
    }

    /** 单条标已读 */
    @Transactional
    public void markRead(Long id) {
        mapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getId, id).set(SysNotification::getIsRead, true));
    }

    /** 全部标已读 */
    @Transactional
    public void markAllRead(Long userId) {
        mapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId).set(SysNotification::getIsRead, true));
    }
}
