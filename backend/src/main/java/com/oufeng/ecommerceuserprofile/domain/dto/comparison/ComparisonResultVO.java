package com.oufeng.ecommerceuserprofile.domain.dto.comparison;

import java.math.BigDecimal;
import java.util.List;

/**
 * 画像对比分析结果 VO。
 * 封装人群包 A vs 人群包 B 的多维度对比统计数据。
 */
public record ComparisonResultVO(
        /** 人群包 A 名称 */
        String groupAName,
        /** 人群包 B 名称 */
        String groupBName,
        /** 人群包 A 总人数 */
        int groupACount,
        /** 人群包 B 总人数 */
        int groupBCount,
        /** 各维度对比统计 */
        List<DimensionStat> dimensions
) {
    /**
     * 单个对比维度的统计（如性别、年龄、客单价、分层）。
     */
    public record DimensionStat(
            /** 维度名称（如 "gender"、"age"、"avg_payment"、"segment"） */
            String dimension,
            /** 维度中文标签 */
            String label,
            /** 该维度下各值的对比数据 */
            List<DimensionItem> items
    ) {}

    /**
     * 维度值级别的对比数据。
     */
    public record DimensionItem(
            /** 维度值标签（如 "男"、"18-24"、"高价值用户"） */
            String label,
            /** 人群 A 该值人数 */
            int countA,
            /** 人群 B 该值人数 */
            int countB,
            /** 人群 A 占比 */
            BigDecimal ratioA,
            /** 人群 B 占比 */
            BigDecimal ratioB,
            /** 差异率（ratioA - ratioB），正数表示 A 更显著 */
            BigDecimal diff
    ) {}
}
