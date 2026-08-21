package com.oufeng.ecommerceuserprofile.application;

import com.oufeng.ecommerceuserprofile.infrastructure.mapper.RepeatAnalysisMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 复购与留存分析服务：购买次数分布/复购率/购买间隔/留存 cohort/高复购用户。
 */
@Service
@Transactional(readOnly = true)
public class RepeatAnalysisService {

    private final RepeatAnalysisMapper mapper;

    public RepeatAnalysisService(RepeatAnalysisMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> getPurchaseDistribution() {
        return mapper.queryPurchaseDistribution();
    }

    /** 复购指标：总用户/有购用户/多购用户 + 复购率（多购/有购） */
    public Map<String, Object> getRepeatRate() {
        Map<String, Object> m = mapper.queryRepeatRate();
        long buyers = ((Number) m.getOrDefault("buyerUsers", 0)).longValue();
        long multi = ((Number) m.getOrDefault("multiBuyerUsers", 0)).longValue();
        double rate = buyers > 0 ? Math.round(multi * 10000.0 / buyers) / 100.0 : 0;
        m.put("repeatRate", rate);
        return m;
    }

    public Map<String, Object> getAvgInterval() {
        return mapper.queryAvgInterval();
    }

    public List<Map<String, Object>> getRetentionCohort() {
        return mapper.queryRetentionCohort();
    }

    public List<Map<String, Object>> getTopRepeat() {
        return mapper.queryTopRepeat();
    }
}
