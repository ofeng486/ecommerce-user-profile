package com.oufeng.ecommerceuserprofile.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 标签定义表结构自动迁移器（必须先于 Seeder 执行）。
 * 启动时检查 profile_tag_definition 是否缺少 source_table/rule_expression 列，
 * 缺失则执行 ALTER 补齐（MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，故先查 information_schema）。
 */
@Component
@Order(1)
public class TagSchemaMigrator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TagSchemaMigrator.class);

    private final JdbcTemplate jdbc;

    public TagSchemaMigrator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            List<Map<String, Object>> cols = jdbc.queryForList(
                    "SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'profile_tag_definition'");
            boolean hasSource = cols.stream()
                    .anyMatch(m -> "source_table".equals(String.valueOf(m.get("COLUMN_NAME"))));
            if (!hasSource) {
                jdbc.execute("ALTER TABLE profile_tag_definition "
                        + "ADD COLUMN source_table VARCHAR(50) NULL COMMENT '标签计算数据源表（白名单）' AFTER calculation_rule, "
                        + "ADD COLUMN rule_expression VARCHAR(2000) NULL COMMENT '标签计算规则表达式（SQL CASE WHEN，经安全校验）' AFTER source_table");
                log.info("✅ 标签表结构迁移完成：新增 source_table / rule_expression 列");
            }
        } catch (Exception e) {
            log.warn("标签表结构迁移失败（不影响系统启动）: {}", e.getMessage());
        }
    }
}
