package com.oufeng.ecommerceuserprofile.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 复购与留存分析查询（购买次数分布/复购率/购买间隔/留存 cohort）。
 */
@Mapper
public interface RepeatAnalysisMapper {

    /** 用户购买次数分布（含 0 次） */
    List<Map<String, Object>> queryPurchaseDistribution();

    /** 复购指标：总用户/有购用户/多购用户 */
    Map<String, Object> queryRepeatRate();

    /** 多单用户平均购买间隔（天）：(最近-最早)/(单数-1) 的均值 */
    Map<String, Object> queryAvgInterval();

    /** 月度首购留存 cohort：首购月 × 间隔月数 → 留存用户数 */
    List<Map<String, Object>> queryRetentionCohort();

    /** 高复购用户 Top10 */
    List<Map<String, Object>> queryTopRepeat();
}
