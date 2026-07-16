package com.oufeng.ecommerceuserprofilev2.infrastructure.importer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.Map;

/**
 * product 表导入器。
 * 校验规则：product_code 非空、unit_price >= 0、status 0/1。
 */
@Component
public class ProductImporter extends AbstractCsvImporter {

    @Override public String[] columnNames() {
        return new String[]{"id","product_code","category_id","product_name","brand_name","unit_price","status"};
    }

    public ProductImporter(JdbcTemplate jdbcTemplate) { super(jdbcTemplate); }

    @Override public String tableName() { return "product"; }

    @Override protected String insertSql() {
        return "INSERT INTO product (id,product_code,category_id,product_name,brand_name,unit_price,status) VALUES (?,?,?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE product_code=VALUES(product_code),category_id=VALUES(category_id),"
                + "product_name=VALUES(product_name),brand_name=VALUES(brand_name),unit_price=VALUES(unit_price),status=VALUES(status)";
    }

    @Override
    protected void validateRow(Map<String, String> row) {
        String code = get(row, "product_code");
        if (code.isBlank()) throw new IllegalArgumentException("商品编码不能为空");

        BigDecimal price = new BigDecimal(get(row, "unit_price"));
        if (price.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("商品单价不能为负数");

        int status = Integer.parseInt(get(row, "status"));
        if (status != 0 && status != 1) throw new IllegalArgumentException("status 必须为 0 或 1");
    }

    @Override
    protected void bindRow(PreparedStatement stmt, Map<String, String> row) throws Exception {
        setLong(stmt, 1, get(row, "id"));
        stmt.setString(2, get(row, "product_code"));
        setLong(stmt, 3, get(row, "category_id"));
        stmt.setString(4, get(row, "product_name"));
        stmt.setString(5, get(row, "brand_name"));
        setBigDecimal(stmt, 6, get(row, "unit_price"));
        stmt.setInt(7, Integer.parseInt(get(row, "status")));
    }
}
