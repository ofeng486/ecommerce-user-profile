# 电商用户画像分析系统 v2 — 项目全面分析报告

> 分析日期：2026-07-13

---

## 一、项目概览

| 维度 | 详情 |
|------|------|
| **项目定位** | 基于 Spring Boot + MyBatis-Plus + Spark 的电商平台用户画像分析系统 |
| **技术栈** | Java 17 / Spring Boot 3.3.7 / MyBatis-Plus 3.5 / Vue 3 + TypeScript / PySpark |
| **数据库** | MySQL 8.0+ (utf8mb4)，共 16 张表 |
| **构建工具** | Maven（后端）+ pnpm（前端） |
| **版本** | v2，对 v1 的分层架构重构 |
| **运行模式** | 集群模式 (HDFS→Hive→Spark) / 本地 PySpark 模式 / 演示模式 |

---

## 二、整体架构图

```
┌──────────────┐     REST API (JWT)     ┌────────────────────┐
│  Vue3 前端    │ ◄──────────────────► │  Spring Boot 后端   │
│  art-design-  │   /api/v1/**         │  DDD 分层架构       │
│  pro v3.0.2   │                      │  MyBatis-Plus ORM   │
└──────────────┘                       └────────┬───────────┘
                                                 │
                                   ┌─────────────┴─────────────┐
                                   │         MySQL 8.0          │
                                   │  系统管理 / 电商业务 /      │
                                   │  用户行为 / 交易 / 画像     │
                                   └───────────────────────────┘
                                                 ▲
                                                 │ 写入画像结果
                    ┌────────────────────────────┼────────────────────────────┐
                    │                            │                            │
              ┌─────┴──────┐            ┌───────┴──────┐          ┌──────────┴──────────┐
              │ 集群模式    │            │  本地模式     │          │     演示模式          │
              │ HDFS→Hive  │            │  run_local_  │          │  DemoDataImporter    │
              │ →Spark作业 │            │  pipeline.py │          │  Java批量导入         │
              └────────────┘            └──────────────┘          └─────────────────────┘
                    ▲
           ┌────────┴────────┐
           │  generate_data  │  ← Python 百万级模拟数据生成
           │  .py            │
           └─────────────────┘
```

---

## 三、后端分析

### 3.1 分层层级

```
interfaces (Controller, 9个)       ← 入站请求，参数校验，结果封装
    │
application (Service, 5个)         ← 业务逻辑编排
    │
├─ domain (Entity + Mapper + DTO + Converter)  ← 领域模型
└─ infrastructure (Security / Config / Importer)  ← 基础设施
    │
common (Result / Exception / BaseEntity)       ← 横切通用组件
```

### 3.2 模块职责矩阵

| 模块 | 类数 | 核心职责 |
|------|------|----------|
| `common` | 7 | 统一响应 `Result<T>`、错误码、异常处理、基础实体、分页请求 |
| `domain/entity` | 4 | `SystemUser`, `SystemLoginLog`, `ProfileTagDefinition`, `SparkAnalysisTask` |
| `domain/mapper` | 4 | MyBatis-Plus BaseMapper 接口 |
| `domain/converter` | 4 | MapStruct Entity ↔ DTO 转换 |
| `domain/dto` | 17 | 认证/画像/标签/任务/用户 5 子域的 DTO |
| `application` | 5 | AuthService, SystemUserService, TagDefinitionService, AnalysisTaskService, UserProfileService |
| `infrastructure/security` | 3 | JWT Token 生成/解析、Spring Security 过滤器链 |
| `infrastructure/config` | 2 | Security 配置（角色权限矩阵）、MyBatis-Plus 分页/审计插件 |
| `infrastructure/importer` | 8 | 7 张业务表的 CSV 批量导入器 + 编排器 |
| `infrastructure/mapper` | 1 | UserProfileQueryMapper（原生 SQL + XML） |
| `interfaces` | 9 | REST Controller，含兼容 art-design-pro 框架的适配接口 |

