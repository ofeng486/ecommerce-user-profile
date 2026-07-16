package com.oufeng.ecommerceuserprofilev2.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofilev2.common.ResultCode;
import com.oufeng.ecommerceuserprofilev2.domain.converter.TaskConverter;
import com.oufeng.ecommerceuserprofilev2.domain.dto.task.AnalysisTaskResponse;
import com.oufeng.ecommerceuserprofilev2.domain.dto.task.CreateAnalysisTaskRequest;
import com.oufeng.ecommerceuserprofilev2.domain.entity.SparkAnalysisTask;
import com.oufeng.ecommerceuserprofilev2.domain.mapper.SparkAnalysisTaskMapper;
import com.oufeng.ecommerceuserprofilev2.common.BusinessException;
import com.oufeng.ecommerceuserprofilev2.infrastructure.importer.DataImportOrchestrator;
import com.oufeng.ecommerceuserprofilev2.infrastructure.importer.ImportReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.oufeng.ecommerceuserprofilev2.infrastructure.config.CacheConfig;

/**
 * 管理员分析任务服务 v2。
 *
 * 相比 v1 的改进：
 * <ul>
 *   <li>{@link #createDirectoryImport} 正确创建 DATA_IMPORT 任务（修复 importFromPath bug）</li>
 *   <li>导入异常时写入详细的 ImportReport 到 error_message 字段</li>
 *   <li>线程池可配置大小，避免单线程瓶颈</li>
 * </ul>
 */
