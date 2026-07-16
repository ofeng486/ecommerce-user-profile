package com.oufeng.ecommerceuserprofilev2.domain.dto.profile;

import java.math.BigDecimal;

/** 标签值分布统计。 */
public record TagDistributionResponse(String tagCode, String tagValue, long userCount, BigDecimal userRatio) {}
