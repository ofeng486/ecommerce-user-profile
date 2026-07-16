# MySQL 数据库模型设计

## 1. 设计目标

数据库用于保存系统管理数据、电商基础业务模拟数据、Spark 分析任务状态以及用户画像结果。原始海量行为数据后续可进入 HDFS/Hive，MySQL 主要承担后台管理、结果查询和可视化接口的数据支撑。

## 2. 领域划分

| 领域 | 数据表 | 主要用途 |
| --- | --- | --- |
| 系统管理 | `sys_user`、`sys_login_log` | 后台账号、User/Admin 权限和登录审计 |
| 电商业务 | `ecommerce_user`、`product_category`、`product` | 被分析用户与商品基础信息 |
| 用户行为 | `user_browse_behavior`、`user_login_behavior` | 浏览、点击、收藏、加购和登录行为 |
| 交易业务 | `sales_order`、`sales_order_item` | 订单与订单商品明细 |
| 画像分析 | `profile_tag_definition`、`user_profile_tag`、`user_profile_summary`、`user_segment`、`ads_user_rfm` | 标签体系、用户画像、RFM 分层与 8 分类结果 |
| 任务管理 | `spark_analysis_task`、`comparison_task`、`comparison_result` | Spark 任务状态、画像对比分析与结果 |
| 人群圈选 | `audience_package`、`audience_rule` | 智能人群包圈选与动态条件存储 |

## 3. 核心关系

- 一个 `sys_user` 可以产生多条 `sys_login_log`，仅用于系统登录，不直接作为画像分析对象。
- 一个 `ecommerce_user` 可以产生多条浏览行为、登录行为和订单记录。
- 一个商品分类可以包含多个商品，商品可以出现在多条行为和订单明细中。
- 一个订单属于一个电商用户，并包含一条或多条订单明细。
- 一个画像用户可以拥有多个标签；标签定义通过 `profile_tag_definition` 统一管理。
- 一个画像用户保留一条当前汇总画像和一条当前用户分层结果，Spark 重算时更新版本和统计时间。

## 4. 统一规范

- 表名与字段名使用小写蛇形命名法。
- 主键统一采用 `BIGINT`，模拟数据生成时允许预先分配 ID，系统管理表可使用自增 ID。
- 金额统一使用 `DECIMAL(18,2)`，禁止使用浮点数保存金额。
- 业务时间使用 `DATETIME(3)`，创建和更新时间使用 `TIMESTAMP(3)`。
- 字符集统一使用 `utf8mb4`，存储引擎使用 `InnoDB`。
- 角色数据库值固定为 `User`、`Admin`，Java 枚举内部使用 `USER`、`ADMIN` 并显式转换。
- 高频查询字段建立普通或联合索引；不为低区分度字段单独建立无效索引。
- 海量行为表避免过多外键级联操作，保留必要引用索引，由生成和清洗流程保证数据一致性。

## 5. MySQL 与数仓边界

- `sys_*`、标签定义和 Spark 任务表仅保存在 MySQL。
- 电商用户、商品、行为和订单数据首先由 Python 生成，可导入 MySQL 用于系统演示，同时进入 Hive `ODS` 层。
- Spark 从 Hive `DWD/DWS` 层计算画像，最终将面向接口查询的结果同步到 MySQL 画像结果表。
- Hive `ADS` 保存可追溯的批次结果，MySQL 保存当前或近期可视化查询结果。
