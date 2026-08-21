package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.application.IAnalysisTaskService;
import com.oufeng.ecommerceuserprofile.application.IDataGenerationService;
import com.oufeng.ecommerceuserprofile.application.DataClearService;
import com.oufeng.ecommerceuserprofile.application.DataGenerationParams;
import com.oufeng.ecommerceuserprofile.common.Result;
import com.oufeng.ecommerceuserprofile.common.ResultCode;
import com.oufeng.ecommerceuserprofile.domain.dto.task.AnalysisTaskResponse;
import com.oufeng.ecommerceuserprofile.domain.converter.TaskConverter;
import com.oufeng.ecommerceuserprofile.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理员数据生成 RESTful API。
 * 在线调用 Python 脚本批量生成电商模拟数据并自动导入数据库。
 */
@Tag(name = "数据生成")
@RestController
@RequestMapping("/api/v1/admin/data-generate")
public class AdminDataGenerateController {

    private final IDataGenerationService dataGenerationService;
    private final IAnalysisTaskService analysisTaskService;
    private final DataClearService dataClearService;

    public AdminDataGenerateController(IDataGenerationService dataGenerationService,
                                        IAnalysisTaskService analysisTaskService,
                                        DataClearService dataClearService) {
        this.dataGenerationService = dataGenerationService;
        this.analysisTaskService = analysisTaskService;
        this.dataClearService = dataClearService;
    }

    /**
     * 获取预设方案（快速测试 / 默认 / 中等规模）
     */
    @Operation(summary = "获取预设生成方案")
    @GetMapping("/presets")
    public Result<Map<String, Object>> presets() {
        Map<String, Object> presets = new LinkedHashMap<>();
        var small = DataGenerationParams.small();
        var defaults = DataGenerationParams.defaults();
        var medium = DataGenerationParams.medium();

        presets.put("small", toMap("快速测试", small, "100用户/50商品/1000行为/200订单，秒级完成"));
        presets.put("default", toMap("默认规模", defaults, "1000用户/200商品/1万行为/2000订单，约30秒"));
        presets.put("medium", toMap("中等规模", medium, "5000用户/500商品/5万行为/1万订单，约2分钟"));
        return Result.success(presets);
    }

    /**
     * 生成数据并导入数据库（异步执行）
     * 使用 @RequestBody 接收 JSON，与前端的 axios post 配合
     * （之前用 @RequestParam + POST body 会被 axios 的 params→data 自动转换破坏，导致只能收到默认值）
     */
    @Operation(summary = "生成数据并导入数据库")
    @PostMapping("/generate")
    public Result<AnalysisTaskResponse> generate(
            @RequestBody DataGenerateRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {

        // 参数校验（DTO 已带 @Min/@Max，但保险起见在 service 之外也校验一次）
        if (req.users() < 1 || req.users() > 50000)
            return Result.failure(ResultCode.BAD_REQUEST, "用户数量需在 1-50000 之间");
        if (req.products() < 1 || req.products() > 10000)
            return Result.failure(ResultCode.BAD_REQUEST, "商品数量需在 1-10000 之间");
        if (req.behaviors() < 1 || req.behaviors() > 500000)
            return Result.failure(ResultCode.BAD_REQUEST, "行为数量需在 1-500000 之间");
        if (req.orders() < 1 || req.orders() > 100000)
            return Result.failure(ResultCode.BAD_REQUEST, "订单数量需在 1-100000 之间");

        var params = new DataGenerationParams(req.users(), req.products(), req.behaviors(), req.orders(), req.seed());
        var task = dataGenerationService.generateAndImport(params, req.taskName(), user.userId());
        return Result.success(TaskConverter.INSTANCE.toResponse(task));
    }

    /**
     * 数据生成请求体。
     * 前端 axios post 调用时会以 application/json 格式发送，
     * 这里用 record + jakarta validation 注解做入参校验。
     */
    public record DataGenerateRequest(
            @Min(1) @Max(50000) int users,
            @Min(1) @Max(10000) int products,
            @Min(1) @Max(500000) int behaviors,
            @Min(1) @Max(100000) int orders,
            @Min(1) @Max(99999) int seed,
            @NotBlank String taskName
    ) {}

    /**
     * 查询生成任务列表（复用分析任务列表接口）
     */
    @Operation(summary = "查询生成任务列表")
    @GetMapping("/tasks")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<AnalysisTaskResponse>> listTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String taskStatus,
            @RequestParam(required = false) String keyword) {
        return Result.success(analysisTaskService.listTasks(page, size, taskType, taskStatus, keyword, null, null));
    }

    /**
     * 清空所有电商业务数据（用户、商品、订单、行为、画像结果），保留系统管理数据。
     */
    @Operation(summary = "清空所有电商业务数据")
    @DeleteMapping("/clear")
    public Result<Map<String, Long>> clearAllData() {
        return Result.success(dataClearService.clearAll());
    }

    private Map<String, Object> toMap(String label, DataGenerationParams params, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("desc", desc);
        m.put("users", params.users());
        m.put("products", params.products());
        m.put("behaviors", params.behaviors());
        m.put("orders", params.orders());
        m.put("seed", params.seed());
        return m;
    }
}
