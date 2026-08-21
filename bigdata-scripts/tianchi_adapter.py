"""天池数据集适配脚本（支持两种数据集格式，自动检测）。

格式 1：经典「淘宝用户行为数据集」UserBehavior
  user_id,item_id,item_category,behavior_type,timestamp
  behavior_type ∈ {pv, buy, cart, fav} → View/Purchase/Cart/Favorite
  输出：用户数据.csv + 商品数据.csv + 互动数据.csv

格式 2：电商订单/发票数据集（如天池电商发票样本）
  invoice_no,customer_id,gender,age,category,quantity,price,payment_method,invoice_date
  → 用户档案（gender/age 为真实值）、商品（按 category 建档）、
    交易数据（订单按 invoice_no 汇总，金额=Σ 单价×数量）
  输出：用户数据.csv + 商品数据.csv + 交易数据.csv

用法示例：
  python tianchi_adapter.py user_behavior.csv -o ./out --limit 20000
"""

from __future__ import annotations

import argparse
import csv
import random
import re
from datetime import datetime, timezone, timedelta
from pathlib import Path

# ─── 格式 1：天池行为类型 → 本系统浏览行为类型 ───
BEHAVIOR_MAP = {"pv": "View", "buy": "Purchase", "cart": "Cart", "fav": "Favorite"}
VALID_BEHAVIORS = set(BEHAVIOR_MAP.keys())

# ─── 用户档案模拟（数据集无档案字段时按真实分布生成） ───
GENDERS = ["Male", "Female"]
PROVINCES = [
    ("广东省", "广州市"), ("广东省", "深圳市"), ("浙江省", "杭州市"), ("浙江省", "宁波市"),
    ("江苏省", "南京市"), ("江苏省", "苏州市"), ("上海市", "上海市"), ("北京市", "北京市"),
    ("四川省", "成都市"), ("湖北省", "武汉市"), ("福建省", "厦门市"), ("山东省", "青岛市"),
]
CHANNELS = ["APP", "APP", "APP", "Web", "Web", "Miniprogram"]
LEVELS = ["Normal", "Normal", "Normal", "Silver", "Gold"]

DATE_FORMAT = "%Y-%m-%d %H:%M:%S"


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="天池数据集 → 本系统导入模板（自动检测格式）")
    p.add_argument("input", help="数据集 CSV 文件路径")
    p.add_argument("-o", "--output", default=".", help="输出目录（默认当前目录）")
    p.add_argument("--limit", type=int, default=20000,
                   help="数据行数上限（默认 20000；0 = 全部）")
    p.add_argument("--has-header", action="store_true", help="输入文件带表头（默认自动检测）")
    p.add_argument("--seed", type=int, default=42, help="随机种子（保证可复现）")
    return p.parse_args()


def read_file(path: Path):
    """读取 CSV（自动处理 UTF-8 / GBK），返回 (header, rows)。header 可能为 None（无表头）。"""
    raw = None
    for enc in ("utf-8", "gbk"):
        try:
            raw = path.read_bytes().decode(enc)
            break
        except UnicodeDecodeError:
            continue
    if raw is None:
        raise SystemExit(f"无法识别文件编码: {path}")
    lines = [ln.strip() for ln in raw.splitlines() if ln.strip()]
    if not lines:
        raise SystemExit("文件为空")

    first = lines[0].lower()
    # 表头自动检测：首行包含数据集关键字
    has_header = any(k in first for k in ("user_id", "item_id", "behavior_type", "invoice", "customer_id"))
    header = None
    start = 0
    if has_header:
        header = [c.strip() for c in lines[0].split(",")]
        start = 1
    rows = []
    for line in lines[start:]:
        parts = [c.strip() for c in line.split(",")]
        if len(parts) >= 5:
            rows.append(parts)
    return header, rows


def detect_format(header, rows):
    """格式检测：返回 'user_behavior' / 'invoice' / None"""
    if header:
        joined = ",".join(h.lower() for h in header)
        if "behavior_type" in joined:
            return "user_behavior"
        if "invoice" in joined or "customer_id" in joined or "invoice_date" in joined:
            return "invoice"
    # 无表头：按第 4 列取值猜测（行为值集合 vs 性别值集合）
    if rows:
        sample = {r[3].strip().lower() for r in rows[:50]}
        if sample <= VALID_BEHAVIORS | {"unknown"} and sample:
            return "user_behavior"
    return None


