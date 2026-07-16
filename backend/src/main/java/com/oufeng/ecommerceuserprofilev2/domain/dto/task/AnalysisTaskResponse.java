package com.oufeng.ecommerceuserprofilev2.domain.dto.task;

import java.time.LocalDateTime;

/** 分析任务响应。 */
public record AnalysisTaskResponse(
        Long id, String taskName, String taskType, String taskStatus,
        String dataVersion, Long submitterId,
        LocalDateTime startedAt, LocalDateTime finishedAt,
        String errorMessage, java.time.Instant createdAt
) {}
