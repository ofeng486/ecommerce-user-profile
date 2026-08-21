package com.oufeng.ecommerceuserprofile;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 电商用户画像分析系统启动类。
 * MapperScan 仅扫描 domain.mapper 包下的 MyBatis-Plus Mapper 接口，
 * 避免误扫描 application 包下的业务接口。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan({"com.oufeng.ecommerceuserprofile.domain.mapper", "com.oufeng.ecommerceuserprofile.infrastructure.mapper"})
public class EcommerceUserProfileApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceUserProfileApplication.class, args);
    }
}
