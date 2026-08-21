package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.application.RepeatAnalysisService;
import com.oufeng.ecommerceuserprofile.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 复购与留存分析 REST 接口。
 */
@Tag(name = "复购与留存分析")
@RestController
@RequestMapping("/api/v1/admin/repeat-analysis")
public class RepeatAnalysisController {

    private final RepeatAnalysisService service;

    public RepeatAnalysisController(RepeatAnalysisService service) {
        this.service = service;
    }

    @Operation(summary = "购买次数分布")
    @GetMapping("/purchase-distribution")
    public Result<List<Map<String, Object>>> purchaseDistribution() {
        return Result.success(service.getPurchaseDistribution());
    }

    @Operation(summary = "复购率指标")
    @GetMapping("/repeat-rate")
    public Result<Map<String, Object>> repeatRate() {
        return Result.success(service.getRepeatRate());
    }

    @Operation(summary = "平均购买间隔（天）")
    @GetMapping("/avg-interval")
    public Result<Map<String, Object>> avgInterval() {
        return Result.success(service.getAvgInterval());
    }

    @Operation(summary = "月度首购留存 cohort")
    @GetMapping("/retention-cohort")
    public Result<List<Map<String, Object>>> retentionCohort() {
        return Result.success(service.getRetentionCohort());
    }

    @Operation(summary = "高复购用户 Top10")
    @GetMapping("/top-repeat")
    public Result<List<Map<String, Object>>> topRepeat() {
        return Result.success(service.getTopRepeat());
    }
}
