package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.Map;
import java.util.List;

/**
 * product_category 表导入器。
 * 校验规则：category_level 必须为 1/2/3，status 必须为 0/1。
 */
@Component
public class ProductCategoryImporter extends AbstractCsvImporter {
    /** 业务唯一键（重复数据识别用） */
    protected String uniqueKeyColumn() { return "category_name"; }
    /** 同批导入时需同步映射的外键引用 */
    protected List<FkRef> foreignKeyRefs() { return List.of(new FkRef("product_category", "parent_id")); }


    @Override public String[] columnNames() {
        return new String[]{"id","parent_id","category_name","category_level","status"};
    }

    public ProductCategoryImporter(JdbcTemplate jdbcTemplate) { super(jdbcTemplate); }

    @Override public String tableName() { return "product_category"; }

    @Override protected String insertSql() {
        return "INSERT INTO product_category (id,parent_id,category_name,category_level,status) VALUES (?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),category_name=VALUES(category_name),"
                + "category_level=VALUES(category_level),status=VALUES(status)";
    }

    @Override
    protected void validateRow(Map<String, String> row) {
        String name = get(row, "category_name");
        if (name.isBlank()) throw new IllegalArgumentException("分类名称不能为空");

        int level = Integer.parseInt(get(row, "category_level"));
        if (level < 1 || level > 3) throw new IllegalArgumentException("分类层级必须在 1-3 之间");

        int status = Integer.parseInt(get(row, "status"));
        if (status != 0 && status != 1) throw new IllegalArgumentException("status 必须为 0 或 1");
    }

    @Override
    protected void bindRow(PreparedStatement stmt, Map<String, String> row) throws Exception {
        setLong(stmt, 1, get(row, "id"));
        setNullableLong(stmt, 2, get(row, "parent_id"));
        stmt.setString(3, get(row, "category_name"));
        stmt.setInt(4, Integer.parseInt(get(row, "category_level")));
        stmt.setInt(5, Integer.parseInt(get(row, "status")));
    }
}
