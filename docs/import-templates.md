# 数据导入 · CSV 模板说明

> 数据导入功能共支持 **7 张业务表**，对应 7 个 CSV 模板（在「数据导入」页面点击"下载 CSV 模板"获取）。
> **建议一次性选择 7 个 CSV 文件一起上传**，系统会按文件名自动识别对应表，并按外键依赖关系自动排序导入。

## 通用规则

- 模板第一行为**列名**，列名不可修改，从第二行开始填写数据；
- `id` 主键一般留空（自动生成），也可填不重复的数字；
- 时间列格式统一为 `YYYY-MM-DD HH:mm:ss`；
- 单行数据校验失败不会影响其他行（行级容错，错误会在导入报告中列出）。

---

## 1. `ecommerce_user` — 用户基本信息表

| 列 | 说明 |
|---|---|
| `id` | 主键（可空，自动生成） |
| `user_code` | 用户编码（必填，唯一标识） |
| `gender` | 性别：`男/女` 或 `Male/Female` |
| `age` | 年龄（数字） |
| `province` / `city` | 所在省 / 市 |
| `register_channel` | 注册渠道（如 `App` / `Web` / `WeChat`） |
| `membership_level` | 会员等级（如 `普通` / `银卡` / `金卡`） |
| `registered_at` | 注册时间 |
| `status` | 状态（如 `active` / `disabled`） |

## 2. `product_category` — 商品分类表（先导）

| 列 | 说明 |
|---|---|
| `id` / `parent_id` | 主键 / 父分类 ID（一级分类填 0 或空） |
| `category_name` | 分类名称（如"数码产品"） |
| `category_level` | 层级：`1` = 一级分类，`2` = 二级 |
| `status` | 状态 |

## 3. `product` — 商品表

| 列 | 说明 |
|---|---|
| `id` / `product_code` | 主键 / 商品编码 |
| `category_id` | 关联分类表 `id`（须先导入分类） |
| `product_name` | 商品名称 |
| `brand_name` | 品牌 |
| `unit_price` | 单价（数字） |
| `status` | 状态 |

## 4. `sales_order` — 订单主表（核心）

| 列 | 说明 |
|---|---|
| `id` / `order_no` | 主键 / 订单号 |
| `user_id` | 关联用户表 `id`（须先导入用户） |
| `order_status` | 订单状态 |
| `total_amount` / `discount_amount` / `payment_amount` | 订单总额 / 优惠金额 / 实付金额（系统校验：总额 − 优惠 ≈ 实付） |
| `payment_method` | 支付方式（如 `支付宝` / `微信` / `银行卡`） |
| `ordered_at` / `paid_at` / `completed_at` | 下单 / 支付 / 完成时间 |

## 5. `sales_order_item` — 订单明细表

| 列 | 说明 |
|---|---|
| `id` | 主键 |
| `order_id` | 关联订单表 `id` |
| `product_id` | 关联商品表 `id` |
| `product_name_snapshot` | 商品名称快照（下单时名称） |
| `unit_price` / `quantity` / `item_amount` | 单价 / 数量 / 小计（校验：单价 × 数量 ≈ 小计） |

## 6. `user_browse_behavior` — 浏览/互动行为表（画像计算重要输入）

| 列 | 说明 |
|---|---|
| `id` | 主键 |
| `user_id` / `product_id` | 关联用户 / 商品 |
| `behavior_type` | **枚举，仅限：`View`（浏览）/ `Click`（点击）/ `Favorite`（收藏）/ `Cart`（加购）** |
| `session_id` | 会话 ID |
| `device_type` | 设备（如 `Mobile` / `PC`） |
| `channel` | 访问渠道 |
| `behavior_at` | 行为时间 |

## 7. `user_login_behavior` — 登录行为表

| 列 | 说明 |
|---|---|
| `id` | 主键 |
| `user_id` | 关联用户 |
| `session_id` / `device_type` / `login_channel` | 会话 / 设备 / 登录渠道 |
| `login_at` / `logout_at` | 登录 / 登出时间 |
| `duration_seconds` | 在线时长（秒） |

---

## 导入要点

1. **外键依赖顺序**：分类 → 商品 → 用户 → 订单 → 订单明细 → 行为。系统在一次性全选上传时会自动按依赖排序，无需手动分批。
2. **行为枚举**：`behavior_type` 仅接受 `View` / `Click` / `Favorite` / `Cart` 四个值，写错该行会被拒绝并记录到导入报告。
3. **金额校验**：订单表 `总额 − 优惠 ≈ 实付`、明细表 `单价 × 数量 ≈ 小计`，不满足的行会被标记错误。
4. **批量导入性能**：后端已启用 `rewriteBatchedStatements` 批量写入，万级数据导入速度约提升 5-10 倍。

## 更省事的替代方式

- **数据生成功能**：Admin → 数据生成，调用 `bigdata-scripts/generate_data.py` 一键生成 7 表合规数据并自动导入（支持 small / default / medium 三档规模）；
- **演示数据**：直接使用 `bigdata-scripts/test-output/` 目录下的现成 CSV 文件，通过"目录导入"或"上传导入"即可。
