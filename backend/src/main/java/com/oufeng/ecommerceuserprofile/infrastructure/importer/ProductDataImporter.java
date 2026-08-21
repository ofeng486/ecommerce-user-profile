package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.util.*;

/**
 * 商品数据合并导入器（方案 B 合并模板：商品分类 + 商品一文件）。
 *
 * 模板行规则：
 * - 只填 category_name（无 product_code）→ 分类行（parent_category_name 可选，表示二级分类）
 * - 填了 product_code → 商品行（category_name 引用分类，分类不存在自动创建）
 * 先处理全部分类行，再处理商品行（保证商品引用的分类已落库）。
 */
@Component
public class ProductDataImporter extends AbstractCsvImporter {

    private static final Logger log = LoggerFactory.getLogger(ProductDataImporter.class);

    /** 商品数据模板列（合并视图，非真实表结构） */
    @Override public String[] columnNames() {
        return new String[]{
                "id", "category_name", "parent_category_name", "product_code",
                "product_name", "brand_name", "unit_price", "status"
        };
    }

    @Override public String tableName() { return "product_data"; }

    @Override protected String insertSql() { throw new UnsupportedOperationException("合并导入器不使用单表 SQL"); }
    @Override protected void bindRow(PreparedStatement stmt, Map<String, String> row) { throw new UnsupportedOperationException("合并导入器不使用单表绑定"); }

    private final ProductCategoryImporter categoryImporter;
    private final ProductImporter productImporter;

    public ProductDataImporter(JdbcTemplate jdbcTemplate, ProductCategoryImporter categoryImporter,
                               ProductImporter productImporter) {
        super(jdbcTemplate);
        this.categoryImporter = categoryImporter;
        this.productImporter = productImporter;
    }

    private Map<String, Long> categoryNameToId;
    private Map<String, Long> productCodeToId;
    private ImportIdAllocator categoryAlloc;
    private ImportIdAllocator productAlloc;
    private final List<String> errorSamples = new ArrayList<>();

    private void loadMaps() {
        categoryNameToId = new HashMap<>();
        jdbcTemplate.query("SELECT id, category_name FROM product_category", rs -> { while (rs.next()) categoryNameToId.put(rs.getString(2), rs.getLong(1)); return null; });
        productCodeToId = new HashMap<>();
        jdbcTemplate.query("SELECT id, product_code FROM product", rs -> { while (rs.next()) productCodeToId.put(rs.getString(2), rs.getLong(1)); return null; });
        categoryAlloc = new ImportIdAllocator(new HashSet<>(categoryNameToId.values()));
        productAlloc = new ImportIdAllocator(new HashSet<>(productCodeToId.values()));
    }

    private void addError(String msg) {
        if (errorSamples.size() < maxErrorSamples()) errorSamples.add(msg);
    }

    /**
     * 合并导入：先分类行（含父分类自动创建），后商品行。
     * @return [inserted, updated, remapped, errors, skipped]
     */
    @Override
    public int[] importRows(List<Map<String, String>> rows, ImportIdMapper idMapper) {
        errorSamples.clear();
        loadMaps();
        int inserted = 0, updated = 0, errors = 0;
        int lineNo = 0;

        // 第一趟：分类行（product_code 为空且 category_name 非空）
        for (Map<String, String> row : rows) {
            lineNo++;
            String code = get(row, "product_code").trim();
            String name = get(row, "category_name").trim();
            if (!code.isEmpty() || name.isEmpty()) continue;
            try {
                String parentName = get(row, "parent_category_name").trim();
                Long parentId = null;
                if (!parentName.isEmpty()) parentId = ensureCategory(parentName, null);
                int[] st = upsertCategory(name, parentId);
                inserted += st[0]; updated += st[1];
            } catch (Exception e) {
                errors++;
                addError(String.format("第 %d 行分类处理失败: %s", lineNo,
                        e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }
        }

        // 第二趟：商品行（product_code 非空）
        lineNo = 0;
        for (Map<String, String> row : rows) {
            lineNo++;
            String code = get(row, "product_code").trim();
            if (code.isEmpty()) continue;
            try {
                String catName = get(row, "category_name").trim();
                Long catId = categoryNameToId.get(catName);
                if (catId == null) throw new IllegalArgumentException("分类 " + catName + " 不存在（请先在本文件填写分类行）");
                int[] st = upsertProduct(code, catId, row);
                inserted += st[0]; updated += st[1];
            } catch (Exception e) {
                log.warn("商品行处理异常", e);
                errors++;
                addError(String.format("第 %d 行商品处理失败: %s", lineNo,
                        e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            }
        }

        log.info("商品数据导入完成: 插入 {}, 更新 {}, 错误 {}（错误样例: {}）", inserted, updated, errors, errorSamples);
        return new int[]{inserted, updated, 0, errors, 0};
    }

    /** 分类 upsert（唯一键 category_name：已存在 → 更新；否则分配新 id 插入） */
    private int[] upsertCategory(String name, Long parentId) {
        Long existing = categoryNameToId.get(name);
        long id;
        int inserted, updated;
        if (existing != null) { id = existing; inserted = 0; updated = 1; }
        else { id = categoryAlloc.allocate(null); inserted = 1; updated = 0; }
        Map<String, String> c = new LinkedHashMap<>();
        c.put("id", String.valueOf(id));
        c.put("parent_id", parentId == null ? "" : String.valueOf(parentId));
        c.put("category_name", name);
        c.put("category_level", parentId == null ? "1" : "2");
        c.put("status", "1");
        jdbcTemplate.update(categoryImporter.insertSql(),
                ps -> { try { categoryImporter.bindRow(ps, c); } catch (Exception ex) { throw new RuntimeException(ex); } });
        categoryNameToId.put(name, id);
        return new int[]{inserted, updated};
    }

    /** 商品 upsert（唯一键 product_code） */
    private int[] upsertProduct(String code, Long catId, Map<String, String> row) {
        Long existing = productCodeToId.get(code);
        long id;
        int inserted, updated;
        if (existing != null) { id = existing; inserted = 0; updated = 1; }
        else { id = productAlloc.allocate(null); inserted = 1; updated = 0; }
        Map<String, String> p = new LinkedHashMap<>();
        p.put("id", String.valueOf(id));
        p.put("product_code", code);
        p.put("category_id", String.valueOf(catId));
        p.put("product_name", get(row, "product_name").isBlank() ? code : get(row, "product_name"));
        p.put("brand_name", get(row, "brand_name"));
        p.put("unit_price", get(row, "unit_price"));
        p.put("status", get(row, "status").isBlank() ? "1" : get(row, "status"));
        jdbcTemplate.update(productImporter.insertSql(),
                ps -> { try { productImporter.bindRow(ps, p); } catch (Exception ex) { throw new RuntimeException(ex); } });
        productCodeToId.put(code, id);
        return new int[]{inserted, updated};
    }

    /** 确保分类存在（父分类不存在时自动创建，最多两级） */
    private long ensureCategory(String name, Long parentId) {
        Long existing = categoryNameToId.get(name);
        if (existing != null) return existing;
        int[] st = upsertCategory(name, parentId);
        return categoryNameToId.get(name);
    }
}