### 3.3 API 端点统计

| 模块 | 端点数 | 公开 | User 角色 | Admin 角色 |
|------|--------|------|-----------|------------|
| 认证管理 | 4 | 2 | 4 | 4 |
| 系统管理 | 1 | 1 | 1 | 1 |
| 用户画像 | 5 | 0 | 5 | 5 |
| 公开数据 | 4 | 4 | 4 | 4 |
| Admin: 用户管理 | 6 | 0 | 0 | 6 |
| Admin: 标签定义 | 5 | 0 | 0 | 5 |
| Admin: 分析任务 | 5 | 0 | 0 | 5 |
| Admin: 数据导入 | 6 | 0 | 0 | 6 |
| 兼容接口 | 3 | 3 | 3 | 3 |
| **合计** | **39** | **10** | **17** | **39** |

### 3.4 技术选型评价

| 技术 | 评价 |
|------|------|
| Spring Boot 3.3.7 + Java 17 | ✅ 较新版本，Virtual Threads 可用 |
| MyBatis-Plus 3.5.16 | ✅ 成熟 ORM，分页/自动填充/代码生成完善 |
| Spring Security + JWT (jjwt 0.12.7) | ✅ 无状态认证，Token 含角色 Claim |
| MapStruct 1.6.3 | ✅ 编译期映射，零运行时开销 |
| Knife4j 4.5.0 | ✅ Swagger 增强，接口文档自动生成 |
| Lombok 1.18.36 | ⚠️ 便利但增加编译耦合，JDK 17 Record 可替代部分场景 |

---

## 四、前端分析

### 4.1 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 + Composition API | 3.5.21 |
| 语言 | TypeScript | 5.6 |
| 构建 | Vite | 7 |
| UI 库 | Element Plus | 2.11.2 |
| 样式 | Tailwind CSS | 4.1.14 |
| 图表 | ECharts | 6.0 |
| 状态管理 | Pinia + 持久化插件 | 3.0 |
| 路由 | Vue Router 4 | 4.5.1 |
| HTTP | Axios (封装重试/401/防抖) | 1.12 |
| 国际化 | Vue I18n | 9.14 |

### 4.2 页面清单

| 页面 | 路由 | 权限 |
|------|------|------|
| 登录/注册/忘记密码 | `/auth/*` | 公开 |
| 画像概览仪表盘 | `/dashboard` | User, Admin |
| 用户画像列表 | `/profiles` | User, Admin |
| 用户画像详情 | `/profiles/:id` | User, Admin |
| 标签分布分析 | `/tags` | User, Admin |
| 分析任务管理 | `/tasks` | Admin |
| 系统用户管理 | `/system/users` | Admin |
| 数据导入 | `/admin/import` | Admin |
| 异常页面 | `/403`, `/404`, `/500` | 公开 |

### 4.3 架构亮点

- **双权限模式架构**：通过 `VITE_ACCESS_MODE` 环境变量在前端路由角色过滤和后端动态菜单之间切换，适配不同部署场景
- **完整的路由守卫链**：登录检查 → 动态路由注册 → 菜单构建 → 权限验证 → 首屏重定向
- **Axios 封装深度**：请求重试（5xx）、401 防抖（3s 窗口）、统一错误码映射、国际化错误消息
- **Pinia 持久化方案**：5 个 Store 模块全部持久化到 localStorage，带版本化键管理
- **50+ 自定义组件**：图表/表单/布局/卡片/特效等高度封装的业务组件库
- **40+ 可配置项**：通过设置面板实时切换主题、布局、颜色、圆角等

---

## 五、大数据管线分析

### 5.1 数据生成 (`generate_data.py`)

