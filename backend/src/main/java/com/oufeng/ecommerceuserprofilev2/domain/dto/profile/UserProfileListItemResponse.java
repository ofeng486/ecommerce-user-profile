package com.oufeng.ecommerceuserprofilev2.domain.dto.profile;

import java.math.BigDecimal;

/** 用户画像分页列表项。 */
public record UserProfileListItemResponse(
        Long userId, String userCode, String gender, Integer age,
        String province, String city, long totalOrderCount,
        BigDecimal totalPaymentAmount, String segmentCode,
        String segmentName, BigDecimal segmentScore
) {}
