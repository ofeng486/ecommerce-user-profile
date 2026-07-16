package com.oufeng.ecommerceuserprofilev2;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 电商用户画像分析系统 v2 启动类。
 * MapperScan 仅扫描 domain.mapper 包下的 MyBatis-Plus Mapper 接口，
 * 避免误扫描 application 包下的业务接口。
 */
@SpringBootApplication
@MapperScan({"com.oufeng.ecommerceuserprofilev2.domain.mapper", "com.oufeng.ecommerceuserprofilev2.infrastructure.mapper"})
public class EcommerceUserProfileV2Application {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceUserProfileV2Application.class, args);
    }
}
