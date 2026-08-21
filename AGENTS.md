# Ecommerce User Profile

基于 Spring Boot + MyBatis-Plus + Spark 的电商平台用户画像分析系统。

## 项目简介

针对电商行业海量用户行为数据分散、分析效率低、运营不够精准的痛点，本系统基于主流大数据技术搭建用户画像分析平台。依托分布式技术完成海量数据的清洗、转换与统计，构建标准化用户标签库并完成用户分层，深度挖掘用户属性、消费偏好与行为规律。通过可视化图表展示分析结果，识别高价值用户，为电商精准营销、活动策划与精细化运营提供数据支撑。

**数据说明：** 通过 Python 脚本批量生成合规电商模拟数据，覆盖用户基本信息、浏览轨迹、消费订单、登录行为等业务数据。数据贴近真实电商场景，无隐私风险，数据规模可按需调整（支持百级到十万级），满足分布式数据处理、用户画像挖掘的开发与演示需求。

## Project

- 后端目录：`backend/`
- Java 版本：17；Spring Boot：3.3.7；构建工具：Maven
- 持久层：MyBatis-Plus 3.5+，Mapper XML 存放于 `resources/mapper/`
- 前端目录：`frontend/`（Vue 3 + TypeScript + Element Plus + Tailwind CSS，已完全剥离 art-design-pro 模板）
- 大数据脚本目录：`bigdata-scripts/`（Python 数据生成、PySpark 画像计算、聚类分析）
- 文档目录：`docs/`

## Commands

在 `backend/` 下执行：

- 构建：`./mvnw clean package`
- 测试：`./mvnw test`
- 启动：`./mvnw spring-boot:run`

在 `frontend/` 下执行：

- 安装依赖：`pnpm install`
- 启动开发：`pnpm dev`
- 生产构建：`pnpm build`

## Architecture

后端分层架构（单模块 + 包分层）：

- `common`：统一返回结构 `Result<T>`、`ResultCode` 错误码、`UserRole` 枚举、`BusinessException` 业务异常、`BaseEntity` 基础实体
- `domain`：MyBatis-Plus Entity 实体类、Mapper 接口、DTO、MapStruct Converter 接口
- `infrastructure`：Spring Security 配置、JWT 令牌管理、MyBatis-Plus 分页/自动填充插件、数据导入实现
- `application`：Service 业务逻辑层
- `interfaces`：Controller REST 接口层、Knife4j Swagger 文档

前端架构：

- 自定义组件体系（`components/ui/` 下的 SvgIcon、AuthLayout 等），无模板框架依赖
- 路由守卫 + JWT 认证
- ECharts 可视化图表（仪表盘、分层分布、标签分析）

## Conventions

- 系统仅包含 `User` 与 `Admin` 两种角色，权限边界必须明确。
- 所有 REST 接口统一使用 `Result<T>` 封装响应，路径前缀 `/api/v1`。
- Java 代码需提供清晰、详细的中文注释。
- Controller 只负责参数接收与响应，业务逻辑放入 Service。
- Mapper 接口继承 `BaseMapper<T>`，复杂查询使用 XML 或 `@Select` 注解。
- 分页查询统一使用 MyBatis-Plus `Page<T>` + `LambdaQueryWrapper`。
- Entity ↔ DTO 转换统一使用 MapStruct Converter 接口。
- 不在源码或文档中提交密码、密钥及其他敏感配置。

## Notes

- v1 源码见 `../v1_archive/`（已归档），当前保持 API 接口契约不变。
- 数据库：MySQL，22 张表，初始化脚本 `docs/sql/mysql-schema.sql`。
- 运行模式：本地 PySpark 模式（数据生成 → 画像计算 → 聚类分析全链路），支持 CSV/天池数据导入。
