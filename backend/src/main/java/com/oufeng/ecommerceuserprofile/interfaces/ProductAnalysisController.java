package com.oufeng.ecommerceuserprofile.interfaces;

import com.oufeng.ecommerceuserprofile.common.Result;
import com.oufeng.ecommerceuserprofile.application.ProductAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 商品分析 REST 接口（销售排行/品类占比/价格带/头部贡献度）。
 */
@Tag(name = "商品分析")
@RestController
@RequestMapping("/api/v1/admin/product-analysis")
public class ProductAnalysisController {

    private final ProductAnalysisService service;

    public ProductAnalysisController(ProductAnalysisService service) {
        this.service = service;
    }

    @Operation(summary = "商品总览指标（商品数/销量/销售额/平均单价）")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(service.getOverview());
    }

    @Operation(summary = "销售 Top10（按销售额）")
    @GetMapping("/top-sales")
    public Result<List<Map<String, Object>>> topSales() {
        return Result.success(service.getTopSales(10));
    }

    @Operation(summary = "品类销售占比")
    @GetMapping("/category-share")
    public Result<List<Map<String, Object>>> categoryShare() {
        return Result.success(service.getCategoryShare());
    }

    @Operation(summary = "价格带分布")
    @GetMapping("/price-bands")
    public Result<List<Map<String, Object>>> priceBands() {
        return Result.success(service.getPriceBands());
    }

    @Operation(summary = "头部商品贡献度（Top10 销售额占比）")
    @GetMapping("/concentration")
    public Result<Map<String, Object>> concentration() {
        return Result.success(service.getConcentration());
    }
}
