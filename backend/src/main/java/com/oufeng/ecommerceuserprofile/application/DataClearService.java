package com.oufeng.ecommerceuserprofile.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 电商数据清空服务。
 * 清空所有模拟生成的业务数据（用户、商品、订单、行为、画像结果），
 * 保留系统管理数据（系统用户、登录日志、标签定义、任务历史等）。
 */
@Service
public class DataClearService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataClearService.class);

    private final JdbcTemplate jdbc;

    /** 按外键依赖顺序排列（子表在前），避免外键约束冲突 */
    private static final String[] BUSINESS_TABLES = {
            "sales_order_item",
            "user_browse_behavior",
            "user_login_behavior",
            "sales_order",
            "user_profile_tag",
            "user_profile_summary",
            "user_segment",
            "ads_user_rfm",
            "product",
            "product_category",
            "ecommerce_user"
    };

    public DataClearService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 清空所有电商业务数据并返回各表删除行数。
     * 实现说明：
     *   - 用 DELETE 替代 TRUNCATE，因为 TRUNCATE 在 MySQL 8.0+ 上对被外键引用的表
     *     即使设置了 FOREIGN_KEY_CHECKS=0 也会报错 1701（Cannot truncate ... referenced in FK），
     *     会导致 product_category 等被外键引用的表清空失败，旧数据残留 → 下次导入时
     *     uniqueKey 重映射会把新数据 id 改写到旧 id，而 CSV 的外键引用仍指向新 id → 外键断裂 → 大量错误
     *   - DELETE 完后再 ALTER TABLE ... AUTO_INCREMENT = 1，确保下次生成数据 id 从 1 开始
     */
    @Transactional
    public Map<String, Long> clearAll() {
        Map<String, Long> result = new LinkedHashMap<>();

        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            for (String table : BUSINESS_TABLES) {
                Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
                // DELETE 不受 FK_CHECKS 影响，被外键引用的表也能清空
                jdbc.execute("DELETE FROM " + table);
                // 重置 AUTO_INCREMENT，避免下次导入时 id 跳号（被外键引用的子表 id 全部打乱会导致数据关联错误）
                jdbc.execute("ALTER TABLE " + table + " AUTO_INCREMENT = 1");
                result.put(table, count != null ? count : 0L);
            }
        } finally {
            jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
        }

        // 清空 generated-data 目录下的 CSV 文件
        clearGeneratedCsvFiles();

        LOGGER.info("电商业务数据已全部清空，共处理 {} 张表", BUSINESS_TABLES.length);
        return result;
    }

    /** 清空 generated-data 目录下的 CSV 文件。 */
    private void clearGeneratedCsvFiles() {
        try {
            Path dir = Paths.get("../bigdata-scripts/generated-data");
            if (Files.isDirectory(dir)) {
                try (Stream<Path> files = Files.list(dir)) {
                    files.filter(Files::isRegularFile)
                         .filter(f -> f.getFileName().toString().endsWith(".csv"))
                         .forEach(f -> {
                             try { Files.deleteIfExists(f); }
                             catch (IOException ignored) { }
                         });
                }
            }
        } catch (IOException e) {
            LOGGER.warn("清空 CSV 文件失败: {}", e.getMessage());
        }
    }
}
