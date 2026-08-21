package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据总表（Composite CSV）导入器。
 *
 * 一个 CSV 文件包含全部 7 张表的数据：第一列固定为「表名」，
 * 其余列为 7 张表的列并集（每列只出现一次）。数据行在第一列写明所属表，
 * 只填写该表对应的列，其余列留空。导入时按表名分派到各单表导入器，
 * 共享同一个 ImportIdMapper，保证同批跨表主键重映射与唯一键去重语义一致。
 *
 * 模板示例（仅表头行）：
 *   表名,id,parent_id,category_name,...,behavior_at,...
 *   ecommerce_user,1,U0000000001,Male,25,...
 *   product_category,2,,数码,1,...
 */
public class CompositeCsvImporter {

    private static final Logger log = LoggerFactory.getLogger(CompositeCsvImporter.class);

    /** 表名列名（总表第一列） */
    public static final String TABLE_COLUMN = "表名";

    /**
     * 总表模板精简黑名单：这些列在导入器中均为“留空即可”的列
     * （主键自动分配 / 可空字段 / 无校验），从总表模板中剔除以降低填写负担，
     * 导入时缺列=空=与不填等效，功能不受影响。
     */
    public static final Set<String> OMIT_TEMPLATE_COLUMNS = Set.of(
            "id", "brand_name", "logout_at", "paid_at",
            "completed_at", "duration_seconds", "product_name_snapshot", "parent_category_name",
            "discount_amount", "item_amount"
    );

    /**
     * 计算总表模板列（全部 importer 列并集，按表顺序去重，剔除黑名单列）。
     * 供模板下载（Controller）与 {@link #templateHeader()} 统一使用。
     */
    public static List<String> compositeColumns(List<AbstractCsvImporter> importers) {
        LinkedHashSet<String> cols = new LinkedHashSet<>();
        for (AbstractCsvImporter imp : importers) {
            for (String c : imp.columnNames()) {
                if (!OMIT_TEMPLATE_COLUMNS.contains(c)) cols.add(c);
            }
        }
        return new java.util.ArrayList<>(cols);
    }

    private final List<AbstractCsvImporter> importers; // 按外键依赖顺序

    public CompositeCsvImporter(List<AbstractCsvImporter> importers) {
        this.importers = importers;
    }

    /** 生成总表模板表头：表名 + 全部表列并集（去重、剔除精简黑名单列，列名输出中文） */
    public String templateHeader() {
        return TABLE_COLUMN + "," + compositeColumns(importers).stream()
                .map(CsvColumnNames::toChinese)
                .collect(java.util.stream.Collectors.joining(","));
    }

    /** 判断文件表头是否为总表格式（第一列含「表名」） */
    public static boolean isCompositeHeader(String[] headers) {
        for (String h : headers) {
            if (TABLE_COLUMN.equals(h.trim())) return true;
        }
        return false;
    }

    /**
     * 导入总表文件（第一行为表头，后续每行第一列填表名）。
     * @return [inserted, updated, remapped, errors, skipped]
     */
    public int[] importComposite(BufferedReader reader, ImportIdMapper idMapper) throws IOException {
        String headerLine = reader.readLine();
        if (headerLine == null) return new int[]{0, 0, 0, 0, 0};
        headerLine = AbstractCsvImporter.stripBom(headerLine);

        String[] headers = AbstractCsvImporter.CsvParser.splitLine(headerLine);
        // 中文列名 → 英文（模板列名为中文；「表名」不在映射内保持原样）
        headers = CsvColumnNames.translateHeader(headers);
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerIndex.put(headers[i].trim(), i);
        }
        Integer tableIdx = headerIndex.get(TABLE_COLUMN);
        if (tableIdx == null) {
            throw new IllegalArgumentException("总表模板缺少「" + TABLE_COLUMN + "」列（第一列必须为表名）");
        }

        Map<String, AbstractCsvImporter> byTable = new HashMap<>();
        for (AbstractCsvImporter imp : importers) {
            byTable.put(imp.tableName(), imp);
        }

        // 按表分派数据行
        Map<String, List<Map<String, String>>> rowsByTable = new LinkedHashMap<>();
        AtomicInteger errors = new AtomicInteger(0);
        List<String> errorSamples = Collections.synchronizedList(new ArrayList<>());
        int lineNo = 1; // 表头为第 1 行
        String line;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            if (line.isBlank()) continue;
            String[] values = AbstractCsvImporter.CsvParser.splitLine(line);
            String table = tableIdx < values.length ? values[tableIdx].trim() : "";
            AbstractCsvImporter imp = byTable.get(table);
            if (imp == null) {
                errors.incrementAndGet();
                if (errorSamples.size() < 10) {
                    errorSamples.add("第 " + lineNo + " 行未知表名: " + (table.isEmpty() ? "(空)" : table));
                }
                log.warn("总表第 {} 行未知表名: {}", lineNo, table);
                continue;
            }
            // 只提取该表对应的列（其余列留空忽略）
            Map<String, String> row = new LinkedHashMap<>();
            for (String col : imp.columnNames()) {
                Integer idx = headerIndex.get(col);
                row.put(col, idx != null && idx < values.length ? values[idx].trim() : "");
            }
            rowsByTable.computeIfAbsent(table, k -> new ArrayList<>()).add(row);
        }

        // 逐表导入（共享 idMapper：主表 id 重分配后从表外键自动映射）
        int[] total = new int[5];
        for (Map.Entry<String, List<Map<String, String>>> e : rowsByTable.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            int[] st = byTable.get(e.getKey()).importRows(e.getValue(), idMapper);
            for (int i = 0; i < 5; i++) total[i] += st[i];
            log.info("总表分派 {}: {} 行 → 插入{} 更新{} 重分配{} 错误{} 跳过{}",
                    e.getKey(), e.getValue().size(), st[0], st[1], st[2], st[3], st[4]);
        }
        total[3] += errors.get();
        if (!errorSamples.isEmpty()) {
            log.warn("总表导入错误样例: {}", errorSamples);
        }

        log.info("总表导入完成: 插入 {}, 更新 {}, 重分配 {}, 错误 {}, 跳过 {}",
                total[0], total[1], total[2], total[3], total[4]);
        return total;
    }
}
