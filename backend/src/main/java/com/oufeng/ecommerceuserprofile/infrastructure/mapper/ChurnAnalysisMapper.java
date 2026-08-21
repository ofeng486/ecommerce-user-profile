package com.oufeng.ecommerceuserprofile.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 流失预警查询：基于画像 recency_days 分级（>180 高 / 90-180 中 / 30-90 低 / <=30 活跃 / 无订单）。
 */
@Mapper
public interface ChurnAnalysisMapper {

    /** 流失等级分布 */
    List<Map<String, Object>> queryLevels();

    /** 流失名单分页（等级筛选，支持 orderBy=recencyDays|orderCount|totalPaymentAmount 排序） */
    List<Map<String, Object>> queryChurnList(@Param("level") String level,
                                             @Param("offset") long offset, @Param("limit") int limit,
                                             @Param("orderBy") String orderBy, @Param("orderDir") String orderDir);

    /** 流失名单总数 */
    long countChurnList(@Param("level") String level);

    /** 数据版本与统计截止 */
    Map<String, Object> queryDataVersion();
}
