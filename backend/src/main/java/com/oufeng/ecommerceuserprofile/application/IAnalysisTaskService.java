package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.domain.dto.task.AnalysisTaskResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.task.CreateAnalysisTaskRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理员分析任务接口。
 */
public interface IAnalysisTaskService {

    /** 创建 PROFILE_FULL 分析任务，异步执行 PySpark 管线。 */
    AnalysisTaskResponse create(CreateAnalysisTaskRequest request, Long submitterId);

    /** 创建 DATA_IMPORT 任务从目录导入。 */
    AnalysisTaskResponse createDirectoryImport(String taskName, String dataVersion,
                                                String taskType, Long submitterId);

    /** 创建 DATA_IMPORT 任务，从上传文件异步导入。 */
    AnalysisTaskResponse createUploadImport(MultipartFile[] files, String taskName, Long submitterId);

    /** 天池数据集：上传 CSV → 调适配脚本转换 → 异步导入 */
    AnalysisTaskResponse createTianchiImport(MultipartFile file, String taskName, int limit, Long submitterId);

    /** 创建 CLUSTER_RECALC 任务：按指定 K 值独立重算 K-Means 聚类。mergeSimilar 控制是否自动合并相似簇。 */
    AnalysisTaskResponse createClusterRecalc(int k, Long submitterId, boolean mergeSimilar);

    /** 分页查询分析任务列表。 */
    Page<AnalysisTaskResponse> listTasks(int page, int size, String taskType, String taskStatus, String keyword, String orderBy, String orderDir);

    /** 查询单个任务详情。 */
    AnalysisTaskResponse getTask(Long taskId);

    /** 取消待处理或运行中的任务。 */
    AnalysisTaskResponse cancel(Long taskId);

    /** 删除任务。 */
    void delete(Long taskId);
}
