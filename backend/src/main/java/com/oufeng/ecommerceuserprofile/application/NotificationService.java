package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.common.BusinessException;
import com.oufeng.ecommerceuserprofile.common.ResultCode;
import com.oufeng.ecommerceuserprofile.domain.entity.SysNotification;
import com.oufeng.ecommerceuserprofile.domain.entity.SystemUser;
import com.oufeng.ecommerceuserprofile.domain.mapper.SysNotificationMapper;
import com.oufeng.ecommerceuserprofile.domain.mapper.SystemUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final SysNotificationMapper mapper;
    private final SystemUserMapper userMapper;

    public NotificationService(SysNotificationMapper mapper, SystemUserMapper userMapper) {
        this.mapper = mapper;
        this.userMapper = userMapper;
    }

    /** 发送通知 */
    @Transactional
    public void send(Long userId, String type, String title, String content, String refType, Long refId) {
        mapper.insert(new SysNotification(userId, type, title, content, refType, refId));
    }

    /** 广播给所有启用用户（任务结果、数据更新、平台公告等全员通知） */
    @Transactional
    public void broadcast(String type, String title, String content, String refType, Long refId) {
        List<SystemUser> users = userMapper.selectList(new LambdaQueryWrapper<SystemUser>()
                .eq(SystemUser::getStatus, (byte) 1));
        if (users.isEmpty()) return;
        for (SystemUser u : users) {
            mapper.insert(new SysNotification(u.getId(), type, title, content, refType, refId));
        }
    }

    /** 分页查当前用户通知（type 可选前缀匹配：TASK → TASK_SUCCESS/TASK_FAILED 等；unread 可选只看未读） */
    public Page<SysNotification> listByUser(Long userId, int page, int size, String type, Boolean unread) {
        return mapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .likeRight(type != null && !type.isBlank(), SysNotification::getType, type)
                        .eq(unread != null && unread, SysNotification::getIsRead, false)
                        .orderByDesc(SysNotification::getCreatedAt));
    }

    /** 未读数量 */
    public long unreadCount(Long userId) {
        return mapper.selectCount(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, false));
    }

    /** 单条标已读（校验归属：仅本人通知可操作） */
    @Transactional
    public void markRead(Long id, Long userId) {
        int updated = mapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getId, id)
                .eq(SysNotification::getUserId, userId)
                .set(SysNotification::getIsRead, true));
        if (updated == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在或无权操作");
        }
    }

    /** 全部标已读 */
    @Transactional
    public void markAllRead(Long userId) {
        mapper.update(null, new LambdaUpdateWrapper<SysNotification>()
                .eq(SysNotification::getUserId, userId).set(SysNotification::getIsRead, true));
    }

    /** 删除单条通知（校验归属：仅本人通知可操作） */
    @Transactional
    public void deleteOne(Long id, Long userId) {
        int deleted = mapper.delete(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getId, id)
                .eq(SysNotification::getUserId, userId));
        if (deleted == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在或无权操作");
        }
    }
}
