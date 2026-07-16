# 项目运行与部署说明

## 1. 环境要求

- Java 17；
- Node.js 20 或更高版本；
- Python 3.10 或更高版本；
- MySQL 9.7.1（当前本地端口为 `23307`）；
- Hadoop、Hive、Spark 集群（运行大数据任务时需要）。

## 2. MySQL 与后端

1. 在 Navicat 中执行 `docs/sql/mysql-schema.sql`；
2. 在 IntelliJ IDEA 的 Spring Boot 运行配置中设置：

```text
DB_URL=jdbc:mysql://localhost:23307/ecommerce_user_profile?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=<MySQL密码>
JWT_SECRET=<至少32字节随机密钥>
JPA_DDL_AUTO=validate
```

3. 运行 `EcommerceUserProfileBackendApplication`；
4. 后端地址：`http://localhost:8080`。

## 3. 前端

```bash
cd frontend
npm install
npm run dev
```

开发地址：`http://localhost:3006`。Vite 会将 `/api` 代理到 `http://localhost:8080`。

生产构建：

```bash
npm run build
```

构建产物位于 `frontend/dist/`。

## 4. 数据生成与 Hive

```bash
python bigdata-scripts/generate_data.py --output generated-data/million
bash bigdata-scripts/hive/upload_to_hdfs.sh generated-data/million /warehouse/ecommerce_profile/ods/raw
bash bigdata-scripts/hive/run_hive_pipeline.sh /warehouse/ecommerce_profile/ods/raw 2026-01-01 20260101000000
```

## 5. Spark 画像与 MySQL 同步

运行画像任务：

```bash
spark-submit bigdata-scripts/spark/rfm_profile_job.py --data-version 20260101000000
```

同步结果时设置密码环境变量，并通过 `--jars` 提供 MySQL Connector/J：

```bash
export MYSQL_PASSWORD='<MySQL密码>'
spark-submit \
  --jars /path/mysql-connector-j-9.7.0.jar \
  bigdata-scripts/spark/sync_ads_to_mysql.py \
  --data-version 20260101000000 \
  --jdbc-url 'jdbc:mysql://mysql-host:23307/ecommerce_user_profile?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai' \
  --mysql-user root
```

## 6. 验证命令

```bash
cd backend && ./mvnw test
cd frontend && npm run build
python -m unittest bigdata-scripts/test_generate_data.py -v
python -m unittest bigdata-scripts/hive/test_hive_scripts.py -v
python -m unittest bigdata-scripts/spark/test_rfm_profile_job.py -v
python -m unittest bigdata-scripts/spark/test_sync_ads_to_mysql.py -v
```

## 7. 安全要求

- 不提交数据库密码、JWT 密钥、管理员明文密码和 Access Token；
- 正式环境为 MySQL 创建最小权限应用账号，不使用 `root`；
- 当前 Spark 任务管理 API 记录任务状态，实际 `spark-submit` 由集群脚本执行。
