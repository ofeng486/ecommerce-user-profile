package com.oufeng.ecommerceuserprofile.domain.converter;

import com.oufeng.ecommerceuserprofile.domain.dto.task.AnalysisTaskResponse;
import com.oufeng.ecommerceuserprofile.domain.entity.SparkAnalysisTask;
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
