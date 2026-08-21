package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * 交易数据合并导入器（方案 B 合并模板：订单 + 明细一文件）。
 *
 * 模板为「明细行」格式：一行一条商品明细，同一 order_no 的多行自动合并生成订单主表
 * （订单金额 = Σ 单价×数量）与订单明细表。
 *
 * 商品支持自动创建：product_code 在库中不存在时，按行内 product_name/brand_name/unit_price
 * 自动建档（需 category_name 指定分类，分类不存在时自动创建一级分类）。
 */
@Component
public class TransactionImporter extends AbstractCsvImporter {

    private static final Logger log = LoggerFactory.getLogger(TransactionImporter.class);

    /** 交易数据模板列（合并视图，非真实表结构） */
    @Override public String[] columnNames() {
        return new String[]{
                "id", "order_no", "user_id", "product_code", "product_name",
                "brand_name", "unit_price", "quantity", "category_name",
                "order_status", "payment_method", "ordered_at", "paid_at", "completed_at"
        };
    }

    @Override public String tableName() { return "transaction_data"; }

    @Override protected String insertSql() { throw new UnsupportedOperationException("合并导入器不使用单表 SQL"); }
    @Override protected void bindRow(PreparedStatement stmt, Map<String, String> row) { throw new UnsupportedOperationException("合并导入器不使用单表绑定"); }

    private final SalesOrderImporter orderImporter;
    private final OrderItemImporter itemImporter;
    private final ProductImporter productImporter;

    public TransactionImporter(JdbcTemplate jdbcTemplate, SalesOrderImporter orderImporter,
                               OrderItemImporter itemImporter, ProductImporter productImporter) {
        super(jdbcTemplate);
        this.orderImporter = orderImporter;
        this.itemImporter = itemImporter;
        this.productImporter = productImporter;
    }

    // ─── 预查库上下文 ───
    private Map<String, Long> productCodeToId;
    private Map<String, Long> categoryNameToId;
    private Map<String, Long> orderNoToId;
    private Set<Long> userIds;
    private ImportIdAllocator orderAlloc;
    private ImportIdAllocator productAlloc;
    private ImportIdAllocator categoryAlloc;
    private final List<String> errorSamples = new ArrayList<>();

    private void loadMaps() {
        productCodeToId = new HashMap<>();
        jdbcTemplate.query("SELECT id, product_code FROM product", rs -> { while (rs.next()) productCodeToId.put(rs.getString(2), rs.getLong(1)); return null; });
        categoryNameToId = new HashMap<>();
        jdbcTemplate.query("SELECT id, category_name FROM product_category", rs -> { while (rs.next()) categoryNameToId.put(rs.getString(2), rs.getLong(1)); return null; });
        orderNoToId = new HashMap<>();
        jdbcTemplate.query("SELECT id, order_no FROM sales_order", rs -> { while (rs.next()) orderNoToId.put(rs.getString(2), rs.getLong(1)); return null; });
        orderAlloc = new ImportIdAllocator(new HashSet<>(orderNoToId.values()));
        productAlloc = new ImportIdAllocator(new HashSet<>(productCodeToId.values()));
        categoryAlloc = new ImportIdAllocator(new HashSet<>(categoryNameToId.values()));
        userIds = new HashSet<>(jdbcTemplate.queryForList("SELECT id FROM ecommerce_user", Long.class));
    }

    private void addError(String msg) {
        if (errorSamples.size() < maxErrorSamples()) errorSamples.add(msg);
    }

