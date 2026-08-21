package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 天池适配脚本输出集成验证（H2 内存库，自包含）。
 * 导入 bigdata-scripts/test-output/tianchi-sample/out 下由 tianchi_adapter.py
 * 生成的 3 个模板文件，验证：用户主键=原 user_id 直入、商品/分类自动建档、
 * 行为类型映射（View/Purchase/Cart/Favorite）入库、外键关联正确。
 */
@SpringBootTest
@Sql(scripts = "/merged-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TianchiAdapterIntegrationTest {

    @Autowired DataImportOrchestrator orchestrator;
    @Autowired JdbcTemplate jdbc;

    @Test
    void importTianchiAdaptedFiles() {
        var report = orchestrator.importFromDirectory(Path.of("../bigdata-scripts/test-output/tianchi-sample/out"));
        for (var tr : report.getTableReports()) {
            System.out.println("表报告: " + tr.table() + " 总" + tr.totalRows() + " 插" + tr.inserted()
                    + " 更" + tr.updated() + " 错" + tr.errors() + " 跳" + tr.skipped());
        }
        assertThat(report.getTotalErrors()).as("天池适配导入不应有错误").isEqualTo(0);

        // 用户：42 个（原 user_id 1001~1050 抽样），主键=原 user_id 直入
        Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM ecommerce_user WHERE user_code BETWEEN '1001' AND '1050'", Integer.class);
        assertThat(users).isGreaterThanOrEqualTo(30);
        Integer userByPk = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ecommerce_user WHERE CAST(id AS VARCHAR) = user_code", Integer.class);
        assertThat(userByPk).isEqualTo(users); // 主键=编码=原 user_id

        // 商品 + 分类：item_id 直入，item_category 自动建分类
        Integer products = jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE product_code LIKE '50%'", Integer.class);
        assertThat(products).isGreaterThanOrEqualTo(50);
        Integer cats = jdbc.queryForObject("SELECT COUNT(*) FROM product_category WHERE category_name IN ('1000','2000','3000')", Integer.class);
        assertThat(cats).isEqualTo(3);

        // 互动：100 行行为，映射后四种类型入库，外键关联成功
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_browse_behavior WHERE session_id LIKE '10%'", Integer.class);
        assertThat(total).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_browse_behavior WHERE behavior_type='View'", Integer.class)).isPositive();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_browse_behavior WHERE behavior_type='Purchase'", Integer.class)).isPositive();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_browse_behavior WHERE behavior_type='Cart'", Integer.class)).isPositive();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_browse_behavior WHERE behavior_type='Favorite'", Integer.class)).isPositive();
        // 外键有效：行为行的 user_id/product_id 都能 join 到主表
        Integer orphans = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_browse_behavior b LEFT JOIN ecommerce_user u ON b.user_id=u.id "
                        + "LEFT JOIN product p ON b.product_id=p.id WHERE u.id IS NULL OR p.id IS NULL", Integer.class);
        assertThat(orphans).isZero();

        System.out.println("天池适配导入集成验证通过");
    }
}
