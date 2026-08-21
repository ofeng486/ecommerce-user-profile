package com.oufeng.ecommerceuserprofile.application;

import com.oufeng.ecommerceuserprofile.infrastructure.mapper.ChurnAnalysisMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 流失预警服务：等级分布/名单分页/CSV 导出。
 */
@Service
@Transactional(readOnly = true)
public class ChurnAnalysisService {

    private final ChurnAnalysisMapper mapper;

    public ChurnAnalysisService(ChurnAnalysisMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> getLevels() {
        return mapper.queryLevels();
    }

    public Map<String, Object> getChurnList(String level, int page, int size, String orderBy, String orderDir) {
        long offset = (long) page * Math.min(size, 100);
        int limit = Math.min(size, 100);
        List<Map<String, Object>> records = mapper.queryChurnList(level, offset, limit, orderBy, orderDir);
        long total = mapper.countChurnList(level);
        return Map.of("records", records, "total", total);
    }

    public Map<String, Object> getDataVersion() {
        return mapper.queryDataVersion();
    }

    /** 流失名单 CSV 导出（UTF-8 BOM） */
    public byte[] exportCsv(String level) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("用户编码,性别,年龄,省份,用户分层,RFM分组,流失等级,距最近购买(天),订单数,累计消费\r\n");
        long offset = 0;
        while (true) {
            List<Map<String, Object>> rows = mapper.queryChurnList(level, offset, 1000, null, null);
            if (rows.isEmpty()) break;
            for (Map<String, Object> r : rows) {
                sb.append(csv(String.valueOf(r.getOrDefault("userCode", "")))).append(',')
                  .append(csv(String.valueOf(r.getOrDefault("gender", "")))).append(',')
                  .append(String.valueOf(r.getOrDefault("age", ""))).append(',')
                  .append(csv(String.valueOf(r.getOrDefault("province", "")))).append(',')
                  .append(csv(String.valueOf(r.getOrDefault("segmentName", "")))).append(',')
                  .append(csv(String.valueOf(r.getOrDefault("rfmGroupName", "")))).append(',')
                  .append(csv(String.valueOf(r.getOrDefault("level", "")))).append(',')
                  .append(String.valueOf(r.getOrDefault("recencyDays", ""))).append(',')
                  .append(String.valueOf(r.getOrDefault("orderCount", ""))).append(',')
                  .append(String.valueOf(r.getOrDefault("totalPaymentAmount", ""))).append("\r\n");
            }
            offset += rows.size();
            if (rows.size() < 1000) break;
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csv(String v) {
        if (v == null || "null".equals(v)) return "";
        return v.contains(",") || v.contains("\"") ? '"' + v.replace("\"", "\"\"") + '"' : v;
    }
}
