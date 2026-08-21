package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.application.ChurnAnalysisService;
import com.oufeng.ecommerceuserprofile.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 流失预警 REST 接口（等级分布/名单/CSV 导出）。
 */
@Tag(name = "流失预警")
@RestController
@RequestMapping("/api/v1/admin/churn-analysis")
public class ChurnAnalysisController {

    private final ChurnAnalysisService service;

    public ChurnAnalysisController(ChurnAnalysisService service) {
        this.service = service;
    }

    @Operation(summary = "流失等级分布")
    @GetMapping("/levels")
    public Result<List<Map<String, Object>>> levels() {
        return Result.success(service.getLevels());
    }

    @Operation(summary = "流失名单分页（支持 orderBy=recencyDays|orderCount|totalPaymentAmount 排序）")
    @GetMapping("/users")
    public Result<Map<String, Object>> users(@RequestParam(required = false) String level,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) String orderBy,
                                             @RequestParam(required = false) String orderDir) {
        return Result.success(service.getChurnList(level, page, size, orderBy, orderDir));
    }

    @Operation(summary = "流失数据版本与统计截止")
    @GetMapping("/version")
    public Result<Map<String, Object>> version() {
        return Result.success(service.getDataVersion());
    }

    @Operation(summary = "流失名单 CSV 导出")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String level) {
        byte[] data = service.exportCsv(level);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=churn_list.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }
}
