package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 发票数据集适配集成验证（H2 内存库，自包含）。
 * 导入 bigdata-scripts/test-output/tianchi-invoice/out 下由 tianchi_adapter.py
 * 按「发票格式」生成的 3 个模板文件，验证：
 * 用户（真实性别/年龄、主键=customer_id 数字部分）、商品/分类自动建档、
 * 交易数据（订单按 invoice_no 汇总金额=Σ单价×数量）、外键关联正确。
 */
@SpringBootTest
@Sql(scripts = "/merged-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TianchiInvoiceIntegrationTest {

    @Autowired DataImportOrchestrator orchestrator;
    @Autowired JdbcTemplate jdbc;

    @Test
    void importInvoiceAdaptedFiles() {
        var report = orchestrator.importFromDirectory(Path.of("../bigdata-scripts/test-output/tianchi-invoice/out"));
        for (var tr : report.getTableReports()) {
            System.out.println("表报告: " + tr.table() + " 总" + tr.totalRows() + " 插" + tr.inserted()
                    + " 更" + tr.updated() + " 错" + tr.errors() + " 跳" + tr.skipped());
        }
        assertThat(report.getTotalErrors()).as("发票适配导入不应有错误").isEqualTo(0);

        // 用户：200 个，主键=customer_id 数字部分（真实性别/年龄）
        Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM ecommerce_user", Integer.class);
        assertThat(users).isGreaterThanOrEqualTo(190);
        Integer realGender = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ecommerce_user WHERE gender IN ('Male','Female') AND age IS NOT NULL", Integer.class);
        assertThat(realGender).isGreaterThanOrEqualTo(190);
        Integer pkEqCode = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ecommerce_user WHERE CAST(id AS VARCHAR) = user_code", Integer.class);
        assertThat(pkEqCode).isEqualTo(users);

        // 商品 + 分类：8 个类目（分类行 + 商品行）
        Integer cats = jdbc.queryForObject("SELECT COUNT(*) FROM product_category WHERE category_level=1", Integer.class);
        assertThat(cats).isEqualTo(8);
        Integer products = jdbc.queryForObject("SELECT COUNT(*) FROM product", Integer.class);
        assertThat(products).isEqualTo(8);

        // 交易：200 明细 → 订单按 invoice_no 去重（同发票多商品合并）+ 明细 200 行
        Integer orders = jdbc.queryForObject("SELECT COUNT(*) FROM sales_order", Integer.class);
        assertThat(orders).isGreaterThan(150);
        Integer items = jdbc.queryForObject("SELECT COUNT(*) FROM sales_order_item", Integer.class);
        assertThat(items).isEqualTo(200);
        // 订单金额 = 明细汇总校验（任取一单）
        Integer amountOk = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_order o WHERE o.total_amount = "
                        + "(SELECT COALESCE(SUM(i.item_amount),0) FROM sales_order_item i WHERE i.order_id=o.id)", Integer.class);
        assertThat(amountOk).isEqualTo(orders);

        // 外键：订单用户、明细商品均可 join
        Integer orphans = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_order o LEFT JOIN ecommerce_user u ON o.user_id=u.id WHERE u.id IS NULL", Integer.class);
        assertThat(orphans).isZero();
        Integer itemOrphans = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_order_item i LEFT JOIN product p ON i.product_id=p.id WHERE p.id IS NULL", Integer.class);
        assertThat(itemOrphans).isZero();

        System.out.println("发票数据集适配导入集成验证通过");
    }
}
