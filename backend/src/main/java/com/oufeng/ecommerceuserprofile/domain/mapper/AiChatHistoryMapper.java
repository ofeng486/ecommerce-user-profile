package com.oufeng.ecommerceuserprofile.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oufeng.ecommerceuserprofile.domain.entity.AiChatHistory;
import org.apache.ibatis.annotations.Mapper;

/** AI 对话历史 Mapper。 */
@Mapper
public interface AiChatHistoryMapper extends BaseMapper<AiChatHistory> {
}
