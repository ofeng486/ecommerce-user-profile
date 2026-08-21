package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.common.ResultCode;
import com.oufeng.ecommerceuserprofile.domain.converter.TaskConverter;
import com.oufeng.ecommerceuserprofile.domain.dto.task.AnalysisTaskResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.task.CreateAnalysisTaskRequest;
import com.oufeng.ecommerceuserprofile.domain.entity.SparkAnalysisTask;
import com.oufeng.ecommerceuserprofile.domain.entity.SystemUser;
import com.oufeng.ecommerceuserprofile.domain.mapper.SparkAnalysisTaskMapper;
import com.oufeng.ecommerceuserprofile.domain.mapper.SystemUserMapper;
import com.oufeng.ecommerceuserprofile.common.BusinessException;
import com.oufeng.ecommerceuserprofile.infrastructure.importer.DataImportOrchestrator;
import com.oufeng.ecommerceuserprofile.infrastructure.util.BigdataPathResolver;
import com.oufeng.ecommerceuserprofile.infrastructure.importer.ImportReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 管理员分析任务服务。
 *
 * 负责画像计算任务（PROFILE_FULL）与数据导入任务（DATA_IMPORT）的创建、
 * 异步执行与状态流转，并在任务完成后清除画像缓存。
 */
@Service
public class AnalysisTaskServiceImpl implements IAnalysisTaskService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisTaskServiceImpl.class);

    private final SparkAnalysisTaskMapper taskMapper;
    private final DataImportOrchestrator orchestrator;
    private final NotificationService notificationService;
    private final SystemUserMapper userMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
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
                               NotificationService notificationService,
                               SystemUserMapper userMapper,
                               org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.taskMapper = taskMapper;
        this.orchestrator = orchestrator;
        this.notificationService = notificationService;
        this.userMapper = userMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 应用重启后，把上次遗留的 Pending/Running 任务标记为 FAILED（防止永久悬挂） */
    @PostConstruct
    public void recoverInterruptedTasks() {
        try {
            int affected = taskMapper.update(null,
                    new LambdaUpdateWrapper<SparkAnalysisTask>()
                            .in(SparkAnalysisTask::getTaskStatus, "Pending", "Running")
                            .set(SparkAnalysisTask::getTaskStatus, "FAILED"));
            if (affected > 0) {
                log.info("启动任务恢复：{} 个中断任务已标记为 FAILED", affected);
            }
        } catch (Exception e) {
            log.warn("启动任务恢复失败（不影响系统启动）", e);
        }
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

    /** 创建 CLUSTER_RECALC 任务：按指定 K 值独立重算 K-Means 聚类（不动画像/标签/分层数据）。 */
    public AnalysisTaskResponse createClusterRecalc(int k, Long submitterId, boolean mergeSimilar) {
        if (k < 2 || k > 20) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "K 值须在 2-20 之间");
        }
        SparkAnalysisTask task = new SparkAnalysisTask();
        task.setTaskName(mergeSimilar ? "聚类重算 K=" + k : "聚类重算 K=" + k + "（不合并）");
        task.setTaskType("CLUSTER_RECALC");
        task.setTaskStatus("Pending");
        // data_version 携带合并标记：_RAW 后缀 = 不自动合并（严格按 K 输出）
        String base = "K_" + k + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        task.setDataVersion(mergeSimilar ? base : base + "_RAW");
        task.setSubmitterId(submitterId);
        taskMapper.insert(task);
        executor.submit(() -> executeClusterRecalc(task.getId(), k, mergeSimilar));
        return TaskConverter.INSTANCE.toResponse(taskMapper.selectById(task.getId()));
    }

    @SuppressWarnings("unchecked")
    public Page<AnalysisTaskResponse> listTasks(int page, int size, String taskType, String taskStatus, String keyword, String orderBy, String orderDir) {
        LambdaQueryWrapper<SparkAnalysisTask> entityWrapper = new LambdaQueryWrapper<SparkAnalysisTask>();
        entityWrapper
                        .eq(taskType != null && !taskType.isBlank(), SparkAnalysisTask::getTaskType, taskType)
                        .eq(taskStatus != null && !taskStatus.isBlank(), SparkAnalysisTask::getTaskStatus, taskStatus)
                        .like(keyword != null && !keyword.isBlank(), SparkAnalysisTask::getTaskName, keyword);
        // 排序白名单（防注入）：duration=耗时、createdAt=创建时间；默认创建时间倒序
        String dir = "asc".equalsIgnoreCase(orderDir) ? "ASC" : "DESC";
        if ("duration".equals(orderBy)) {
            entityWrapper.last("ORDER BY TIMESTAMPDIFF(SECOND, created_at, finished_at) " + dir + ", id DESC");
        } else if ("createdAt".equals(orderBy)) {
            entityWrapper.last("ORDER BY created_at " + dir + ", id DESC");
        } else {
            entityWrapper.orderByDesc(SparkAnalysisTask::getCreatedAt);
        }
        Page<SparkAnalysisTask> entityPage = taskMapper.selectPage(
                new Page<>(page, Math.min(size, 100)), entityWrapper);
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
            SparkAnalysisTask task = taskMapper.selectById(taskId);
            // 统计基准日：取业务数据中的最大订单日期，避免“当天”与数据时间窗口错位
            // （数据固定生成在某历史日期，若用当天会导致近 30 日指标与活跃标签失真）
            String dataVersion = (task != null && task.getDataVersion() != null && !task.getDataVersion().isBlank())
                    ? task.getDataVersion()
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            // 相对路径按工作目录向上探测解析（兼容 IDEA 项目根 / Maven backend 两种工作目录）
            String script = com.oufeng.ecommerceuserprofile.infrastructure.util.BigdataPathResolver.resolve(scriptPath);
            ProcessBuilder pb = new ProcessBuilder(pythonPath, script,
                    "--data-version", dataVersion,
                    "--mysql-user", mysqlUser,
                    "--jdbc-url", jdbcUrl);
            // PySpark 需 JDK 17（JDK 23+ 移除 Subject.getSubject 导致 Spark 启动失败）：
            // 显式把当前 JVM 的 java.home 传给子进程，保证管线与后端运行在同版本 JDK
            String javaHome = System.getProperty("java.home");
            if (javaHome != null && !javaHome.isBlank()) {
                pb.environment().put("JAVA_HOME", javaHome);
                String jdkBin = javaHome + File.separator + "bin";
                String oldPath = pb.environment().get("PATH");
                pb.environment().put("PATH", jdkBin + (oldPath == null || oldPath.isEmpty() ? "" : File.pathSeparator + oldPath));
            }
            pb.environment().put("MYSQL_PASSWORD", mysqlPassword);
            pb.environment().put("JAVA_HOME", javaHome);
            // 合并 stderr 到 stdout，避免错误信息丢失
            pb.redirectErrorStream(true);
            Process process = pb.start();
            // 记录脚本输出尾部（最多 40 行），失败时写入任务错误信息便于定位
            java.util.ArrayDeque<String> tail = new java.util.ArrayDeque<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(line -> {
                    log.info("[Spark-{}] {}", taskId, line);
                    tail.addLast(line);
                    if (tail.size() > 40) tail.removeFirst();
                });
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
                // 从脚本输出中提取统计日（格式：统计日：yyyy-MM-dd），成功摘要展示给用户
                String statDay = tail.stream()
                        .filter(l -> l.contains("统计日"))
                        .map(l -> l.substring(l.indexOf("统计日") + "统计日".length()).trim())
                        .findFirst().orElse("");
                setTaskSucceeded(taskId, "画像分析完成：用户画像数据已全量更新" + (statDay.isEmpty() ? "" : "（统计日 " + statDay + "）"));
            } else {
                // 失败时附带脚本输出尾部，帮助快速定位（如路径/依赖/Java 版本问题）
                String detail = String.join("\n", tail);
                if (detail.length() > 450) detail = detail.substring(detail.length() - 450);
                setTaskFailed(taskId, "PySpark 退出码: " + exitCode + "\n" + detail);
            }
        } catch (Exception e) {
            log.error("PySpark 管线执行失败", e);
            setTaskFailed(taskId, truncateError(e.getMessage()));
        }
    }

    /** 异步执行聚类重算：调用 run_cluster_only.py --k N [--no-merge]，仅重算 K-Means 聚类。 */
    private void executeClusterRecalc(Long taskId, int k, boolean mergeSimilar) {
        try {
            setTaskRunning(taskId);
            SparkAnalysisTask task = taskMapper.selectById(taskId);
            String dataVersion = (task != null && task.getDataVersion() != null && !task.getDataVersion().isBlank())
                    ? task.getDataVersion()
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            // 独立聚类脚本与主管线脚本同目录：run_cluster_only.py
            String pipelineScript = com.oufeng.ecommerceuserprofile.infrastructure.util.BigdataPathResolver.resolve(scriptPath);
            Path scriptDir = Path.of(pipelineScript).toAbsolutePath().getParent();
            String script = scriptDir.resolve("run_cluster_only.py").toString();
            ProcessBuilder pb;
            if (mergeSimilar) {
                pb = new ProcessBuilder(pythonPath, script,
                        "--k", String.valueOf(k),
                        "--data-version", dataVersion,
                        "--mysql-user", mysqlUser,
                        "--jdbc-url", jdbcUrl);
            } else {
                // 不自动合并：--no-merge 严格按 K 输出（data_version 带 _RAW 后缀）
                pb = new ProcessBuilder(pythonPath, script,
                        "--k", String.valueOf(k),
                        "--data-version", dataVersion,
                        "--no-merge",
                        "--mysql-user", mysqlUser,
                        "--jdbc-url", jdbcUrl);
            }
            // PySpark 需 JDK 17：显式把当前 JVM 的 java.home 传给子进程
            String javaHome = System.getProperty("java.home");
            if (javaHome != null && !javaHome.isBlank()) {
                pb.environment().put("JAVA_HOME", javaHome);
                String jdkBin = javaHome + File.separator + "bin";
                String oldPath = pb.environment().get("PATH");
                pb.environment().put("PATH", jdkBin + (oldPath == null || oldPath.isEmpty() ? "" : File.pathSeparator + oldPath));
            }
            pb.environment().put("MYSQL_PASSWORD", mysqlPassword);
            pb.environment().put("JAVA_HOME", javaHome);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            java.util.ArrayDeque<String> tail = new java.util.ArrayDeque<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(line -> {
                    log.info("[Cluster-{}] {}", taskId, line);
                    tail.addLast(line);
                    if (tail.size() > 40) tail.removeFirst();
                });
            }
            boolean finished = process.waitFor(15, TimeUnit.MINUTES);
            if (!finished) {
                log.warn("聚类重算超时 (15min)，强制终止: taskId={}", taskId);
                process.destroyForcibly();
                setTaskFailed(taskId, "聚类重算脚本执行超时 (15 分钟)，已自动终止");
                return;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                setTaskSucceeded(taskId, "聚类重算完成：K=" + k + "，user_cluster 已更新（版本 " + dataVersion + "）");
            } else {
                String detail = String.join("\n", tail);
                if (detail.length() > 450) detail = detail.substring(detail.length() - 450);
                setTaskFailed(taskId, "PySpark 退出码: " + exitCode + "\n" + detail);
            }
        } catch (Exception e) {
            log.error("聚类重算执行失败", e);
            setTaskFailed(taskId, truncateError(e.getMessage()));
        }
    }

    private void executeDirectoryImport(Long taskId, Path dir) {
        try {
            setTaskRunning(taskId);
            ImportReport report = orchestrator.importFromDirectory(dir);
            if (report.isSuccess()) {
                setTaskSucceeded(taskId, buildImportSummary(report));
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
    /** 天池数据集导入：保存天池 CSV → 同步调适配脚本转换（秒级）→ 异步导入转换产物 */
    public AnalysisTaskResponse createTianchiImport(MultipartFile file, String taskName, int limit, Long submitterId) {
        SparkAnalysisTask task = new SparkAnalysisTask();
        task.setTaskName(taskName);
        task.setTaskType("DATA_IMPORT");
        task.setTaskStatus("Pending");
        task.setDataVersion("TIANCHI_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        task.setSubmitterId(submitterId);
        taskMapper.insert(task);

        Path workDir;
        try {
            // 1. 保存原始 CSV（MultipartFile 必须在 HTTP 线程内持久化）
            workDir = Files.createTempDirectory("tianchi_");
            String name = file.getOriginalFilename();
            String safeName = name == null || name.isBlank() ? "tianchi.csv" : Path.of(name).getFileName().toString();
            Files.copy(file.getInputStream(), workDir.resolve(safeName));
            // 2. 调适配脚本转换（输出到 workDir/out/）
            Path outDir = workDir.resolve("out");
            runTianchiAdapter(workDir.resolve(safeName).toString(), outDir.toString(), limit);
            // 产物校验：用户数据必须存在；互动/交易至少一个（行为数据集→互动；发票数据集→交易）
            boolean hasUser = Files.isRegularFile(outDir.resolve("用户数据.csv"));
            boolean hasInteraction = Files.isRegularFile(outDir.resolve("互动数据.csv"));
            boolean hasTransaction = Files.isRegularFile(outDir.resolve("交易数据.csv"));
            if (!hasUser || (!hasInteraction && !hasTransaction)) {
                throw new IllegalStateException("转换产物不完整（缺少 用户数据.csv，且 互动数据/交易数据 均缺失）");
            }
        } catch (Exception e) {
            log.error("天池数据转换失败", e);
            setTaskFailed(task.getId(), "天池数据转换失败: " + truncateError(e.getMessage()));
            return TaskConverter.INSTANCE.toResponse(taskMapper.selectById(task.getId()));
        }
        // 3. 异步导入转换产物
        executor.submit(() -> executeDirectoryImport(task.getId(), workDir.resolve("out")));
        return TaskConverter.INSTANCE.toResponse(taskMapper.selectById(task.getId()));
    }

    /** 调用 tianchi_adapter.py 完成转换（同步等待，超时 120s） */
    private void runTianchiAdapter(String inputCsv, String outDir, int limit) throws Exception {
        String script = BigdataPathResolver.resolve("bigdata-scripts/tianchi_adapter.py");
        ProcessBuilder pb = new ProcessBuilder(pythonPath, script, inputCsv, "-o", outDir, "--limit", String.valueOf(limit));
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
                if (output.length() > 4000) break;
            }
        }
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("转换超时（120s）");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("适配脚本退出码 " + process.exitValue() + "：\n" + output);
        }
    }

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
        notifyTaskResult(taskId, true, summary);
    }

    private void setTaskFailed(Long taskId, String error) {
        // 错误信息翻译为"人话原因 + 技术日志"，对非开发人员友好
        String friendly = com.oufeng.ecommerceuserprofile.infrastructure.util.TaskErrorTranslator.translate(error);
        taskMapper.update(null, new LambdaUpdateWrapper<SparkAnalysisTask>()
                .eq(SparkAnalysisTask::getId, taskId)
                .set(SparkAnalysisTask::getTaskStatus, "Failed")
                .set(SparkAnalysisTask::getErrorMessage, friendly)
                .set(SparkAnalysisTask::getFinishedAt, LocalDateTime.now()));
        notifyTaskResult(taskId, false, friendly);
    }

    /** 任务结束（成功/失败）时给提交者发送系统通知，形成任务闭环提醒 */
    private void notifyTaskResult(Long taskId, boolean success, String detail) {
        try {
            SparkAnalysisTask task = taskMapper.selectById(taskId);
            if (task == null || task.getSubmitterId() == null) return;
            // 数据导入/生成任务归入「数据更新」通知；画像分析等任务归入「任务」通知
            boolean isDataTask = "DATA_IMPORT".equals(task.getTaskType()) || "DATA_GENERATE".equals(task.getTaskType());
            String title;
            String type;
            if (isDataTask) {
                title = success ? "数据更新完成" : "数据更新失败";
                type = "DATA";
            } else {
                title = success ? "任务完成" : "任务失败";
                type = success ? "TASK_SUCCESS" : "TASK_FAILED";
            }
            String taskName = task.getTaskName() == null ? "分析任务" : task.getTaskName();
            // 通知仅作完成提醒：成功附简短统计，失败不携带长错误（详见任务结果详情弹窗）
            String content;
            if (success) {
                String stat = (detail == null ? "" : detail.trim());
                if (stat.length() > 120) stat = stat.substring(0, 120) + "…";
                content = taskName + " 执行成功" + (stat.isEmpty() ? "" : "（" + stat + "）");
            } else {
                content = taskName + " 执行失败，原因详见任务结果详情";
            }
            // 任务结果统一广播：分析任务与数据导入/生成都会影响运营所看到的数据，让全员及时感知数据刷新
            notificationService.broadcast(type, title, content, "TASK", taskId);
            // 画像任务成功后：检测高价值用户流失风险，有结果则追加预警通知
            if (success && "PROFILE_FULL".equals(task.getTaskType())) {
                sendChurnAlert();
            }
        } catch (Exception e) {
            log.warn("发送任务通知失败: taskId={}, {}", taskId, e.getMessage());
        }
    }

    /**
     * 高价值用户流失预警：画像任务（数据刷新）后立即检测。
     * 判定：HIGH_VALUE 分层 ∩（近30天登录 < 2 次 或 15 天未活跃）→ 存在流失风险。
     * 有结果则广播预警通知（文案带数据时间戳，避免陈旧数据误导）。
     */
    private void sendChurnAlert() {
        try {
            String sql = """
                    SELECT s.user_id, u.user_code, s.segment_score, p.login_count_30d, p.last_active_at
                    FROM user_segment s
                    JOIN user_profile_summary p ON p.user_id = s.user_id
                    JOIN ecommerce_user u ON u.id = s.user_id
                    WHERE s.segment_code = 'HIGH_VALUE'
                      AND (p.login_count_30d < 2 OR p.last_active_at < DATE_SUB(NOW(), INTERVAL 15 DAY))
                    ORDER BY s.segment_score DESC
                    LIMIT 20
                    """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            if (rows == null || rows.isEmpty()) return;
            // 前 3 名用户编码举例（增强可读性）
            StringBuilder examples = new StringBuilder();
            int show = Math.min(3, rows.size());
            for (int i = 0; i < show; i++) {
                if (i > 0) examples.append("、");
                examples.append(rows.get(i).get("user_code"));
            }
            String content = String.format("基于 %s 画像数据，发现 %d 名高价值用户活跃度显著下降（近30天登录少于2次或15天未活跃），建议重点关注：%s 等。",
                    java.time.LocalDate.now(), rows.size(), examples);
            notificationService.broadcast("TASK_SUCCESS", "高价值用户流失预警", content, "TASK", null);
            log.info("流失预警：{} 名高价值用户存在流失风险", rows.size());
        } catch (Exception e) {
            log.warn("流失预警检测失败", e);
        }
    }

    /** 构建导入结果摘要（供存储到 error_message 字段） */
    private String buildImportSummary(ImportReport report) {
        Map<String, Object> summary = report.toSummary();
        return String.format("插入:%d 更新:%d 重分配:%d 错误:%d 跳过:%d",
                summary.get("totalInserted"), summary.get("totalUpdated"),
                summary.get("totalRemapped"), summary.get("totalErrors"), summary.get("totalSkipped"));
    }

    private String truncateError(String msg) {
        return msg == null ? null : (msg.length() > 500 ? msg.substring(0, 500) : msg);
    }
}
