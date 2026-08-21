package com.oufeng.ecommerceuserprofile.infrastructure.util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * bigdata-scripts 相对路径解析工具。
 *
 * 后端进程的工作目录可能是 backend/（Maven/命令行启动）也可能是项目根（IDEA 默认），
 * 相对路径 "../bigdata-scripts/..." 在不同工作目录下会解析到不同位置。
 * 本工具从进程工作目录向上逐级探测，找到第一个存在的目标路径，兼容两种启动方式。
 */
public final class BigdataPathResolver {

    private BigdataPathResolver() {}

    /**
     * 将 bigdata-scripts 下的相对路径解析为绝对路径。
     * 绝对路径原样返回；相对路径（如 "../bigdata-scripts/..."）先剥掉前导 ../，
     * 再从进程工作目录向上逐级探测，找到第一个存在的目标路径（兼容 IDEA 项目根 / Maven backend 工作目录）。
     */
    public static String resolve(String rel) {
        return resolveFrom(rel, Path.of("").toAbsolutePath());
    }

    /** 带工作目录参数的解析（供测试验证不同启动方式） */
    static String resolveFrom(String rel, Path cwd) {
        Path p = Path.of(rel);
        if (p.isAbsolute()) return p.toString();
        // 剥掉前导 "../"（相对路径中的 .. 是相对 backend 工作目录的，不能直接参与向上探测）
        String stripped = rel.replaceFirst("^\\.\\.(/|\\\\)*", "");
        for (Path d = cwd; d != null; d = d.getParent()) {
            Path candidate = d.resolve(stripped).normalize();
            if (Files.exists(candidate)) {
                return candidate.toString();
            }
        }
        // 兜底：按工作目录语义解析（与旧行为一致）
        return cwd.resolve(stripped).normalize().toString();
    }
}
