package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.Map;
import java.util.List;

/**
 * user_login_behavior 表导入器。
 * 校验规则：login_at 必须有值、duration_seconds >= 0。
 */
@Component
public class LoginBehaviorImporter extends AbstractCsvImporter {
    /** 业务唯一键（重复数据识别用） */
    protected String uniqueKeyColumn() { return null; }
    /** 同批导入时需同步映射的外键引用 */
    protected List<FkRef> foreignKeyRefs() { return List.of(new FkRef("ecommerce_user", "user_id")); }


    @Override public String[] columnNames() {
        return new String[]{"id","user_id","session_id","device_type","login_channel","login_at","logout_at","duration_seconds"};
    }

    public LoginBehaviorImporter(JdbcTemplate jdbcTemplate) { super(jdbcTemplate); }

    @Override public String tableName() { return "user_login_behavior"; }

    @Override protected String insertSql() {
        return "INSERT INTO user_login_behavior (id,user_id,session_id,device_type,login_channel,login_at,logout_at,duration_seconds) "
                + "VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE user_id=VALUES(user_id),session_id=VALUES(session_id),"
                + "device_type=VALUES(device_type),login_channel=VALUES(login_channel),login_at=VALUES(login_at),"
                + "logout_at=VALUES(logout_at),duration_seconds=VALUES(duration_seconds)";
    }

    @Override
    protected void validateRow(Map<String, String> row) {
        String loginAt = get(row, "login_at");
        if (loginAt.isBlank()) throw new IllegalArgumentException("登录时间不能为空");

        String dur = get(row, "duration_seconds");
        if (!dur.isBlank()) {
            int d = Integer.parseInt(dur);
            if (d < 0) throw new IllegalArgumentException("登录时长不能为负数");
        }
    }

    @Override
    protected void bindRow(PreparedStatement stmt, Map<String, String> row) throws Exception {
        setLong(stmt, 1, get(row, "id"));
        setLong(stmt, 2, get(row, "user_id"));
        stmt.setString(3, get(row, "session_id"));
        stmt.setString(4, get(row, "device_type"));
        stmt.setString(5, get(row, "login_channel"));
        setTimestamp(stmt, 6, get(row, "login_at"));
        setNullableTimestamp(stmt, 7, get(row, "logout_at"));
        setNullableInteger(stmt, 8, get(row, "duration_seconds"));
    }
}
