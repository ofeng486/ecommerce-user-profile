package com.oufeng.ecommerceuserprofile.infrastructure.mapper;

import com.oufeng.ecommerceuserprofile.domain.dto.profile.*;
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

    /** 标签交叉矩阵：活跃档 × 消费档 */
    List<TagCrossResponse> queryTagCross();

    /** 分页查询用户画像列表（支持省份/金额区间/标签与排序） */
    List<UserProfileListItemResponse> queryProfiles(
            @Param("keyword") String keyword,
            @Param("segmentCode") String segmentCode,
            @Param("province") String province,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("tagCode") String tagCode,
            @Param("tagValue") String tagValue,
            @Param("orderColumn") String orderColumn,
            @Param("orderDir") String orderDir,
            @Param("offset") long offset,
            @Param("limit") long limit);

    /** 分页查询用户画像总数（省份/金额区间/标签过滤） */
    long countProfiles(
            @Param("keyword") String keyword,
            @Param("segmentCode") String segmentCode,
            @Param("province") String province,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("tagCode") String tagCode,
            @Param("tagValue") String tagValue);

    /** 画像核心指标（总数/订单/消费/流失风险） */
    ProfileMetricsResponse queryProfileMetrics();

    /** TOP 省份消费排名（省名/消费金额/画像用户数，最多 6 条） */
    List<java.util.Map<String, Object>> queryProvinceAmountRanking();

    /** 查询单个用户画像详情 */
    Optional<UserProfileResponse> queryUserProfile(@Param("userId") Long userId);

    /** 查询用户的行为标签列表（画像详情用） */
    java.util.List<java.util.Map<String, Object>> queryUserTags(@Param("userId") Long userId);

    // ─── 智能人群圈选 ───

    /** 全量用户总数（无条件时使用） */
    long countAllUsers();

    /** 全量用户分页（无条件时使用） */
    List<UserProfileListItemResponse> queryAllProfilesPaged(
            @Param("offset") long offset, @Param("limit") long limit);

    /** 按用户 ID 列表查画像（人群包导出用） */
    List<UserProfileListItemResponse> queryUsersByIds(@Param("userIds") java.util.List<Long> userIds);

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
    java.util.List<java.util.Map<String, Object>> compareTags(@Param("userIds") List<Long> userIds, @Param("tagCode") String tagCode);

    java.util.List<java.util.Map<String, Object>> compareConsumptionLevel(@Param("userIds") List<Long> userIds);

    /** 全部分类 id → 名称（画像对比品类维度展示用） */
    java.util.List<java.util.Map<String, Object>> queryCategoryNames();
}
