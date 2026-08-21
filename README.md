# 电商用户画像分析系统

基于 **Spring Boot + MyBatis-Plus + Apache Spark** 的电商平台用户画像分析系统。

针对电商行业海量用户行为数据分散、分析效率低、运营不够精准的痛点，本项目基于主流大数据技术搭建用户画像分析平台：依托分布式技术完成海量数据的清洗、转换与统计，构建标准化用户标签库并完成用户分层，深度挖掘用户属性、消费偏好与行为规律；通过可视化图表展示分析结果，识别高价值用户，为电商精准营销、活动策划与精细化运营提供数据支撑。

> 数据说明：通过 Python 脚本批量生成合规的电商模拟数据，覆盖用户基本信息、浏览轨迹、消费订单、登录行为等。数据贴近真实电商场景、无隐私风险，规模可按需调整（百级到十万级），满足分布式数据处理与用户画像挖掘的演示需求。

---

## ✨ 主要功能

- **管理端（Admin）**：数据生成、多格式数据导入、画像分析任务、商品分析、复购与留存、流失预警、用户聚类、标签定义、人群包管理、系统用户管理
- **用户门户（User）**：画像总览、个人画像详情、标签分析、AI 智能分析对话框、通知中心
- **可视化**：仪表盘、RFM 分层分布、标签分析（ECharts）

## 🏗️ 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.3.7、MyBatis-Plus 3.5+、MapStruct、Spring Security + JWT、Knife4j |
| 前端 | Vue 3、TypeScript、Element Plus、Tailwind CSS、ECharts、Pinia、Vite |
| 大数据 | Python 3.10+、PySpark（本地模式）、MySQL 8.0 |
| 文档 | Knife4j Swagger、Markdown |

## 📁 目录结构

```
ecommerce-user-profile/
├── backend/            # 后端（Spring Boot，分层架构 common/domain/infrastructure/application/interfaces）
├── frontend/           # 前端（Vue 3 + TypeScript）
├── bigdata-scripts/    # Python 数据生成、PySpark 画像计算、天池数据适配
├── design-system/      # 设计系统文档
└── docs/               # 文档（SQL 脚本、部署说明、数据字典、架构图等）
```

---

## 🚀 从 GitHub 下载源码到本地运行

以下步骤面向「下载源码 zip 包 → 在本地跑起来」的新手。

### 0. 环境准备（前置依赖）

- **JDK 17**（后端运行）
- **Node.js ≥ 20** 与 **pnpm**（前端构建）
- **Python ≥ 3.10**（数据生成与画像计算）
- **MySQL ≥ 8.0**（数据库，默认端口 `3306`）
- **Apache Spark**（选装，仅画像计算环节需要；纯前端+后端演示可跳过，见第 6 步说明）

> 检查命令：`java -version`、`node -v`、`pnpm --version`、`python --version`。
> pnpm 若未安装：`npm install -g pnpm`。

### 1. 解压并进入项目

```bash
cd ecommerce-user-profile
```

### 2. 初始化数据库

1. 在 MySQL 中执行建库建表脚本（会自动创建 `ecommerce_user_profile` 库及全部表、种子标签数据）：

   ```sql
   SOURCE docs/sql/mysql-schema.sql;
   ```
   或在 Navicat / MySQL Workbench 中**直接运行**该脚本文件。

2. 确认账号可登录，记下 `DB_USERNAME`（默认 `root`）和密码。

### 3. 配置后端环境变量并启动

后端启动需要以下环境变量（**必填**：`DB_PASSWORD`、`JWT_SECRET`，不提供则启动失败）：

**Windows（PowerShell / CMD 临时设置）**

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/ecommerce_user_profile?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的MySQL密码"
$env:JWT_SECRET="一段至少32字节的随机字符串，例如一串足够长的英文密钥"
$env:AI_API_KEY=""     # 可选；DeepSeek API Key，留空则 AI 自动降级为本地模拟
```

（CMD 版本把 `$env:X="y"` 换成 `set X=y`。）

启动后端：

```bash
cd backend
# Windows
mvnw.cmd spring-boot:run
# macOS / Linux
./mvnw spring-boot:run
```

也可以用 IDE（IDEA 等）运行启动类 `com.oufeng.ecommerceuserprofile.EcommerceUserProfileApplication`。

后端就绪后接口地址：`http://localhost:8080`，Swagger 文档：`http://localhost:8080/swagger-ui.html`。

