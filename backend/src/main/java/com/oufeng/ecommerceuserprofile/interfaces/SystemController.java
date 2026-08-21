package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统状态接口。
 */
@Tag(name = "系统管理")
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of("service", "ecommerce-user-profile-backend", "status", "UP"));
    }
}
