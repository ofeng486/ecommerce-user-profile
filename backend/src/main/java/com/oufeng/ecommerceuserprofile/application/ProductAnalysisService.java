package com.oufeng.ecommerceuserprofile.application;

import com.oufeng.ecommerceuserprofile.infrastructure.mapper.ProductAnalysisMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 商品分析服务：销售排行/品类占比/价格带/头部贡献度（只读统计）。
 */
@Service
@Transactional(readOnly = true)
public class ProductAnalysisService {

    private final ProductAnalysisMapper mapper;

    public ProductAnalysisService(ProductAnalysisMapper mapper) {
        this.mapper = mapper;
    }

    /** 商品总览指标 */
    public Map<String, Object> getOverview() {
        return mapper.queryProductOverview();
    }

    /** 销售 Top N（按销售额） */
    public List<Map<String, Object>> getTopSales(int limit) {
        return mapper.queryTopSales(limit);
    }

    /** 品类销售占比 */
    public List<Map<String, Object>> getCategoryShare() {
        return mapper.queryCategoryShare();
    }

    /** 价格带分布 */
    public List<Map<String, Object>> getPriceBands() {
        return mapper.queryPriceBands();
    }

    /** 头部商品贡献度：Top10 销售额占总销售额比例 */
    public Map<String, Object> getConcentration() {
        List<Map<String, Object>> top = mapper.queryTopSales(10);
        Map<String, Object> total = mapper.queryTotalAmount();
        double topAmount = top.stream()
                .mapToDouble(m -> ((Number) m.getOrDefault("amount", 0)).doubleValue()).sum();
        double totalAmount = ((Number) total.getOrDefault("totalAmount", 0)).doubleValue();
        double ratio = totalAmount > 0 ? Math.round(topAmount / totalAmount * 10000) / 100.0 : 0;
        return Map.of(
                "topAmount", topAmount,
                "totalAmount", totalAmount,
                "ratio", ratio,
                "topCount", top.size());
    }
}
