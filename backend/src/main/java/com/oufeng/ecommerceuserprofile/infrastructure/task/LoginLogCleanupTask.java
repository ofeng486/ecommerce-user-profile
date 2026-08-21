package com.oufeng.ecommerceuserprofile.infrastructure.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 登录审计日志定时清理任务。
 * 每天凌晨清理保留期之外的 sys_login_log，防止审计表无限增长。
 * 可通过配置开关与保留天数调整（默认开启，保留 90 天）。
 */
@Component
public class LoginLogCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(LoginLogCleanupTask.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${data.cleanup.login-log-enabled:true}")
    private boolean enabled;

    @Value("${data.cleanup.login-log-retention-days:90}")
    private int retentionDays;

    public LoginLogCleanupTask(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** 每天 03:30 执行（可经 data.cleanup.login-log-cron 覆盖） */
    @Scheduled(cron = "${data.cleanup.login-log-cron:0 30 3 * * ?}")
    public void cleanLoginLogs() {
        if (!enabled) return;
        try {
            // 保留天数收敛到 30-365，防止误配置导致数据全删
            int days = Math.max(30, Math.min(retentionDays, 365));
            LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
            int deleted = jdbcTemplate.update(
                    "DELETE FROM sys_login_log WHERE login_at < ?", cutoff);
            if (deleted > 0) {
                log.info("登录日志清理完成：删除 {} 条 {} 天前的记录", deleted, days);
            }
        } catch (Exception e) {
            log.warn("登录日志清理失败: {}", e.getMessage());
        }
    }
}
