package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oufeng.ecommerceuserprofile.domain.entity.AiChatHistory;
import com.oufeng.ecommerceuserprofile.domain.mapper.AiChatHistoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 对话历史服务。
 * 保存问答记录，按用户分页查询历史，支持前端会话回看。
 */
@Service
public class AiChatHistoryService {

    private final AiChatHistoryMapper mapper;

    public AiChatHistoryService(AiChatHistoryMapper mapper) { this.mapper = mapper; }

    /** 保存一条问答历史（dataJson 为 SQL 查询结果序列化后的 JSON，可为空） */
    @Transactional
    public void save(Long userId, String question, String answer, String dataJson) {
        if (userId == null || question == null || question.isBlank()) return;
        String safeAnswer = answer == null ? "" : answer;
        String safeData = dataJson == null || dataJson.isBlank() ? null : dataJson;
        mapper.insert(new AiChatHistory(userId, question, safeAnswer, safeData));
    }

    /** 分页查询当前用户的历史（按时间倒序） */
    public Page<AiChatHistory> listByUser(Long userId, int page, int size) {
        return mapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AiChatHistory>()
                        .eq(AiChatHistory::getUserId, userId)
                        .orderByDesc(AiChatHistory::getCreatedAt));
    }

    /** 删除单条历史（校验归属，防越权删除他人记录） */
    @Transactional
    public boolean deleteById(Long id, Long userId) {
        return mapper.delete(new LambdaQueryWrapper<AiChatHistory>()
                .eq(AiChatHistory::getId, id)
                .eq(AiChatHistory::getUserId, userId)) > 0;
    }

    /** 清空当前用户的全部历史 */
    @Transactional
    public int clearByUser(Long userId) {
        return mapper.delete(new LambdaQueryWrapper<AiChatHistory>()
                .eq(AiChatHistory::getUserId, userId));
    }
}