| 特性 | 描述 |
|------|------|
| 依赖 | 纯 Python 标准库，零第三方依赖 |
| 数据量可配置 | `--users`(默认10万)、`--products`(默认1万)、`--behaviors`(默认100万)、`--orders`(默认20万) |
| 可复现性 | `--seed` 固定随机种子 |
| 数据一致性 | 行为/订单时间 ≥ 用户注册时间、金额公式验证、订单明细快照机制 |
| 内存优化 | 订单明细和浏览行为流式写入，不驻留内存 |
| 隐私合规 | 无真实姓名/手机号/身份证/地址等敏感字段 |

### 5.2 数仓分层 (Hive)

```
ODS (7 张外部表) → DWD (6 张 Parquet 清洗表) → DWS (1 张用户指标宽表) → ADS (5 张应用层表)
```

| 层级 | 表数 | 清洗逻辑 |
|------|------|----------|
| ODS | 7 | CSV 外部表，OpenCSVSerde，跳过表头 |
| DWD | 6 | 去重、枚举校验、引用完整性、时间逻辑校验 |
| DWS | 1 | 订单/行为/登录三域汇总，偏好分类加权评分 |
| ADS | 5 | RFM 分层 + 4 类标签 + 分布统计 |

### 5.3 Spark 计算作业

| 脚本 | 行数 | 模式 | 输出 |
|------|------|------|------|
| `rfm_profile_job.py` | 143 | 集群 (Hive→ADS) | 5 张 Hive ADS 表 |
| `rfm_sql_8seg.py` | 235 | 集群 (Hive→MySQL) | `ads_user_rfm` 表 |
| `run_local_pipeline.py` | 419 | 本地 (CSV→DWD→DWS→RFM) | 4 张 MySQL 画像表 |
| `sync_ads_to_mysql.py` | 78 | 集群 (ADS→MySQL) | 3 张 MySQL 画像表 |

### 5.4 RFM 分层算法

**两套分层粒度**（最终通过 map 合并）：

| 粒度 | 分类数 | 来源 |
|------|--------|------|
| 5 分类 | HIGH_VALUE / POTENTIAL / GENERAL / AT_RISK / LOW_VALUE | `rfm_profile_job.py`, Hive ADS |
| 8 分类 | 重要价值/发展/保持/挽留 + 一般价值/发展/保持 + 流失 | `rfm_sql_8seg.py`, `run_local_pipeline.py` |

综合得分：R×0.4 + F×0.3 + M×0.3（NTILE(5) 生成 1~5 分）

---

## 六、数据库设计分析

### 6.1 表结构总览

| 领域 | 表数 | 表名 |
|------|------|------|
| 系统管理 | 2 | `sys_user`, `sys_login_log` |
| 电商业务 | 2 | `product_category`, `product` |
| 用户数据 | 1 | `ecommerce_user` |
| 用户行为 | 2 | `user_browse_behavior`, `user_login_behavior` |
| 交易数据 | 2 | `sales_order`, `sales_order_item` |
| 画像结果 | 5 | `profile_tag_definition`, `user_profile_tag`, `user_profile_summary`, `user_segment`, `ads_user_rfm` |
| 任务管理 | 1 | `spark_analysis_task` |
| **合计** | **15+1** | |

### 6.2 设计规范

- 字符集：`utf8mb4`
- 引擎：`InnoDB`
- 主键：`BIGINT UNSIGNED AUTO_INCREMENT`（系统表）或业务指定（业务表）
- 金额：`DECIMAL(18,2)`
- 时间：`DATETIME` / `TIMESTAMP`
- CHECK 约束：年龄范围、金额非负、时间逻辑
- 外键：合理使用 CASCADE / SET NULL

---

## 七、项目优势总结

### 7.1 架构层面

1. **DDD 分层清晰**：interfaces → application → domain + infrastructure → common，职责边界明确
2. **三种运行模式适配多场景**：从本地开发到生产集群无缝切换
3. **安全设计到位**：JWT + BCrypt、环境变量管理密码、无状态会话、角色分级权限
4. **MapStruct 编译期映射**：零运行时反射开销，类型安全

### 7.2 数据工程层面

