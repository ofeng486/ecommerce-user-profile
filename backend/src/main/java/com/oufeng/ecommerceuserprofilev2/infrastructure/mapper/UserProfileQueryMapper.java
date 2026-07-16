package com.oufeng.ecommerceuserprofilev2.infrastructure.mapper;

import com.oufeng.ecommerceuserprofilev2.domain.dto.profile.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户画像查询 Mapper（只读原生 SQL）。
 * 画像结果由 Spark 写入 MySQL，使用原生 SQL 只读查询。
 */
@Mapper
public interface UserProfileQueryMapper {

    /** 画像概览统计 */
    ProfileOverviewResponse queryOverview();

    /** 用户分层分布 */
    List<SegmentDistributionResponse> querySegmentDistribution();

    /** 标签分布统计（tagCode 为空时查全部） */
    List<TagDistributionResponse> queryTagDistribution(@Param("tagCode") String tagCode);

    /** 分页查询用户画像列表 */
    List<UserProfileListItemResponse> queryProfiles(
            @Param("keyword") String keyword,
            @Param("segmentCode") String segmentCode,
            @Param("offset") long offset,
            @Param("limit") long limit);

    /** 分页查询用户画像总数 */
    long countProfiles(
            @Param("keyword") String keyword,
            @Param("segmentCode") String segmentCode);

    /** 查询单个用户画像详情 */
    Optional<UserProfileResponse> queryUserProfile(@Param("userId") Long userId);

    // ─── 智能人群圈选 ───

    /** 全量用户总数（无条件时使用） */
    long countAllUsers();

    /** 全量用户分页（无条件时使用） */
    List<UserProfileListItemResponse> queryAllProfilesPaged(
            @Param("offset") long offset, @Param("limit") long limit);

    /** 人群圈选预估人数 */
    long countAudience(
            @Param("conditions") java.util.List<java.util.Map<String, Object>> conditions,
            @Param("logic") String logic);

    /** 人群圈选分页查询 */
    List<UserProfileListItemResponse> queryAudience(
            @Param("conditions") java.util.List<java.util.Map<String, Object>> conditions,
            @Param("logic") String logic,
            @Param("offset") long offset,
            @Param("limit") long limit);

    // ─── 画像对比分析 ───

    /** 性别分布对比 */
    java.util.List<java.util.Map<String, Object>> compareGender(@Param("userIds") List<Long> userIds);

    /** 年龄段分布对比 */
    java.util.List<java.util.Map<String, Object>> compareAge(@Param("userIds") List<Long> userIds);

    /** 分层分布对比 */
    java.util.List<java.util.Map<String, Object>> compareSegment(@Param("userIds") List<Long> userIds);

    /** 平均客单价对比 */
    java.util.Map<String, Object> compareAvgPayment(@Param("userIds") List<Long> userIds);

    /** 标签值分布对比 */
    java.util.List<java.util.Map<String, Object>> compareTags(@Param("userIds") List<Long> userIds);

    java.util.List<java.util.Map<String, Object>> compareConsumptionLevel(@Param("userIds") List<Long> userIds);
}
