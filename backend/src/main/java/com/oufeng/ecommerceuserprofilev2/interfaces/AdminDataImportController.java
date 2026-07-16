package com.oufeng.ecommerceuserprofilev2.interfaces;

import com.oufeng.ecommerceuserprofilev2.common.Result;
import com.oufeng.ecommerceuserprofilev2.domain.dto.task.AnalysisTaskResponse;
import com.oufeng.ecommerceuserprofilev2.domain.dto.task.CreateAnalysisTaskRequest;
import com.oufeng.ecommerceuserprofilev2.infrastructure.importer.*;
import com.oufeng.ecommerceuserprofilev2.infrastructure.security.AuthenticatedUser;
import com.oufeng.ecommerceuserprofilev2.application.IAnalysisTaskService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 管理员数据导入 RESTful API v2。
 *
 * 相比 v1 的改进：
 * <ul>
 *   <li>使用 {@link ImportTableGuesser} 统一文件类型猜测（消除重复逻辑）</li>
 *   <li>fix: importFromPath 现在正确创建 DATA_IMPORT 任务而非 PROFILE_FULL</li>
 *   <li>preview 接口增加样本行预览，帮助用户确认数据格式</li>
 *   <li>上传文件增加安全校验（MIME 类型 + 大小限制）</li>
 * </ul>
 */
@Tag(name = "数据导入")
@RestController
@RequestMapping("/api/v1/admin/import")
public class AdminDataImportController {

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final int PREVIEW_SAMPLE_LINES = 5;

    private final IAnalysisTaskService analysisTaskService;
    private final ImportTableGuesser tableGuesser;
    private final Map<String, AbstractCsvImporter> importers;

    public AdminDataImportController(IAnalysisTaskService analysisTaskService,
                                      ImportTableGuesser tableGuesser,
                                      ProductCategoryImporter ci, ProductImporter pi,
                                      EcommerceUserImporter ui, BrowseBehaviorImporter bi,
                                      LoginBehaviorImporter li, SalesOrderImporter oi,
                                      OrderItemImporter oii) {
        this.analysisTaskService = analysisTaskService;
        this.tableGuesser = tableGuesser;
        // 使用 Map.ofEntries 替代 Map.of —— 因为 Map.of 最多支持 10 对 K-V，
        // 而本项目有 7 个 Importer 注入，共 14 个参数会超限
        this.importers = Map.ofEntries(
            Map.entry(ci.tableName(), ci),
            Map.entry(pi.tableName(), pi),
            Map.entry(ui.tableName(), ui),
            Map.entry(bi.tableName(), bi),
            Map.entry(li.tableName(), li),
            Map.entry(oi.tableName(), oi),
            Map.entry(oii.tableName(), oii)
        );
    }

    @Operation(summary = "获取所有表模板列名")
    @GetMapping("/templates")
    public Result<List<Map<String, Object>>> templates() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (var e : importers.entrySet()) {
            list.add(Map.of("table", e.getKey(), "columns", (Object) e.getValue().columnNames()));
        }
        return Result.success(list);
    }

    @Operation(summary = "下载 CSV 模板（含表头）")
    @GetMapping("/template/{table}")
    public void downloadTemplate(@PathVariable String table, HttpServletResponse resp) throws IOException {
        AbstractCsvImporter imp = importers.get(table);
        if (imp == null) throw new IllegalArgumentException("未知表名: " + table);
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + table + ".csv\"");
        resp.getWriter().write(String.join(",", imp.columnNames()));
        resp.getWriter().write('\n');
        resp.getWriter().flush();
    }

    @Operation(summary = "预览上传文件：表头 + 样本行 + 推断目标表")
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> preview(@RequestParam("file") MultipartFile file) throws IOException {
        // 安全校验
        validateUploadFile(file);

        String name = file.getOriginalFilename();
        // 使用统一的 guesser
        String guessed = tableGuesser.guessTable(name);

        // 读取前 N+1 行（表头 + 样本行）
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count <= PREVIEW_SAMPLE_LINES) {
                lines.add(line);
                count++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", name);
        result.put("guessedTable", guessed);
        result.put("header", lines.isEmpty() ? "" : lines.get(0));
        if (lines.size() > 1) {
            result.put("sampleRows", lines.subList(1, lines.size()));
        }
        result.put("hasMore", lines.size() > PREVIEW_SAMPLE_LINES + 1);
        return Result.success(result);
    }

    @Operation(summary = "从服务器目录导入 CSV 数据（异步执行 DATA_IMPORT 任务）")
    @PostMapping("/path")
    public Result<AnalysisTaskResponse> importFromPath(
            @Valid @RequestBody CreateAnalysisTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        // fix: 传入 DATA_IMPORT 类型和 dataVersion/inputDir
        return Result.success(analysisTaskService.createDirectoryImport(
                request.taskName(), request.dataVersion(), request.taskType(), user.userId()));
    }

    @Operation(summary = "上传 CSV 文件导入数据（异步执行）")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AnalysisTaskResponse> importFromUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("taskName") String taskName,
            @AuthenticationPrincipal AuthenticatedUser user) {
        // 安全校验
        for (MultipartFile file : files) {
            validateUploadFile(file);
        }
        return Result.success(analysisTaskService.createUploadImport(files, taskName, user.userId()));
    }

    @Operation(summary = "查询导入任务列表")
    @GetMapping("/tasks")
    public Result<Page<AnalysisTaskResponse>> listImportTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(analysisTaskService.listTasks(page, size));
    }

    // ─── 内部方法 ───

    /** 上传文件安全校验 */
    private void validateUploadFile(MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("上传文件为空");

        // 文件大小限制
        if (file.getSize() > MAX_FILE_SIZE)
            throw new IllegalArgumentException("文件大小超过限制: " + MAX_FILE_SIZE / 1024 / 1024 + "MB");

        // 文件名检查
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) throw new IllegalArgumentException("文件名为空");

        // 检查扩展名
        String lower = name.toLowerCase();
        if (!lower.endsWith(".csv") && !lower.endsWith(".txt"))
            throw new IllegalArgumentException("仅支持 .csv 或 .txt 文件，当前文件: " + name);

        // MIME 类型白名单检查
        String contentType = file.getContentType();
        if (contentType != null &&
            !contentType.equals("text/csv") &&
            !contentType.equals("text/plain") &&
            !contentType.equals("application/csv") &&
            !contentType.equals("application/octet-stream")) {
            throw new IllegalArgumentException("不支持的文件类型: " + contentType + "，仅支持 CSV 文本文件");
        }
    }
}
