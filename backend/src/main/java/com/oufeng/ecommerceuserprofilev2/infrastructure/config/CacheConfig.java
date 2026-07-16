package com.oufeng.ecommerceuserprofilev2.infrastructure.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache 配置。
 * 使用内存缓存（ConcurrentHashMap），适用于单节点部署。
 * 生产环境可替换为 Redis CacheManager。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 缓存名称常量 */
    public static final String CACHE_PROFILE_OVERVIEW = "profileOverview";
    public static final String CACHE_SEGMENT_DIST = "segmentDistribution";
    public static final String CACHE_TAG_DIST = "tagDistribution";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CACHE_PROFILE_OVERVIEW,
                CACHE_SEGMENT_DIST,
                CACHE_TAG_DIST
        );
    }
}
