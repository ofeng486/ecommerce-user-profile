package com.oufeng.ecommerceuserprofilev2.infrastructure.importer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.Map;

/**
 * ecommerce_user 表导入器。
 * 校验规则：user_code 非空、age 1-120、gender 枚举、status 0/1。
 */
@Component
public class EcommerceUserImporter extends AbstractCsvImporter {

    @Override public String[] columnNames() {
        return new String[]{"id","user_code","gender","age","province","city","register_channel","membership_level","registered_at","status"};
    }

    public EcommerceUserImporter(JdbcTemplate jdbcTemplate) { super(jdbcTemplate); }

    @Override public String tableName() { return "ecommerce_user"; }

    @Override protected String insertSql() {
        return "INSERT INTO ecommerce_user (id,user_code,gender,age,province,city,register_channel,membership_level,registered_at,status) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE user_code=VALUES(user_code),gender=VALUES(gender),"
                + "age=VALUES(age),province=VALUES(province),city=VALUES(city),register_channel=VALUES(register_channel),"
                + "membership_level=VALUES(membership_level),registered_at=VALUES(registered_at),status=VALUES(status)";
    }

    @Override
    protected void validateRow(Map<String, String> row) {
        String code = get(row, "user_code");
        if (code.isBlank()) throw new IllegalArgumentException("用户编码不能为空");

        int age = Integer.parseInt(get(row, "age"));
        if (age < 1 || age > 120) throw new IllegalArgumentException("年龄必须在 1-120 之间");

        String gender = get(row, "gender");
        // 同时兼容中英文：Male/Female/Unknown 或 男/女/未知
        if (!gender.equals("男") && !gender.equals("女") && !gender.equals("未知")
                && !gender.equals("Male") && !gender.equals("Female") && !gender.equals("Unknown"))
            throw new IllegalArgumentException("性别必须为 Male/Female/Unknown 或 男/女/未知");

        int status = Integer.parseInt(get(row, "status"));
        if (status != 0 && status != 1) throw new IllegalArgumentException("status 必须为 0 或 1");
    }

    @Override
    protected void bindRow(PreparedStatement stmt, Map<String, String> row) throws Exception {
        setLong(stmt, 1, get(row, "id"));
        stmt.setString(2, get(row, "user_code"));
        stmt.setString(3, get(row, "gender"));
        stmt.setInt(4, Integer.parseInt(get(row, "age")));
        stmt.setString(5, get(row, "province"));
        stmt.setString(6, get(row, "city"));
        stmt.setString(7, get(row, "register_channel"));
        stmt.setString(8, get(row, "membership_level"));
        setTimestamp(stmt, 9, get(row, "registered_at"));
        stmt.setInt(10, Integer.parseInt(get(row, "status")));
    }
}
