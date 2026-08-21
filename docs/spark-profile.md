# Spark 用户画像任务说明

## RFM 指标

- `R`（Recency）：距离最近一次有效消费的天数，越近得分越高；
- `F`（Frequency）：有效订单数量，越多得分越高；
- `M`（Monetary）：有效消费金额，越高得分越高。

任务使用五分位法（NTILE(5)）生成 1～5 分，R 取反向分（6 - ntile），无订单用户 R=1。综合得分权重：**R×0.4 + F×0.3 + M×0.3**，与本地 `run_local_pipeline.py` 保持一致。

## 用户分层（规则分类，非综合得分阈值）

| 编码 | 名称 | 规则 |
| --- | --- | --- |
| `HIGH_VALUE` | 高价值用户 | R≥4 AND F≥4 AND M≥4 |
| `POTENTIAL` | 潜力用户 | R≥4 AND (F≥3 OR M≥3) |
| `AT_RISK` | 流失风险用户 | R≤2 AND (F≥4 OR M≥4) |
| `LOW_VALUE` | 低价值用户 | F≤2 AND M≤2 |
| `GENERAL` | 一般用户 | 其余 |

## 提交任务

```bash
python bigdata-scripts/spark/run_local_pipeline.py \
  --data-version 20260711
```

任务结果直接写入 MySQL 画像结果表：

- `user_segment`（用户分层）
- `user_profile_tag`（标签结果）
- `user_profile_summary`（画像汇总）
- `ads_user_rfm`（RFM 明细）