    /**
     * 合并导入：按 order_no 分组 → 商品自动创建 → 订单汇总写入 → 明细写入。
     * @return [inserted, updated, remapped, errors, skipped]
     */
    @Override
    public int[] importRows(List<Map<String, String>> rows, ImportIdMapper idMapper) {
        errorSamples.clear();
        loadMaps();
        int inserted = 0, updated = 0, errors = 0, skipped = 0;

        // 1. 行级基础校验 + 按订单分组（保持出现顺序）
        LinkedHashMap<String, List<Map<String, String>>> groups = new LinkedHashMap<>();
        int lineNo = 0;
        for (Map<String, String> row : rows) {
            lineNo++;
            try {
                if (get(row, "order_no").isBlank()) throw new IllegalArgumentException("订单号(order_no)不能为空");
                if (get(row, "user_id").isBlank()) throw new IllegalArgumentException("用户ID(user_id)不能为空");
                if (get(row, "product_code").isBlank()) throw new IllegalArgumentException("商品编码(product_code)不能为空");
                if (get(row, "quantity").isBlank()) throw new IllegalArgumentException("数量(quantity)不能为空");
                if (get(row, "unit_price").isBlank()) throw new IllegalArgumentException("单价(unit_price)不能为空");
                int qty = Integer.parseInt(get(row, "quantity"));
                if (qty <= 0) throw new IllegalArgumentException("数量必须大于 0");
                if (new BigDecimal(get(row, "unit_price")).compareTo(BigDecimal.ZERO) < 0)
                    throw new IllegalArgumentException("单价不能为负数");
                groups.computeIfAbsent(get(row, "order_no").trim(), k -> new ArrayList<>()).add(row);
            } catch (NumberFormatException e) {
                errors++;
                addError(String.format("第 %d 行数量/单价格式错误", lineNo));
            } catch (IllegalArgumentException e) {
                errors++;
                addError(String.format("第 %d 行校验失败: %s", lineNo, e.getMessage()));
            }        }

        // 2. 逐订单处理
        for (Map.Entry<String, List<Map<String, String>>> entry : groups.entrySet()) {
            List<Map<String, String>> group = entry.getValue();
            String orderNo = entry.getKey();
            try {
                // 2.1 商品准备（自动创建；单行失败只跳过该行，不影响订单其他明细）
                List<Map<String, String>> okItems = new ArrayList<>();
                for (Map<String, String> row : group) {
                    try {
                        ensureProduct(row, idMapper);
                        okItems.add(row);
                    } catch (IllegalArgumentException e) {
                        errors++;
                        addError(String.format("订单 %s 商品处理失败: %s", orderNo, e.getMessage()));
                    }
                }
                if (okItems.isEmpty()) continue;

                // 2.2 金额汇总（订单金额 = Σ 单价×数量）
                BigDecimal total = BigDecimal.ZERO;
                for (Map<String, String> row : okItems) {
                    total = total.add(new BigDecimal(get(row, "unit_price"))
                            .multiply(BigDecimal.valueOf(Long.parseLong(get(row, "quantity")))));
                }

                // 2.3 订单 upsert（复用 SalesOrderImporter 的 SQL/绑定）
                int[] orderStat = upsertOrder(orderNo, group.get(0), total, idMapper);
                inserted += orderStat[0];
                updated += orderStat[1];

                // 2.4 明细批量写入（复用 OrderItemImporter，外键为已落库的数字 id）
                List<Map<String, String>> items = new ArrayList<>(okItems.size());
                for (Map<String, String> row : okItems) {
                    String name = get(row, "product_name").isBlank() ? get(row, "product_code") : get(row, "product_name");
                    BigDecimal unitPrice = new BigDecimal(get(row, "unit_price"));
                    long qty = Long.parseLong(get(row, "quantity"));
                    Map<String, String> it = new LinkedHashMap<>();
                    it.put("id", "");
                    it.put("order_id", String.valueOf(orderStat[2]));
                    it.put("product_id", String.valueOf(row.get("_product_id")));
                    it.put("product_name_snapshot", name);
                    it.put("unit_price", unitPrice.toPlainString());
                    it.put("quantity", String.valueOf(qty));
                    it.put("item_amount", unitPrice.multiply(BigDecimal.valueOf(qty)).toPlainString());
                    items.add(it);
                }
                int[] st = itemImporter.importRows(items, idMapper);
                // 基类 stats 顺序: [inserted, updated, remapped, errors, skipped]
                inserted += st[0]; updated += st[1]; errors += st[3]; skipped += st[4];
            } catch (Exception e) {
                // 订单级失败（如 user_id 不存在 / 订单字段非法）→ 该订单全部明细行计入错误
                errors += group.size();
                addError(String.format("订单 %s 处理失败: %s", orderNo,
                        e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                log.warn("交易数据订单 {} 处理失败: {}", orderNo, e.getMessage());
            }
        }

        log.info("交易数据导入完成: 插入 {}, 更新 {}, 错误 {}, 跳过 {}（错误样例: {}）",
                inserted, updated, errors, skipped, errorSamples);
        return new int[]{inserted, updated, 0, errors, skipped};
    }

    /** 商品准备：product_code 已在库 → 映射；不在 → 自动创建（需分类） */
    private void ensureProduct(Map<String, String> row, ImportIdMapper idMapper) throws IllegalArgumentException {
        String code = get(row, "product_code").trim();
        Long pid = productCodeToId.get(code);
        if (pid != null) {
            row.put("_product_id", String.valueOf(pid));
            if (idMapper != null) idMapper.record("product", String.valueOf(pid), pid);
            return;
        }
        String catName = get(row, "category_name");
        if (catName.isBlank()) {
            throw new IllegalArgumentException("商品 " + code + " 不在库中且未填写分类(category_name)，无法自动创建");
        }
        Long catId = categoryNameToId.get(catName.trim());
        if (catId == null) catId = ensureCategory(catName.trim());
        long newId = productAlloc.allocate(null);
        try {
            Map<String, String> p = new LinkedHashMap<>();
            p.put("id", String.valueOf(newId));
            p.put("product_code", code);
            p.put("category_id", String.valueOf(catId));
            p.put("product_name", get(row, "product_name").isBlank() ? code : get(row, "product_name"));
            p.put("brand_name", get(row, "brand_name"));
            p.put("unit_price", get(row, "unit_price"));
            p.put("status", "1");
            jdbcTemplate.update(productImporter.insertSql(),
                    ps -> { try { productImporter.bindRow(ps, p); } catch (Exception ex) { throw new RuntimeException(ex); } });
            productCodeToId.put(code, newId);
            row.put("_product_id", String.valueOf(newId));
            if (idMapper != null) idMapper.record("product", String.valueOf(newId), newId);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("商品自动创建失败: "
                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    /** 自动创建一级分类（唯一键 category_name 已存在则复用） */
    private long ensureCategory(String name) throws IllegalArgumentException {
        Long existing = categoryNameToId.get(name);
        if (existing != null) return existing;
        long id = categoryAlloc.allocate(null);
        try {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("id", String.valueOf(id));
            c.put("parent_id", "");
            c.put("category_name", name);
            c.put("category_level", "1");
            c.put("status", "1");
            jdbcTemplate.update(categoryImporterInsertSql(),
                    ps -> { try { categoryBindRow(ps, c); } catch (Exception ex) { throw new RuntimeException(ex); } });
            categoryNameToId.put(name, id);
            return id;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("分类自动创建失败: "
                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    // 分类 SQL 与绑定直接内联（避免再注入一个 importer 实例）
    private String categoryImporterInsertSql() {
        return "INSERT INTO product_category (id,parent_id,category_name,category_level,status) VALUES (?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),category_name=VALUES(category_name),"
                + "category_level=VALUES(category_level),status=VALUES(status)";
    }

    private void categoryBindRow(PreparedStatement stmt, Map<String, String> row) throws Exception {
        setLong(stmt, 1, get(row, "id"));
        setNullableLong(stmt, 2, get(row, "parent_id"));
        stmt.setString(3, get(row, "category_name"));
        stmt.setInt(4, Integer.parseInt(get(row, "category_level")));
        stmt.setInt(5, Integer.parseInt(get(row, "status")));
    }

    /** 订单 upsert。返回 [inserted, updated, orderId] */
    private int[] upsertOrder(String orderNo, Map<String, String> sample, BigDecimal total, ImportIdMapper idMapper) throws Exception {
        String uid = get(sample, "user_id").trim();
        if (!userIds.contains(Long.parseLong(uid))) {
            throw new IllegalArgumentException("用户ID " + uid + " 在 ecommerce_user 表中不存在（先导入用户数据）");
        }
        Long existing = orderNoToId.get(orderNo);
        long id;
        int inserted, updated;
        if (existing != null) { id = existing; inserted = 0; updated = 1; }
        else { id = orderAlloc.allocate(null); inserted = 1; updated = 0; }

        Map<String, String> o = new LinkedHashMap<>();
        o.put("id", String.valueOf(id));
        o.put("order_no", orderNo);
        o.put("user_id", uid);
        String status = get(sample, "order_status");
        o.put("order_status", status.isBlank() ? "Completed" : status);
        o.put("total_amount", total.toPlainString());
        o.put("discount_amount", "0");
        o.put("payment_amount", total.toPlainString());
        o.put("payment_method", get(sample, "payment_method"));
        o.put("ordered_at", get(sample, "ordered_at"));
        o.put("paid_at", get(sample, "paid_at"));
        o.put("completed_at", get(sample, "completed_at"));
        jdbcTemplate.update(orderImporter.insertSql(),
                ps -> { try { orderImporter.bindRow(ps, o); } catch (Exception ex) { throw new RuntimeException(ex); } });
        orderNoToId.put(orderNo, id);
        // 预记录本批自建/复用的订单 id：明细行外键校验直接命中映射，避免跨订单的缓存过期问题
        if (idMapper != null) idMapper.record("sales_order", String.valueOf(id), id);
        return new int[]{inserted, updated, (int) id};
    }
}
