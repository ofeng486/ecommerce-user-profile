package com.oufeng.ecommerceuserprofile.infrastructure.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务失败信息翻译器测试：技术堆栈 → 人话原因。
 */
class TaskErrorTranslatorTest {

    @Test
    @DisplayName("脚本文件未找到 → 友好提示 + 保留技术日志")
    void shouldTranslateMissingScript() {
        String raw = "PySpark 退出码: 2\nD:\\python\\python310\\python.exe: can't open file "
                + "'E:\\bigdata-scripts\\spark\\run_local_pipeline.py': [Errno 2] No such file or directory";
        String out = TaskErrorTranslator.translate(raw);
        assertThat(out).startsWith("【失败原因】任务执行脚本未找到");
        assertThat(out).contains("【技术日志】");
        assertThat(out).contains("No such file");
    }

    @Test
    @DisplayName("JDK 兼容问题 → 提示使用 JDK 17")
    void shouldTranslateJdkIssue() {
        String out = TaskErrorTranslator.translate("Exception: getSubject is not supported");
        assertThat(out).contains("JDK 17");
    }

    @Test
    @DisplayName("数据库连接失败 → 提示检查数据库")
    void shouldTranslateDbIssue() {
        assertThat(TaskErrorTranslator.translate("Communications link failure")).contains("数据库连接失败");
        assertThat(TaskErrorTranslator.translate("Access denied for user")).contains("数据库连接失败");
    }

    @Test
    @DisplayName("内存不足 → 提示减少数据量")
    void shouldTranslateOom() {
        assertThat(TaskErrorTranslator.translate("java.lang.OutOfMemoryError: Java heap space")).contains("内存不足");
    }

    @Test
    @DisplayName("未知错误 → 通用提示，空输入原样返回")
    void shouldTranslateUnknownAndBlank() {
        String unknown = TaskErrorTranslator.translate("some weird internal error");
        assertThat(unknown).startsWith("【失败原因】任务执行失败");
        assertThat(TaskErrorTranslator.translate("")).isEmpty();
        assertThat(TaskErrorTranslator.translate(null)).isNull();
    }
}
