package com.oufeng.ecommerceuserprofilev2.domain.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建分析任务请求。
 */
public record CreateAnalysisTaskRequest(
        @NotBlank(message = "任务名称不能为空")
        @Size(max = 200, message = "任务名称不能超过200个字符")
        String taskName,

        @NotBlank(message = "任务类型不能为空")
        @Size(max = 50, message = "任务类型不能超过50个字符")
        String taskType,

        @NotBlank(message = "数据版本不能为空")
        @Size(max = 50, message = "数据版本不能超过50个字符")
        String dataVersion
) {}
