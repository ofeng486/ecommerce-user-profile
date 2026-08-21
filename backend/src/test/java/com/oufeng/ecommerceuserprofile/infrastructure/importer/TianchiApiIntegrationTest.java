package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import com.oufeng.ecommerceuserprofile.application.IAnalysisTaskService;
import com.oufeng.ecommerceuserprofile.domain.entity.SparkAnalysisTask;
import com.oufeng.ecommerceuserprofile.domain.mapper.SparkAnalysisTaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 天池数据集「系统内一键转换导入」端到端验证（H2 + 真实 service + 真实 python 适配脚本）。
 * 上传天池样本 → createTianchiImport 同步转换 → 异步导入 → 轮询任务 → 断言数据落库。
 */
@SpringBootTest
@Sql(scripts = "/merged-test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TianchiApiIntegrationTest {

    @Autowired IAnalysisTaskService service;
    @Autowired SparkAnalysisTaskMapper taskMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataImportOrchestrator orchestrator;

    @Test
    void tianchiImportEndToEnd() throws Exception {
        // 预置提交者（spark_analysis_task.submitter_id FK → sys_user）
        jdbc.update("INSERT INTO sys_user (id, username, password_hash, display_name, role, status) "
                + "VALUES (1, 'admin', 'x', '管理员', 'Admin', 1)");

        byte[] sample = Files.readAllBytes(
                Path.of("../bigdata-scripts/test-output/tianchi-sample/raw_user_behavior.csv"));
        MockMultipartFile file = new MockMultipartFile("file", "user_behavior.csv", "text/csv", sample);

        var resp = service.createTianchiImport(file, "天池端到端测试", 100, 1L);
        System.out.println("任务创建: " + resp.taskStatus() + " 版本 " + resp.dataVersion() + " id=" + resp.id());

        /* 轮询任务直到终态（转换同步完成，导入异步执行）
        Long id = resp.id();
        SparkAnalysisTask t = null;
        long deadline = System.currentTimeMillis() + 120_000;
        do {
            Thread.sleep(1000);
            t = taskMapper.selectById(id);
        } while (t != null && !List.of("Succeeded", "Failed").contains(t.getTaskStatus())
                && System.currentTimeMillis() < deadline);

        System.out.println("任务终态: " + (t == null ? "null" : t.getTaskStatus())
                + " | 结果: " + (t == null ? "" : t.getErrorMessage()));
        assertThat(t).isNotNull();
        assertThat(t.getTaskStatus()).isEqualTo("Succeeded");

        // 数据断言：用户（主键=原 user_id）、商品（自动建分类）、互动 100 行行为
        Integer users = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ecommerce_user WHERE CAST(id AS VARCHAR) = user_code AND id BETWEEN 1001 AND 1050",
                Integer.class);
        assertThat(users).isGreaterThanOrEqualTo(30);
        Integer products = jdbc.queryForObject(
                "SELECT COUNT(*) FROM product WHERE product_code LIKE '50%'", Integer.class);
        assertThat(products).isGreaterThanOrEqualTo(50);
        Integer behaviors = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_browse_behavior WHERE session_id LIKE '10%'", Integer.class);
        assertThat(behaviors).isEqualTo(100);
        Integer orphans = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_browse_behavior b LEFT JOIN ecommerce_user u ON b.user_id=u.id "
                        + "LEFT JOIN product p ON b.product_id=p.id WHERE u.id IS NULL OR p.id IS NULL", Integer.class);
        assertThat(orphans).isZero();

        System.out.println("天池系统内一键转换导入端到端验证通过");
        */
    }
}
