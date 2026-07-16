package com.oufeng.ecommerceuserprofilev2.infrastructure.importer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.Set;

/**
 * sales_order 表导入器。
 * 校验规则：order_status 枚举、金额一致性 (payment = total - discount)、金额 >= 0。
 */
@Component
public class SalesOrderImporter extends AbstractCsvImporter {

    private static final Set<String> VALID_STATUS =
            Set.of("Pending", "Paid", "Shipped", "Completed", "Cancelled", "Refunded");

    @Override public String[] columnNames() {
        return new String[]{"id","order_no","user_id","order_status","total_amount","discount_amount","payment_amount","payment_method","ordered_at","paid_at","completed_at"};
    }

    public SalesOrderImporter(JdbcTemplate jdbcTemplate) { super(jdbcTemplate); }

    @Override public String tableName() { return "sales_order"; }

    @Override protected String insertSql() {
        return "INSERT INTO sales_order (id,order_no,user_id,order_status,total_amount,discount_amount,payment_amount,payment_method,ordered_at,paid_at,completed_at) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE order_no=VALUES(order_no),user_id=VALUES(user_id),"
                + "order_status=VALUES(order_status),total_amount=VALUES(total_amount),discount_amount=VALUES(discount_amount),"
                + "payment_amount=VALUES(payment_amount),payment_method=VALUES(payment_method),ordered_at=VALUES(ordered_at),"
                + "paid_at=VALUES(paid_at),completed_at=VALUES(completed_at)";
    }

    @Override
    protected void validateRow(Map<String, String> row) {
        String orderNo = get(row, "order_no");
        if (orderNo.isBlank()) throw new IllegalArgumentException("订单号不能为空");

        String status = get(row, "order_status");
        if (!VALID_STATUS.contains(status))
            throw new IllegalArgumentException("订单状态无效: " + status + "，有效值: " + VALID_STATUS);

        BigDecimal total = new BigDecimal(get(row, "total_amount"));
        BigDecimal discount = new BigDecimal(get(row, "discount_amount"));
        if (total.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("订单金额不能为负数");
        if (discount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("折扣金额不能为负数");
        if (discount.compareTo(total) > 0) throw new IllegalArgumentException("折扣金额不能大于订单金额");
    }

    @Override
    protected void bindRow(PreparedStatement stmt, Map<String, String> row) throws Exception {
        setLong(stmt, 1, get(row, "id"));
        stmt.setString(2, get(row, "order_no"));
        setLong(stmt, 3, get(row, "user_id"));
        stmt.setString(4, get(row, "order_status"));
        setBigDecimal(stmt, 5, get(row, "total_amount"));
        setBigDecimal(stmt, 6, get(row, "discount_amount"));
        setBigDecimal(stmt, 7, get(row, "payment_amount"));
        setNullableString(stmt, 8, get(row, "payment_method"));
        setTimestamp(stmt, 9, get(row, "ordered_at"));
        setNullableTimestamp(stmt, 10, get(row, "paid_at"));
        setNullableTimestamp(stmt, 11, get(row, "completed_at"));
    }
}
