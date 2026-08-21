import os
import pymysql

# 密码从环境变量读取，勿硬编码提交
conn = pymysql.connect(host='localhost', port=3306, user='root',
                       password=os.environ.get('DB_PASSWORD', ''),
                       database='ecommerce_user_profile', charset='utf8mb4')
cur = conn.cursor()

cur.execute('SELECT COUNT(*) FROM product')
print('总商品数：', cur.fetchone()[0])
print()

print('=== 1. 商品名模式分布 ===')
cur.execute("""
SELECT 
  CASE
    WHEN product_name LIKE '商品-美妆个护模拟商%' THEN 'A: 商品-美妆个护模拟商...(天池发票-长名)'
    WHEN product_name LIKE '商品-食品饮料模拟商%' THEN 'B: 商品-食品饮料模拟商...(天池发票-长名)'
    WHEN product_name LIKE '商品-数码家电%' THEN 'C: 商品-数码家电(天池)'
    WHEN product_name LIKE '商品-服装鞋靴%' THEN 'D: 商品-服装鞋靴(天池)'
    WHEN product_name LIKE '商品-家居生活%' THEN 'E: 商品-家居生活(天池)'
    WHEN product_name LIKE '商品-图书文娱%' THEN 'F: 商品-图书文娱(天池)'
    WHEN product_name LIKE '商品-文创礼品%' THEN 'G: 商品-文创礼品(天池)'
    WHEN product_name LIKE '商品-玩具乐器%' THEN 'H: 商品-玩具乐器(天池)'
    WHEN product_name LIKE '商品-服饰鞋包%' THEN 'I: 商品-服饰鞋包(天池)'
    WHEN product_name LIKE '商品-其他%' THEN 'J: 商品-其他(天池)'
    WHEN product_name LIKE '商品-Technology%' THEN 'K: 商品-Technology(英文)'
    WHEN product_name LIKE '商品-Shoes%' THEN 'L: 商品-Shoes(英文)'
    WHEN product_name LIKE '商品-Food %' THEN 'M: 商品-Food & B...(英文)'
    WHEN product_name LIKE '商品-Toys%' THEN 'N: 商品-Toys(英文)'
    WHEN product_name LIKE '商品-服装鞋帽%' THEN 'O: 商品-服装鞋帽(旧分类)'
    WHEN product_name LIKE '商品-电子产品%' THEN 'P: 商品-电子产品(旧分类)'
    WHEN product_name LIKE '商品-%' THEN 'Z: 其他商品-前缀(待识别)'
    ELSE '0: 正常/其他'
  END AS pattern,
  COUNT(*) AS cnt
FROM product GROUP BY pattern ORDER BY cnt DESC
""")
total = 0
for r in cur.fetchall():
    print(f'  {r[0]:50s} {r[1]:>6d}')
    total += r[1]
print(f'  {"合计":50s} {total:>6d}')
print()

print('=== 2. 涉及销量的脏商品（数量 + 销售额贡献） ===')
cur.execute("""
SELECT 
  COUNT(DISTINCT p.id) AS dirty_total,
  SUM(CASE WHEN oi.id IS NOT NULL THEN 1 ELSE 0 END) AS dirty_with_sales,
  COALESCE(SUM(oi.item_amount), 0) AS dirty_amount
FROM product p
LEFT JOIN sales_order_item oi ON oi.product_id=p.id
LEFT JOIN sales_order o ON o.id=oi.order_id AND o.order_status IN ('Paid','Shipped','Completed')
WHERE p.product_name LIKE '商品-%'
""")
r = cur.fetchone()
print(f'  脏名商品总数：{r[0]}')
print(f'  有销量的脏名商品数：{r[1]}')
print(f'  脏名商品贡献销售额：{r[2]:.2f}')
print()

print('=== 3. 全部脏商品（按销售额降序，含完整原名） ===')
cur.execute("""
SELECT p.id, p.product_name, p.product_code,
  COALESCE(SUM(oi.quantity),0) AS qty,
  COALESCE(SUM(oi.item_amount),0) AS amount,
  COUNT(DISTINCT oi.order_id) AS order_cnt
FROM product p
JOIN sales_order_item oi ON oi.product_id=p.id
JOIN sales_order o ON o.id=oi.order_id AND o.order_status IN ('Paid','Shipped','Completed')
WHERE p.product_name LIKE '商品-%'
GROUP BY p.id, p.product_name, p.product_code
ORDER BY amount DESC
""")
print(f'  {"id":>5s} {"qty":>8s} {"amount":>16s} {"orders":>6s}  name')
for r in cur.fetchall():
    print(f'  {r[0]}  {r[3]}  {r[4]}  {r[5]}  {r[1]}')
print()

print('=== 3b. 其他"商品-"但无销量商品 ===')
cur.execute("""
SELECT id, product_name, product_code FROM product
WHERE product_name LIKE '商品-%' AND id NOT IN (
  SELECT DISTINCT product_id FROM sales_order_item
)
""")
for r in cur.fetchall():
    print(f'  {r[0]:>5d}  {r[1]}  ({r[2]})')
print()

print('=== 4. 正常的非商品-前缀商品样本（前 10） ===')
cur.execute("SELECT id, product_name FROM product WHERE product_name NOT LIKE '商品-%' LIMIT 10")
for r in cur.fetchall():
    print(f'  {r[0]:>4d}  {r[1]}')
print()

print('=== 5. 完整的"商品-X%" 前缀值清单（Z 未识别桶的具体内容） ===')
cur.execute("""
SELECT 
  SUBSTRING_INDEX(product_name, '-', 2) AS prefix,
  COUNT(*) AS cnt
FROM product
WHERE product_name LIKE '商品-%'
GROUP BY prefix
ORDER BY cnt DESC
""")
for r in cur.fetchall():
    print(f'  {r[0]:50s} {r[1]:>6d}')
print()

print('=== 6. 当前类目表 ===')
cur.execute('SELECT id, category_name FROM product_category ORDER BY id')
for r in cur.fetchall():
    print(f'  {r[0]:>3d}  {r[1]}')

conn.close()
