package com.oufeng.ecommerceuserprofilev2.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofilev2.domain.dto.task.AnalysisTaskResponse;
import com.oufeng.ecommerceuserprofilev2.domain.dto.task.CreateAnalysisTaskRequest;
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

    /** 分页查询分析任务列表。 */
    Page<AnalysisTaskResponse> listTasks(int page, int size);

    /** 查询单个任务详情。 */
    AnalysisTaskResponse getTask(Long taskId);

    /** 取消待处理或运行中的任务。 */
    AnalysisTaskResponse cancel(Long taskId);

    /** 删除任务。 */
    void delete(Long taskId);
}
