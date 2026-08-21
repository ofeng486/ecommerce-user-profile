# Spark 用户画像任务说明

## RFM 指标

- `R`（Recency）：距离最近一次有效消费的天数，越近得分越高；
- `F`（Frequency）：有效订单数量，越多得分越高；
- `M`（Monetary）：有效消费金额，越高得分越高。

任务使用五分位法（NTILE(5)）生成 1～5 分，R 取反向分（6 - ntile），无订单用户 R=1。综合得分权重：**R×0.4 + F×0.3 + M×0.3**，与本地 `run_local_pipeline.py` 保持一致。

## 用户分层

管线先以 R/F/M 各 3 分为阈值生成 **8 分类**（`high/low × high/low × high/low`，共 8 组），再合并映射为 **5 分层**：

| 编码 | 名称 | 来源（rfm_group 映射） |
| --- | --- | --- |
| `HIGH_VALUE` | 高价值用户 | HIGH_VALUE（R≥3 且 F≥3 且 M≥3） |
| `POTENTIAL` | 潜力用户 | HIGH_DEVELOP / HIGH_RETAIN（R≥3 且至少一个指标高） |
| `AT_RISK` | 流失风险用户 | LOST_RETAIN / GEN_DEVELOP / GEN_RETAIN（R<3 但至少一个指标高） |
| `GENERAL` | 一般用户 | GEN_VALUE（R≥3 但两个指标都低） |
| `LOW_VALUE` | 低价值用户 | LOST（R 与指标均低） |

> 注意：分层基于 **rfm_group 分类合并**，非综合得分阈值；8 分类阈值以 3 为界，与早期版本"R≥4/F≥4"的规则口径不同。

## 提交任务

管线直读 MySQL 原始表计算（非 CSV）：

```bash
python bigdata-scripts/spark/run_local_pipeline.py \
  --data-version 20260711 \
  --jdbc-url "jdbc:mysql://localhost:3306/ecommerce_user_profile?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai" \
  --mysql-user root
```

> 参数均有默认值（`--data-version` 缺省取当前时间戳、`--jdbc-url` 默认 localhost:3306）。仅重算聚类而不跑全量画像时，使用 `spark/run_cluster_only.py`。

任务结果直接写入 MySQL 画像结果表：

- `user_profile_summary`（画像汇总）
- `user_segment`（用户 5 分层）
- `user_profile_tag`（标签结果）
- `ads_user_rfm`（RFM 8 分类明细）
- `user_cluster`（K-Means 聚类结果）

采用 `truncate + overwrite` 覆盖写入，画像结果表只保留最近一次成功结果。
