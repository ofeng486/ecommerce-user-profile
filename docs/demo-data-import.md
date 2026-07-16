# Java 自动导入 MySQL 演示数据

该功能通过 Spring Boot 的 `demo-import` Profile 将 `generated-data/demo` 中的 CSV 批量导入 MySQL，并生成本地演示画像结果。

## IntelliJ IDEA 运行配置

复制现有 `EcommerceUserProfileBackendApplication` 运行配置，命名为：

```text
ImportDemoData
```

保留原有数据库环境变量，并增加：

```text
SPRING_PROFILES_ACTIVE=demo-import
DEMO_IMPORT_DIRECTORY=E:/ecommerce-user-profile/v2/bigdata-scripts/test-output
```

完整变量示例：

```text
DB_URL=jdbc:mysql://localhost:23307/ecommerce_user_profile?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=<Navicat使用的MySQL密码>
JWT_SECRET=<至少32字节随机密钥>
JPA_DDL_AUTO=validate
SPRING_PROFILES_ACTIVE=demo-import
DEMO_IMPORT_DIRECTORY=E:/ecommerce-user-profile/v2/bigdata-scripts/test-output
```

在 IntelliJ 中运行 `ImportDemoData`。控制台出现以下内容表示导入成功：

```text
演示业务数据和画像结果已成功导入 MySQL
```

导入完成后立即停止该运行配置，删除或禁用：

```text
SPRING_PROFILES_ACTIVE=demo-import
```

再使用普通 `EcommerceUserProfileBackendApplication` 配置启动后端，避免每次启动重复清空并导入演示数据。

## 导入内容

- 5 个商品分类；
- 200 个商品；
- 1000 个电商用户；
- 10000 条浏览行为；
- 1000 条登录行为；
- 2000 个订单；
- 5019 条订单明细；
- 1000 条用户画像汇总；
- 1000 条 RFM 用户分层；
- 约 4000 条用户标签。

`sys_user`、`sys_login_log` 和管理员账号不会被清理。

## Navicat 验证 SQL

```sql
SELECT 'ecommerce_user' table_name, COUNT(*) row_count FROM ecommerce_user
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'user_browse_behavior', COUNT(*) FROM user_browse_behavior
UNION ALL SELECT 'user_login_behavior', COUNT(*) FROM user_login_behavior
UNION ALL SELECT 'sales_order', COUNT(*) FROM sales_order
UNION ALL SELECT 'sales_order_item', COUNT(*) FROM sales_order_item
UNION ALL SELECT 'user_profile_summary', COUNT(*) FROM user_profile_summary
UNION ALL SELECT 'user_segment', COUNT(*) FROM user_segment
UNION ALL SELECT 'user_profile_tag', COUNT(*) FROM user_profile_tag;
```
