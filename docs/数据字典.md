# MySQL 数据字典

## 1. 公共约定

| 项目 | 约定 |
| --- | --- |
| 字符编码 | `utf8mb4` |
| 存储引擎 | `InnoDB` |
| 主键 | `BIGINT UNSIGNED` |
| 金额 | `DECIMAL(18,2)` |
| 业务时间 | `DATETIME(3)` |
| 审计时间 | `TIMESTAMP(3)` |
| 系统角色 | `User`、`Admin` |
| 数据版本 | 建议使用 `yyyyMMddHHmmss` 或分析批次号 |

## 2. 系统管理表

### 2.1 `sys_user`

系统后台登录账号，与被分析的电商用户完全独立。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT UNSIGNED` | PK, AUTO_INCREMENT | 系统用户主键 |
| `username` | `VARCHAR(50)` | UNIQUE, NOT NULL | 登录用户名 |
| `password_hash` | `VARCHAR(100)` | NOT NULL | BCrypt 密码摘要，禁止明文 |
| `display_name` | `VARCHAR(50)` | NOT NULL | 页面显示名称 |
| `role` | `ENUM` | NOT NULL | 仅允许 `User`、`Admin` |
| `status` | `TINYINT UNSIGNED` | NOT NULL | 0禁用，1启用 |
| `last_login_at` | `DATETIME(3)` | NULL | 最后成功登录时间 |
| `created_at` | `TIMESTAMP(3)` | NOT NULL | 创建时间 |
| `updated_at` | `TIMESTAMP(3)` | NOT NULL | 更新时间 |

### 2.2 `sys_login_log`

记录系统后台登录成功或失败情况。登录失败时 `sys_user_id` 可以为空，但应保留提交的 `username`。

## 3. 电商基础数据表

### 3.1 `ecommerce_user`

画像分析对象。`user_code` 是模拟生成的脱敏业务编码，不生成真实姓名、身份证、手机或详细地址。

主要模拟规则：

- `gender`：`Unknown`、`Male`、`Female`；
- `age`：建议 15～80 岁，数据库允许 1～120；
- `province/city`：从固定地区字典中成对抽样；
- `register_channel`：如 `Web`、`App`、`MiniProgram`、`Offline`；
- `registered_at`：必须早于该用户所有行为和订单时间。

### 3.2 `product_category`

支持父子分类。建议模拟两级分类，例如“数码产品/手机”“服装/男装”。

### 3.3 `product`

商品属于一个分类，`unit_price` 必须大于等于 0。商品历史成交价以订单明细快照为准，不直接使用当前商品价格回算历史订单。

## 4. 行为数据表

### 4.1 `user_browse_behavior`

| 字段 | 说明 |
| --- | --- |
| `behavior_type` | `View` 浏览、`Click` 点击、`Favorite` 收藏、`Cart` 加购 |
| `session_id` | 一次访问会话编码，同一会话可产生多条行为 |
| `device_type` | 如 `PC`、`Android`、`iOS`、`Tablet` |
| `behavior_at` | 必须晚于用户注册时间 |

生成时应提高 `View` 和 `Click` 占比，`Favorite` 和 `Cart` 占比较低；热门商品获得更多行为，使数据呈现长尾分布。

### 4.2 `user_login_behavior`

- `logout_at` 不得早于 `login_at`；
- `duration_seconds` 应等于退出与登录时间差；
- 未正常退出的会话允许 `logout_at` 和 `duration_seconds` 为空。

## 5. 交易数据表

### 5.1 `sales_order`

订单状态包括：`Pending`、`Paid`、`Shipped`、`Completed`、`Cancelled`、`Refunded`。

一致性规则：

- `payment_amount = total_amount - discount_amount`；
- `total_amount` 应等于全部订单明细 `item_amount` 之和；
- `paid_at` 不早于 `ordered_at`；
- `completed_at` 不早于 `ordered_at`；
- 取消订单可以没有支付时间，完成订单必须具有支付时间和完成时间；
- 用户价值计算默认仅统计 `Paid`、`Shipped`、`Completed` 中符合分析口径的有效订单，具体口径由 Spark 作业固定。

### 5.2 `sales_order_item`

- `quantity > 0`；
- `item_amount = unit_price * quantity`；
- `product_name_snapshot` 保存下单时商品名称，避免商品改名影响历史展示。

## 6. 用户画像表

### 6.1 `profile_tag_definition`

管理员维护的标准标签定义。`value_type` 包括 `String`、`Number`、`Boolean`、`Date`，实际值统一序列化保存到用户标签结果表。

### 6.2 `user_profile_tag`

同一用户、同一标签、同一 `data_version` 仅保留一条结果。`score` 可保存置信度、权重或标签计算得分；永久标签的 `expires_at` 可为空。

### 6.3 `user_profile_summary`

保存接口常用的当前用户画像聚合结果：累计订单数、累计消费、平均客单价、近30日浏览/登录次数、最近活跃时间和偏好分类。

### 6.4 `user_segment`

保存当前 RFM 分层结果：

- `r_score`：最近一次有效消费距统计日越近，得分越高；
- `f_score`：统计周期内有效消费次数越多，得分越高；
- `m_score`：统计周期内有效消费金额越高，得分越高；
- 三项得分范围均为 1～5；
- `segment_code` 建议使用稳定英文编码，如 `HIGH_VALUE`、`POTENTIAL`、`GENERAL`、`AT_RISK`。

### 6.5 `ads_user_rfm`

RFM 8 分类精细化分层结果（由 `run_local_pipeline.py` 写入）：

- `r_value` / `f_value` / `m_value`：R/F/M 原始值；
- `r_score` / `f_score` / `m_score`：NTILE(5) 打分（1～5）；
- `rfm_group`：8 分类编码（HIGH_VALUE / HIGH_DEVELOP / HIGH_RETAIN / LOST_RETAIN / GEN_VALUE / GEN_DEVELOP / GEN_RETAIN / LOST）；
- `rfm_group_name`：8 分类中文名（重要价值客户 / 重要发展客户 / 重要保持客户 / 重要挽留客户 / 一般价值客户 / 一般发展客户 / 一般保持客户 / 流失客户）；
- `data_version`：分析批次版本，`calculated_at`：计算时间。

## 7. Spark 任务表

### 7.1 `spark_analysis_task`

任务状态流转建议：

```text
Pending -> Running -> Succeeded
                   -> Failed
