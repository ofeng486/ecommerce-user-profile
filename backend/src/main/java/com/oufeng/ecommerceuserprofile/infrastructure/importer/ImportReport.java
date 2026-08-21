package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导入结果报告 —— 提供分表、分行级别的详细统计信息。
 * 替代原先只返回 totalRows 的 ImportResult。
 */
public class ImportReport {

    private final String dataVersion;
    private final LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private boolean success;
    private int totalInserted;
    private int totalUpdated;
    private int totalRemapped;
    private int totalErrors;
    private int totalSkipped;
    private final List<TableReport> tableReports = new ArrayList<>();
    private final List<String> globalErrors = new ArrayList<>();

    public ImportReport(String dataVersion) {
        this.dataVersion = dataVersion;
        this.startedAt = LocalDateTime.now();
    }

    public void addTableReport(TableReport report) {
        tableReports.add(report);
    }

    public void addGlobalError(String error) {
        globalErrors.add(error);
    }

    public void finish(boolean success) {
        this.success = success;
        this.finishedAt = LocalDateTime.now();
        // 汇总
        this.totalInserted = tableReports.stream().mapToInt(TableReport::inserted).sum();
        this.totalUpdated = tableReports.stream().mapToInt(TableReport::updated).sum();
        this.totalRemapped = tableReports.stream().mapToInt(TableReport::remapped).sum();
        this.totalErrors = tableReports.stream().mapToInt(TableReport::errors).sum();
        this.totalSkipped = tableReports.stream().mapToInt(TableReport::skipped).sum();
    }

    /**
     * 导出为前端友好的 Map 结构
     */
    public Map<String, Object> toSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("success", success);
        summary.put("dataVersion", dataVersion);
        summary.put("durationSeconds", finishedAt != null
                ? java.time.Duration.between(startedAt, finishedAt).getSeconds() : -1);
        summary.put("totalInserted", totalInserted);
        summary.put("totalUpdated", totalUpdated);
        summary.put("totalRemapped", totalRemapped);
        summary.put("totalErrors", totalErrors);
        summary.put("totalSkipped", totalSkipped);
        summary.put("totalAffected", totalInserted + totalUpdated);
        List<Map<String, Object>> tables = new ArrayList<>();
        for (TableReport tr : tableReports) {
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("table", tr.table());
            tm.put("inserted", tr.inserted());
            tm.put("updated", tr.updated());
            tm.put("remapped", tr.remapped());
            tm.put("errors", tr.errors());
            tm.put("skipped", tr.skipped());
            tm.put("totalRows", tr.totalRows());
            if (!tr.errorSamples().isEmpty()) {
                tm.put("errorSamples", tr.errorSamples().subList(0,
                        Math.min(5, tr.errorSamples().size())));
            }
            tables.add(tm);
        }
        summary.put("tableDetails", tables);
        if (!globalErrors.isEmpty()) {
            summary.put("globalErrors", globalErrors);
        }
        return summary;
    }

    // ─── Getters ───

    public boolean isSuccess() { return success; }
    public int getTotalInserted() { return totalInserted; }
    public int getTotalUpdated() { return totalUpdated; }
    public int getTotalRemapped() { return totalRemapped; }
    public int getTotalErrors() { return totalErrors; }
    public int getTotalSkipped() { return totalSkipped; }
    public List<TableReport> getTableReports() { return tableReports; }

    /**
     * 单表导入报告
     */
    public record TableReport(
            String table,
            int totalRows,
            int inserted,
            int updated,
            int remapped,
            int errors,
            int skipped,
            List<String> errorSamples) {

        public static TableReport of(String table, int totalRows, int inserted,
                                      int updated, int remapped, int errors, int skipped,
                                      List<String> errorSamples) {
            return new TableReport(table, totalRows, inserted, updated, remapped, errors, skipped, errorSamples);
        }
    }
}
