package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 合并模板集成验证（H2 内存库，自包含）。
 * 动态生成 4 个合并模板 CSV → importFromDirectory 导入 →
 * 验证：订单自动汇总、明细生成、商品/分类自动创建、浏览/登录分拣、用户导入。
 */
@SpringBootTest
@Sql(scripts = "/merged-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MergedTemplateIntegrationTest {

    @Autowired DataImportOrchestrator orchestrator;
    @Autowired JdbcTemplate jdbc;

    @TempDir Path tempDir;

    @Test
    void importMergedTemplates() throws IOException {
        // 预置用户 id=1（CSV 中 user_id 引用它）
        jdbc.update("INSERT INTO ecommerce_user (id, user_code, gender, age, province, city, register_channel, membership_level, registered_at, status) "
                + "VALUES (1,'U1','Male',20,'广东省','深圳市','APP','Normal','2026-01-01 00:00:00',1)");

        writeCsv("用户数据.csv",
                "主键,用户编码,性别,年龄,省份,城市,注册渠道,会员等级,注册时间,状态",
                ",UTEST001,Male,30,广东省,深圳市,APP,Gold,2026-08-01 10:00:00,1");
        writeCsv("交易数据.csv",
                "主键,订单号,用户ID,商品编码,商品名称,品牌,单价,数量,分类名称,订单状态,支付方式,下单时间,支付时间,完成时间",
                ",T-MERGE-001,1,P-TEST-A,测试商品A,测试品牌,100.00,2,测试分类,Completed,Alipay,2026-08-01 12:00:00,2026-08-01 12:01:00,2026-08-01 12:05:00",
                ",T-MERGE-001,1,P-TEST-A,测试商品A,测试品牌,100.00,1,测试分类,Completed,Alipay,2026-08-01 12:00:00,,",
                ",T-MERGE-002,1,P-TEST-A,测试商品A,测试品牌,50.00,3,测试分类,Paid,WeChat Pay,2026-08-02 09:00:00,,");
        writeCsv("互动数据.csv",
                "主键,用户ID,行为类型,商品编码,会话ID,设备类型,访问渠道,登录渠道,登录时间,登出时间,登录时长(秒),行为时间",
                ",1,View,P-TEST-A,S-MERGE-1,Phone,APP,,, ,,2026-08-01 12:10:00",
                ",1,Login,,S-MERGE-2,Phone,,APP,2026-08-01 12:20:00,2026-08-01 12:50:00,1800,");
        writeCsv("商品数据.csv",
                "主键,分类名称,父分类名称,商品编码,商品名称,品牌,单价,状态",
                ",测试分类B,,, ,,,",
                ",测试分类B,,P-TEST-B,测试商品B,品牌B,88.00,1");

        var report = orchestrator.importFromDirectory(tempDir);
        System.out.println("导入报告: 插入" + report.getTotalInserted() + " 更新" + report.getTotalUpdated()
                + " 错误" + report.getTotalErrors() + " 跳过" + report.getTotalSkipped());
        for (var tr : report.getTableReports()) {
            System.out.println("表报告: " + tr.table() + " 总" + tr.totalRows() + " 插" + tr.inserted()
                    + " 更" + tr.updated() + " 错" + tr.errors() + " 跳" + tr.skipped());
        }
        System.out.println("全局错误: " + report.toSummary().get("globalErrors"));
        assertThat(report.getTotalErrors()).as("不应有导入错误").isEqualTo(0);

        // 1. 订单自动汇总：T-MERGE-001 = 100*2 + 100*1 = 300；T-MERGE-002 = 150
        Double t1 = jdbc.queryForObject("SELECT total_amount FROM sales_order WHERE order_no='T-MERGE-001'", Double.class);
        assertThat(t1).isEqualTo(300.0);
        Double t2 = jdbc.queryForObject("SELECT total_amount FROM sales_order WHERE order_no='T-MERGE-002'", Double.class);
        assertThat(t2).isEqualTo(150.0);
        Integer itemCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_order_item WHERE order_id=(SELECT id FROM sales_order WHERE order_no='T-MERGE-001')", Integer.class);
        assertThat(itemCount).isEqualTo(2);

        // 2. 商品/分类自动创建（交易数据内）+ 商品数据模板（分类行+商品行）
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE product_code='P-TEST-A'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_category WHERE category_name='测试分类'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE product_code='P-TEST-B'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_category WHERE category_name='测试分类B'", Integer.class)).isEqualTo(1);

        // 3. 互动分拣：浏览 1 行 + 登录 1 行
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_browse_behavior WHERE session_id='S-MERGE-1'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_login_behavior WHERE session_id='S-MERGE-2'", Integer.class)).isEqualTo(1);

        // 4. 用户数据导入
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ecommerce_user WHERE user_code='UTEST001'", Integer.class)).isEqualTo(1);

        System.out.println("合并模板集成验证通过 ✓");
    }

    private void writeCsv(String name, String header, String... rows) throws IOException {
        StringBuilder sb = new StringBuilder(header).append('\n');
        for (String r : rows) sb.append(r).append('\n');
        Files.write(tempDir.resolve(name), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
