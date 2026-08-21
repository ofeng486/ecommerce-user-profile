"""百万级电商模拟数据生成器。

使用 Python 标准库流式写入 CSV，避免将百万级行为和订单明细全部保存在内存中。
支持固定随机种子与统计截止时间，保证毕业设计实验可复现。
"""

from __future__ import annotations

import argparse
import csv
import random
from dataclasses import dataclass
from datetime import datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import Iterable, Iterator, Sequence

DATETIME_FORMAT = "%Y-%m-%d %H:%M:%S"
MONEY_UNIT = Decimal("0.01")

CATEGORIES = [
    (1, "数码家电"),
    (2, "服装鞋靴"),
    (3, "家居生活"),
    (4, "食品饮料"),
    (5, "美妆个护"),
]

PROVINCES = [
    ("广东省", "广州市"),
    ("浙江省", "杭州市"),
    ("江苏省", "南京市"),
    ("四川省", "成都市"),
    ("湖北省", "武汉市"),
]
DEVICES = ["PC", "Android", "iOS", "Tablet"]


@dataclass(frozen=True)
class GeneratorConfig:
    """生成器配置。"""

    output: Path
    users: int
    products: int
    behaviors: int
    orders: int
    seed: int
    reference_time: datetime

    @property
    def login_records(self) -> int:
        """登录记录规模与用户数、浏览行为数保持合理比例。"""
        return max(self.users, self.behaviors // 10)


@dataclass(frozen=True)
class ProductSnapshot:
    """订单和行为生成所需的商品快照。"""

    product_id: int
    category_id: int
    name: str
    unit_price: Decimal


def format_datetime(value: datetime | None) -> str:
    """将时间转换为数据库可导入的统一格式。"""
    return "" if value is None else value.strftime(DATETIME_FORMAT)


def money(value: Decimal) -> Decimal:
    """按人民币分进行四舍五入。"""
    return value.quantize(MONEY_UNIT, rounding=ROUND_HALF_UP)


def write_csv(path: Path, header: Sequence[str], rows: Iterable[Sequence[object]]) -> None:
    """以 UTF-8 流式写入 CSV。"""
    with path.open("w", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)
        writer.writerow(header)
        writer.writerows(rows)


def random_time(rng: random.Random, start: datetime, end: datetime) -> datetime:
    """在闭合时间范围内生成随机时间。"""
    if end < start:
        return start
    seconds = int((end - start).total_seconds())
    return start + timedelta(seconds=rng.randint(0, seconds))


def generate_categories(output: Path) -> None:
    """生成一级商品分类。"""
    write_csv(
        output / "product_category.csv",
        ["id", "parent_id", "category_name", "category_level", "status"],
        ((category_id, "", name, 1, 1) for category_id, name in CATEGORIES),
    )


def generate_products(output: Path, count: int, rng: random.Random) -> list[ProductSnapshot]:
    """生成商品文件，并返回订单生成所需的小规模商品快照。"""
    products: list[ProductSnapshot] = []

    def rows() -> Iterator[Sequence[object]]:
        for product_id in range(1, count + 1):
            category_id, category_name = rng.choice(CATEGORIES)
            price = money(Decimal(rng.randint(990, 399900)) / Decimal(100))
            # 商品名用"品类+模拟商品+全局唯一编号"（编号随 product_id 递增，全局不重复）
            name = f"{category_name}模拟商品{product_id}"
            snapshot = ProductSnapshot(product_id, category_id, name, price)
            products.append(snapshot)
            yield (
                product_id,
                f"P{product_id:08d}",
                category_id,
                name,
                f"品牌{rng.randint(1, 30)}",
                price,
                1,
            )

    write_csv(
        output / "product.csv",
        ["id", "product_code", "category_id", "product_name", "brand_name", "unit_price", "status"],
        rows(),
    )
    return products


def generate_users(
    output: Path,
    count: int,
    rng: random.Random,
    start: datetime,
    reference_time: datetime,
) -> list[datetime]:
    """生成脱敏电商用户，并保留注册时间用于约束后续业务时间。"""
    registration_times: list[datetime] = []

    def rows() -> Iterator[Sequence[object]]:
        latest_registration = reference_time - timedelta(days=1)
        for user_id in range(1, count + 1):
            registered_at = random_time(rng, start, latest_registration)
            registration_times.append(registered_at)
            province, city = rng.choice(PROVINCES)
            yield (
                user_id,
                f"U{user_id:010d}",
                rng.choices(["Unknown", "Male", "Female"], [5, 48, 47])[0],
                rng.randint(15, 80),
                province,
                city,
                rng.choice(["Web", "App", "MiniProgram", "Offline"]),
                rng.choices(["Normal", "Silver", "Gold", "Platinum"], [65, 20, 12, 3])[0],
                format_datetime(registered_at),
                1,
            )

    write_csv(
        output / "ecommerce_user.csv",
        ["id", "user_code", "gender", "age", "province", "city", "register_channel", "membership_level", "registered_at", "status"],
        rows(),
    )
    return registration_times


def generate_browse_behaviors(
    output: Path,
    count: int,
    rng: random.Random,
    registration_times: Sequence[datetime],
    products: Sequence[ProductSnapshot],
    reference_time: datetime,
) -> None:
    """生成浏览、点击、收藏和加购行为。"""
    session_upper_bound = max(len(registration_times), count // 5)

    def rows() -> Iterator[Sequence[object]]:
        for behavior_id in range(1, count + 1):
            user_id = rng.randint(1, len(registration_times))
            product = rng.choice(products)
            yield (
                behavior_id,
                user_id,
                product.product_id,
                rng.choices(["View", "Click", "Favorite", "Cart"], [60, 25, 7, 8])[0],
                f"S{rng.randint(1, session_upper_bound):012d}",
                rng.choice(DEVICES),
                rng.choice(["Direct", "Search", "Recommend", "Campaign"]),
                format_datetime(random_time(rng, registration_times[user_id - 1], reference_time)),
            )

    write_csv(
        output / "user_browse_behavior.csv",
        ["id", "user_id", "product_id", "behavior_type", "session_id", "device_type", "channel", "behavior_at"],
        rows(),
    )


def generate_login_behaviors(
    output: Path,
    count: int,
    rng: random.Random,
    registration_times: Sequence[datetime],
    reference_time: datetime,
) -> None:
    """生成登录行为，确保退出时间不超过统计截止时间。"""

    def rows() -> Iterator[Sequence[object]]:
        for login_id in range(1, count + 1):
            user_id = rng.randint(1, len(registration_times))
            login_at = random_time(rng, registration_times[user_id - 1], reference_time)
            max_duration = min(14_400, max(0, int((reference_time - login_at).total_seconds())))
            has_logout = max_duration >= 60 and rng.random() >= 0.03
            duration = rng.randint(60, max_duration) if has_logout else None
            logout_at = login_at + timedelta(seconds=duration) if duration is not None else None
            yield (
                login_id,
                user_id,
                f"L{login_id:012d}",
                rng.choice(DEVICES),
                rng.choice(["Password", "Quick"]),
                format_datetime(login_at),
                format_datetime(logout_at),
                "" if duration is None else duration,
            )

    write_csv(
        output / "user_login_behavior.csv",
        ["id", "user_id", "session_id", "device_type", "login_channel", "login_at", "logout_at", "duration_seconds"],
        rows(),
    )


def generate_orders(
    output: Path,
    count: int,
    rng: random.Random,
    registration_times: Sequence[datetime],
    products: Sequence[ProductSnapshot],
    reference_time: datetime,
) -> None:
    """同步流式生成订单和明细，保证金额及状态时间一致。"""
    order_path = output / "sales_order.csv"
    item_path = output / "sales_order_item.csv"
    with (
        order_path.open("w", newline="", encoding="utf-8") as order_file,
        item_path.open("w", newline="", encoding="utf-8") as item_file,
    ):
        order_writer = csv.writer(order_file)
        item_writer = csv.writer(item_file)
        order_writer.writerow([
            "id", "order_no", "user_id", "order_status", "total_amount", "discount_amount",
            "payment_amount", "payment_method", "ordered_at", "paid_at", "completed_at",
        ])
        item_writer.writerow([
            "id", "order_id", "product_id", "product_name_snapshot", "unit_price", "quantity", "item_amount",
        ])

        item_id = 1
        for order_id in range(1, count + 1):
            user_id = rng.randint(1, len(registration_times))
            ordered_at = random_time(rng, registration_times[user_id - 1], reference_time)
            total_amount = Decimal("0.00")
            selected_products = rng.sample(products, k=min(rng.randint(1, 4), len(products)))
            for product in selected_products:
                quantity = rng.randint(1, 3)
                item_amount = money(product.unit_price * quantity)
                total_amount += item_amount
                item_writer.writerow([
                    item_id,
                    order_id,
                    product.product_id,
                    product.name,
                    product.unit_price,
                    quantity,
                    item_amount,
                ])
                item_id += 1

            total_amount = money(total_amount)
            discount_rate = Decimal(rng.randint(0, 20)) / Decimal(100)
            discount_amount = money(total_amount * discount_rate)
            payment_amount = money(total_amount - discount_amount)
            status = rng.choices(
                ["Completed", "Paid", "Shipped", "Cancelled", "Refunded"],
                [65, 10, 10, 10, 5],
            )[0]

            paid_at: datetime | None = None
            completed_at: datetime | None = None
            if status != "Cancelled":
                paid_at = min(ordered_at + timedelta(minutes=rng.randint(1, 120)), reference_time)
            if status == "Completed":
                completed_at = min(
                    max(paid_at or ordered_at, ordered_at + timedelta(days=rng.randint(1, 10))),
                    reference_time,
                )

            order_writer.writerow([
                order_id,
                f"O{order_id:012d}",
                user_id,
                status,
                total_amount,
                discount_amount,
                payment_amount,
                "" if status == "Cancelled" else rng.choice(["Alipay", "WeChat", "BankCard"]),
                format_datetime(ordered_at),
                format_datetime(paid_at),
                format_datetime(completed_at),
            ])


def generate(config: GeneratorConfig) -> None:
    """按照配置生成全部电商模拟数据文件。"""
    validate_config(config)
    rng = random.Random(config.seed)
    config.output.mkdir(parents=True, exist_ok=True)
    start = config.reference_time - timedelta(days=730)

    generate_categories(config.output)
    products = generate_products(config.output, config.products, rng)
    registration_times = generate_users(
        config.output, config.users, rng, start, config.reference_time
    )
    generate_browse_behaviors(
        config.output, config.behaviors, rng, registration_times, products, config.reference_time
    )
    generate_login_behaviors(
        config.output, config.login_records, rng, registration_times, config.reference_time
    )
    generate_orders(
        config.output, config.orders, rng, registration_times, products, config.reference_time
    )


def validate_config(config: GeneratorConfig) -> None:
    """校验规模参数，避免生成无意义数据或触发采样错误。"""
    if min(config.users, config.products, config.behaviors, config.orders) <= 0:
        raise ValueError("各数据规模参数必须大于0")


def parse_datetime(value: str) -> datetime:
    """解析命令行传入的统计截止时间。"""
    try:
        return datetime.strptime(value, DATETIME_FORMAT)
    except ValueError as exception:
        raise argparse.ArgumentTypeError(
            f"时间格式必须为 {DATETIME_FORMAT}，例如 2026-01-01 00:00:00"
        ) from exception


def main() -> None:
    """命令行入口。"""
    parser = argparse.ArgumentParser(description="生成合规、可复现的电商模拟数据")
    parser.add_argument("--output", type=Path, default=Path("generated-data"))
    parser.add_argument("--users", type=int, default=100_000)
    parser.add_argument("--products", type=int, default=10_000)
    parser.add_argument("--behaviors", type=int, default=1_000_000)
    parser.add_argument("--orders", type=int, default=200_000)
    parser.add_argument("--seed", type=int, default=2026)
    parser.add_argument(
        "--reference-time",
        type=parse_datetime,
        default=datetime(2026, 1, 1, 0, 0, 0),
        help=f"统计截止时间，格式：{DATETIME_FORMAT}",
    )
    args = parser.parse_args()
    config = GeneratorConfig(
        output=args.output,
        users=args.users,
        products=args.products,
        behaviors=args.behaviors,
        orders=args.orders,
        seed=args.seed,
        reference_time=args.reference_time,
    )
    try:
        generate(config)
    except ValueError as exception:
        parser.error(str(exception))
    print(f"模拟数据已生成到：{config.output.resolve()}")


if __name__ == "__main__":
    main()
