package com.oufeng.ecommerceuserprofilev2.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofilev2.domain.dto.profile.*;

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

    /** 分页查询用户画像列表。 */
    Page<UserProfileListItemResponse> listProfiles(String keyword, String segmentCode, int page, int size);

    /** 查询单个用户画像详情。 */
    UserProfileResponse getUserProfile(Long userId);
}
