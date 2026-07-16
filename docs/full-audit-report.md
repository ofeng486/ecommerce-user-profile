# 电商用户画像系统 v2 — 全链路端到端审查报告

> 审查日期：2026-07-13 23:30

---

## 一、完整数据链路图

```
                          ┌─────────────────────────────────────────┐
                          │            用户操作流程                    │
                          └─────────────────────────────────────────┘

  第1步                     第2步                        第3步                    第4步
  ┌──────┐                ┌──────────┐                ┌──────────┐             ┌──────────┐
  │ CSV  │   前端上传     │ Spring   │   JDBC 批量    │ MySQL    │   Spark    │ MySQL    │   前端查询   │ 仪表盘   │
  │ 数据 │ ──→ 后端导入 → │ Boot     │ ──→ 写入 ──→  │ 7张业务表│ ──→ 计算 →  │ 4张画像表│ ──→ API ──→ │ 展示    │
  │ 文件 │                │ Importer │                │          │            │          │             │          │
  └──────┘                └──────────┘                └──────────┘            └──────────┘             └──────────┘
                                                          ↑                        │
                                                          │                        │
                                                    导入后概览数字              导入+计算后
                                                    只 totalUsers 变化          全部数字变化
```

### 链路中的 7 张业务表（CSV 导入）

| CSV 文件 | 目标表 | 外键依赖 |
|----------|--------|----------|
| product_category.csv | product_category | 无 |
| product.csv | product | → product_category |
| ecommerce_user.csv | ecommerce_user | 无 |
| user_browse_behavior.csv | user_browse_behavior | → ecommerce_user, product |
| user_login_behavior.csv | user_login_behavior | → ecommerce_user |
| sales_order.csv | sales_order | → ecommerce_user |
| sales_order_item.csv | sales_order_item | → sales_order, product |

### 链路中的 4 张画像结果表（Spark 生成）

| 画像表 | 内容 | 概览页用到？ |
|--------|------|-------------|
| user_profile_summary | 用户消费/行为/登录汇总指标 | ✅ totalUsers, profiledUsers, totalPaymentAmount |
| user_segment | RFM 用户价值分层 | ✅ highValueUsers |
| user_profile_tag | 用户标签结果 | ✅ 标签分布图 |
| ads_user_rfm | RFM 8 分类 | ❌ 前端未使用 |

---

## 二、发现的问题清单（按严重度排序）

### 🔴 P0 — 系统无法工作

| # | 问题 | 文件 | 现状 | 影响 |
|---|------|------|------|------|
| 1 | Spark 脚本路径指向 v1 | AnalysisTaskService.java:55 | `E:/ecommerce-user-profile/bigdata-scripts/...` | v2 路径应该是 `E:/ecommerce-user-profile/v2/bigdata-scripts/...` |
| 2 | 导入数据目录路径错误 | AnalysisTaskService.java:57 | `E:/ecommerce-user-profile-v2/...` | 应该是 `E:/ecommerce-user-profile/v2/...` |
| 3 | Spark 未传 --jdbc-url | AnalysisTaskService.java:164-165 | 只传 --input 和 --mysql-user | Python 默认用 3306 端口，但用户 MySQL 可能不是 3306 |
| 4 | MySQL 密码默认为空 | application.properties:16 | `${DB_PASSWORD:}` | Spark 需要 MYSQL_PASSWORD 环境变量 |
| 5 | 缺少 spark 配置段 | application.properties | spark.pipeline.* 只在 @Value 注解里 | 用户不知道要配什么 |

### 🟡 P1 — 功能不完整

| # | 问题 | 影响 |
|---|------|------|
| 6 | 概览缓存导致数据不刷新 | 已修复：导入/Spark 完成后 evictProfileCache() |
| 7 | 前端轮询定时器未在组件卸载时清理 | 内存泄漏，可能 30s 内持续发无效请求 |
| 8 | preview 返回的 sampleRows 前端未展示 | 用户无法预览样本数据 |
| 9 | 导入进度无实时反馈 | 用户只看到 Running，不知道进度 |
| 10 | 轮询只持续 30s | 大文件导入超过 30s 后状态不更新 |

### 🟢 P2 — 可优化

| # | 问题 | 影响 |
|---|------|------|
| 11 | admin.ts 缺少导入 API 封装 | API 调用散落在视图组件中 |
| 12 | dashboard 的 catch 块为空 | 图表静默不渲染，用户不知道哪里出错 |
| 13 | fetchAdminTasks 和导入任务列表是不同端点 | 可能混淆使用 |

