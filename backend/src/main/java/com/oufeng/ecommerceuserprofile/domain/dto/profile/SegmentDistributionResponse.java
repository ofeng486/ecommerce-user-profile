package com.oufeng.ecommerceuserprofile.domain.dto.profile;

/** 用户分层分布。 */
public record SegmentDistributionResponse(String segmentCode, String segmentName, long userCount) {}
