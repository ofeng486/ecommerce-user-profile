package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.Map;
import java.util.Set;
import java.util.List;

/**
 * user_browse_behavior 表导入器。
 * 校验规则：behavior_type 必须为枚举值之一。
 */
@Component
public class BrowseBehaviorImporter extends AbstractCsvImporter {
    /** 业务唯一键（重复数据识别用） */
    protected String uniqueKeyColumn() { return null; }
    /** 同批导入时需同步映射的外键引用 */
    protected List<FkRef> foreignKeyRefs() { return List.of(new FkRef("ecommerce_user", "user_id"), new FkRef("product", "product_id")); }


    private static final Set<String> VALID_BEHAVIOR_TYPES =
            Set.of("View", "Click", "Favorite", "Cart", "Purchase");

    @Override public String[] columnNames() {
        return new String[]{"id","user_id","product_id","behavior_type","session_id","device_type","channel","behavior_at"};
    }

    public BrowseBehaviorImporter(JdbcTemplate jdbcTemplate) { super(jdbcTemplate); }

    @Override public String tableName() { return "user_browse_behavior"; }

    @Override protected String insertSql() {
        return "INSERT INTO user_browse_behavior (id,user_id,product_id,behavior_type,session_id,device_type,channel,behavior_at) "
                + "VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE user_id=VALUES(user_id),product_id=VALUES(product_id),"
                + "behavior_type=VALUES(behavior_type),session_id=VALUES(session_id),device_type=VALUES(device_type),"
                + "channel=VALUES(channel),behavior_at=VALUES(behavior_at)";
    }

    @Override
    protected void validateRow(Map<String, String> row) {
        String bt = get(row, "behavior_type");
        if (!VALID_BEHAVIOR_TYPES.contains(bt))
            throw new IllegalArgumentException("行为类型无效: " + bt + "，有效值: " + VALID_BEHAVIOR_TYPES);
    }

    @Override
    protected void bindRow(PreparedStatement stmt, Map<String, String> row) throws Exception {
        setLong(stmt, 1, get(row, "id"));
        setLong(stmt, 2, get(row, "user_id"));
        setLong(stmt, 3, get(row, "product_id"));
        stmt.setString(4, get(row, "behavior_type"));
        stmt.setString(5, get(row, "session_id"));
        stmt.setString(6, get(row, "device_type"));
        stmt.setString(7, get(row, "channel"));
        setTimestamp(stmt, 8, get(row, "behavior_at"));
    }
}