Pending/Running -> Cancelled
```

失败时写入截断、脱敏后的错误摘要，完整 Spark 日志保存在本地日志文件中，不直接写入数据库。

## 8. 画像计算分层映射（本地 PySpark 管线）

| MySQL 业务数据 | 管线阶段 | 说明 |
| --- | --- | --- |
| `ecommerce_user`、`product_category`、`product` | DWD 清洗 | 维度基础数据清洗 |
| `user_browse_behavior`、`user_login_behavior` | DWD 清洗 | 行为去重、时间和字段标准化 |
| `sales_order`、`sales_order_item` | DWD 清洗 | 订单口径清洗和明细关联 |
| 用户级汇总指标 | DWS 聚合 | 按用户聚合浏览、登录和消费指标 |
| 标签、画像、用户分层 | 结果写回 | Spark 结果同步给后端查询 |

## 9. 索引与数据量说明

- 行为数据主要按“用户 + 时间”和“商品 + 时间”查询，因此采用联合索引；
- 订单主要按“用户 + 下单时间”和“状态 + 下单时间”查询；
- 标签结果主要按用户查询，也支持按标签和值筛选用户群；
- 百万级行为数据批量导入时建议分批提交，必要时先导入再建立次要索引；
- 不对海量行为表设置级联外键，数据引用完整性由 Python 生成器和 Spark 清洗过程共同保证。

## 9. 人群圈选

### 9.1 `audience_package`

存储管理员圈选保存的人群包：

- `package_name`：人群包名称；
- `description`：描述说明；
- `total_count`：实际圈选覆盖的用户总数；
- `status`：1=已保存，0=已删除（支持软删除恢复）；
- `created_by`：关联 `sys_user.id`。

### 9.2 `audience_rule`

存储人群包的动态圈选条件，支持 AND/OR 组合：

- `rule_group`：规则组名，用于嵌套条件分组（同级规则共享同一 group）；
- `field_name`：圈选字段（gender / age / segment_code / tag_value 等）；
- `operator`：运算符（EQ / NEQ / IN / BETWEEN / GT / LT / CONTAINS）；
- `value`：条件值，支持 JSON 格式（如 `"女"`、`[20,30]`、`["高消费"]`）；
- `logic_op`：AND / OR，与同组下一条规则的逻辑关系；
- `sort_order`：排序号。

## 10. 画像对比分析

### 10.1 `comparison_task`

记录人群包 A vs 人群包 B 的画像对比分析任务：

- `package_id_a / package_id_b`：被对比的两个人人群包；
- `comparison_dimensions`：JSON 数组，指定的对比维度（如 `["gender","age","consumption"]`）；
- `status`：任务状态流转（Pending → Running → Succeeded / Failed）。

### 10.2 `comparison_result`

存储每个对比维度下的详细统计数据：

- `dimension / dimension_value`：维度及其具体值；
- `count_a / count_b`：人群 A/B 在该维度值下的人数；
- `ratio_a / ratio_b`：人群 A/B 的占比；
- `diff_ratio`：差异率（ratio_a - ratio_b），正值表示 A 更显著。