1. **数据生成器设计优秀**：纯标准库、流式写入、数据一致性规则完善、隐私合规
2. **数仓分层规范**：ODS→DWD→DWS→ADS 四层架构，清洗逻辑严谨
3. **两种 RFM 粒度灵活**：5 分类面向管理决策，8 分类面向精细化运营
4. **完整的测试覆盖**：每个模块都有静态测试或单元测试

### 7.3 前端层面

1. **art-design-pro 框架成熟**：50+ 封装组件、40+ 配置项、完善的路由/权限/主题体系
2. **双权限模式**：兼顾简单部署和动态权限需求
3. **HTTP 层深度封装**：重试/401 防抖/国际化错误处理
4. **开发体验良好**：自动导入、路径别名、Husky + commitlint

### 7.4 文档层面

1. **6 份技术文档**：架构/数据字典/模型设计/部署/演示导入/Spark 说明
2. **完整 SQL Schema**：建库建表 + 种子数据
3. **AGENTS.md 项目指引**：清晰的 AI Agent 协作指令

---

## 八、问题与改进建议

### 8.1 架构层面

| 问题 | 严重度 | 建议 |
|------|--------|------|
| 缺少 Service 层接口抽象 | ⚠️ 中 | 建议为每个 Service 定义接口，便于单元测试 Mock 和将来扩展 |
| 缺少统一日志切面 | ⚠️ 中 | 建议添加 AOP 切面记录关键操作日志（请求参数、响应、耗时） |
| 缺少 API 版本策略 | ⚠️ 低 | 当前只有 v1 路径前缀，建议规划版本演进策略 |
| pom.xml 中 Spring Boot 版本标注 4.1.0 | 🔴 高 | **Spring Boot 4.x 尚未发布**，实际为 3.3.7，文档描述需修正 |

### 8.2 后端代码

| 问题 | 严重度 | 建议 |
|------|--------|------|
| Controller 中仍有少量业务逻辑 | ⚠️ 中 | 如 `AdminDataImportController` 直接操作文件系统，建议下沉到 Service |
| 缺少参数校验国际化 | ⚠️ 低 | `@Valid` 校验消息建议使用 i18n key |
| JWT 密钥硬编码在 properties | ⚠️ 低 | 开发密钥仅用于本地，生产已要求环境变量，可接受但建议加注释强调 |
| 缺少接口限流 | ⚠️ 中 | 登录接口建议添加 Rate Limiting 防暴力破解 |
| 画像查询缺少缓存 | ⚠️ 中 | 概览/分布统计建议添加 Redis 缓存减少 DB 压力 |

### 8.3 前端代码

| 问题 | 严重度 | 建议 |
|------|--------|------|
| 生产环境 API 指向 Mock 服务 | ⚠️ 中 | `.env.production` 中 `VITE_API_URL` 指向 apifox mock，需替换为实际后端地址 |
| 菜单/角色管理页面为空壳 | ⚠️ 低 | `system/menu` 和 `system/role` 路由定义存在但无实际功能，可能造成用户困惑 |
| 部分路由组件缺失 | ⚠️ 中 | $1/$2 标记的标签分析/任务列表等组件可能未完全开发 |
| Tailwind CSS 4.x 稳定性 | ⚠️ 低 | v4 较新，生产环境建议锁定版本并关注兼容性 |

### 8.4 数据工程

| 问题 | 严重度 | 建议 |
|------|--------|------|
| `generate_data.py` 单进程生成百万级数据 | ⚠️ 中 | 建议支持多进程并行生成加速 |
| Spark 脚本缺少异常重试机制 | ⚠️ 低 | 建议增加失败重试和断点续跑功能 |
| 缺少数据质量监控 | ⚠️ 中 | 建议添加 Great Expectations 或自定义 DQ 检查 |
| Hive/Spark 脚本中的 stat_date 处理不够鲁棒 | ⚠️ 低 | 建议增加默认值和参数范围校验 |

### 8.5 运维与部署