def parse_datetime(value: str, fallback: datetime) -> datetime:
    """兼容解析：Unix 秒/毫秒时间戳、yyyy/MM/dd、yyyy-MM-dd、yyyy-MM-dd HH:mm:ss 等"""
    v = value.strip()
    if not v:
        return fallback
    # 纯数字 → Unix 时间戳
    if v.isdigit():
        num = int(v)
        if num > 10 ** 12:
            num //= 1000
        return datetime.fromtimestamp(num, tz=timezone.utc).astimezone(timezone(timedelta(hours=8))).replace(tzinfo=None)
    for fmt in ("%Y/%m/%d %H:%M:%S", "%Y/%m/%d", "%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M", "%Y-%m-%d", "%Y%m%d"):
        try:
            return datetime.strptime(v, fmt)
        except ValueError:
            continue
    return fallback


def extract_digits(value: str) -> str | None:
    """提取字符串中的数字部分（C241288 → 241288）；无数字返回 None"""
    m = re.search(r"\d+", value)
    return m.group(0) if m else None


def write_users(path: Path, users: dict, rng: random.Random, gender_of: dict | None = None,
               age_of: dict | None = None):
    """输出 用户数据.csv：主键=用户ID（原样直入，保证外键可关联）"""
    with open(path, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["主键", "用户编码", "性别", "年龄", "省份", "城市", "注册渠道", "会员等级", "注册时间", "状态"])
        for uid, first_dt in sorted(users.items()):
            gender = gender_of.get(uid) if gender_of else None
            age = age_of.get(uid) if age_of else None
            if gender is None:
                gender = "Unknown" if rng.random() < 0.02 else rng.choice(GENDERS)
            if age is None:
                age = rng.randint(18, 55)
            province, city = rng.choice(PROVINCES)
            register = first_dt - timedelta(days=rng.randint(30, 800))
            w.writerow([uid, uid, gender, age, province, city,
                        rng.choice(CHANNELS), rng.choice(LEVELS),
                        register.strftime(DATE_FORMAT), "1"])


def convert_user_behavior(rows, limit: int, out_dir: Path, rng: random.Random):
    """格式 1：UserBehavior → 用户/商品/互动"""
    if limit and limit > 0 and len(rows) > limit:
        rows = rng.sample(rows, limit)
    behaviors, users, items = [], {}, {}
    unknown = 0
    for r in rows:
        try:
            bt = r[3].strip().lower()
            if bt not in VALID_BEHAVIORS:
                unknown += 1
                continue
            uid = r[0].strip()
            iid = r[1].strip()
            cat = r[2].strip()
            dt = parse_datetime(r[4], datetime(2017, 12, 1, 12, 0, 0))
            behaviors.append((uid, iid, cat, bt, dt))
            users.setdefault(uid, dt)
            items.setdefault(iid, cat)
        except IndexError:
            continue
    if unknown:
        print(f"      忽略未知行为类型 {unknown} 行")

    write_users(out_dir / "用户数据.csv", users, rng)

    with open(out_dir / "商品数据.csv", "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["主键", "分类名称", "父分类名称", "商品编码", "商品名称", "品牌", "单价", "状态"])
        for cat in sorted(set(items.values())):
            w.writerow(["", cat, "", "", "", "", "", "1"])
        for iid, cat in sorted(items.items()):
            price = rng.randint(10, 300) * 10 - 9
            w.writerow(["", cat, "", iid, f"商品{iid}", f"品牌{cat}", str(price), "1"])

    with open(out_dir / "互动数据.csv", "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["主键", "用户ID", "行为类型", "商品编码", "会话ID", "设备类型", "访问渠道",
                    "登录渠道", "登录时间", "登出时间", "登录时长(秒)", "行为时间"])
        for uid, iid, cat, bt, dt in behaviors:
            session = f"{uid}-{dt.strftime('%Y%m%d')}"
            w.writerow(["", uid, BEHAVIOR_MAP[bt], iid, session,
                        rng.choice(["Phone", "Phone", "PC", "Tablet"]),
                        rng.choice(["APP", "APP", "Web"]),
                        "", "", "", "", dt.strftime(DATE_FORMAT)])
    print(f"      有效行为 {len(behaviors)} 行 | 用户 {len(users)} | 商品 {len(items)}")


