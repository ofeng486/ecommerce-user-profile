package com.oufeng.ecommerceuserprofile.domain.dto.profile;

import java.math.BigDecimal;

/** 用户画像总览统计。 */
public record ProfileOverviewResponse(
        long totalUsers,
        long profiledUsers,
        long highValueUsers,
        long atRiskUsers,
        BigDecimal totalPaymentAmount,
        String dataVersion,
        String calculatedAt
) {}
