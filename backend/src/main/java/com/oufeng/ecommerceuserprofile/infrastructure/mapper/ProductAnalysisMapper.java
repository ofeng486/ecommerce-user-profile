package com.oufeng.ecommerceuserprofile.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 商品分析查询（销售排行/品类占比/价格带，纯只读统计）。
 */
@Mapper
public interface ProductAnalysisMapper {

    /** 商品总览：商品数/总销量/总销售额/平均单价 */
    Map<String, Object> queryProductOverview();

    /** 销售 Top10：按商品聚合销量/金额/订单数 */
    List<Map<String, Object>> queryTopSales(@Param("limit") int limit);

    /** 品类销售占比（金额/销量） */
    List<Map<String, Object>> queryCategoryShare();

    /** 价格带分布：<100 / 100-500 / 500-1000 / 1000-5000 / >5000 */
    List<Map<String, Object>> queryPriceBands();

    /** 全部销售额（供头部贡献度计算） */
    Map<String, Object> queryTotalAmount();
}