---

## 三、修复方案

### 3.1 修复 Spark 管线路径配置（P0 #1 #2 #3 #4 #5）

在 `application.properties` 中添加 Spark 管线配置段，修正所有路径。

### 3.2 修复数据校验不匹配（P0 — 新发现）

`EcommerceUserImporter` 的 `validateRow` 只接受中文性别值（男/女/未知），
但测试数据用的是英文（Male/Female/Unknown），导致**所有用户数据校验失败被跳过**。
已修复为同时兼容中英文。

### 3.3 修复缓存不刷新（P1 #6）

在 `AnalysisTaskService` 中添加 `evictProfileCache()` 方法，
在数据导入和 Spark 任务完成后清除画像查询缓存。

### 3.4 修复前端轮询泄漏（P1 #7 #10）

- 添加 `onUnmounted` 钩子清理定时器
- 轮询时间从 30s 延长到 5min
- 添加 `checkAndStopPolling()` 在所有任务完成后自动停止

### 3.5 增强前端预览（P1 #8）

在导入页面展示后端返回的 `sampleRows`（最多 5 行样本数据）。

### 3.6 添加操作指引（P1 #9）

在导入页面顶部添加 4 步操作指引，帮助用户理解完整流程。

---

## 四、完整的系统使用流程

### 第1步：准备数据库
```sql
CREATE DATABASE ecommerce_user_profile CHARACTER SET utf8mb4;
```
执行 `docs/sql/mysql-schema.sql` 建表。

### 第2步：配置后端
在 IDEA 运行配置中设置环境变量：
- `DB_PASSWORD` = 你的 MySQL 密码
- `DB_URL` = `jdbc:mysql://localhost:3306/ecommerce_user_profile`（如端口不是 3306 需修改）

### 第3步：启动后端
IDEA 运行 `EcommerceUserProfileV2Application.java`，后端在 `localhost:8080`。

### 第4步：启动前端
```bash
cd v2/frontend
pnpm install
pnpm dev
```
前端在 `localhost:3006`。

### 第5步：注册/登录
- 访问 `localhost:3006`，注册一个账号（默认 User 角色）
- 或者用 MySQL 直接插入一个 Admin 账号：
  ```sql
  INSERT INTO sys_user (username, password_hash, display_name, role, status)
  VALUES ('admin', '$2a$10$...BCrypt hash...', '管理员', 'Admin', 1);
  ```

### 第6步：导入 CSV 数据
- 进入「数据导入」页面
- 选择 `v2/bigdata-scripts/test-output/` 目录下的全部 7 个 CSV 文件
- 输入任务名称，点击「开始导入」
- 等待导入完成（状态变为 Succeeded）

### 第7步：运行 Spark 画像任务
- 进入「分析任务」页面
- 创建任务，类型选 `PROFILE_FULL`
- 任务会异步调用 PySpark 计算画像
- **前提**：系统需要安装 Python + PySpark + Java 17

### 第8步：查看仪表盘
- 导入数据后：总用户数会更新
- Spark 任务完成后：已画像用户数、高价值用户数、累计消费金额都会更新
- 分层分布饼图和标签分布柱状图也会显示数据

---

## 五、当前修复状态

| 问题 | 状态 | 修复文件 |
|------|------|---------|
| Spark 脚本路径指向 v1 | ✅ 已修复 | application.properties, AnalysisTaskService.java |
| 导入数据目录路径错误 | ✅ 已修复 | application.properties, AnalysisTaskService.java |
| Spark 未传 --jdbc-url | ✅ 已修复 | AnalysisTaskService.java |
| 性别校验不兼容英文值 | ✅ 已修复 | EcommerceUserImporter.java |
| 缓存导致概览不刷新 | ✅ 已修复 | AnalysisTaskService.java |
| 前端轮询定时器泄漏 | ✅ 已修复 | import/index.vue |
| 前端轮询时间太短 | ✅ 已修复 | import/index.vue |
| 前端未展示样本行 | ✅ 已修复 | import/index.vue |
| 缺少操作指引 | ✅ 已修复 | import/index.vue |
| Spark stderr 丢失 | ✅ 已修复 | AnalysisTaskService.java (redirectErrorStream) |
