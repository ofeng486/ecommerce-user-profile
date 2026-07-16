package com.oufeng.ecommerceuserprofilev2.application;

/**
 * 数据生成参数。
 *
 * @param users      用户数量（建议 100-10000）
 * @param products   商品数量（建议 50-5000）
 * @param behaviors  浏览行为数量（建议 500-100000）
 * @param orders     订单数量（建议 100-20000）
 * @param seed       随机种子（相同种子生成相同数据）
 */
public record DataGenerationParams(
        int users,
        int products,
        int behaviors,
        int orders,
        int seed
) {
    /** 默认参数：适合笔记本演示 */
    public static DataGenerationParams defaults() {
        return new DataGenerationParams(1000, 200, 10000, 2000, 2026);
    }

    /** 小规模：快速测试 */
    public static DataGenerationParams small() {
        return new DataGenerationParams(100, 50, 1000, 200, 2026);
    }

    /** 中等规模：毕设演示 */
    public static DataGenerationParams medium() {
        return new DataGenerationParams(5000, 500, 50000, 10000, 2026);
    }
}
