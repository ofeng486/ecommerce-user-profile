package com.oufeng.ecommerceuserprofilev2.interfaces;

import com.oufeng.ecommerceuserprofilev2.application.NotificationService;
import com.oufeng.ecommerceuserprofilev2.common.Result;
import com.oufeng.ecommerceuserprofilev2.domain.entity.SysNotification;
import com.oufeng.ecommerceuserprofilev2.infrastructure.security.AuthenticatedUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "系统通知")
@RestController
@RequestMapping("/api/v1/notifications")
public class SysNotificationController {

    private final NotificationService service;

    public SysNotificationController(NotificationService service) { this.service = service; }

    @Operation(summary = "分页查通知")
    @GetMapping
    public Result<Page<SysNotification>> list(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(service.listByUser(user.userId(), page, size));
    }

    @Operation(summary = "未读数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(service.unreadCount(user.userId()));
    }

    @Operation(summary = "单条标已读")
    @PatchMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        service.markRead(id);
        return Result.success(null);
    }

    @Operation(summary = "全部标已读")
    @PatchMapping("/read-all")
    public Result<Void> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        service.markAllRead(user.userId());
        return Result.success(null);
    }
}
