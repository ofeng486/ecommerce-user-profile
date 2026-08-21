package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.common.Result;
import com.oufeng.ecommerceuserprofile.domain.dto.tag.*;
import com.oufeng.ecommerceuserprofile.infrastructure.security.AuthenticatedUser;
import com.oufeng.ecommerceuserprofile.application.ITagDefinitionService;
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
    private final com.oufeng.ecommerceuserprofile.application.TagRuleComputeService computeService;

    public AdminTagDefinitionController(ITagDefinitionService tagDefinitionService,
                                         com.oufeng.ecommerceuserprofile.application.TagRuleComputeService computeService) {
        this.tagDefinitionService = tagDefinitionService;
        this.computeService = computeService;
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

    @Operation(summary = "删除标签定义（预设标签受保护；删除连带清理该标签画像结果）")
    @DeleteMapping("/{tagId}")
    public Result<Void> deleteTag(@PathVariable Long tagId) {
        tagDefinitionService.deleteTag(tagId);
        return Result.success(null);
    }

    @Operation(summary = "预览标签规则：按分档规则统计各标签值人数（不落库）")
    @PostMapping("/preview")
    public Result<java.util.List<java.util.Map<String, Object>>> previewRule(
            @RequestBody java.util.Map<String, String> body) {
        return Result.success(computeService.preview(body.get("sourceTable"), body.get("ruleExpression")));
    }

    @Operation(summary = "重算全部启用标签：按配置的分档规则重新生成所有用户的标签结果")
    @PostMapping("/recalculate")
    public Result<java.util.Map<String, Object>> recalculateAll() {
        return Result.success(computeService.recalculateAll());
    }
}
