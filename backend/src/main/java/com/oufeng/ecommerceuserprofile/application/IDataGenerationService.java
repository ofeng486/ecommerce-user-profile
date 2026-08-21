package com.oufeng.ecommerceuserprofile.application;

import com.oufeng.ecommerceuserprofile.domain.entity.SparkAnalysisTask;

/**
 * 数据生成服务接口。
 */
public interface IDataGenerationService {

    /** 异步生成数据并导入数据库。 */
    SparkAnalysisTask generateAndImport(DataGenerationParams params,
                                         String taskName, Long submitterId);
}
