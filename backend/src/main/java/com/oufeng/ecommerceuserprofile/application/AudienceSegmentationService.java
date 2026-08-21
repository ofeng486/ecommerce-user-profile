package com.oufeng.ecommerceuserprofile.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oufeng.ecommerceuserprofile.domain.dto.profile.UserProfileListItemResponse;
import com.oufeng.ecommerceuserprofile.domain.dto.segmentation.ConditionDTO;
import com.oufeng.ecommerceuserprofile.infrastructure.mapper.UserProfileQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class AudienceSegmentationService {

    private final UserProfileQueryMapper queryMapper;
    private static final String EMPTY_CONDITION = "__NO_VALUE__";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AudienceSegmentationService(UserProfileQueryMapper queryMapper) {
        this.queryMapper = queryMapper;
    }

    public long estimateCount(List<ConditionDTO> conditions, String logic) {
        if (conditions == null || conditions.isEmpty()) return queryMapper.countAllUsers();
        return queryMapper.countAudience(buildConditionMaps(conditions), resolveLogic(logic));
    }

    public Page<UserProfileListItemResponse> segmentUsers(
            List<ConditionDTO> conditions, String logic, int page, int size) {
        if (conditions == null || conditions.isEmpty()) {
            long offset = (long) page * size;
            long total = queryMapper.countAllUsers();
            List<UserProfileListItemResponse> records = queryMapper.queryAllProfilesPaged(offset, size);
            Page<UserProfileListItemResponse> result = new Page<>(page, size, total);
            result.setRecords(records);
            return result;
        }
        List<Map<String, Object>> condMaps = buildConditionMaps(conditions);
        String safeLogic = resolveLogic(logic);
        long total = queryMapper.countAudience(condMaps, safeLogic);
        long offset = (long) page * size;
        List<UserProfileListItemResponse> records = queryMapper.queryAudience(condMaps, safeLogic, offset, size);
        Page<UserProfileListItemResponse> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }

    /** 构建条件 Map，兼容从 audience_rule 读取的 JSON 数组字符串 */
    private List<Map<String, Object>> buildConditionMaps(List<ConditionDTO> conditions) {
        List<Map<String, Object>> list = new ArrayList<>(conditions.size());
        for (ConditionDTO c : conditions) {
            Map<String, Object> m = new HashMap<>();
            m.put("field", c.field());
            m.put("operator", c.operator());
            // 条件间独立逻辑：透传 logicOp（首个条件 null → SQL trim 自动去前缀，默认 AND）
            m.put("logicOp", c.logicOp());
            Object val = c.value();
            if ("between".equals(c.operator())) {
                if (val instanceof List<?> l && l.size() >= 2) {
                    m.put("valueFrom", l.get(0)); m.put("valueTo", l.get(1));
                } else if (val instanceof String s && s.startsWith("[")) {
                    try { List<?> p = objectMapper.readValue(s, List.class);
                        m.put("valueFrom", p.get(0)); m.put("valueTo", p.get(1)); }
                    catch (Exception e) { m.put("value", s); }
                } else { m.put("value", val != null ? val.toString() : EMPTY_CONDITION); }
            } else if ("in".equals(c.operator())) {
                if (val instanceof List<?>) m.put("valueList", val);
                else if (val instanceof String s && s.startsWith("[")) {
                    try { m.put("valueList", objectMapper.readValue(s, List.class)); }
                    catch (Exception e) { m.put("value", s); }
                } else m.put("value", val != null ? val.toString() : EMPTY_CONDITION);
            } else {
                m.put("value", val != null ? val.toString() : EMPTY_CONDITION);
            }
            list.add(m);
        }
        return list;
    }

    private String resolveLogic(String logic) {
        return "OR".equalsIgnoreCase(logic) ? "OR" : "AND";
    }
}