def convert_invoice(rows, limit: int, out_dir: Path, rng: random.Random):
    """格式 2：订单发票 → 用户（真实性别年龄）/商品（按类目）/交易（订单+明细）"""
    # 英文类目 → 中文（发票数据常见类目；未覆盖的保留原文）
    # 注意：类目名必须与 product_category 表一致（服装类统一为'服装鞋靴'，避免导入匹配失败兜底产生同义分裂）
    CATEGORY_CN = {
        "Clothing": "服装鞋靴", "Shoes": "服装鞋靴", "Books": "图书文娱",
        "Cosmetics": "美妆个护", "Food & Beverage": "食品饮料", "Food&Beverage": "食品饮料",
        "Souvenir": "文创礼品", "Technology": "数码家电", "Electronics": "数码家电",
        "Sports": "运动户外", "Toys": "玩具乐器", "Household": "家居生活",
    }

    def cn(cat: str) -> str:
        return CATEGORY_CN.get(cat, cat)
    if limit and limit > 0 and len(rows) > limit:
        rows = rng.sample(rows, limit)
    users = {}            # 数字用户ID -> 最早时间
    user_gender = {}      # 数字用户ID -> 性别（真实值）
    user_age = {}         # 数字用户ID -> 年龄（真实值）
    items = {}            # category -> True
    cat_prices = {}       # category -> [(price, qty)] 用于加权平均单价
    details = []          # (order_no, uid, cat, quantity, price, datetime, payment)
    invalid_ids = 0
    for r in rows:
        try:
            invoice_no, customer_id, gender, age, category, quantity, price, payment, invoice_date = r[:9]
            uid = extract_digits(customer_id)
            if not uid:
                invalid_ids += 1
                continue
            qty = int(float(quantity))
            unit = float(price)
            if qty <= 0 or unit < 0:
                continue
            dt = parse_datetime(invoice_date, datetime(2021, 1, 1, 0, 0, 0))
            users.setdefault(uid, dt)
            user_gender[uid] = gender if gender in ("Male", "Female") else "Unknown"
            user_age[uid] = int(float(age)) if str(age).isdigit() else None
            items[category] = True
            cat_prices.setdefault(category, []).append((unit, qty))
            details.append((invoice_no, uid, category, qty, unit, dt, payment))
        except (IndexError, ValueError):
            continue
    if invalid_ids:
        print(f"      忽略无法识别客户编号的行 {invalid_ids} 行")

    write_users(out_dir / "用户数据.csv", users, rng, user_gender, user_age)

    # 类目加权平均单价（商品档案价）
    def avg_price(cat: str) -> float:
        pairs = cat_prices[cat]
        total_qty = sum(q for _, q in pairs)
        return round(sum(p * q for p, q in pairs) / total_qty, 2) if total_qty else 100.0

    with open(out_dir / "商品数据.csv", "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["主键", "分类名称", "父分类名称", "商品编码", "商品名称", "品牌", "单价", "状态"])
        for cat in sorted(items):
            name = cn(cat)
            w.writerow(["", name, "", "", "", "", "", "1"])          # 分类行
            # 商品编码保留英文原值（编码稳定、重导入可命中更新），名称/分类显示中文
            w.writerow(["", name, "", cat, f"商品-{name}", "", f"{avg_price(cat):.2f}", "1"])  # 商品行

    with open(out_dir / "交易数据.csv", "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["主键", "订单号", "用户ID", "商品编码", "商品名称", "品牌", "单价", "数量",
                    "分类名称", "订单状态", "支付方式", "下单时间", "支付时间", "完成时间"])
        for invoice_no, uid, cat, qty, unit, dt, payment in details:
            name = cn(cat)
            # 支付时间/完成时间 = 下单时间：发票数据支付即完成（画像清洗要求 paid_at 非空）
            # 商品编码保留英文原值（与商品行编码一致，重导入可命中更新）
            w.writerow(["", invoice_no, uid, cat, f"商品-{name}", "", f"{unit:.2f}", qty,
                        name, "Completed", payment, dt.strftime(DATE_FORMAT),
                        dt.strftime(DATE_FORMAT), dt.strftime(DATE_FORMAT)])
    print(f"      订单明细 {len(details)} 行 | 用户 {len(users)} | 商品 {len(items)}")


def main() -> None:
    args = parse_args()
    inp = Path(args.input)
    out_dir = Path(args.output)
    out_dir.mkdir(parents=True, exist_ok=True)
    rng = random.Random(args.seed)

    print(f"[1/4] 读取数据集: {inp}")
    header, rows = read_file(inp)
    print(f"      共 {len(rows)} 行（{'带表头: ' + ','.join(header[:5]) if header else '无表头'}）")

    fmt = detect_format(header, rows)
    if fmt is None:
        raise SystemExit(
            "无法识别数据集格式。支持：\n"
            "  ① 淘宝用户行为数据集（user_id,item_id,item_category,behavior_type,timestamp）\n"
            "  ② 电商订单/发票数据集（invoice_no,customer_id,gender,age,category,quantity,price,payment_method,invoice_date）")
    print(f"[2/4] 识别格式: {'淘宝用户行为数据集' if fmt == 'user_behavior' else '电商订单/发票数据集'}")

    if fmt == "user_behavior":
        convert_user_behavior(rows, args.limit, out_dir, rng)
    else:
        convert_invoice(rows, args.limit, out_dir, rng)

    print(f"\n完成！输出目录: {out_dir.resolve()}")
    print("导入步骤：数据导入页 → 天池数据集一键导入（或上传输出目录的全部 CSV）")


if __name__ == "__main__":
    main()
