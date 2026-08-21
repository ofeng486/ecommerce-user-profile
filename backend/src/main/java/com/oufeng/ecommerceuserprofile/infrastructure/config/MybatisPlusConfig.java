package com.oufeng.ecommerceuserprofile.infrastructure.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

/**
 * MyBatis-Plus 配置。
 * 包括分页插件和审计字段自动填充。
 * 注意：MyBatis-Plus 3.5.16 起 PaginationInnerInterceptor 已移除，
 * MybatisPlusInterceptor 直接启用分页能力。
 */
@Configuration
public class MybatisPlusConfig {

    /** MyBatis-Plus 拦截器（分页能力内置） */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        return new MybatisPlusInterceptor();
    }

    /** 审计字段自动填充：createdAt 插入时填充，updatedAt 插入和更新时填充 */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                Instant now = Instant.now();
                this.strictInsertFill(metaObject, "createdAt", Instant.class, now);
                this.strictInsertFill(metaObject, "updatedAt", Instant.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", Instant.class, Instant.now());
            }
        };
    }
}
