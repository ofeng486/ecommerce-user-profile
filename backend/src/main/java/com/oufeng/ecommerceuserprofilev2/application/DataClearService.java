package com.oufeng.ecommerceuserprofilev2.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
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
    private final CacheManager cacheManager;

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

    public DataClearService(JdbcTemplate jdbc, CacheManager cacheManager) {
        this.jdbc = jdbc;
        this.cacheManager = cacheManager;
    }

    /**
     * 清空所有电商业务数据并返回各表删除行数。
     */
    @Transactional
    public Map<String, Long> clearAll() {
        Map<String, Long> result = new LinkedHashMap<>();

        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            for (String table : BUSINESS_TABLES) {
                Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
                if (count != null && count > 0) {
                    jdbc.execute("TRUNCATE TABLE " + table);
                }
                result.put(table, count != null ? count : 0L);
            }
        } finally {
            jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
        }

        // 清除画像缓存
        clearCaches();

        // 清空 generated-data 目录下的 CSV 文件
        clearGeneratedCsvFiles();

        LOGGER.info("电商业务数据已全部清空，共处理 {} 张表", BUSINESS_TABLES.length);
        return result;
    }

    /** 清除画像相关缓存，确保前端查询实时反映清空结果。 */
    private void clearCaches() {
        try {
            var overview = cacheManager.getCache("profileOverview");
            var segments = cacheManager.getCache("profileSegmentDist");
            var tags = cacheManager.getCache("profileTagDist");
            if (overview != null) overview.clear();
            if (segments != null) segments.clear();
            if (tags != null) tags.clear();
        } catch (Exception e) {
            LOGGER.warn("清除画像缓存失败: {}", e.getMessage());
        }
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
