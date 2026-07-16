# 系统架构与模块规划

## 整体架构

```text
┌──────────────────┐    REST API (JWT)    ┌──────────────────────┐
│  Vue 3 前端       │ ◄──────────────────► │  Spring Boot 后端     │
│  Element Plus     │                      │  Java 17 / Maven     │
│  ECharts 可视化    │                      │  MySQL 业务数据       │
└──────────────────┘                      └────────┬─────────────┘
                                                   │ JDBC 同步
┌──────────────────┐    Hive/Spark         ┌───────▼─────────────┐
│  Hadoop 集群      │ ◄──────────────────► │  MySQL 画像结果表     │
│  HDFS 存储        │                      │  user_profile_*      │
│  Hive 数据仓库     │                      │  user_segment        │
│  PySpark 计算      │                      └─────────────────────┘
└──────────────────┘
```

## 用户端（User）

普通用户登录后可访问 `/api/v1/profiles/**` 下的接口：

- 数据概览：`GET /profiles/overview` — 用户总数、画像覆盖率、高价值用户数、累计消费金额
- 用户分层分布：`GET /profiles/segments/distribution` — 五种价值分层的用户数量分布
- 标签分布：`GET /profiles/tags/distribution` — 按标签编码查询各标签值的用户数及占比
- 用户画像列表：`GET /profiles/users` — 分页查询，支持关键字搜索和分层筛选
- 用户画像详情：`GET /profiles/users/{userId}` — 用户维度指标、行为统计、分层信息
- 个人信息与认证：`POST /auth/login`、`GET /auth/me`

## 管理端（Admin）

管理员登录后可访问 `/api/v1/admin/**` 下的接口：

- 系统用户管理：`GET/POST /admin/users`、`PATCH /admin/users/{id}/status` — CRUD 与启停
- 登录审计日志：`GET /admin/login-logs` — 分页查询登录成功/失败记录
- 标签定义管理：`GET/POST/PUT/PATCH /admin/tags` — 标签字典的 CRUD 与启停控制
- 分析任务管理：`GET/POST /admin/tasks` — Spark 分析任务的创建与状态跟踪

## 后端 API 分层架构

```text
Controller  ──►  Service  ──►  Repository  ──►  MySQL
  (参数校验)       (业务逻辑)      (数据访问)
                   │
                   ├── JPA Repository (sys_user, profile_tag_definition, spark_analysis_task)
                   └── Native SQL Repository (user_profile_*, user_segment — Spark 写入的只读表)
```

- `common/`：`Result<T>` 统一响应、`ResultCode` 业务状态码、`UserRole` 枚举
- `config/`：`SecurityConfig` — JWT 无状态认证 + User/Admin 角色权限
- `security/`：`JwtTokenProvider`（签发/校验）、`JwtAuthenticationFilter`（拦截器）、`AuthenticatedUser`（上下文身份）
- `exception/`：`BusinessException` + `GlobalExceptionHandler` — 业务异常统一转换为 `Result<T>` 并映射 HTTP 状态码
- `tools/`：`DemoDataImporter` — `demo-import` Profile 下用 MySQL 聚合生成演示画像结果

## 大数据处理流程

1. **数据生成**：`bigdata-scripts/generate_data.py` — Python 流式生成百万级模拟数据 CSV
2. **数据入库**：CSV 上传 HDFS，通过 Hive 外部表接入
3. **数仓分层**：
   - `ODS`：外部表映射原始 CSV，保持字段原貌
   - `DWD`：ROW_NUMBER 去重、有效性校验、维度关联（用户/商品/分类）
   - `DWS`：按用户汇总订单指标、30 日行为统计、偏好分类（加权评分）
   - `ADS`：画像汇总、RFM 分层、4 类标签、标签与分层分布统计
4. **Spark 画像计算**：`bigdata-scripts/spark/rfm_profile_job.py` — 从 DWS 读取，NTILE(5) 计算 RFM 得分，生成 5 层用户分群（高价值/潜力/一般/流失风险/低价值），写入 ADS
5. **结果同步 MySQL**：`bigdata-scripts/spark/sync_ads_to_mysql.py` — JDBC truncate + overwrite 到 MySQL 画像结果表
6. **API 查询**：Spring Boot 通过原生 SQL 只读查询画像结果表
7. **可视化展示**：Vue 3 前端通过 ECharts 展示 Dashboard 仪表盘、分层分布、标签分析

## MySQL 存储域划分

| 域 | 表 | 说明 |
| --- | --- | --- |
| 系统管理 | `sys_user`、`sys_login_log` | 后台账号、角色和登录审计 |
| 电商业务 | `ecommerce_user`、`product`、`product_category` | 画像分析对象基础信息 |
| 用户行为 | `user_browse_behavior`、`user_login_behavior` | 浏览轨迹和登录活跃数据 |
| 交易业务 | `sales_order`、`sales_order_item` | 订单及明细 |
| 画像分析 | `profile_tag_definition`、`user_profile_tag`、`user_profile_summary`、`user_segment` | 标签体系和画像结果 |
| 任务管理 | `spark_analysis_task` | Spark 作业状态记录 |

## 前端架构

- **技术栈**：Vue 3 + Composition API + TypeScript + Vite + Element Plus + ECharts + Pinia + Vue Router
- **页面**：登录页 (/login)、仪表盘 (/)、用户画像列表 (/profiles)、画像详情 (/profiles/:id)、标签分析 (/tags)、任务管理 (/tasks)、系统用户管理 (/system-users)
- **路由守卫**：`meta.requiresAuth` 校验 JWT 登录态，`meta.admin` 校验 ADMIN 角色
- **API 封装**：Axios 实例，自动注入 `Authorization: Bearer` 请求头
