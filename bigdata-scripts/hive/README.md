# Hive 用户画像数仓脚本

本目录实现 `ODS -> DWD -> DWS -> ADS` 四层数据处理流程。

## 分层说明

- `01_ods_tables.sql`：创建 7 张 CSV 外部表，保留原始模拟数据；
- `02_dwd_tables.sql`：去重、校验枚举和金额、过滤无效引用，保存 Parquet 明细；
- `03_dws_tables.sql`：按用户汇总累计消费、近30日行为、活跃度和偏好分类；
- `04_ads_tables.sql`：生成画像汇总、用户标签、RFM 分层和 ECharts 分布数据。

## 运行流程

1. 生成模拟数据：

```bash
python bigdata-scripts/generate_data.py --output generated-data/million
```

2. 上传 CSV 到 HDFS：

```bash
bash bigdata-scripts/hive/upload_to_hdfs.sh \
  generated-data/million \
  /warehouse/ecommerce_profile/ods/raw
```

3. 执行完整数仓流水线：

```bash
export HIVE_JDBC_URL='jdbc:hive2://localhost:10000/default'
bash bigdata-scripts/hive/run_hive_pipeline.sh \
  /warehouse/ecommerce_profile/ods/raw \
  2026-01-01 \
  20260101000000
```

参数依次为：ODS 原始数据 HDFS 根目录、统计日期、数据版本。统计日期应与生成器的 `--reference-time` 日期保持一致。

## 核心口径

- 有效订单：`Paid`、`Shipped`、`Completed`；
- 近30日：统计日及之前共30个自然日；
- 偏好分类权重：`View=1`、`Click=2`、`Favorite=4`、`Cart=5`；
- RFM 使用 `NTILE(5)` 生成 1～5 分；
- 用户分层：`HIGH_VALUE`、`POTENTIAL`、`GENERAL`、`AT_RISK`、`LOW_VALUE`；
- ADS 每次全量覆盖，并写入显式 `data_version`，便于同步到 MySQL。

## 运行要求

- Hadoop/HDFS 客户端；
- HiveServer2 和 Beeline；
- Hive 支持 `OpenCSVSerde`、窗口函数和 Parquet；
- 执行账号对 `/warehouse/ecommerce_profile` 具有读写权限。

当前开发环境未安装 Hadoop/Hive，因此仓库内只执行脚本静态检查；应在目标集群上完成语法和数据量验证。
