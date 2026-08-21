import os
import pymysql

# 密码从环境变量读取，勿硬编码提交
conn = pymysql.connect(host='localhost', port=3306, user='root',
                       password=os.environ.get('DB_PASSWORD', ''),
                       database='ecommerce_user_profile', charset='utf8mb4')
cur = conn.cursor()

# 改名映射：id -> 新名（按 category_id 中文类目名 + 模拟商品编号 901-908，避开现有 100+ 编号）
RENAME = {
    291: '服装鞋靴模拟商品901',   # Clothing / cat 22
    296: '数码家电模拟商品901',   # Technology / cat 20
    292: '美妆个护模拟商品901',   # Cosmetics / cat 10
    294: '服饰鞋包模拟商品901',   # Shoes / cat 7
    293: '食品饮料模拟商品901',   # Food & Beverage / cat 9
    297: '玩具乐器模拟商品901',   # Toys / cat 21
    290: '图书文娱模拟商品901',   # Books / cat 14
    295: '文创礼品模拟商品901',   # Souvenir / cat 19
}

print('=== 执行前：当前 8 条脏记录 ===')
cur.execute("SELECT id, product_name FROM product WHERE id IN %s ORDER BY id", (tuple(RENAME.keys()),))
for r in cur.fetchall():
    print(f'  {r[0]:>3d}  {r[1]}')

print()
print('=== 模拟预览：改名后 ===')
for pid in sorted(RENAME):
    print(f'  {pid:>3d}  {RENAME[pid]}')

print()
print('=== 检查新名是否与现有商品冲突 ===')
conflicts = []
for new_name in RENAME.values():
    cur.execute("SELECT id FROM product WHERE product_name = %s", (new_name,))
    row = cur.fetchone()
    if row:
        conflicts.append((new_name, row[0]))
if conflicts:
    print('  ⚠️ 冲突：', conflicts)
else:
    print('  ✅ 无冲突，可安全执行')

print()
print('=== 执行前先备份（product_dirty_backup_20260807） ===')
cur.execute("""
CREATE TABLE IF NOT EXISTS product_dirty_backup_20260807 AS
SELECT * FROM product WHERE id IN %s
""", (tuple(RENAME.keys()),))
cur.execute("SELECT COUNT(*) FROM product_dirty_backup_20260807")
print(f'  已备份 {cur.fetchone()[0]} 行到 product_dirty_backup_20260807')
conn.commit()

print()
print('=== 执行 UPDATE ===')
case_sql = ' '.join([f"WHEN {pid} THEN '{name}'" for pid, name in RENAME.items()])
sql = f"UPDATE product SET product_name = CASE id {case_sql} END WHERE id IN ({','.join(map(str, RENAME.keys()))})"
cur.execute(sql)
conn.commit()
print(f'  已更新 {cur.rowcount} 行')

print()
print('=== 验证：8 条记录现状 ===')
cur.execute("SELECT id, product_name, category_id FROM product WHERE id IN %s ORDER BY id", (tuple(RENAME.keys()),))
for r in cur.fetchall():
    print(f'  {r[0]:>3d}  {r[1]}  (cat {r[2]})')

print()
print('=== 验证：销售 Top10 预览（改后显示效果） ===')
cur.execute("""
SELECT p.product_name, COALESCE(SUM(oi.item_amount),0) AS amount
FROM sales_order_item oi
JOIN sales_order o ON o.id = oi.order_id AND o.order_status IN ('Paid','Shipped','Completed')
JOIN product p ON p.id = oi.product_id
GROUP BY p.product_name
ORDER BY amount DESC
LIMIT 10
""")
for i, r in enumerate(cur.fetchall(), 1):
    print(f'  {i:>2d}. {r[0]:30s} ¥{r[1]:>12,.2f}')

conn.close()
print()
print('全部完成 ✅')
