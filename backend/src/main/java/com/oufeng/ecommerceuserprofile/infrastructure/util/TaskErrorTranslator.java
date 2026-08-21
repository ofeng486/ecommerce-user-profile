package com.oufeng.ecommerceuserprofile.infrastructure.util;

/**
 * 任务失败信息翻译器：把技术堆栈输出转成"非开发人员可读"的失败原因。
 * 输出格式：【失败原因】人话描述 + 【技术日志】原始输出（供管理员排障）。
 */
public final class TaskErrorTranslator {

    private TaskErrorTranslator() {}

    /** 翻译任务失败信息；空输入原样返回 */
    public static String translate(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        String lower = raw.toLowerCase();
        String reason;

        if (lower.contains("can't open file") && lower.contains("no such file")) {
            reason = "任务执行脚本未找到：系统找不到数据分析脚本文件，请检查大数据脚本目录（bigdata-scripts）是否存在或被移动。";
        } else if (lower.contains("getsubject is not supported")) {
            reason = "运行环境不兼容：分析引擎需要 JDK 17，当前使用了更高版本的 Java，请调整后端的 JDK 配置后重试。";
        } else if (lower.contains("access denied") || lower.contains("communications link failure")
                || lower.contains("unknown database") || lower.contains("connection refused")) {
            reason = "数据库连接失败：请检查 MySQL 服务是否已启动、连接账号密码与数据库名配置是否正确。";
        } else if (lower.contains("out of memory") || lower.contains("memoryerror") || lower.contains("java heap space")) {
            reason = "内存不足：数据规模过大或运行内存不够，请减少数据量或增大运行内存后重试。";
        } else if (lower.contains("pyspark") && (lower.contains("退出码") || lower.contains("exit code"))) {
            reason = "分析引擎执行失败（详见下方技术日志）：常见原因包括脚本路径、运行环境或数据异常，请将技术日志复制给管理员排查。";
        } else if (lower.contains("command not found") || lower.contains("'python' 不是内部或外部命令")) {
            reason = "Python 环境未找到：请确认系统已安装 Python 并配置环境变量后重试。";
        } else {
            reason = "任务执行失败，请查看下方技术日志，或将日志复制给管理员排查。";
        }
        return "【失败原因】" + reason + "\n\n【技术日志】\n" + raw;
    }
}
