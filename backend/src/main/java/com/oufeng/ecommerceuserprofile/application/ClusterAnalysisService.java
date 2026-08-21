package com.oufeng.ecommerceuserprofile.application;

import com.oufeng.ecommerceuserprofile.infrastructure.mapper.ClusterAnalysisMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 用户聚类分析服务：簇分布/特征均值/簇内用户分页。
 */
@Service
@Transactional(readOnly = true)
public class ClusterAnalysisService {

    private final ClusterAnalysisMapper mapper;

    public ClusterAnalysisService(ClusterAnalysisMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> getOverview() {
        return mapper.queryClusterOverview();
    }

    public Map<String, Object> getClusterUsers(int cluster, int page, int size, String orderBy, String orderDir) {
        long offset = (long) page * Math.min(size, 100);
        int limit = Math.min(size, 100);
        return Map.of(
                "records", mapper.queryClusterUsers(cluster, offset, limit, orderBy, orderDir),
                "total", mapper.countClusterUsers(cluster));
    }

    public Map<String, Object> getDataVersion() {
        return mapper.queryDataVersion();
    }

    /** 簇内用户 CSV 导出（UTF-8 BOM，供 Excel 直接打开） */
    public byte[] exportCsv(int cluster) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("用户编码,性别,年龄,省份,用户分层,订单数,累计消费\r\n");
        List<Map<String, Object>> rows = mapper.queryClusterUsersAll(cluster);
        for (Map<String, Object> r : rows) {
            sb.append(csv(String.valueOf(r.getOrDefault("userCode", "")))).append(',')
              .append(csv(String.valueOf(r.getOrDefault("gender", "")))).append(',')
              .append(String.valueOf(r.getOrDefault("age", ""))).append(',')
              .append(csv(String.valueOf(r.getOrDefault("province", "")))).append(',')
              .append(csv(String.valueOf(r.getOrDefault("segmentName", "")))).append(',')
              .append(String.valueOf(r.getOrDefault("totalOrderCount", ""))).append(',')
              .append(String.valueOf(r.getOrDefault("totalPaymentAmount", ""))).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csv(String v) {
        if (v == null || "null".equals(v)) return "";
        return v.contains(",") || v.contains("\"") ? '"' + v.replace("\"", "\"\"") + '"' : v;
    }
}
