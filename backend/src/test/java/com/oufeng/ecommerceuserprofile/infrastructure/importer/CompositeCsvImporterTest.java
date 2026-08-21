package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 数据总表（一个 CSV 导入全部 7 表）分派逻辑测试。
 * 使用桩导入器（仅记录收到的行，不碰数据库）。
 */
class CompositeCsvImporterTest {

    /** 桩导入器：记录收到的行与共享 mapper，返回固定统计 */
    static class StubImporter extends AbstractCsvImporter {
        final String table;
        final List<Map<String, String>> received = new ArrayList<>();
        ImportIdMapper lastMapper;

        StubImporter(String table, String... columns) {
            super(null); // 桩实现不访问 jdbcTemplate
            this.table = table;
            this.columns = columns;
        }

        private final String[] columns;

        @Override public String tableName() { return table; }
        @Override public String[] columnNames() { return columns; }
        @Override protected String insertSql() { return ""; }
        @Override protected void bindRow(java.sql.PreparedStatement stmt, Map<String, String> row) { }

        @Override
        public int[] importRows(List<Map<String, String>> rows, ImportIdMapper idMapper) {
            received.addAll(rows);
            lastMapper = idMapper;
            return new int[]{rows.size(), 0, 0, 0, 0};
        }
    }

    @Test
    @DisplayName("总表按表名分派各行，未知表名计入错误")
    void shouldDispatchByTableColumn() throws Exception {
        StubImporter user = new StubImporter("ecommerce_user", "id", "user_code", "gender", "age");
        StubImporter category = new StubImporter("product_category", "id", "parent_id", "category_name", "category_level", "status");
        CompositeCsvImporter composite = new CompositeCsvImporter(List.of(category, user));

        String csv = "表名,id,parent_id,category_name,category_level,status,user_code,gender,age\n"
                + "ecommerce_user,,,,,,U0001,Male,25\n"
                + "ecommerce_user,,,,,,U0002,Female,30\n"
                + "product_category,1,0,数码,1,1,,,\n"
                + "unknown_table,,,,,,,,\n";

        ImportIdMapper mapper = new ImportIdMapper();
        int[] stats = composite.importComposite(new BufferedReader(new StringReader(csv)), mapper);

        assertThat(user.received).hasSize(2);
        assertThat(user.received.get(0)).containsEntry("user_code", "U0001").containsEntry("age", "25");
        assertThat(category.received).hasSize(1);
        assertThat(category.received.get(0)).containsEntry("category_name", "数码").containsEntry("status", "1");
        // 不串表：user 行不应带 category 列，category 行不应带 user 列
        assertThat(user.received.get(0)).doesNotContainKey("category_name");
        assertThat(category.received.get(0)).doesNotContainKey("user_code");
        // 共享 mapper：两个导入器收到同一个实例
        assertThat(user.lastMapper).isSameAs(mapper);
        assertThat(category.lastMapper).isSameAs(mapper);
        // 未知表名 1 行计入错误
        assertThat(stats[3]).isEqualTo(1);
        assertThat(stats[0]).isEqualTo(3); // 2 用户 + 1 分类（桩统计=行数）
    }

    @Test
    @DisplayName("表头缺少「表名」列时抛出异常")
    void shouldRejectHeaderWithoutTableColumn() {
        CompositeCsvImporter composite = new CompositeCsvImporter(List.of(new StubImporter("ecommerce_user", "id")));
        String csv = "id,user_code\n1,U0001\n";
        assertThatThrownBy(() ->
                composite.importComposite(new BufferedReader(new StringReader(csv)), new ImportIdMapper()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("表名");
    }

    @Test
    @DisplayName("总表模板剔除精简黑名单列，其余列保留")
    void templateHeaderOmitsRedundantColumns() {
        StubImporter user = new StubImporter("ecommerce_user", "id", "user_code", "gender", "age", "status");
        StubImporter login = new StubImporter("user_login_behavior", "id", "user_id", "session_id", "login_at", "logout_at", "duration_seconds");
        CompositeCsvImporter composite = new CompositeCsvImporter(List.of(user, login));

        String header = composite.templateHeader();
        String[] cols = header.split(",");

        // 黑名单列（主键/登出时间/登录时长）不出现（精确列匹配）
        assertThat(cols).doesNotContain("主键", "登出时间", "登录时长(秒)");
        // 必填/常用列保留（中文列名）
        assertThat(cols).contains("用户编码", "性别", "年龄", "状态", "用户ID", "会话ID", "登录时间");
        // 首列为表名
        assertThat(header).startsWith("表名,");
    }

    @Test
    @DisplayName("模板表头 = 表名 + 各表列并集（中文列名；剔除精简黑名单列 id）")
    void shouldBuildTemplateHeader() {
        CompositeCsvImporter composite = new CompositeCsvImporter(List.of(
                new StubImporter("product", "id", "product_code", "unit_price"),
                new StubImporter("ecommerce_user", "id", "user_code")));
        String header = composite.templateHeader();
        assertThat(header).startsWith("表名,商品编码,单价,用户编码");
        assertThat(header.split(",")).containsExactly("表名", "商品编码", "单价", "用户编码");
    }

    @Test
    @DisplayName("中文表头总表文件可正常导入（按表名分派、列名翻译回英文）")
    void shouldImportChineseHeaderComposite() throws Exception {
        StubImporter user = new StubImporter("ecommerce_user", "id", "user_code", "gender", "age");
        CompositeCsvImporter composite = new CompositeCsvImporter(List.of(user));

        // 中文列名（与模板输出一致）：表名,主键,用户编码,性别,年龄
        String csv = "表名,主键,用户编码,性别,年龄\n"
                + "ecommerce_user,,U0001,Male,25\n";

        int[] stats = composite.importComposite(new BufferedReader(new StringReader(csv)), new ImportIdMapper());

        assertThat(user.received).hasSize(1);
        assertThat(user.received.get(0)).containsEntry("user_code", "U0001").containsEntry("age", "25");
        assertThat(stats[3]).isZero();
    }

    @Test
    @DisplayName("中英列名映射互转")
    void chineseEnglishMapping() {
        assertThat(CsvColumnNames.toChinese("order_no")).isEqualTo("订单号");
        assertThat(CsvColumnNames.toEnglish("订单号")).isEqualTo("order_no");
        assertThat(CsvColumnNames.toChinese("unknown_col")).isEqualTo("unknown_col");
        assertThat(CsvColumnNames.toEnglish("表名")).isEqualTo("表名"); // 表名不在映射，保持原样
    }
}
