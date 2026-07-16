# 电商模拟数据生成器

`generate_data.py` 使用 Python 标准库生成合规、脱敏、可复现的电商模拟 CSV 数据，不需要安装第三方依赖。

## 输出文件

- `product_category.csv`：商品分类；
- `product.csv`：商品信息；
- `ecommerce_user.csv`：被分析的电商用户；
- `user_browse_behavior.csv`：浏览、点击、收藏和加购行为；
- `user_login_behavior.csv`：用户登录行为；
- `sales_order.csv`：订单；
- `sales_order_item.csv`：订单明细。

字段与 `docs/sql/mysql-schema.sql`、`docs/data-dictionary.md` 保持一致，可用于导入 MySQL 或 HDFS/Hive。

## 快速生成小规模数据

在项目根目录执行：

```bash
python bigdata-scripts/generate_data.py \
  --output generated-data/sample \
  --users 1000 \
  --products 200 \
  --behaviors 10000 \
  --orders 2000 \
  --seed 2026 \
  --reference-time "2026-01-01 00:00:00"
```

## 生成百万级实验数据

```bash
python bigdata-scripts/generate_data.py \
  --output generated-data/million \
  --users 100000 \
  --products 10000 \
  --behaviors 1000000 \
  --orders 200000 \
  --seed 2026 \
  --reference-time "2026-01-01 00:00:00"
```

订单明细和浏览行为采用流式写入，不会整体驻留内存。内存主要用于保存用户注册时间和商品价格快照。

## 参数说明

| 参数 | 默认值 | 说明 |
| --- | ---: | --- |
| `--output` | `generated-data` | CSV 输出目录 |
| `--users` | `100000` | 电商用户数量 |
| `--products` | `10000` | 商品数量 |
| `--behaviors` | `1000000` | 浏览行为数量 |
| `--orders` | `200000` | 订单数量，每个订单生成 1～4 条明细 |
| `--seed` | `2026` | 随机种子，相同参数生成相同结果 |
| `--reference-time` | `2026-01-01 00:00:00` | 统计截止时间，所有业务时间不晚于该值 |

登录行为数量取 `max(users, behaviors // 10)`。

## 数据一致性

- 所有行为和订单时间不早于对应用户注册时间；
- 商品行为和订单明细引用有效商品 ID；
- 订单明细成交价来源于商品快照；
- `item_amount = unit_price × quantity`；
- 订单总金额等于全部明细金额之和；
- `payment_amount = total_amount - discount_amount`；
- `Completed` 订单具有支付和完成时间；
- 登录退出时间不早于登录时间，且不超过统计截止时间；
- 不生成姓名、手机号、身份证和详细地址等真实隐私字段。

## 运行测试

```bash
python -m unittest bigdata-scripts/test_generate_data.py -v
```