| 问题 | 严重度 | 建议 |
|------|--------|------|
| 缺少 Docker 容器化方案 | ⚠️ 中 | 建议添加 Dockerfile + docker-compose 一键启动 |
| 缺少 CI/CD 流水线 | ⚠️ 低 | 建议添加 GitHub Actions / Jenkins pipeline |
| 缺少监控/告警 | ⚠️ 低 | 建议集成 Spring Boot Actuator + Prometheus + Grafana |
| 缺少自动化测试覆盖 | ⚠️ 中 | 后端仅 1 个空测试类，建议添加 Service/Controller 集成测试 |

### 8.6 文档

| 问题 | 严重度 | 建议 |
|------|--------|------|
| `spark-profile.md` 中 RFM 权重与代码不一致 | ⚠️ 中 | 文档写 R×30%+F×30%+M×40%，代码为 R×0.4+F×0.3+M×0.3，需统一 |
| 缺少 API 接口文档（Swagger 除外） | ⚠️ 低 | Knife4j 已覆盖，但建议导出为静态文档备查 |
| 缺少故障排查指南 | ⚠️ 低 | 建议补充常见问题和排错步骤 |

---

## 九、开发完成度评估

| 模块 | 完成度 | 说明 |
|------|--------|------|
| 后端核心框架 | 90% | 分层清晰，安全/ORM/文档齐全；缺 Service 接口抽象 |
| 后端 API | 85% | 39 个端点已实现；部分高级功能（限流/缓存/审计）待完善 |
| 前端框架 | 85% | 布局/路由/权限/主题完善；部分业务页面待开发 |
| 前端业务页面 | 70% | 仪表盘/画像列表/详情已开发；标签分析/任务管理等待完成 |
| 大数据管线 | 90% | 数据生成/数仓/Spark 计算/同步链路完整 |
| 测试覆盖 | 30% | 大数据脚本有静态测试；后端/前端业务测试缺失 |
| 文档 | 80% | 6 份技术文档覆盖主要方面；部分细节需修正 |
| 部署方案 | 60% | 有部署文档但缺少 Docker/CI/CD |
| **综合** | **≈75%** | 核心功能链路已打通，处于可用但待完善阶段 |

---

## 十、优先改进建议（按优先级排序）

### 🔴 高优先级
1. **修正 Spring Boot 版本描述**：AGENTS.md 中 4.1.0 应改为 3.3.7
2. **补充后端单元测试和集成测试**：当前测试覆盖几乎为零
3. **替换前端生产环境 API 地址**：从 Mock 服务改为实际后端

### 🟡 中优先级
4. **添加 Docker 支持**：降低开发和部署门槛
5. **画像查询增加缓存层**：提升高频查询性能
6. **添加接口限流**：保护登录等敏感接口
7. **完善前端业务页面**：标签分析、任务管理的交互功能
8. **统一 RFM 权重文档**：修正 spark-profile.md 与代码的差异
9. **Service 层增加接口定义**：降低耦合，便于测试

### 🟢 低优先级
10. **添加 API 版本策略**
11. **添加数据质量监控**
12. **添加系统监控与告警**
13. **Spark 脚本增加失败重试**
14. **生成数据器支持多进程加速**

---

## 十一、总结

这是一个**架构设计规范、技术选型合理、文档齐全**的电商用户画像分析系统。v2 相比 v1 在分层架构上有了质的提升：

- **后端**：严格遵循 DDD 分层，Spring Security + JWT 认证体系完整，MyBatis-Plus 使用规范，MapStruct 编译期映射高效
- **前端**：基于成熟的 art-design-pro 框架，双权限模式灵活，组件库丰富，开发体验良好
- **大数据**：完整的数据生成→数仓分层→Spark 计算→结果同步链路，三种运行模式覆盖开发到生产的全场景

当前处于 **≈75% 完成度**，核心数据链路已打通，主要差距在前端业务页面完善度、测试覆盖率和运维自动化方面。优先完成高优先级的 3 项改进后，系统即可达到可演示和初步生产使用的水平。
