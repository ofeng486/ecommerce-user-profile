package com.oufeng.ecommerceuserprofile.interfaces;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.application.AiChatHistoryService;
import com.oufeng.ecommerceuserprofile.common.Result;
import com.oufeng.ecommerceuserprofile.domain.entity.AiChatHistory;
import com.oufeng.ecommerceuserprofile.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** AI 对话历史接口：查询当前用户的问答记录（保存由 AI 对话接口自动完成），支持删除与清空。 */
@Tag(name = "AI 对话历史")
@RestController
@RequestMapping("/api/v1/ai/history")
public class AiChatHistoryController {

    private final AiChatHistoryService service;

    public AiChatHistoryController(AiChatHistoryService service) { this.service = service; }

    @Operation(summary = "分页查询我的 AI 对话历史")
    @GetMapping
    public Result<Page<AiChatHistory>> list(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(service.listByUser(user.userId(), page, size));
    }

    @Operation(summary = "删除单条对话历史（仅限本人）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteOne(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        service.deleteById(id, user.userId());
        return Result.success(null);
    }

    @Operation(summary = "清空当前用户全部对话历史")
    @DeleteMapping
    public Result<Void> clearAll(@AuthenticationPrincipal AuthenticatedUser user) {
        service.clearByUser(user.userId());
        return Result.success(null);
    }
}
