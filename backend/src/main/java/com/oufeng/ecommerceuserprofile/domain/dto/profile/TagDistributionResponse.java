package com.oufeng.ecommerceuserprofile.domain.dto.profile;

import java.math.BigDecimal;

/**
 * 标签值分布统计。
 *
 * @param tagValue        展示用标签值（品类为合并后类目名，如"服装鞋靴"）
 * @param filterTagValue  下钻用原始值（逗号分隔多个 tag_value，如 "22,7"；非品类标签与原值相同）
 * @param avgAmount       该标签下用户人均消费（品类双指标用）
 */
public record TagDistributionResponse(String tagCode, String tagValue, String filterTagValue,
                                      long userCount, BigDecimal userRatio, BigDecimal avgAmount) {}
