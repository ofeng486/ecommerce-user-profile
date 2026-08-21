package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.application.ClusterAnalysisService;
import com.oufeng.ecommerceuserprofile.application.IAnalysisTaskService;
import com.oufeng.ecommerceuserprofile.common.Result;
import com.oufeng.ecommerceuserprofile.domain.dto.task.AnalysisTaskResponse;
import com.oufeng.ecommerceuserprofile.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户聚类分析 REST 接口。
 */
@Tag(name = "用户聚类分析")
@RestController
@RequestMapping("/api/v1/admin/cluster-analysis")
public class ClusterAnalysisController {

    private final ClusterAnalysisService service;
    private final IAnalysisTaskService analysisTaskService;

    public ClusterAnalysisController(ClusterAnalysisService service, IAnalysisTaskService analysisTaskService) {
        this.service = service;
        this.analysisTaskService = analysisTaskService;
    }

    @Operation(summary = "簇分布与特征均值")
    @GetMapping("/overview")
    public Result<List<Map<String, Object>>> overview() {
        return Result.success(service.getOverview());
    }

    @Operation(summary = "簇内用户分页（支持 orderBy=orderCount|totalPaymentAmount 排序）")
    @GetMapping("/users")
    public Result<Map<String, Object>> users(@RequestParam int cluster,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) String orderBy,
                                             @RequestParam(required = false) String orderDir) {
        return Result.success(service.getClusterUsers(cluster, page, size, orderBy, orderDir));
    }

    @Operation(summary = "聚类数据版本")
    @GetMapping("/version")
    public Result<Map<String, Object>> version() {
        return Result.success(service.getDataVersion());
    }

    @Operation(summary = "簇内用户 CSV 导出")
    @GetMapping("/users/export")
    public ResponseEntity<byte[]> exportUsers(@RequestParam int cluster) {
        byte[] data = service.exportCsv(cluster);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cluster_" + cluster + "_users.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }

    @Operation(summary = "按指定 K 值重算聚类（异步 CLUSTER_RECALC 任务，仅重算聚类不动画像数据）")
    @PostMapping("/recalc")
    // 聚类重算为数据生产动作，仅 Admin 可触发（User 角色仅查看聚类结果）
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AnalysisTaskResponse> recalc(@RequestBody RecalcRequest request,
                                                @AuthenticationPrincipal AuthenticatedUser user) {
        boolean merge = request.mergeSimilar() == null || request.mergeSimilar();
        return Result.success(analysisTaskService.createClusterRecalc(request.k(), user.userId(), merge));
    }

    /** 聚类重算请求体（POST 参数经前端封装自动放入 body） */
    record RecalcRequest(int k, Boolean mergeSimilar) {} // mergeSimilar: null/true=自动合并相似簇；false=严格按 K 输出
}
