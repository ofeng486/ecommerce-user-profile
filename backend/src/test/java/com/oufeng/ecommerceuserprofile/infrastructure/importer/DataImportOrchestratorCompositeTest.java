package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 上传链路总表识别测试：一个 CSV（第一列=表名）上传后，
 * Orchestrator 应识别为数据总表并按表名分派到各单表导入器。
 * 各记录型导入器继承真实 importer（保留真实 tableName/columnNames），仅记录收到的行。
 */
class DataImportOrchestratorCompositeTest {

    static class CategoryRec extends ProductCategoryImporter {
        final List<Map<String, String>> received = new ArrayList<>();
        CategoryRec() { super(null); }
        @Override public int[] importRows(List<Map<String, String>> rows, ImportIdMapper m) { received.addAll(rows); return new int[]{rows.size(), 0, 0, 0, 0}; }
    }
    static class ProductRec extends ProductImporter {
        final List<Map<String, String>> received = new ArrayList<>();
        ProductRec() { super(null); }
        @Override public int[] importRows(List<Map<String, String>> rows, ImportIdMapper m) { received.addAll(rows); return new int[]{rows.size(), 0, 0, 0, 0}; }
    }
    static class UserRec extends EcommerceUserImporter {
        final List<Map<String, String>> received = new ArrayList<>();
        UserRec() { super(null); }
        @Override public int[] importRows(List<Map<String, String>> rows, ImportIdMapper m) { received.addAll(rows); return new int[]{rows.size(), 0, 0, 0, 0}; }
    }
    static class BrowseRec extends BrowseBehaviorImporter {
        final List<Map<String, String>> received = new ArrayList<>();
        BrowseRec() { super(null); }
        @Override public int[] importRows(List<Map<String, String>> rows, ImportIdMapper m) { received.addAll(rows); return new int[]{rows.size(), 0, 0, 0, 0}; }
    }
    static class LoginRec extends LoginBehaviorImporter {
        final List<Map<String, String>> received = new ArrayList<>();
        LoginRec() { super(null); }
        @Override public int[] importRows(List<Map<String, String>> rows, ImportIdMapper m) { received.addAll(rows); return new int[]{rows.size(), 0, 0, 0, 0}; }
    }
    static class OrderRec extends SalesOrderImporter {
        final List<Map<String, String>> received = new ArrayList<>();
        OrderRec() { super(null); }
        @Override public int[] importRows(List<Map<String, String>> rows, ImportIdMapper m) { received.addAll(rows); return new int[]{rows.size(), 0, 0, 0, 0}; }
    }
    static class ItemRec extends OrderItemImporter {
        final List<Map<String, String>> received = new ArrayList<>();
        ItemRec() { super(null); }
        @Override public int[] importRows(List<Map<String, String>> rows, ImportIdMapper m) { received.addAll(rows); return new int[]{rows.size(), 0, 0, 0, 0}; }
    }

    @Test
    @DisplayName("上传总表文件：识别为数据总表并按表名分派到各表")
    void shouldImportCompositeFromUpload() throws Exception {
        CategoryRec category = new CategoryRec();
        ProductRec product = new ProductRec();
        UserRec user = new UserRec();
        BrowseRec browse = new BrowseRec();
        LoginRec login = new LoginRec();
        OrderRec order = new OrderRec();
        ItemRec item = new ItemRec();

        ImportTableGuesser guesser = mock(ImportTableGuesser.class);
        TransactionImporter tx = mock(TransactionImporter.class);
        InteractionImporter ia = mock(InteractionImporter.class);
        ProductDataImporter pd = mock(ProductDataImporter.class);
        when(guesser.getImportersInOrder(category, product, user, browse, login, order, item, tx, ia, pd))
                .thenReturn(new AbstractCsvImporter[]{category, product, user, browse, login, order, item, tx, ia, pd});

        DataImportOrchestrator orch = new DataImportOrchestrator(
                category, product, user, browse, login, order, item, tx, ia, pd, guesser);

        MockMultipartFile file = new MockMultipartFile("files", "全部数据.csv", "text/csv",
                compositeCsv().getBytes(StandardCharsets.UTF_8));

        ImportReport report = orch.importFromUpload(new MockMultipartFile[]{file});

        // 每表恰好收到 1 行（列内容提取正确性已由 CompositeCsvImporterTest 覆盖）
        assertThat(category.received).hasSize(1);
        assertThat(product.received).hasSize(1);
        assertThat(user.received).hasSize(1);
        assertThat(browse.received).hasSize(1);
        assertThat(login.received).hasSize(1);
        assertThat(order.received).hasSize(1);
        assertThat(item.received).hasSize(1);
        // 行内不串表：product 行不应含 user 专属列
        assertThat(product.received.get(0)).doesNotContainKey("user_code");
        assertThat(user.received.get(0)).doesNotContainKey("behavior_type");
        // 报告中出现总表记录
        assertThat(report.getTableReports()).anyMatch(r -> r.table().contains("数据总表"));
    }

    private static String compositeCsv() {
        return "表名,id,parent_id,category_name,category_level,status,product_code,category_id,product_name,brand_name,unit_price,user_code,gender,age,province,city,register_channel,membership_level,registered_at,user_id,product_id,behavior_type,session_id,device_type,channel,behavior_at,login_channel,login_at,logout_at,duration_seconds,order_no,order_status,total_amount,discount_amount,payment_amount,payment_method,ordered_at,paid_at,completed_at,order_id,product_name_snapshot,quantity,item_amount\n"
                + "product_category,,,数码,1,1,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n"
                + "product,,,1,手机,品牌A,1999.00,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,\n"
                + "ecommerce_user,,,,,,,,,U999999,Male,25,广东省,深圳市,APP注册,普通会员,2025-01-01 10:00:00,1,,,,,,,,,,,,,,,,,,,,,,,,,,,\n"
                + "user_browse_behavior,,,,,,,,,,,,,,,,,,,,1001,1,View,SESS1,Phone,APP,2025-01-02 10:00:00,,,,,,,,,,,,,,,,,\n"
                + "user_login_behavior,,,,,,,,,,,,,,,,,,,,1001,SESS2,Phone,APP,2025-01-03 10:00:00,2025-01-03 10:30:00,1800,,,,,,,,,,,,,\n"
                + "sales_order,,,,,,,,,,,,,,,,,,,,1001,已支付,100.00,0.00,100.00,Alipay,2025-01-04 10:00:00,2025-01-04 10:01:00,2025-01-04 10:05:00,,,,\n"
                + "sales_order_item,,,,,,,,,,,,,,,,,,,,1001,1,手机,1999.00,1,1999.00,,\n";
    }
}
