package com.oufeng.ecommerceuserprofilev2.interfaces;

import com.oufeng.ecommerceuserprofilev2.common.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 兼容 art-design-pro 框架默认调用的接口，
 * 返回空数据以消除 404 错误。
 */
@Tag(name = "兼容接口")
@RestController
public class CompatController {

    /** 角色列表——暂不支持，返回空 */
    @GetMapping({"/api/role/list", "/api/roles"})
    public Result<List<Map<String, Object>>> roleList() {
        return Result.success(List.of());
    }

    /** 菜单列表——暂不支持，返回空 */
    @GetMapping("/api/v3/system/menus/simple")
    public Result<List<Map<String, Object>>> menusSimple() {
        return Result.success(List.of());
    }
}
