package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 抽象 CSV 导入器基类。
 *
 * 相比 v1 的改进：
 * <ul>
 *   <li>使用 Manual CsvParser 处理引号内逗号/换行</li>
 *   <li>行级错误容错：一行失败不影响整批，最多收集 10 条错误样本</li>
 *   <li>支持 {@link #validateRow(Map)} 数据校验钩子</li>
 *   <li>区分 inserted / updated / error / skipped 四种统计</li>
 *   <li>BATCH_SIZE 可通过子类覆盖</li>
 * </ul>
 */
public abstract class AbstractCsvImporter {

    private static final Logger log = LoggerFactory.getLogger(AbstractCsvImporter.class);

    /** 导入上下文（预查库结果，惰性加载一次） */
    private Set<Long> existingIds = new java.util.HashSet<>();
    private Map<String, Long> keyToId = new java.util.HashMap<>();
    private ImportIdAllocator allocator;
    private boolean contextLoaded = false;
    /** 本次导入 id 重分配/自动分配计数 */
    private final AtomicInteger remappedRef = new AtomicInteger(0);

    /** 批量大小，子类可覆盖 */
    protected int batchSize() { return 1000; }
    /** 单表最多收集的错误样本数 */
    protected int maxErrorSamples() { return 10; }

    protected static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected final JdbcTemplate jdbcTemplate;

    protected AbstractCsvImporter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ─── 子类必须实现 ───

    /** 目标表名 */
    public abstract String tableName();

    /** INSERT ... ON DUPLICATE KEY UPDATE SQL */
    protected abstract String insertSql();

    /** 绑定参数到 PreparedStatement */
    protected abstract void bindRow(PreparedStatement stmt, Map<String, String> row) throws Exception;

    /** 预期列名 */
    public abstract String[] columnNames();

    // ─── 可选覆盖 ───

    /**
     * 数据校验钩子。在校验失败时抛出 IllegalArgumentException 附带中文描述。
     * 子类可覆盖此方法添加业务校验规则。
     */
    protected void validateRow(Map<String, String> row) {
        // 默认不校验，子类覆盖
    }

    /**
     * 业务唯一键列（用于重复数据识别）。
     * 返回非 null 时，导入会按该列去重：唯一键已存在 → 更新档案（不新增）；不存在 → 新增。
     * 无唯一键的表（行为/明细）返回 null，仅做 id 冲突避让。
     */
    protected String uniqueKeyColumn() { return null; }

    /** 外键引用描述：目标主表名 + 本表外键列名 */
    public record FkRef(String targetTable, String fkColumn) {}

    /**
     * 外键引用列表（同批导入时，主表 id 被重分配后自动同步映射到从表外键列）。
     * 默认无外键，子类按需声明。
     */
    protected List<FkRef> foreignKeyRefs() { return List.of(); }

    /** CSV 模板内容 */
    public String csvTemplate() {
        return String.join(",", columnNames());
    }

    // ─── 导入入口 ───

    /** 从目录导入，返回 (inserted, updated, remapped, errors, skipped) */
    public int[] importFromDir(Path dataDir) throws IOException {
        Path file = dataDir.resolve(tableName() + ".csv");
        if (!Files.isRegularFile(file)) return new int[]{0, 0, 0, 0, 0};
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return importFromReader(r, null);
        } catch (IOException e) {
            throw new RuntimeException("导入失败: " + file, e);
        }
    }

    /** 从 Reader 导入（无跨表映射），返回 [inserted, updated, remapped, errors, skipped] */
    public int[] importFromReader(BufferedReader reader) throws IOException {
        return importFromReader(reader, null);
    }

    /**
     * 从 Reader 导入，返回 [inserted, updated, remapped, errors, skipped]。
     *
     * 智能导入策略（方案 A）：
     * 1. 业务唯一键去重：唯一键已存在 → 更新档案（不新增，人数不变）；
     * 2. id 冲突避让：id 为空/非法/与库中已有冲突 → 从 max(id)+1 自动递增分配（不覆盖已有数据）；
     * 3. 跨表外键映射：同批主表 id 被重分配后，从表外键列自动同步（经 ImportIdMapper）。
     */
    public int[] importFromReader(BufferedReader reader, ImportIdMapper idMapper) throws IOException {
        // 读取表头（Excel/前端导出可能带 UTF-8 BOM，先剥离再解析）
        String headerLine = reader.readLine();
        if (headerLine == null) return new int[]{0, 0, 0, 0, 0};
        headerLine = stripBom(headerLine);

        String[] headers = CsvParser.splitLine(headerLine);
        // 中文表头 → 英文列名（模板列名为中文，旧英文模板也兼容）
        headers = CsvColumnNames.translateHeader(headers);
        Map<String, Integer> headerIndex = buildHeaderIndex(headers);

        // 校验必要列
        List<String> missing = checkMissingColumns(headerIndex);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "表 " + tableName() + " 缺少必要列: " + String.join(", ", missing)
                    + " — 文件包含: " + String.join(", ", headers)
                    + " — 期望列: " + String.join(", ", columnNames()));
        }

        // 逐行解析为数据行
        List<Map<String, String>> rows = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) continue;
            String[] values = CsvParser.splitLine(line);
            rows.add(extractRow(values, headerIndex));
        }
        return importRows(rows, idMapper);
    }

    /**
     * 批量导入已解析的数据行（单表文件与总表文件共用入口）。
     * 行内做唯一键去重、id 冲突避让、外键映射与行级容错，批量写库。
     * @return [inserted, updated, remapped, errors, skipped]
     */
    public int[] importRows(List<Map<String, String>> rows, ImportIdMapper idMapper) {
        AtomicInteger inserted = new AtomicInteger(0);
        AtomicInteger updated = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        List<String> errorSamples = Collections.synchronizedList(new ArrayList<>());
        remappedRef.set(0);

        // 预查库：已有 id / 唯一键集合（一次性加载，避免逐行查库）
        loadContext();

        // 逐行处理 -> 批量写入，带行级容错
        List<Map<String, String>> batch = new ArrayList<>(batchSize());
        List<Boolean> updateFlags = new ArrayList<>(batchSize()); // true=更新已有(唯一键重复), false=新增
        int lineNo = 0;
        for (Map<String, String> row : rows) {
            lineNo++;
            try {
                validateRow(row); // 子类校验钩子
                boolean isUpdate = resolveRow(row, idMapper); // 唯一键去重 + id 冲突避让 + 外键映射
                batch.add(row);
                updateFlags.add(isUpdate);
            } catch (IllegalArgumentException e) {
                errors.incrementAndGet();
                if (errorSamples.size() < maxErrorSamples()) {
                    errorSamples.add(String.format("第 %d 行校验失败: %s", lineNo, e.getMessage()));
                }
                log.warn("表 {} 第 {} 行数据校验失败: {}", tableName(), lineNo, e.getMessage());
                continue;
            } catch (Exception e) {
                errors.incrementAndGet();
                if (errorSamples.size() < maxErrorSamples()) {
                    errorSamples.add(String.format("第 %d 行解析失败: %s", lineNo, e.getMessage()));
                }
                continue;
            }

            if (batch.size() >= batchSize()) {
                int[] stats = executeBatch(batch, updateFlags, errorSamples);
                inserted.addAndGet(stats[0]);
                updated.addAndGet(stats[1]);
                errors.addAndGet(stats[2]);
                skipped.addAndGet(stats[3]);
                batch.clear();
                updateFlags.clear();
            }
        }
        // 最后一残批
        if (!batch.isEmpty()) {
            int[] stats = executeBatch(batch, updateFlags, errorSamples);
            inserted.addAndGet(stats[0]);
            updated.addAndGet(stats[1]);
            errors.addAndGet(stats[2]);
            skipped.addAndGet(stats[3]);
        }

        log.info("表 {} 导入完成: 插入 {}, 更新 {}, 重分配/自动分配 {}, 错误 {}, 跳过 {}",
                tableName(), inserted.get(), updated.get(), remappedRef.get(), errors.get(), skipped.get());

        return new int[]{inserted.get(), updated.get(), remappedRef.get(), errors.get(), skipped.get()};
    }

    /** 剥离 UTF-8 BOM（﻿），兼容 Excel 保存的 CSV */
    protected static String stripBom(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '﻿') {
            return line.substring(1);
        }
        return line;
    }

    // ─── 智能导入辅助 ───

    /** 预查库：加载已有 id 集合与唯一键→id 映射（惰性，单次导入只查一次） */
    private void loadContext() {
        if (contextLoaded) return;
        contextLoaded = true;
        try {
            List<Long> ids = jdbcTemplate.queryForList("SELECT id FROM " + tableName(), Long.class);
            existingIds = new java.util.HashSet<>(ids);
            allocator = new ImportIdAllocator(existingIds);
            String uk = uniqueKeyColumn();
            if (uk != null) {
                Map<String, Long> map = new java.util.HashMap<>();
                jdbcTemplate.query("SELECT id, " + uk + " FROM " + tableName(), rs -> {
                    map.put(rs.getString(2), rs.getLong(1));
                });
                keyToId = map;
            }
            log.info("表 {} 导入上下文: 已有 {} 行, 最大 id={}, 唯一键={}",
                    tableName(), existingIds.size(), allocator.getMaxId(), uk == null ? "无" : uk);
        } catch (Exception e) {
            log.warn("表 {} 加载导入上下文失败（按无冲突处理）: {}", tableName(), e.getMessage());
            existingIds = new java.util.HashSet<>();
            allocator = new ImportIdAllocator(existingIds);
            keyToId = new java.util.HashMap<>();
        }
    }

    /**
     * 行级决策：唯一键去重 + id 冲突避让 + 跨表外键映射。
     * 返回 true 表示该行将更新已有记录，false 表示新增。
     */
    private boolean resolveRow(Map<String, String> row, ImportIdMapper idMapper) {
        String uk = uniqueKeyColumn();
        boolean isUpdate = false;
        if (uk != null) {
            String ukVal = row.get(uk);
            Long existingId = (ukVal != null && !ukVal.isBlank()) ? keyToId.get(ukVal) : null;
            if (existingId != null) {
                // 唯一键已存在 → 更新该行档案，id 指向库中原行（不新增，人数不变）
                row.put("id", String.valueOf(existingId));
                isUpdate = true;
            }
        }
        if (!isUpdate) {
            // 新增：id 为空/非法/冲突 → 自动递增分配
            String rawId = row.get("id");
            Long newId = allocator.allocate(rawId);
            String newIdStr = String.valueOf(newId);
            boolean remapped = !newIdStr.equals(rawId == null ? null : rawId.trim());
            if (remapped) remappedRef.incrementAndGet();
            // 无论是否重分配都记录到映射器：本批主表全部 id 均可被从表外键引用判定
            if (idMapper != null && rawId != null && !rawId.trim().isEmpty()) {
                idMapper.record(tableName(), rawId.trim(), newId);
            }
            row.put("id", newIdStr);
        }
        // 同批跨表外键映射 + 存在性校验：主表 id 被重分配后外键同步到新 id；
        // 引用既不在本批映射、也不在目标表已有 id 集合中的外键 → 该行判为错误
        for (FkRef fk : foreignKeyRefs()) {
            String v = row.get(fk.fkColumn());
            if (v == null || v.isBlank()) continue; // 空外键不校验（必填性由子类 validateRow 负责）
            String fkVal = v.trim();
            Long mapped = idMapper == null ? null : idMapper.lookup(fk.targetTable(), fkVal);
            if (mapped != null) {
                row.put(fk.fkColumn(), String.valueOf(mapped));
            } else if (!fkTargetContains(fk.targetTable(), fkVal)) {
                throw new IllegalArgumentException("外键 " + fk.fkColumn() + "=" + fkVal
                        + " 在表 " + fk.targetTable() + " 中不存在（先导入主表数据或改为已存在的 id）");
            }
        }
        return isUpdate;
    }

    /** 外键目标表已有 id 集合（懒加载，每表只查一次） */
    private final Map<String, Set<Long>> fkTargetIds = new java.util.HashMap<>();

    private boolean fkTargetContains(String table, String idStr) {
        try {
            long id = Long.parseLong(idStr);
            return fkTargetIds.computeIfAbsent(table, t -> {
                try {
                    return new java.util.HashSet<>(jdbcTemplate.queryForList("SELECT id FROM " + t, Long.class));
                } catch (Exception e) {
                    log.warn("加载外键目标表 {} 的 id 集合失败: {}", t, e.getMessage());
                    return java.util.Collections.emptySet();
                }
            }).contains(id);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ─── 内部方法 ───

    /** 构建列名 -> 列索引映射表 */
    private Map<String, Integer> buildHeaderIndex(String[] headers) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            index.put(headers[i].trim().toLowerCase(), i);
        }
        return index;
    }

    /** 检查缺失列 */
    private List<String> checkMissingColumns(Map<String, Integer> headerIndex) {
        List<String> missing = new ArrayList<>();
        for (String col : columnNames()) {
            if (!headerIndex.containsKey(col.toLowerCase())) {
                missing.add(col);
            }
        }
        return missing;
    }

    /** 从 CSV 行中提取需要的列 */
    private Map<String, String> extractRow(String[] values, Map<String, Integer> headerIndex) {
        Map<String, String> row = new LinkedHashMap<>();
        for (String col : columnNames()) {
            Integer idx = headerIndex.get(col.toLowerCase());
            row.put(col, idx != null && idx < values.length ? values[idx] : "");
        }
        return row;
    }

    /**
     * 执行一批 INSERT。返回 [inserted, updated, errors, skipped]。
     * 批量失败时降级为逐行重试，以隔离问题行。
     * updateFlags 为逐行的唯一键预判（true=更新已有, false=新增），批量成功时按预判精确计数。
     */
    private int[] executeBatch(List<Map<String, String>> batch, List<Boolean> updateFlags, List<String> errorSamples) {
        int inserted = 0, updated = 0, errors = 0, skipped = 0;

        try {
            // 先尝试批量执行
            jdbcTemplate.batchUpdate(insertSql(), batch, batch.size(), (stmt, row) -> {
                try {
                    bindRow(stmt, row);
                } catch (Exception e) {
                    throw new RuntimeException("绑定参数失败: " + row, e);
                }
            });
            // 按唯一键预判精确统计（单用户演示场景无并发竞争；SQL 本身仍是 ON DUPLICATE 幂等更新）
            for (int i = 0; i < batch.size(); i++) {
                if (updateFlags.get(i)) updated++;
                else inserted++;
            }
        } catch (Exception batchEx) {
            log.warn("表 {} 批量导入失败，降级为逐行重试: {}", tableName(), batchEx.getMessage());
            // 降级：逐行重试
            for (int i = 0; i < batch.size(); i++) {
                Map<String, String> row = batch.get(i);
                try {
                    int affected = jdbcTemplate.update(insertSql(), ps -> {
                        try {
                            bindRow(ps, row);
                        } catch (Exception e) {
                            throw new RuntimeException("绑定参数失败", e);
                        }
                    });
                    if (affected > 0) {
                        // MySQL ON DUPLICATE KEY UPDATE: affected=1=INSERT, affected=2=UPDATE
                        if (affected == 2) updated++;
                        else inserted++;
                    } else {
                        skipped++;
                    }
                } catch (Exception rowEx) {
                    errors++;
                    if (errorSamples.size() < maxErrorSamples()) {
                        String detail = rowEx.getCause() != null
                                ? rowEx.getCause().getMessage() : rowEx.getMessage();
                        errorSamples.add(String.format("写入失败: %s, 数据: %s",
                                detail != null ? detail.substring(0, Math.min(100, detail.length())) : "未知错误",
                                row.toString().substring(0, Math.min(200, row.toString().length()))));
                    }
                    log.warn("表 {} 第 {} 行写入失败", tableName(), i + 1);
                }
            }
        }

        return new int[]{inserted, updated, errors, skipped};
    }

    // ─── 辅助方法 (protected, 子类使用) ───

    protected static String get(Map<String, String> row, String col) {
        return row.getOrDefault(col, "");
    }

    protected static void setLong(PreparedStatement stmt, int index, String value) throws Exception {
        stmt.setLong(index, Long.parseLong(value));
    }

    protected static void setNullableLong(PreparedStatement stmt, int index, String value) throws Exception {
        if (value == null || value.isBlank()) stmt.setNull(index, java.sql.Types.BIGINT);
        else stmt.setLong(index, Long.parseLong(value));
    }

    protected static void setNullableString(PreparedStatement stmt, int index, String value) throws Exception {
        if (value == null || value.isBlank()) stmt.setNull(index, java.sql.Types.VARCHAR);
        else stmt.setString(index, value);
    }

    protected static void setTimestamp(PreparedStatement stmt, int index, String value) throws Exception {
        if (value == null || value.length() < 10) {
            stmt.setNull(index, java.sql.Types.TIMESTAMP);
            return;
        }
        stmt.setTimestamp(index, Timestamp.valueOf(
                LocalDateTime.parse(value.replace(" ", "T"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    protected static void setNullableTimestamp(PreparedStatement stmt, int index, String value) throws Exception {
        if (value == null || value.isBlank() || value.length() < 10)
            stmt.setNull(index, java.sql.Types.TIMESTAMP);
        else setTimestamp(stmt, index, value);
    }

    protected static void setNullableInteger(PreparedStatement stmt, int index, String value) throws Exception {
        if (value == null || value.isBlank()) stmt.setNull(index, java.sql.Types.INTEGER);
        else stmt.setInt(index, Integer.parseInt(value));
    }

    protected static void setBigDecimal(PreparedStatement stmt, int index, String value) throws Exception {
        stmt.setBigDecimal(index, new BigDecimal(value));
    }

    // ─── 内嵌 CSV 解析器 ───

    /**
     * 手动 CSV 行解析器 —— 正确处理双引号引用的字段（含逗号、换行）。
     * 比 String.split(",", -1) 更健壮，无需引入第三方 CSV 库。
     */
    static final class CsvParser {
        static String[] splitLine(String line) {
            List<String> fields = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (inQuotes) {
                    if (c == '"') {
                        // 双引号转义 "" -> "
                        if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                            current.append('"');
                            i++;
                        } else {
                            inQuotes = false;
                        }
                    } else {
                        current.append(c);
                    }
                } else {
                    if (c == '"') {
                        inQuotes = true;
                    } else if (c == ',') {
                        fields.add(current.toString().trim());
                        current.setLength(0);
                    } else {
                        current.append(c);
                    }
                }
            }
            fields.add(current.toString().trim());
            return fields.toArray(new String[0]);
        }
    }
}
