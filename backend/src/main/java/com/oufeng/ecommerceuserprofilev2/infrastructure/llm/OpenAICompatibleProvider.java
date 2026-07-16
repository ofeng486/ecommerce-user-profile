package com.oufeng.ecommerceuserprofilev2.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenAICompatibleProvider implements LLMProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAICompatibleProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern JSON_BLOCK = Pattern.compile("```json\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public OpenAICompatibleProvider(RestTemplate restTemplate, String baseUrl, String apiKey, String model) {
        this.restTemplate = restTemplate;
        String url = baseUrl;
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        this.baseUrl = url;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        Exception lastException = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                String responseBody = callApi(systemPrompt, userMessage);
                if (responseBody != null) {
                    return extractContent(responseBody);
                }
                return "抱歉，AI 服务返回空响应。";
            } catch (Exception e) {
                lastException = e;
                if (attempt == 0) {
                    LOGGER.warn("LLM 调用失败，准备重试", e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        LOGGER.error("LLM 调用失败（已重试）", lastException);
        return "抱歉，AI 服务暂时不可用，请稍后重试。";
    }

    private String callApi(String systemPrompt, String userMessage) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.3);
        body.put("max_tokens", 2048);

        ArrayNode messages = MAPPER.createArrayNode();

        ObjectNode sysMsg = MAPPER.createObjectNode();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        ObjectNode userMsg = MAPPER.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        body.set("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String requestUrl = baseUrl + "/v1/chat/completions";
        String requestBody = body.toString();
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                requestUrl,
                HttpMethod.POST,
                requestEntity,
                String.class);

        return resp.getBody();
    }

    private String extractContent(String responseBody) {
        try {
            String content = tryParseJson(responseBody);
            if (content != null) {
                return content;
            }
        } catch (Exception ignored) {
            // 直接解析失败
        }

        String fromJsonBlock = tryExtractJsonBlock(responseBody);
        if (fromJsonBlock != null) {
            return fromJsonBlock;
        }

        String fromAnyBlock = tryExtractAnyBlock(responseBody);
        if (fromAnyBlock != null) {
            return fromAnyBlock;
        }

        return responseBody;
    }

    private String tryParseJson(String responseBody) throws Exception {
        JsonNode root = MAPPER.readTree(responseBody);
        String content = root.path("choices").get(0).path("message").path("content").asText();
        if (content != null && !content.isEmpty()) {
            return content;
        }
        return null;
    }

    private String tryExtractJsonBlock(String responseBody) {
        Matcher m = JSON_BLOCK.matcher(responseBody);
        if (m.find()) {
            String jsonBlock = m.group(1).trim();
            try {
                String content = tryParseJson(jsonBlock);
                if (content != null) {
                    return content;
                }
            } catch (Exception ignored) {
                // 代码块也无法解析
            }
            return jsonBlock;
        }
        return null;
    }

    private String tryExtractAnyBlock(String responseBody) {
        Pattern pattern = Pattern.compile("```\\w*\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(responseBody);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    public static OpenAICompatibleProvider create(RestTemplate restTemplate,
                                                  String baseUrl,
                                                  String apiKey,
                                                  String model) {
        return new OpenAICompatibleProvider(restTemplate, baseUrl, apiKey, model);
    }
}
