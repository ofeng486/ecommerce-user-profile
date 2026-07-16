package com.oufeng.ecommerceuserprofilev2.domain.dto.profile;

import java.math.BigDecimal;

/** 用户画像总览统计。 */
public record ProfileOverviewResponse(
        long totalUsers,
        long profiledUsers,
        long highValueUsers,
        BigDecimal totalPaymentAmount
) {}
