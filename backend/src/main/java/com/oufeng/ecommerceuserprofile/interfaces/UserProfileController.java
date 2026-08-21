package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.common.Result;
import com.oufeng.ecommerceuserprofile.domain.dto.profile.*;
import com.oufeng.ecommerceuserprofile.application.IUserProfileService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @Operation(summary = "标签交叉矩阵（活跃档 × 消费档）")
    @GetMapping("/tags/cross")
    public Result<List<TagCrossResponse>> tagCross() {
        return Result.success(userProfileService.getTagCross());
    }

    @Operation(summary = "画像核心指标（总数/订单/消费/流失风险）")
    @GetMapping("/metrics")
    public Result<ProfileMetricsResponse> metrics() {
        return Result.success(userProfileService.getProfileMetrics());
    }

    @Operation(summary = "TOP 省份消费排名")
    @GetMapping("/province-ranking")
    public Result<java.util.List<java.util.Map<String, Object>>> provinceRanking() {
        return Result.success(userProfileService.getProvinceAmountRanking());
    }

    @Operation(summary = "分页查询用户画像列表（支持金额区间/标签与排序）")
    @GetMapping("/users")
    public Result<Page<UserProfileListItemResponse>> listProfiles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String segmentCode,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String tagCode,
            @RequestParam(required = false) String tagValue,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String orderDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(userProfileService.listProfiles(keyword, segmentCode, province, minAmount, maxAmount, tagCode, tagValue, orderBy, orderDir, page, size));
    }

    @Operation(summary = "查询单个用户画像详情")
    @GetMapping("/users/{userId}")
    public Result<UserProfileResponse> userProfile(@PathVariable Long userId) {
        return Result.success(userProfileService.getUserProfile(userId));
    }

    @Operation(summary = "导出画像列表 CSV（复用列表筛选条件，UTF-8 BOM 供 Excel 打开）")
    @GetMapping("/users/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String segmentCode,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String tagCode,
            @RequestParam(required = false) String tagValue,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String orderDir) {
        byte[] data = userProfileService.exportUsersCsv(keyword, segmentCode, province, minAmount, maxAmount, tagCode, tagValue, orderBy, orderDir);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=user_profiles.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }
}
