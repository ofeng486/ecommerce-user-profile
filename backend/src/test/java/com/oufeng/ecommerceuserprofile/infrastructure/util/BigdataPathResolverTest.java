package com.oufeng.ecommerceuserprofile.infrastructure.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BigdataPathResolver 单元测试。
 * 验证相对路径在「backend 工作目录」与「项目根工作目录」两种启动方式下都能正确定位。
 */
@DisplayName("BigdataPathResolver 路径解析测试")
class BigdataPathResolverTest {

    private static final String SCRIPT = "../bigdata-scripts/spark/run_local_pipeline.py";

    @Test
    @DisplayName("从 backend 工作目录解析（Maven/命令行启动）")
    void resolveFromBackendCwd() {
        // backend 工作目录：剥掉 ../ 后需向上找一层
        String resolved = BigdataPathResolver.resolveFrom(SCRIPT, Path.of("..").toAbsolutePath());
        assertThat(Files.isRegularFile(Path.of(resolved)))
                .as("应解析到真实存在的脚本: %s", resolved)
                .isTrue();
        assertThat(resolved).contains("bigdata-scripts").endsWith("run_local_pipeline.py");
    }

    @Test
    @DisplayName("从项目根工作目录解析（IDEA 默认）")
    void resolveFromProjectRootCwd() {
        // 项目根工作目录：剥掉 ../ 后直接命中项目根/bigdata-scripts
        Path projectRoot = Path.of("").toAbsolutePath().getParent();
        String resolved = BigdataPathResolver.resolveFrom(SCRIPT, projectRoot);
        assertThat(Files.isRegularFile(Path.of(resolved)))
                .as("应解析到真实存在的脚本: %s", resolved)
                .isTrue();
        assertThat(resolved).contains("bigdata-scripts").endsWith("run_local_pipeline.py");
    }

    @Test
    @DisplayName("绝对路径原样返回")
    void keepAbsolutePath() {
        String abs = Path.of("").toAbsolutePath().resolve("bigdata-scripts").toString();
        assertThat(BigdataPathResolver.resolveFrom(abs, Path.of(""))).isEqualTo(abs);
    }
}
