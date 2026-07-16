package com.oufeng.ecommerceuserprofilev2.domain.dto.profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 单个用户的画像汇总与分层信息。 */
public record UserProfileResponse(
        Long userId, String userCode, String gender, Integer age,
        String province, String city, long totalOrderCount,
        BigDecimal totalPaymentAmount, BigDecimal averageOrderAmount,
        long browseCount30d, long loginCount30d, LocalDateTime lastActiveAt,
        String segmentCode, String segmentName, BigDecimal segmentScore
) {}