@Service
public class AnalysisTaskServiceImpl implements IAnalysisTaskService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTaskServiceImpl.class);

    private final SparkAnalysisTaskMapper taskMapper;
    private final DataImportOrchestrator orchestrator;
    private final CacheManager cacheManager;
    private final ExecutorService executor = Executors.newFixedThreadPool(2); // 允许同时 2 个任务

    @Value("${spark.pipeline.script-path:../bigdata-scripts/spark/run_local_pipeline.py}")
    private String scriptPath;
    @Value("${spark.pipeline.import-dir:../bigdata-scripts/test-output}")
    private String inputDir;
    @Value("${spark.pipeline.mysql-user:root}")
    private String mysqlUser;
    @Value("${spring.datasource.password:${DB_PASSWORD:}}")
    private String mysqlPassword;
    @Value("${spark.pipeline.jdbc-url:jdbc:mysql://localhost:3306/ecommerce_user_profile}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai")
    private String jdbcUrl;
    @Value("${spark.pipeline.java-home:${java.home}}")
    private String javaHome;
    @Value("${data.generator.python-path:python3}")
    private String pythonPath;

    public AnalysisTaskServiceImpl(SparkAnalysisTaskMapper taskMapper,
                               DataImportOrchestrator orchestrator,
                               CacheManager cacheManager) {
        this.taskMapper = taskMapper;
        this.orchestrator = orchestrator;
        this.cacheManager = cacheManager;
    }

    /** 创建 PROFILE_FULL 分析任务，异步执行 PySpark 管线。 */
    public AnalysisTaskResponse create(CreateAnalysisTaskRequest request, Long submitterId) {
        SparkAnalysisTask task = new SparkAnalysisTask();
        task.setTaskName(request.taskName());
        task.setTaskType(request.taskType());
        task.setTaskStatus("Pending");
        task.setDataVersion(request.dataVersion());
        task.setSubmitterId(submitterId);
        taskMapper.insert(task);
        executor.submit(() -> executePipeline(task.getId()));
        return TaskConverter.INSTANCE.toResponse(taskMapper.selectById(task.getId()));
    }

    /**
     * 创建 DATA_IMPORT 任务从目录导入（修复原 importFromPath bug：原来是创建 PROFILE_FULL 任务）。
     */
    public AnalysisTaskResponse createDirectoryImport(String taskName, String dataVersion,
                                                       String taskType, Long submitterId) {
        SparkAnalysisTask task = new SparkAnalysisTask();
        task.setTaskName(taskName);
        task.setTaskType("DATA_IMPORT");
        task.setTaskStatus("Pending");
        task.setDataVersion(dataVersion != null ? dataVersion
                : "DIR_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        task.setSubmitterId(submitterId);
        taskMapper.insert(task);

        String dir = inputDir; // 使用配置或 taskType 指定的目录
        executor.submit(() -> executeDirectoryImport(task.getId(), Path.of(dir)));
        return TaskConverter.INSTANCE.toResponse(taskMapper.selectById(task.getId()));
    }

    /** 创建 DATA_IMPORT 任务，从上传文件异步导入。 */
    public AnalysisTaskResponse createUploadImport(MultipartFile[] files, String taskName, Long submitterId) {
        SparkAnalysisTask task = new SparkAnalysisTask();
        task.setTaskName(taskName);
        task.setTaskType("DATA_IMPORT");
        task.setTaskStatus("Pending");
        task.setDataVersion("UPLOAD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        task.setSubmitterId(submitterId);
        taskMapper.insert(task);

        // 修复：MultipartFile 必须在 HTTP 线程内持久化到磁盘，
        // 异步线程执行时原始 InputStream 已失效（Spring 标准坑）
        Path tempDir;
        try {
            tempDir = saveUploadedFiles(files);
        } catch (Exception e) {
            log.error("保存上传文件失败", e);
            setTaskFailed(task.getId(), "保存上传文件失败: " + e.getMessage());
            return TaskConverter.INSTANCE.toResponse(taskMapper.selectById(task.getId()));
        }
        // 传入磁盘路径而非 MultipartFile[]，避免异步线程的 InputStream 失效问题
        executor.submit(() -> executeUploadImport(task.getId(), tempDir));
        return TaskConverter.INSTANCE.toResponse(taskMapper.selectById(task.getId()));
    }

    @SuppressWarnings("unchecked")
    public Page<AnalysisTaskResponse> listTasks(int page, int size) {
        Page<SparkAnalysisTask> entityPage = taskMapper.selectPage(
                new Page<>(page, Math.min(size, 100)),
                new LambdaQueryWrapper<SparkAnalysisTask>()
                        .orderByDesc(SparkAnalysisTask::getCreatedAt));
        List<AnalysisTaskResponse> records = entityPage.getRecords().stream()
                .map(TaskConverter.INSTANCE::toResponse).toList();
        Page<AnalysisTaskResponse> result = new Page<>(page, size, entityPage.getTotal());
        result.setRecords(records);
        return result;
    }

    public AnalysisTaskResponse getTask(Long taskId) {
        SparkAnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(ResultCode.NOT_FOUND, "分析任务不存在");
        return TaskConverter.INSTANCE.toResponse(task);
    }

    /** 取消待处理的或运行中的任务 */
    public AnalysisTaskResponse cancel(Long taskId) {
        SparkAnalysisTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(ResultCode.NOT_FOUND, "分析任务不存在");
        if (!"Pending".equals(task.getTaskStatus()) && !"Running".equals(task.getTaskStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只能取消待处理或运行中的任务");
        }
        task.setTaskStatus("Cancelled");
        task.setFinishedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        return TaskConverter.INSTANCE.toResponse(task);
    }

    /** 删除任务 */
    public void delete(Long taskId) {
        if (taskMapper.selectById(taskId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分析任务不存在");
        }
        taskMapper.deleteById(taskId);
    }

    // ─── 异步执行 ───

    private void executePipeline(Long taskId) {
        try {
            setTaskRunning(taskId);
            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath,
                    "--input", inputDir,
                    "--mysql-user", mysqlUser,
                    "--jdbc-url", jdbcUrl);
            pb.environment().put("MYSQL_PASSWORD", mysqlPassword);
            pb.environment().put("JAVA_HOME", javaHome);
            // 合并 stderr 到 stdout，避免错误信息丢失
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(line -> log.info("[Spark-{}] {}", taskId, line));
            }
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                log.warn("PySpark 任务超时 (30min)，强制终止: taskId={}", taskId);
                process.destroyForcibly();
                setTaskFailed(taskId, "PySpark 脚本执行超时 (30 分钟)，已自动终止");
                return;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                setTaskSucceeded(taskId, null);
                evictProfileCache(); // Spark 画像完成后清除缓存，让概览数据刷新
            } else {
                setTaskFailed(taskId, "PySpark 退出码: " + exitCode);
            }
        } catch (Exception e) {
            log.error("PySpark 管线执行失败", e);
            setTaskFailed(taskId, truncateError(e.getMessage()));
        }
    }

    private void executeDirectoryImport(Long taskId, Path dir) {
        try {
            setTaskRunning(taskId);
            ImportReport report = orchestrator.importFromDirectory(dir);
            if (report.isSuccess()) {
                setTaskSucceeded(taskId, buildImportSummary(report));
                evictProfileCache();
            } else {
                setTaskFailed(taskId, buildImportSummary(report));
            }
        } catch (Exception e) {
            log.error("目录导入失败", e);
            setTaskFailed(taskId, truncateError(e.getMessage()));
        }
    }

    private void executeUploadImport(Long taskId, Path tempDir) {
        try {
            setTaskRunning(taskId);
            // 修复：从磁盘读取而非从 MultipartFile 读取
            // MultipartFile 在异步线程中已失效（HTTP 请求已结束）
            // 复用目录导入的逻辑，把临时目录交给 Orchestrator
            ImportReport report = orchestrator.importFromDirectory(tempDir);
            // 清理临时文件
            cleanupTempDir(tempDir);
            if (report.isSuccess()) {
                setTaskSucceeded(taskId, buildImportSummary(report));
                evictProfileCache();
            } else {
                setTaskFailed(taskId, buildImportSummary(report));
            }
        } catch (Exception e) {
            log.error("上传导入失败", e);
            setTaskFailed(taskId, truncateError(e.getMessage()));
            cleanupTempDir(tempDir); // 异常时也要清理
        }
    }

    /** 清理临时目录 */
    private void cleanupTempDir(Path dir) {
        try {
            if (dir != null && Files.isDirectory(dir)) {
                try (var files = Files.list(dir)) {
                    for (Path f : files.toList()) Files.deleteIfExists(f);
                }
                Files.deleteIfExists(dir);
            }
        } catch (Exception e) {
            log.warn("清理临时目录失败: {}, 原因: {}", dir, e.getMessage());
        }
    }

    /**
     * 保存上传的 MultipartFile 到临时目录。必须在 HTTP 请求线程内调用，
     * 因为 MultipartFile 的 InputStream 在请求结束后会失效。
     */
    private Path saveUploadedFiles(MultipartFile[] files) throws IOException {
        Path dir = Files.createTempDirectory("import_");
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            if (name == null || name.isBlank() || file.isEmpty()) continue;
            // 防止路径遍历
            String safeName = Path.of(name).getFileName().toString();
            Files.copy(file.getInputStream(), dir.resolve(safeName));
        }
        return dir;
    }

    // ─── 任务状态更新 ───

    private void setTaskRunning(Long taskId) {
        taskMapper.update(null, new LambdaUpdateWrapper<SparkAnalysisTask>()
                .eq(SparkAnalysisTask::getId, taskId)
                .set(SparkAnalysisTask::getTaskStatus, "Running")
                .set(SparkAnalysisTask::getStartedAt, LocalDateTime.now()));
    }

    private void setTaskSucceeded(Long taskId, String summary) {
        taskMapper.update(null, new LambdaUpdateWrapper<SparkAnalysisTask>()
                .eq(SparkAnalysisTask::getId, taskId)
                .set(SparkAnalysisTask::getTaskStatus, "Succeeded")
                .set(SparkAnalysisTask::getErrorMessage, summary)
                .set(SparkAnalysisTask::getFinishedAt, LocalDateTime.now()));
    }

    private void setTaskFailed(Long taskId, String error) {
        taskMapper.update(null, new LambdaUpdateWrapper<SparkAnalysisTask>()
                .eq(SparkAnalysisTask::getId, taskId)
                .set(SparkAnalysisTask::getTaskStatus, "Failed")
                .set(SparkAnalysisTask::getErrorMessage, error)
                .set(SparkAnalysisTask::getFinishedAt, LocalDateTime.now()));
    }

    /** 构建导入结果摘要（供存储到 error_message 字段） */
    private String buildImportSummary(ImportReport report) {
        Map<String, Object> summary = report.toSummary();
        return String.format("插入:%d 更新:%d 错误:%d 跳过:%d",
                summary.get("totalInserted"), summary.get("totalUpdated"),
                summary.get("totalErrors"), summary.get("totalSkipped"));
    }

    private String truncateError(String msg) {
        return msg == null ? null : (msg.length() > 500 ? msg.substring(0, 500) : msg);
    }

    /** 清除画像查询缓存，让概览/分布数据重新从数据库读取 */
    private void evictProfileCache() {
        cacheManager.getCache(CacheConfig.CACHE_PROFILE_OVERVIEW).clear();
        cacheManager.getCache(CacheConfig.CACHE_SEGMENT_DIST).clear();
        cacheManager.getCache(CacheConfig.CACHE_TAG_DIST).clear();
        log.info("画像查询缓存已清除");
    }
}
