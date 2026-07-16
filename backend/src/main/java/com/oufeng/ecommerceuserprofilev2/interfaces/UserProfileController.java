package com.oufeng.ecommerceuserprofilev2.interfaces;

import com.oufeng.ecommerceuserprofilev2.common.Result;
import com.oufeng.ecommerceuserprofilev2.domain.dto.profile.*;
import com.oufeng.ecommerceuserprofilev2.application.IUserProfileService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户画像与可视化统计 RESTful API。
 */
@Tag(name = "用户画像")
@RestController
@RequestMapping("/api/v1/profiles")
public class UserProfileController {

    private final IUserProfileService userProfileService;

    public UserProfileController(IUserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Operation(summary = "画像概览统计")
    @GetMapping("/overview")
    public Result<ProfileOverviewResponse> overview() {
        return Result.success(userProfileService.getOverview());
    }

    @Operation(summary = "用户分层分布")
    @GetMapping("/segments/distribution")
    public Result<List<SegmentDistributionResponse>> segmentDistribution() {
        return Result.success(userProfileService.getSegmentDistribution());
    }

    @Operation(summary = "标签分布统计")
    @GetMapping("/tags/distribution")
    public Result<List<TagDistributionResponse>> tagDistribution(@RequestParam(required = false) String tagCode) {
        return Result.success(userProfileService.getTagDistribution(tagCode));
    }

    @Operation(summary = "分页查询用户画像列表")
    @GetMapping("/users")
    public Result<Page<UserProfileListItemResponse>> listProfiles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String segmentCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(userProfileService.listProfiles(keyword, segmentCode, page, size));
    }

    @Operation(summary = "查询单个用户画像详情")
    @GetMapping("/users/{userId}")
    public Result<UserProfileResponse> userProfile(@PathVariable Long userId) {
        return Result.success(userProfileService.getUserProfile(userId));
    }
}
