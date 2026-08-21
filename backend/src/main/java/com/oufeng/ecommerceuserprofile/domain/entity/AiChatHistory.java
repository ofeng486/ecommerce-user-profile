package com.oufeng.ecommerceuserprofile.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * AI 对话历史实体。
 * 记录用户向 AI 分析助手提问的问答内容，以及 SQL 查询结果（JSON），
 * 支持前端历史会话回看与结果表格/图表渲染。
 */
@TableName("ai_chat_history")
public class AiChatHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提问用户 ID */
    private Long userId;

    /** 用户提问内容 */
    private String question;

    /** AI 回答文本 */
    private String answer;

    /** SQL 查询结果 JSON 数组（可为空） */
    @TableField("data_json")
    private String dataJson;

    /** 创建时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    public AiChatHistory() {}

    public AiChatHistory(Long userId, String question, String answer, String dataJson) {
        this.userId = userId;
        this.question = question;
        this.answer = answer;
        this.dataJson = dataJson;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getDataJson() { return dataJson; }
    public void setDataJson(String dataJson) { this.dataJson = dataJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
