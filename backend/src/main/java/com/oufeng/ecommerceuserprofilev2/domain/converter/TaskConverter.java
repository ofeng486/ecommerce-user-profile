package com.oufeng.ecommerceuserprofilev2.domain.converter;

import com.oufeng.ecommerceuserprofilev2.domain.dto.task.AnalysisTaskResponse;
import com.oufeng.ecommerceuserprofilev2.domain.entity.SparkAnalysisTask;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * SparkAnalysisTask Entity ↔ DTO 转换器。
 */
@Mapper
public interface TaskConverter {

    TaskConverter INSTANCE = Mappers.getMapper(TaskConverter.class);

    /** Entity → Response DTO */
    AnalysisTaskResponse toResponse(SparkAnalysisTask entity);
}
