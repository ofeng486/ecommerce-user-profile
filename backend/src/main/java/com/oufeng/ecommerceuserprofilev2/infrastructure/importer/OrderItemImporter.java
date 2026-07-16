package com.oufeng.ecommerceuserprofilev2.infrastructure.importer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.Map;

/**
 * sales_order_item 表导入器。
 * 校验规则：quantity > 0、unit_price >= 0、item_amount = unit_price * quantity (近似校验)。
 */
@Component
public class OrderItemImporter extends AbstractCsvImporter {

    @Override public String[] columnNames() {
        return new String[]{"id","order_id","product_id","product_name_snapshot","unit_price","quantity","item_amount"};
    }

    public OrderItemImporter(JdbcTemplate jdbcTemplate) { super(jdbcTemplate); }

    @Override public String tableName() { return "sales_order_item"; }

    @Override protected String insertSql() {
        return "INSERT INTO sales_order_item (id,order_id,product_id,product_name_snapshot,unit_price,quantity,item_amount) "
                + "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE order_id=VALUES(order_id),product_id=VALUES(product_id),"
                + "product_name_snapshot=VALUES(product_name_snapshot),unit_price=VALUES(unit_price),quantity=VALUES(quantity),"
                + "item_amount=VALUES(item_amount)";
    }

    @Override
    protected void validateRow(Map<String, String> row) {
        int quantity = Integer.parseInt(get(row, "quantity"));
        if (quantity <= 0) throw new IllegalArgumentException("商品数量必须大于 0");

        BigDecimal unitPrice = new BigDecimal(get(row, "unit_price"));
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("单价不能为负数");

        BigDecimal itemAmount = new BigDecimal(get(row, "item_amount"));
        BigDecimal expected = unitPrice.multiply(BigDecimal.valueOf(quantity));
        // 允许 0.02 的浮点误差
        if (itemAmount.subtract(expected).abs().compareTo(new BigDecimal("0.02")) > 0)
            throw new IllegalArgumentException(String.format(
                    "明细金额 %.2f 与 单价%.2f × 数量%d = %.2f 不一致",
                    itemAmount, unitPrice, quantity, expected));
    }

    @Override
    protected void bindRow(PreparedStatement stmt, Map<String, String> row) throws Exception {
        setLong(stmt, 1, get(row, "id"));
        setLong(stmt, 2, get(row, "order_id"));
        setLong(stmt, 3, get(row, "product_id"));
        stmt.setString(4, get(row, "product_name_snapshot"));
        setBigDecimal(stmt, 5, get(row, "unit_price"));
        stmt.setInt(6, Integer.parseInt(get(row, "quantity")));
        setBigDecimal(stmt, 7, get(row, "item_amount"));
    }
}
