package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.domain.dto.profile.*;

import java.util.List;

/**
 * 用户画像查询接口（只读服务）。
 */
public interface IUserProfileService {

    /** 画像概览统计。 */
    ProfileOverviewResponse getOverview();

    /** 用户分层分布统计。 */
    List<SegmentDistributionResponse> getSegmentDistribution();

    /** 标签分布统计。 */
    List<TagDistributionResponse> getTagDistribution(String tagCode);

    /** 标签交叉矩阵：活跃档 × 消费档。 */
    List<TagCrossResponse> getTagCross();

    /** 分页查询用户画像列表。 */
    Page<UserProfileListItemResponse> listProfiles(String keyword, String segmentCode, String province,
            Double minAmount, Double maxAmount, String tagCode, String tagValue,
            String orderBy, String orderDir, int page, int size);

    /** 画像列表导出 CSV（复用列表筛选条件） */
    byte[] exportUsersCsv(String keyword, String segmentCode, String province,
            Double minAmount, Double maxAmount, String tagCode, String tagValue,
            String orderBy, String orderDir);

    /** 画像核心指标（总数/订单/消费/流失风险） */
    ProfileMetricsResponse getProfileMetrics();

    /** TOP 省份消费排名 */
    java.util.List<java.util.Map<String, Object>> getProvinceAmountRanking();

    /** 查询单个用户画像详情。 */
    UserProfileResponse getUserProfile(Long userId);
}
