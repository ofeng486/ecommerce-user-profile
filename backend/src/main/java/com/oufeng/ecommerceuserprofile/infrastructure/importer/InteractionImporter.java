package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.*;

/**
 * 互动数据合并导入器（方案 B 合并模板：浏览行为 + 登录行为一文件）。
 *
 * 模板按 behavior_type 分拣：
 * - Login / Logout → 登录行为表（login_at/logout_at/duration_seconds）
 * - View / Click / Favorite / Cart / Purchase → 浏览互动表（product_code 须为库中已有商品）
 */
@Component
public class InteractionImporter extends AbstractCsvImporter {

    private static final Logger log = LoggerFactory.getLogger(InteractionImporter.class);

    private static final Set<String> LOGIN_TYPES = Set.of("Login", "Logout");
    private static final Set<String> BROWSE_TYPES = Set.of("View", "Click", "Favorite", "Cart", "Purchase");

    /** 互动数据模板列（合并视图，非真实表结构） */
    @Override public String[] columnNames() {
        return new String[]{
                "id", "user_id", "behavior_type", "product_code", "session_id",
                "device_type", "channel", "login_channel", "login_at",
                "logout_at", "duration_seconds", "behavior_at"
        };
    }

    @Override public String tableName() { return "interaction_data"; }

    @Override protected String insertSql() { throw new UnsupportedOperationException("合并导入器不使用单表 SQL"); }
    @Override protected void bindRow(PreparedStatement stmt, Map<String, String> row) { throw new UnsupportedOperationException("合并导入器不使用单表绑定"); }

    private final BrowseBehaviorImporter browseImporter;
    private final LoginBehaviorImporter loginImporter;

    public InteractionImporter(JdbcTemplate jdbcTemplate, BrowseBehaviorImporter browseImporter,
                               LoginBehaviorImporter loginImporter) {
        super(jdbcTemplate);
        this.browseImporter = browseImporter;
        this.loginImporter = loginImporter;
    }

    private Map<String, Long> productCodeToId;
    private final List<String> errorSamples = new ArrayList<>();

    private void loadMaps() {
        productCodeToId = new HashMap<>();
        jdbcTemplate.query("SELECT id, product_code FROM product", rs -> { while (rs.next()) productCodeToId.put(rs.getString(2), rs.getLong(1)); return null; });
    }

    private void addError(String msg) {
        if (errorSamples.size() < maxErrorSamples()) errorSamples.add(msg);
    }

    /**
     * 合并导入：按 behavior_type 分拣到浏览/登录两表。
     * @return [inserted, updated, remapped, errors, skipped]
     */
    @Override
    public int[] importRows(List<Map<String, String>> rows, ImportIdMapper idMapper) {
        errorSamples.clear();
        loadMaps();
        List<Map<String, String>> browseRows = new ArrayList<>();
        List<Map<String, String>> loginRows = new ArrayList<>();
        int errors = 0;
        int lineNo = 0;
        for (Map<String, String> row : rows) {
            lineNo++;
            try {
                String bt = get(row, "behavior_type").trim();
                if (bt.isEmpty()) throw new IllegalArgumentException("行为类型(behavior_type)不能为空");
                if (LOGIN_TYPES.contains(bt)) {
                    if (get(row, "login_at").isBlank()) throw new IllegalArgumentException("登录行为必须填写 login_at");
                    Map<String, String> l = new LinkedHashMap<>();
                    l.put("id", "");
                    l.put("user_id", get(row, "user_id"));
                    l.put("session_id", get(row, "session_id"));
                    l.put("device_type", get(row, "device_type"));
                    l.put("login_channel", get(row, "login_channel"));
                    l.put("login_at", get(row, "login_at"));
                    l.put("logout_at", get(row, "logout_at"));
                    l.put("duration_seconds", get(row, "duration_seconds"));
                    loginRows.add(l);
                } else if (BROWSE_TYPES.contains(bt)) {
                    String code = get(row, "product_code").trim();
                    if (code.isEmpty()) throw new IllegalArgumentException("浏览行为必须填写 product_code");
                    Long pid = productCodeToId.get(code);
                    if (pid == null) throw new IllegalArgumentException("商品 " + code + " 不存在（先导入商品数据）");
                    if (get(row, "behavior_at").isBlank()) throw new IllegalArgumentException("浏览行为必须填写 behavior_at");
                    Map<String, String> b = new LinkedHashMap<>();
                    b.put("id", "");
                    b.put("user_id", get(row, "user_id"));
                    b.put("product_id", String.valueOf(pid));
                    b.put("behavior_type", bt);
                    b.put("session_id", get(row, "session_id"));
                    b.put("device_type", get(row, "device_type"));
                    b.put("channel", get(row, "channel"));
                    b.put("behavior_at", get(row, "behavior_at"));
                    browseRows.add(b);
                } else {
                    throw new IllegalArgumentException("行为类型无效: " + bt + "（有效值: View/Click/Favorite/Cart/Purchase/Login/Logout）");
                }
            } catch (IllegalArgumentException e) {
                errors++;
                addError(String.format("第 %d 行校验失败: %s", lineNo, e.getMessage()));
            }
        }

        int inserted = 0, updated = 0, skipped = 0;
        if (!browseRows.isEmpty()) {
            int[] st = browseImporter.importRows(browseRows, idMapper);
            // 基类 stats 顺序: [inserted, updated, remapped, errors, skipped]
            inserted += st[0]; updated += st[1]; errors += st[3]; skipped += st[4];
        }
        if (!loginRows.isEmpty()) {
            int[] st = loginImporter.importRows(loginRows, idMapper);
            // 基类 stats 顺序: [inserted, updated, remapped, errors, skipped]
            inserted += st[0]; updated += st[1]; errors += st[3]; skipped += st[4];
        }

        log.info("互动数据导入完成: 浏览 {} 行, 登录 {} 行 → 插入 {}, 更新 {}, 错误 {}, 跳过 {}（错误样例: {}）",
                browseRows.size(), loginRows.size(), inserted, updated, errors, skipped, errorSamples);
        return new int[]{inserted, updated, 0, errors, skipped};
    }
}
