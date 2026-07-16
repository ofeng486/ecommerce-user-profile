package com.oufeng.ecommerceuserprofilev2.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易内存限流拦截器。
 * 对登录接口实施基于 IP 的请求频率限制，防止暴力破解。
 *
 * 通过滑动窗口算法：每个 IP 在指定时间窗口内最多允许 maxRequests 次请求。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    /** 登录接口路径 */
    private static final String LOGIN_PATH = "/api/v1/auth/login";

    /** 时间窗口（秒） */
    private static final long WINDOW_SECONDS = 60;

    /** 窗口内最大请求数 */
    private static final int MAX_REQUESTS_PER_WINDOW = 10;

    /** 每个 IP 的请求时间戳记录 */
    private final Map<String, long[]> requestTimestamps = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 仅对登录接口限流
        if (!request.getRequestURI().equals(LOGIN_PATH)) {
            return true;
        }

        String clientIp = getClientIp(request);
        long now = Instant.now().getEpochSecond();
        long windowStart = now - WINDOW_SECONDS;

        // 滑动窗口：过滤并统计窗口内的请求数
        long[] timestamps = requestTimestamps.computeIfAbsent(clientIp, k -> new long[MAX_REQUESTS_PER_WINDOW]);
        synchronized (timestamps) {
            int count = 0;
            int insertPos = 0;
            for (int i = 0; i < timestamps.length; i++) {
                if (timestamps[i] > windowStart) {
                    count++;
                } else {
                    insertPos = i; // 找到第一个可覆盖的位置
                }
            }

            if (count >= MAX_REQUESTS_PER_WINDOW) {
                log.warn("登录限流触发: IP={}, 窗口内请求数={}", clientIp, count);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json; charset=UTF-8");
                response.getWriter().write(
                        "{\"code\":42900,\"message\":\"请求过于频繁，请稍后再试\"}");
                return false;
            }

            // 记录本次请求时间
            timestamps[insertPos] = now;
        }

        return true;
    }

    /** 获取客户端真实 IP（考虑代理/负载均衡） */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
