package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.common.Result;
import com.oufeng.ecommerceuserprofile.domain.dto.task.*;
import com.oufeng.ecommerceuserprofile.infrastructure.security.AuthenticatedUser;
import com.oufeng.ecommerceuserprofile.application.IAnalysisTaskService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员 Spark 分析任务 RESTful API。
 */
@Tag(name = "分析任务管理")
@RestController
@RequestMapping("/api/v1/admin/analysis-tasks")
public class AdminAnalysisTaskController {

    private final IAnalysisTaskService service;

    public AdminAnalysisTaskController(IAnalysisTaskService service) { this.service = service; }

    @Operation(summary = "创建分析任务")
    @PostMapping
    public Result<AnalysisTaskResponse> create(
            @Valid @RequestBody CreateAnalysisTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(service.create(request, user.userId()));
    }

    @Operation(summary = "分页查询分析任务")
    @GetMapping
    public Result<Page<AnalysisTaskResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String taskStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String orderDir) {
        return Result.success(service.listTasks(page, size, taskType, taskStatus, keyword, orderBy, orderDir));
    }

    @Operation(summary = "查询单个分析任务")
    @GetMapping("/{taskId}")
    public Result<AnalysisTaskResponse> get(@PathVariable Long taskId) {
        return Result.success(service.getTask(taskId));
    }

    @Operation(summary = "取消分析任务")
    @PatchMapping("/{taskId}/cancel")
    public Result<AnalysisTaskResponse> cancel(@PathVariable Long taskId) {
        return Result.success(service.cancel(taskId));
    }

    @Operation(summary = "删除分析任务")
    @DeleteMapping("/{taskId}")
    public Result<Void> delete(@PathVariable Long taskId) {
        service.delete(taskId);
        return Result.success(null);
    }
}
