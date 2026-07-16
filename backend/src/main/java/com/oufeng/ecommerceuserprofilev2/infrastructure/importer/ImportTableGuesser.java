package com.oufeng.ecommerceuserprofilev2.infrastructure.importer;

import org.springframework.stereotype.Component;

/**
 * 导入表名猜测器 —— 统一根据文件名推断目标数据库表。
 * 消除 AdminDataImportController 和 DataImportOrchestrator 中的重复逻辑。
 */
@Component
public class ImportTableGuesser {

    /**
     * 根据文件名猜测目标表名。
     *
     * @param fileName 文件名（可能含路径，取 basename 匹配）
     * @return 表名，无法匹配返回 "unknown"
     */
    public String guessTable(String fileName) {
        if (fileName == null || fileName.isBlank()) return "unknown";
        String n = fileName.toLowerCase();

        // 精确匹配优先（按 specificity 排序）
        if (n.contains("order_item") || n.contains("orderitem") || n.contains("订单明细"))
            return "sales_order_item";
        if (n.contains("category") || n.contains("分类"))
            return "product_category";
        if (n.contains("order") || n.contains("订单"))
            return "sales_order";
        if (n.contains("browse") || n.contains("行为"))
            return "user_browse_behavior";
        if (n.contains("login") || n.contains("登录"))
            return "user_login_behavior";
        if (n.contains("product") || n.contains("商品"))
            return "product";
        if (n.contains("user") || n.contains("用户"))
            return "ecommerce_user";

        return "unknown";
    }

    /**
     * 获取所有的导入器，按外键依赖排序。
     * 左侧表不依赖右侧表，可以安全地顺序导入或分组并行。
     */
    public AbstractCsvImporter[] getImportersInOrder(
            ProductCategoryImporter ci, ProductImporter pi, EcommerceUserImporter ui,
            BrowseBehaviorImporter bi, LoginBehaviorImporter li,
            SalesOrderImporter oi, OrderItemImporter oii) {
        // 导入顺序按外键依赖：先基础数据，后关联数据
        return new AbstractCsvImporter[]{ci, pi, ui, li, bi, oi, oii};
    }
}
