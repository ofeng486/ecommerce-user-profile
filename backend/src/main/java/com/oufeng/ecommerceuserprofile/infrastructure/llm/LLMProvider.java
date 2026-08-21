package com.oufeng.ecommerceuserprofile.infrastructure.llm;

/**
 * LLM 大模型调用接口，支持 OpenAI 兼容 API 的任意提供商。
 */
public interface LLMProvider {

    /**
     * 单轮对话。
     * @param systemPrompt 系统提示词（角色设定、数据上下文）
     * @param userMessage  用户问题
     * @return 模型返回的文本
     */
    String chat(String systemPrompt, String userMessage);
}
