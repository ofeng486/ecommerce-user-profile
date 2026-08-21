package com.oufeng.ecommerceuserprofile.infrastructure.importer;

import java.util.HashMap;
import java.util.Map;

/**
 * CSV 模板列名中英文映射工具。
 *
 * 背景：模板面向非开发人员，列名输出为中文（如「订单号」「用户编码」）；
 * 导入解析时把中文表头翻译回英文列名再匹配，因此旧的英文模板文件仍然兼容。
 * 未知列名（不在映射内）保持原样。
 */
public final class CsvColumnNames {

    private CsvColumnNames() {}

    /** 英文列名 → 中文列名（模板输出用） */
    private static final Map<String, String> CN = new HashMap<>();
    /** 中文列名 → 英文列名（导入解析用） */
    private static final Map<String, String> EN = new HashMap<>();

    static {
        register("id", "主键");
        register("parent_id", "父分类ID");
        register("category_name", "分类名称");
        register("category_level", "分类层级");
        register("status", "状态");
        register("product_code", "商品编码");
        register("category_id", "分类ID");
        register("product_name", "商品名称");
        register("brand_name", "品牌");
        register("unit_price", "单价");
        register("user_code", "用户编码");
        register("gender", "性别");
        register("age", "年龄");
        register("province", "省份");
        register("city", "城市");
        register("register_channel", "注册渠道");
        register("membership_level", "会员等级");
        register("registered_at", "注册时间");
        register("user_id", "用户ID");
        register("session_id", "会话ID");
        register("device_type", "设备类型");
        register("login_channel", "登录渠道");
        register("login_at", "登录时间");
        register("logout_at", "登出时间");
        register("duration_seconds", "登录时长(秒)");
        register("product_id", "商品ID");
        register("behavior_type", "行为类型");
        register("channel", "访问渠道");
        register("behavior_at", "行为时间");
        register("order_no", "订单号");
        register("order_status", "订单状态");
        register("total_amount", "订单金额");
        register("discount_amount", "优惠金额");
        register("payment_amount", "实付金额");
        register("payment_method", "支付方式");
        register("ordered_at", "下单时间");
        register("paid_at", "支付时间");
        register("completed_at", "完成时间");
        register("order_id", "订单ID");
        register("product_name_snapshot", "商品快照");
        register("quantity", "数量");
        register("item_amount", "明细金额");
        register("parent_category_name", "父分类名称");
    }

    private static void register(String en, String cn) {
        CN.put(en, cn);
        EN.put(cn, en);
    }

    /** 英文列名 → 中文（模板输出）；未知返回原样 */
    public static String toChinese(String col) {
        String cn = CN.get(col);
        return cn == null ? col : cn;
    }

    /** 中文列名 → 英文（解析）；未知返回原样 */
    public static String toEnglish(String col) {
        String en = EN.get(col);
        return en == null ? col : en;
    }

    /**
     * 翻译整行表头（数组原地语义不变，返回新数组）：
     * 中文列名 → 英文列名，未知保留原样。
     */
    public static String[] translateHeader(String[] headers) {
        String[] out = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            out[i] = toEnglish(headers[i].trim());
        }
        return out;
    }
}
