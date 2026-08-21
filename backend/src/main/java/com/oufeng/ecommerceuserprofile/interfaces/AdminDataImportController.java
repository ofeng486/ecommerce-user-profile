package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.common.Result;
import com.oufeng.ecommerceuserprofile.domain.dto.task.AnalysisTaskResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.task.CreateAnalysisTaskRequest;
import com.oufeng.ecommerceuserprofile.infrastructure.importer.*;
import com.oufeng.ecommerceuserprofile.infrastructure.security.AuthenticatedUser;
import com.oufeng.ecommerceuserprofile.application.IAnalysisTaskService;
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
 * 管理员数据导入 RESTful API。
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
                                      OrderItemImporter oii, TransactionImporter ti,
                                      InteractionImporter ii, ProductDataImporter pdi) {
        this.analysisTaskService = analysisTaskService;
        this.tableGuesser = tableGuesser;
        this.importers = Map.ofEntries(
            Map.entry(ci.tableName(), ci),
            Map.entry(pi.tableName(), pi),
            Map.entry(ui.tableName(), ui),
            Map.entry(bi.tableName(), bi),
            Map.entry(li.tableName(), li),
            Map.entry(oi.tableName(), oi),
            Map.entry(oii.tableName(), oii),
            Map.entry(ti.tableName(), ti),
            Map.entry(ii.tableName(), ii),
            Map.entry(pdi.tableName(), pdi)
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
        // UTF-8 BOM：Excel/WPS 默认按 GBK 打开无 BOM 的 CSV，中文列名会乱码；加 BOM 后自动识别 UTF-8
        resp.getWriter().write('\uFEFF');
        resp.getWriter().write(java.util.Arrays.stream(imp.columnNames())
                .map(CsvColumnNames::toChinese)
                .collect(java.util.stream.Collectors.joining(",")));
        resp.getWriter().write('\n');
        resp.getWriter().flush();
    }

    @Operation(summary = "下载数据总表模板（一个 CSV 包含全部表，第一列填表名）")
    @GetMapping("/template/total")
    public void downloadCompositeTemplate(HttpServletResponse resp) throws IOException {
        // 总表表头：表名 + 全部表列并集（剔除精简黑名单列，输出中文列名）
        String header = CompositeCsvImporter.TABLE_COLUMN + ","
                + CompositeCsvImporter.compositeColumns(new ArrayList<>(importers.values())).stream()
                .map(CsvColumnNames::toChinese)
                .collect(java.util.stream.Collectors.joining(","));
        resp.setContentType("text/csv; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"数据导入总表.csv\"");
        // UTF-8 BOM：Excel/WPS 默认按 GBK 打开无 BOM 的 CSV，中文列名会乱码；加 BOM 后自动识别 UTF-8
        resp.getWriter().write('\uFEFF');
        resp.getWriter().write(header);
        resp.getWriter().write('\n');
        resp.getWriter().flush();
    }

    @Operation(summary = "预览上传文件：表头 + 样本行 + 推断目标表")
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> preview(@RequestParam("file") MultipartFile file) throws IOException {
        // 安全校验
        validateUploadFile(file);

        String name = file.getOriginalFilename();
        // 读取前 N+1 行（表头 + 样本行），先识别是否为数据总表
        List<String> lines = new ArrayList<>();
        String guessed;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count <= PREVIEW_SAMPLE_LINES) {
                lines.add(line);
                count++;
            }
        }
        String headerLine = lines.isEmpty() ? "" : lines.get(0);
        // 剥离 UTF-8 BOM，避免首列「表名」识别失败
        if (!headerLine.isEmpty() && headerLine.charAt(0) == '\uFEFF') {
            headerLine = headerLine.substring(1);
        }
        if (!headerLine.isBlank() && headerLine.split(",", -1)[0].trim().equals(CompositeCsvImporter.TABLE_COLUMN)) {
            // 总表：一个文件包含全部 7 表数据（第一列为表名）
            guessed = "数据总表";
        } else {
            guessed = tableGuesser.guessTable(name);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", name);
        result.put("guessedTable", guessed);
        result.put("header", String.join(",",
                java.util.Arrays.stream(headerLine.split(",", -1))
                        .map(String::trim)
                        .map(CsvColumnNames::toChinese)
                        .toArray(String[]::new)));
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

    @Operation(summary = "天池淘宝用户行为数据集：上传 CSV → 自动转换 → 异步导入（生成用户/商品/互动数据）")
    @PostMapping(value = "/tianchi", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AnalysisTaskResponse> importTianchi(
            @RequestParam("file") MultipartFile file,
            @RequestParam("taskName") String taskName,
            @RequestParam(value = "limit", defaultValue = "20000") int limit,
            @AuthenticationPrincipal AuthenticatedUser user) {
        validateUploadFile(file);
        if (limit < 0 || limit > 1_000_000) {
            throw new IllegalArgumentException("抽样行数须在 0 ~ 1000000 之间");
        }
        return Result.success(analysisTaskService.createTianchiImport(file, taskName, limit, user.userId()));
    }

    @Operation(summary = "查询导入任务列表")
    @GetMapping("/tasks")
    public Result<Page<AnalysisTaskResponse>> listImportTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) String taskStatus,
            @RequestParam(required = false) String keyword) {
        return Result.success(analysisTaskService.listTasks(page, size, taskType, taskStatus, keyword, null, null));
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