### 4. 准备前端环境变量

前端依赖 `.env`、`.env.development`、`.env.production` 三个文件（不入库）。下载的 zip 包不包含它们，需用模板生成：

```bash
cd frontend
cp .env.example .env
cp .env.example .env.development
cp .env.example .env.production
```

> 默认开发端口 `3006`、API 代理指向 `http://localhost:8080`，一般无需再改。若你的后端端口/数据库不同，按需调整对应变量。

### 5. 安装依赖并启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

前端开发地址：`http://localhost:3006`。Vite 已将 `/api` 自动代理到后端 `8080`。

---

### 6. 登录账号：普通用户 vs 管理员

系统没有预置默认账号。**首次运行通过注册页创建一个账号**，注册得到的均为**普通用户（User）**，可使用用户门户（画像总览、个人画像、标签分析、AI 对话等）。

若要使用**管理端**的全部能力（数据生成、数据导入、画像任务、商品/复购/流失/聚类分析、标签与人群包管理等），需把该账号提升为管理员——在数据库中执行一行 SQL：

```sql
UPDATE sys_user SET role = 'Admin' WHERE username = '你的用户名';
```

> 后端对 `/api/v1/admin/**` 有 `ROLE_ADMIN` 强校验，只有 Admin 才能访问管理端功能。

### 7. 数据生成与画像计算（可选，需要 PySpark）

完整数据链路为：**生成 CSV → 导入 MySQL → PySpark 画像计算 → 写回 MySQL**。

**推荐方式（页面化全链路）——管理端一键完成**

1. 登录管理端，进入「数据生成」页面，选择/填写规模（如万级用户），点击「生成」。系统会调用数据生成器产出模拟 CSV，并**自动通过 CSV 导入器写入 MySQL**。
2. 进入「任务管理」页面，创建画像任务（选择数据版本、聚类数等），系统会调用本地 PySpark 管线（`run_local_pipeline.py`）**从 MySQL 直读原始表**计算，并把画像结果写回 MySQL。

也可以在「数据导入」页面一次上传 7 张表的 CSV 模板（模板可在页面下载）完成入库，再执行画像任务。

**纯命令行（进阶）：生成数据 + 入库 + 画像**

命令行生成器只产出 CSV，不会自动入库；而画像管线是从 MySQL 直读数据，**必须先完成导入**。

```bash
# 步骤 1：生成模拟数据（生成 7 张表的 CSV 到 generated-data/sample）
python bigdata-scripts/generate_data.py \
  --output generated-data/sample \
  --users 1000 --products 200 --behaviors 10000 --orders 2000 \
  --seed 2026 --reference-time "2026-01-01 00:00:00"

# 步骤 2：把上述 CSV 导入 MySQL（需通过管理端「数据导入」页面上传，
#         项目未提供独立的命令行入库脚本）

# 步骤 3：本地 PySpark 画像计算（从 MySQL 直读并写回）
cd backend
python ../bigdata-scripts/spark/run_local_pipeline.py \
  --data-version 20260101000000 \
  --jdbc-url "jdbc:mysql://localhost:3306/ecommerce_user_profile?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai" \
  --mysql-user root
```

> PySpark 需自行安装（`pip install pyspark`），并确保 `JAVA_HOME` 指向 JDK 17。若不安装 PySpark，纯前端+后端演示（注册登录、用户门户、静态画像/标签查看）不受影响，仅画像计算环节不可用。

---

## ⚙️ 常用命令速查

```bash
# 后端构建与测试
cd backend && mvnw clean package          # 或 ./mvnw clean package
cd backend && mvnw test

# 前端生产构建
cd frontend && pnpm build                # 产物在 frontend/dist/

# Python 数据生成测试
python -m unittest bigdata-scripts/test_generate_data.py -v
```

## 📚 文档

运行与部署：见 `docs/deployment.md`。另有数据字典（`docs/data-dictionary.md`）、数据库模型（`docs/database-model.md`）、架构图（`docs/architecture-diagram.html`）、需求规格（`docs/requirements-specification.md`）等，均在 `docs/` 目录下。

## 🔒 安全说明

- 密钥与数据库密码一律通过环境变量注入，仓库不提交任何敏感信息（`.env`、`application-local*.properties` 均被忽略）。
- 正式环境请为 MySQL 使用最小权限账号，不要使用 `root`。

## 📄 许可证

暂无显式开源许可证，仅供学习研究与演示使用。
