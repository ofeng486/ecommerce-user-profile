package com.oufeng.ecommerceuserprofilev2.infrastructure.llm;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * LLM 提供者自动配置。
 * 检测 ai.llm.api-key 是否配置，有则用 OpenAI 兼容远端服务，无则退化为本地模拟模式。
 * 使用 Apache HttpClient 连接池管理 HTTP 连接。
 */
@Configuration
public class LLMConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(LLMConfig.class);

    @Value("${ai.llm.api-key:[redacted]")
    private String apiKey;

    @Value("${ai.llm.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.llm.model:deepseek-chat}")
    private String model;

    @Bean
    public RestTemplate llmRestTemplate() {
        return new RestTemplate(buildFactory(10000, 60000));
    }

    @Bean
    public RestTemplate streamingRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = buildFactory(15000, 120000);
        return new RestTemplate(factory);
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

    @Bean
    public MockLLMProvider mockLLMProvider(JdbcTemplate jdbcTemplate) {
        return new MockLLMProvider(jdbcTemplate);
    }

    @Bean
    public LLMProvider llmProvider(RestTemplate llmRestTemplate, JdbcTemplate jdbcTemplate) {
        if (apiKey != null && !apiKey.isBlank()) {
            LOGGER.info("LLM 模式: 云端 ({} / {})", baseUrl, model);
            return OpenAICompatibleProvider.create(llmRestTemplate, baseUrl, apiKey, model);
        }
        LOGGER.info("LLM 模式: 本地模拟（未配置 AI_API_KEY）");
        return new MockLLMProvider(jdbcTemplate);
    }
}
