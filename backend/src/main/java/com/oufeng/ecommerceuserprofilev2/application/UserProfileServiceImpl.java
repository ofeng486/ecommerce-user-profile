package com.oufeng.ecommerceuserprofilev2.application;

import com.oufeng.ecommerceuserprofilev2.domain.dto.profile.*;
import com.oufeng.ecommerceuserprofilev2.infrastructure.config.CacheConfig;
import com.oufeng.ecommerceuserprofilev2.infrastructure.mapper.UserProfileQueryMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户画像查询服务。
 * 画像结果由 Spark 写入 MySQL，本服务只读查询。
 * 对概览/分布统计等高频查询使用 @Cacheable 缓存，减少数据库压力。
 */
@Service
@Transactional(readOnly = true)
public class UserProfileServiceImpl implements IUserProfileService {

    private final UserProfileQueryMapper queryMapper;

    public UserProfileServiceImpl(UserProfileQueryMapper queryMapper) {
        this.queryMapper = queryMapper;
    }

    @Cacheable(value = CacheConfig.CACHE_PROFILE_OVERVIEW, key = "'overview'")
    public ProfileOverviewResponse getOverview() {
        return queryMapper.queryOverview();
    }

    @Cacheable(value = CacheConfig.CACHE_SEGMENT_DIST, key = "'segments'")
    public List<SegmentDistributionResponse> getSegmentDistribution() {
        return queryMapper.querySegmentDistribution();
    }

    @Cacheable(value = CacheConfig.CACHE_TAG_DIST, key = "#tagCode != null ? #tagCode : 'all'")
    public List<TagDistributionResponse> getTagDistribution(String tagCode) {
        return queryMapper.queryTagDistribution(tagCode);
    }

    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserProfileListItemResponse>
            listProfiles(String keyword, String segmentCode, int page, int size) {
        long offset = (long) page * Math.min(size, 100);
        long limit = Math.min(size, 100);
        List<UserProfileListItemResponse> records =
                queryMapper.queryProfiles(keyword, segmentCode, offset, limit);
        long total = queryMapper.countProfiles(keyword, segmentCode);
        var result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserProfileListItemResponse>(page, limit, total);
        result.setRecords(records);
        return result;
    }

    public UserProfileResponse getUserProfile(Long userId) {
        return queryMapper.queryUserProfile(userId)
                .orElseThrow(() -> new com.oufeng.ecommerceuserprofilev2.common.BusinessException(
                        com.oufeng.ecommerceuserprofilev2.common.ResultCode.NOT_FOUND, "电商用户不存在"));
    }
}
