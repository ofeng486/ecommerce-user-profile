package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.application.NotificationService;
import com.oufeng.ecommerceuserprofile.common.BusinessException;
import com.oufeng.ecommerceuserprofile.common.Result;
import com.oufeng.ecommerceuserprofile.common.ResultCode;
import com.oufeng.ecommerceuserprofile.domain.entity.SysNotification;
import com.oufeng.ecommerceuserprofile.infrastructure.security.AuthenticatedUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Operation(summary = "分页查通知（type 可选前缀过滤：TASK/AI/DATA/SYSTEM；unread 可选只看未读）")
    @GetMapping
    public Result<Page<SysNotification>> list(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type, @RequestParam(required = false) Boolean unread) {
        return Result.success(service.listByUser(user.userId(), page, size, type, unread));
    }

    @Operation(summary = "未读数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(service.unreadCount(user.userId()));
    }

    @Operation(summary = "单条标已读")
    @PatchMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        service.markRead(id, user.userId());
        return Result.success(null);
    }

    @Operation(summary = "全部标已读")
    @PatchMapping("/read-all")
    public Result<Void> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        service.markAllRead(user.userId());
        return Result.success(null);
    }

    @Operation(summary = "删除单条通知（仅本人）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteOne(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        service.deleteOne(id, user.userId());
        return Result.success(null);
    }

    @Operation(summary = "发布平台公告（管理员）：广播 SYSTEM 通知给所有启用用户")
    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> broadcast(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "平台公告");
        String content = body.getOrDefault("content", "");
        if (content.isBlank()) throw new BusinessException(ResultCode.BAD_REQUEST, "公告内容不能为空");
        service.broadcast("SYSTEM", title, content, "SYSTEM", null);
        return Result.success(null);
    }
}
