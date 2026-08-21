package com.oufeng.ecommerceuserprofile.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户聚类查询（K-Means 结果表 user_cluster）。
 */
@Mapper
public interface ClusterAnalysisMapper {

    /** 簇分布与各簇特征均值（消费/订单/浏览/登录） */
    List<Map<String, Object>> queryClusterOverview();

    /** 簇内用户分页（按消费降序，支持 orderBy=orderCount|totalPaymentAmount 排序） */
    List<Map<String, Object>> queryClusterUsers(@Param("cluster") int cluster,
                                                @Param("offset") long offset, @Param("limit") int limit,
                                                @Param("orderBy") String orderBy, @Param("orderDir") String orderDir);

    /** 簇内用户全量（CSV 导出用，按消费降序） */
    List<Map<String, Object>> queryClusterUsersAll(@Param("cluster") int cluster);

    /** 簇内用户总数 */
    long countClusterUsers(@Param("cluster") int cluster);

    /** 数据版本 */
    Map<String, Object> queryDataVersion();
}
