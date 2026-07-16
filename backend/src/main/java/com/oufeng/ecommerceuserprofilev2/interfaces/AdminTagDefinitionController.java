package com.oufeng.ecommerceuserprofilev2.interfaces;

import com.oufeng.ecommerceuserprofilev2.common.Result;
import com.oufeng.ecommerceuserprofilev2.domain.dto.tag.*;
import com.oufeng.ecommerceuserprofilev2.infrastructure.security.AuthenticatedUser;
import com.oufeng.ecommerceuserprofilev2.application.ITagDefinitionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员标签定义管理 RESTful API。
 */
@Tag(name = "标签定义管理")
@RestController
@RequestMapping("/api/v1/admin/tags")
public class AdminTagDefinitionController {

    private final ITagDefinitionService tagDefinitionService;

    public AdminTagDefinitionController(ITagDefinitionService tagDefinitionService) {
        this.tagDefinitionService = tagDefinitionService;
    }

    @Operation(summary = "分页查询所有标签定义")
    @GetMapping
    public Result<Page<TagDefinitionResponse>> listTags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(tagDefinitionService.listTags(page, Math.min(size, 100)));
    }

    @Operation(summary = "查询所有启用的标签定义")
    @GetMapping("/enabled")
    public Result<List<TagDefinitionResponse>> listEnabledTags() {
        return Result.success(tagDefinitionService.listEnabledTags());
    }

    @Operation(summary = "创建新的标签定义")
    @PostMapping
    public Result<TagDefinitionResponse> createTag(
            @Valid @RequestBody CreateTagDefinitionRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(tagDefinitionService.createTag(request, user.userId()));
    }

    @Operation(summary = "更新标签定义")
    @PutMapping("/{tagId}")
    public Result<TagDefinitionResponse> updateTag(
            @PathVariable Long tagId, @Valid @RequestBody UpdateTagDefinitionRequest request) {
        return Result.success(tagDefinitionService.updateTag(tagId, request));
    }

    @Operation(summary = "启用或停用标签定义")
    @PatchMapping("/{tagId}/status")
    public Result<TagDefinitionResponse> updateStatus(
            @PathVariable Long tagId, @RequestParam boolean enabled) {
        return Result.success(tagDefinitionService.updateStatus(tagId, enabled));
    }
}
