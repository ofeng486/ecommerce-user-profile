package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 数据导入编排器。
 *
 * 相比 v1 的改进：
 * <ul>
 *   <li>使用 {@link ImportTableGuesser} 统一表名猜测（消除重复逻辑）</li>
 *   <li>返回详细的 {@link ImportReport}（分表明细 + 错误样本）</li>
 *   <li>上传文件的临时目录在使用后自动清理</li>
 *   <li>支持导入器依赖排序（基表 -> 关联表）</li>
 *   <li>表头列名交集匹配算法更严格（至少匹配 2 列）</li>
 * </ul>
 */
@Service
public class DataImportOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DataImportOrchestrator.class);

    private final ProductCategoryImporter categoryImporter;
    private final ProductImporter productImporter;
    private final EcommerceUserImporter userImporter;
    private final BrowseBehaviorImporter browseImporter;
    private final LoginBehaviorImporter loginImporter;
    private final SalesOrderImporter orderImporter;
    private final OrderItemImporter orderItemImporter;
    private final TransactionImporter transactionImporter;
    private final InteractionImporter interactionImporter;
    private final ProductDataImporter productDataImporter;
    private final ImportTableGuesser tableGuesser;
    private final CompositeCsvImporter compositeImporter; // 总表（一个文件全部 7 表）导入器

    public DataImportOrchestrator(
            ProductCategoryImporter categoryImporter,
            ProductImporter productImporter,
            EcommerceUserImporter userImporter,
            BrowseBehaviorImporter browseImporter,
            LoginBehaviorImporter loginImporter,
            SalesOrderImporter orderImporter,
            OrderItemImporter orderItemImporter,
            TransactionImporter transactionImporter,
            InteractionImporter interactionImporter,
            ProductDataImporter productDataImporter,
            ImportTableGuesser tableGuesser) {
        this.categoryImporter = categoryImporter;
        this.productImporter = productImporter;
        this.userImporter = userImporter;
        this.browseImporter = browseImporter;
        this.loginImporter = loginImporter;
        this.orderImporter = orderImporter;
        this.orderItemImporter = orderItemImporter;
        this.transactionImporter = transactionImporter;
        this.interactionImporter = interactionImporter;
        this.productDataImporter = productDataImporter;
        this.tableGuesser = tableGuesser;
        // 总表导入器复用同一份按依赖排序的导入器
        this.compositeImporter = new CompositeCsvImporter(List.of(orderedImporters()));
    }

    /** 获取所有导入器（按外键依赖排序；合并模板放最后） */
    private AbstractCsvImporter[] orderedImporters() {
        return tableGuesser.getImportersInOrder(
                categoryImporter, productImporter, userImporter,
                browseImporter, loginImporter, orderImporter, orderItemImporter,
                transactionImporter, interactionImporter, productDataImporter);
    }

    /**
     * 从目录导入所有 CSV 文件。
     * 不清空已有数据，按表头列名匹配自动选择导入器。
     */
    @Transactional
    public ImportReport importFromDirectory(Path dataDirectory) {
        ImportReport report = new ImportReport("DIR_" + dataDirectory.getFileName());
        log.info("开始从目录导入数据: {}", dataDirectory);

        doImportFromDir(dataDirectory, report);

        report.finish(report.getTotalErrors() == 0);
        log.info("目录导入完成: 插入 {} 行, 更新 {} 行, 错误 {} 行, 跳过 {} 行",
                report.getTotalInserted(), report.getTotalUpdated(),
                report.getTotalErrors(), report.getTotalSkipped());
        return report;
    }

    /**
     * 从上传文件导入（支持部分文件，不清空已有数据）。
     * 上传文件导入完成后自动清理临时目录。
     */
    @Transactional
    public ImportReport importFromUpload(MultipartFile[] files) {
        Path tempDir = null;
        try {
            tempDir = saveUploadedFiles(files);
            ImportReport report = new ImportReport("UPLOAD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

            doImportFromDir(tempDir, report);

            report.finish(report.getTotalErrors() == 0);
            if (report.getTotalInserted() + report.getTotalUpdated() == 0) {
                report.addGlobalError("没有成功导入任何数据，请检查文件内容");
                report.finish(false);
            }
            return report;
        } catch (IOException e) {
            ImportReport errReport = new ImportReport("UPLOAD_ERROR");
            errReport.addGlobalError("上传文件处理失败: " + e.getMessage());
            errReport.finish(false);
            return errReport;
        } finally {
            // 确保临时文件被清理
            if (tempDir != null) {
                cleanupTempDir(tempDir);
            }
        }
    }

    // ─── 内部方法 ───

    /** 扫描目录中的所有 CSV 文件，按表头列名自动匹配导入 */
    private void doImportFromDir(Path dir, ImportReport report) {
        Map<String, Integer> importOrder = buildImportOrder();
        List<Path> csvFiles = new ArrayList<>();
        // 同批共享主键映射器：主表 id 重分配后，从表外键自动同步
        ImportIdMapper idMapper = new ImportIdMapper();

        // 收集所有 CSV 文件并按依赖排序
        try (var stream = Files.list(dir)) {
            for (Path file : stream.toList()) {
                String name = file.getFileName().toString();
                if (!name.toLowerCase().endsWith(".csv")) continue;
                csvFiles.add(file);
            }
        } catch (IOException e) {
            report.addGlobalError("扫描目录失败: " + e.getMessage());
            return;
        }

        // 按依赖顺序排序后导入
        csvFiles.sort(Comparator.comparingInt(f -> {
            String table = tableGuesser.guessTable(f.getFileName().toString());
            return importOrder.getOrDefault(table, 99);
        }));

        for (Path file : csvFiles) {
            String fileName = file.getFileName().toString();
            try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                importSingleFile(r, fileName, report, idMapper);
            } catch (Exception e) {
                log.warn("导入文件 {} 失败: {}", fileName, e.getMessage());
                report.addGlobalError(String.format("文件 %s 导入失败: %s", fileName, e.getMessage()));
            }
        }
    }

    /** 导入单个文件，自动匹配导入器 */
    private void importSingleFile(BufferedReader reader, String fileName, ImportReport report, ImportIdMapper idMapper) throws Exception {
        // 读取表头（可能带 UTF-8 BOM，先剥离；识别总表需依赖首列「表名」）
        reader.mark(8192);
        String headerLine = reader.readLine();
        if (headerLine == null) return;
        headerLine = AbstractCsvImporter.stripBom(headerLine);
        String[] headers = headerLine.toLowerCase().split(",", -1);
        // 中文列名 → 英文，再参与列匹配（模板已中文化，旧英文模板兼容）
        String[] enHeaders = CsvColumnNames.translateHeader(headers);
        Set<String> headerSet = new HashSet<>();
        for (String h : enHeaders) headerSet.add(h.trim());

        // 总表识别：表头含「表名」列 → 一个文件导入全部 7 表
        if (CompositeCsvImporter.isCompositeHeader(headers)) {
            log.info("文件 {} → 识别为数据总表（一个文件导入全部 7 表）", fileName);
            reader.reset();
            int[] stats = compositeImporter.importComposite(reader, idMapper);
            ImportReport.TableReport tr = ImportReport.TableReport.of(
                    "数据总表(全部7表)",
                    stats[0] + stats[1] + stats[2] + stats[3] + stats[4],
                    stats[0], stats[1], stats[2], stats[3], stats[4],
                    Collections.emptyList());
            report.addTableReport(tr);
            return;
        }

        // 按列名交集匹配最佳导入器（同分时列数更多的优先：合并模板列更全，可覆盖单表）
        AbstractCsvImporter best = null;
        int bestScore = 0;
        for (AbstractCsvImporter imp : orderedImporters()) {
            int score = 0;
            for (String col : imp.columnNames()) {
                if (headerSet.contains(col.toLowerCase())) score++;
            }
            if (score > bestScore
                    || (score == bestScore && best != null && imp.columnNames().length > best.columnNames().length)) {
                bestScore = score;
                best = imp;
            }
        }

        // 至少匹配 2 列才认为有效
        if (best != null && bestScore >= 2) {
            log.info("文件 {} → 匹配到表 {}（{} 列匹配）", fileName, best.tableName(), bestScore);
            reader.reset();
        } else {
            // 兜底：按文件名猜测
            reader.reset();
            String guess = tableGuesser.guessTable(fileName);
            best = findImporterByTable(guess);
            if (best == null) {
                report.addGlobalError("无法识别文件类型: " + fileName + "（列匹配:" + bestScore + ", 猜测表:" + guess + "）");
                return;
            }
            log.info("文件 {} → 按文件名匹配到表 {}", fileName, best.tableName());
        }

        // 执行导入（共享主键映射器，同批跨表外键同步）
        int[] stats = best.importFromReader(reader, idMapper);
        ImportReport.TableReport tableReport = ImportReport.TableReport.of(
                best.tableName(),
                stats[0] + stats[1] + stats[2] + stats[3] + stats[4], // total
                stats[0], stats[1], stats[2], stats[3], stats[4],
                Collections.emptyList() // error samples 已在 Importer 内部收集
        );
        report.addTableReport(tableReport);
    }

    /** 根据表名查找导入器 */
    private AbstractCsvImporter findImporterByTable(String tableName) {
        for (AbstractCsvImporter imp : orderedImporters()) {
            if (imp.tableName().equalsIgnoreCase(tableName)) return imp;
        }
        return null;
    }

    /** 构建表名 -> 导入顺位映射 */
    private Map<String, Integer> buildImportOrder() {
        Map<String, Integer> order = new LinkedHashMap<>();
        AbstractCsvImporter[] importers = orderedImporters();
        for (int i = 0; i < importers.length; i++) {
            order.put(importers[i].tableName(), i);
        }
        return order;
    }

    /** 保存上传的 MultipartFile 到临时目录 */
    private Path saveUploadedFiles(MultipartFile[] files) throws IOException {
        Path dir = Files.createTempDirectory("import_");
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            if (name == null || name.isBlank() || file.isEmpty()) continue;
            // 安全: 只取文件名，防止路径遍历
            String safeName = Path.of(name).getFileName().toString();
            Files.copy(file.getInputStream(), dir.resolve(safeName));
        }
        return dir;
    }

    /** 递归清理临时目录 */
    private void cleanupTempDir(Path dir) {
        try {
            if (Files.isDirectory(dir)) {
                try (var files = Files.list(dir)) {
                    for (Path f : files.toList()) {
                        Files.deleteIfExists(f);
                    }
                }
                Files.deleteIfExists(dir);
            }
        } catch (IOException e) {
            log.warn("清理临时目录失败: {}, 原因: {}", dir, e.getMessage());
        }
    }
}
