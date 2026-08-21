package com.oufeng.ecommerceuserprofile.infrastructure.llm;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * LLM HTTP 客户端配置：流式对话使用带连接池的 RestTemplate。
 * AI 对话的云端/本地降级逻辑由 AI 对话 Controller 自行处理。
 */
@Configuration
public class LLMConfig {

    @Bean
    public RestTemplate streamingRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = buildFactory(15000, 120000);
        return new RestTemplate(factory);
    }

    @Bean
    public MockLLMProvider mockLLMProvider(JdbcTemplate jdbcTemplate) {
        return new MockLLMProvider(jdbcTemplate);
    }

    /**
     * 构建带连接池的 HttpClient 工厂。
     * @param connectTimeoutMs 连接超时（毫秒）
     * @param readTimeoutMs    读取超时（毫秒）
     */
    private HttpComponentsClientHttpRequestFactory buildFactory(int connectTimeoutMs, int readTimeoutMs) {
        // 连接池：最大 20 个连接，每个路由最多 10 个
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(20);
        connManager.setDefaultMaxPerRoute(10);

        // 连接配置（超时通过 ConnectionConfig 设置，不重复在 factory 上设置）
        ConnectionConfig connConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(connectTimeoutMs, TimeUnit.MILLISECONDS))
                .setSocketTimeout(Timeout.of(readTimeoutMs, TimeUnit.MILLISECONDS))
                .build();
        connManager.setDefaultConnectionConfig(connConfig);

        // 请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(5000, TimeUnit.MILLISECONDS))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return factory;
    }
}
