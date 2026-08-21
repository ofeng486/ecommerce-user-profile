package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.oufeng.ecommerceuserprofile.domain.entity.SparkAnalysisTask;
import com.oufeng.ecommerceuserprofile.domain.mapper.SparkAnalysisTaskMapper;
import com.oufeng.ecommerceuserprofile.infrastructure.importer.DataImportOrchestrator;
import com.oufeng.ecommerceuserprofile.infrastructure.importer.ImportReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 数据生成服务。
 * 调用 Python 脚本 (generate_data.py) 批量生成电商模拟数据，
 * 生成完成后自动导入到 MySQL 数据库。
 *
 * 流程：Python 生成 CSV → DataImportOrchestrator 导入 MySQL → 清理临时文件
 */
@Service
public class DataGenerationServiceImpl implements IDataGenerationService {

    private static final Logger log = LoggerFactory.getLogger(DataGenerationServiceImpl.class);

    private final SparkAnalysisTaskMapper taskMapper;
    private final DataImportOrchestrator orchestrator;
    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    @Value("${spark.pipeline.script-path:../bigdata-scripts/spark/run_local_pipeline.py}")
    private String sparkScriptPath;

    /** Python 数据生成脚本路径 */
    @Value("${data.generator.script-path:../bigdata-scripts/generate_data.py}")
    private String generatorScriptPath;

    /** Python 可执行文件路径 */
    @Value("${data.generator.python-path:python3}")
    private String pythonPath;

    public DataGenerationServiceImpl(SparkAnalysisTaskMapper taskMapper,
                                  DataImportOrchestrator orchestrator) {
        this.taskMapper = taskMapper;
        this.orchestrator = orchestrator;
    }

    /**
     * 异步生成数据并导入数据库。
     *
     * @param params    生成参数
     * @param taskName  任务名称
     * @param submitterId 提交者 ID
     * @return 任务记录
     */
    public SparkAnalysisTask generateAndImport(DataGenerationParams params, String taskName, Long submitterId) {
        // 创建任务记录
        SparkAnalysisTask task = new SparkAnalysisTask();
        task.setTaskName(taskName);
        task.setTaskType("DATA_GENERATE");
        task.setTaskStatus("Pending");
        task.setDataVersion("GEN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        task.setSubmitterId(submitterId);
        taskMapper.insert(task);

        // 异步执行：生成 CSV → 导入 MySQL → 清理
        executor.submit(() -> executeGenerateAndImport(task.getId(), params));
        return taskMapper.selectById(task.getId());
    }

    // ─── 异步执行 ───

    private void executeGenerateAndImport(Long taskId, DataGenerationParams params) {
        Path outputDir = null;
        try {
            setTaskRunning(taskId);
            log.info("[DataGen-{}] 开始生成数据: users={}, products={}, behaviors={}, orders={}, seed={}",
                    taskId, params.users(), params.products(), params.behaviors(), params.orders(), params.seed());

            // 第1步：创建持久化输出目录（不删，Spark 画像任务需要读这些 CSV）
            // 使用与 spark.pipeline.import-dir 相同的默认值（按工作目录向上探测，兼容 IDEA/Maven 启动）
            outputDir = Path.of(com.oufeng.ecommerceuserprofile.infrastructure.util.BigdataPathResolver.resolve("../bigdata-scripts/generated-data"));
            Files.createDirectories(outputDir);
            log.info("[DataGen-{}] 输出目录: {}", taskId, outputDir);

            // 第2步：调用 Python 脚本生成 CSV
            setTaskProgress(taskId, "正在生成 CSV 数据...");
            int exitCode = runPythonGenerator(params, outputDir);
            if (exitCode != 0) {
                setTaskFailed(taskId, "Python 数据生成失败，退出码: " + exitCode);
                return;
            }
            log.info("[DataGen-{}] CSV 生成完成", taskId);

            // 第3步：导入到 MySQL
            setTaskProgress(taskId, "正在导入数据库...");
            ImportReport report = orchestrator.importFromDirectory(outputDir);
            log.info("[DataGen-{}] 数据导入完成: 插入={}, 更新={}, 错误={}, 跳过={}",
                    taskId, report.getTotalInserted(), report.getTotalUpdated(),
                    report.getTotalErrors(), report.getTotalSkipped());

            // 不删除 CSV —— Spark 画像任务需要读取这些文件

            // 第4步：更新任务状态
            if (report.isSuccess() && report.getTotalInserted() + report.getTotalUpdated() > 0) {
                setTaskSucceeded(taskId, buildSummary(report));
            } else {
                setTaskFailed(taskId, buildSummary(report));
            }

        } catch (Exception e) {
            log.error("[DataGen-{}] 数据生成导入失败", taskId, e);
            setTaskFailed(taskId, truncate(e.getMessage()));
        }
    }

    /** 调用 Python 脚本生成 CSV 数据 */
    private int runPythonGenerator(DataGenerationParams params, Path outputDir) throws Exception {
        // 脚本相对路径按工作目录向上探测解析（兼容 IDEA 项目根 / Maven backend 两种工作目录）
        String script = com.oufeng.ecommerceuserprofile.infrastructure.util.BigdataPathResolver.resolve(generatorScriptPath);
        ProcessBuilder pb = new ProcessBuilder(
                pythonPath, script,
                "--output", outputDir.toString(),
                "--users", String.valueOf(params.users()),
                "--products", String.valueOf(params.products()),
                "--behaviors", String.valueOf(params.behaviors()),
                "--orders", String.valueOf(params.orders()),
                "--seed", String.valueOf(params.seed())
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 实时输出 Python 脚本的日志
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[DataGen] {}", line);
            }
        }

        if (!process.waitFor(30, TimeUnit.MINUTES)) {
            log.warn("数据生成脚本超时 (30min)，强制终止");
            process.destroyForcibly();
            return -1;
        }
        return process.exitValue();
    }

    // ─── 辅助方法 ───

    private void setTaskRunning(Long taskId) {
        taskMapper.update(null, new LambdaUpdateWrapper<SparkAnalysisTask>()
                .eq(SparkAnalysisTask::getId, taskId)
                .set(SparkAnalysisTask::getTaskStatus, "Running")
                .set(SparkAnalysisTask::getStartedAt, LocalDateTime.now()));
    }

    private void setTaskProgress(Long taskId, String progress) {
        taskMapper.update(null, new LambdaUpdateWrapper<SparkAnalysisTask>()
                .eq(SparkAnalysisTask::getId, taskId)
                .set(SparkAnalysisTask::getErrorMessage, progress));
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

    private String buildSummary(ImportReport report) {
        return String.format("生成并导入完成: 插入 %d 行, 更新 %d 行, 错误 %d 行, 跳过 %d 行",
                report.getTotalInserted(), report.getTotalUpdated(),
                report.getTotalErrors(), report.getTotalSkipped());
    }

    private String truncate(String msg) {
        return msg == null ? null : (msg.length() > 500 ? msg.substring(0, 500) : msg);
    }
}
