"""电商模拟数据生成器自动化测试。"""

from __future__ import annotations

import csv
import importlib.util
import tempfile
import unittest
import sys
from datetime import datetime
from decimal import Decimal
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("generate_data.py")
SPEC = importlib.util.spec_from_file_location("generate_data", MODULE_PATH)
generate_data = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
sys.modules[SPEC.name] = generate_data
SPEC.loader.exec_module(generate_data)


class GenerateDataTest(unittest.TestCase):
    """验证文件规模、可复现性和核心业务一致性。"""

    def make_config(self, output: Path, seed: int = 2026):
        return generate_data.GeneratorConfig(
            output=output,
            users=12,
            products=8,
            behaviors=40,
            orders=15,
            seed=seed,
            reference_time=datetime(2026, 1, 1),
        )

    def read_rows(self, path: Path) -> list[dict[str, str]]:
        with path.open(encoding="utf-8") as file:
            return list(csv.DictReader(file))

    def test_generate_expected_files_and_counts(self) -> None:
        """应生成全套 CSV，并满足配置的数据条数。"""
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            generate_data.generate(self.make_config(output))

            self.assertEqual(5, len(self.read_rows(output / "product_category.csv")))
            self.assertEqual(8, len(self.read_rows(output / "product.csv")))
            self.assertEqual(12, len(self.read_rows(output / "ecommerce_user.csv")))
            self.assertEqual(40, len(self.read_rows(output / "user_browse_behavior.csv")))
            self.assertEqual(12, len(self.read_rows(output / "user_login_behavior.csv")))
            self.assertEqual(15, len(self.read_rows(output / "sales_order.csv")))
            self.assertGreaterEqual(len(self.read_rows(output / "sales_order_item.csv")), 15)

    def test_same_seed_produces_same_content(self) -> None:
        """固定随机种子和截止时间时，结果应可复现。"""
        with tempfile.TemporaryDirectory() as first, tempfile.TemporaryDirectory() as second:
            first_path, second_path = Path(first), Path(second)
            generate_data.generate(self.make_config(first_path, seed=42))
            generate_data.generate(self.make_config(second_path, seed=42))

            for file_name in (
                "product.csv", "ecommerce_user.csv", "user_browse_behavior.csv",
                "user_login_behavior.csv", "sales_order.csv", "sales_order_item.csv",
            ):
                self.assertEqual(
                    (first_path / file_name).read_bytes(),
                    (second_path / file_name).read_bytes(),
                )

    def test_order_amount_and_business_time_are_consistent(self) -> None:
        """订单金额、商品价格和用户业务时间必须保持一致。"""
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            generate_data.generate(self.make_config(output))

            products = {
                int(row["id"]): Decimal(row["unit_price"])
                for row in self.read_rows(output / "product.csv")
            }
            registrations = {
                int(row["id"]): datetime.fromisoformat(row["registered_at"])
                for row in self.read_rows(output / "ecommerce_user.csv")
            }
            item_totals: dict[int, Decimal] = {}
            for item in self.read_rows(output / "sales_order_item.csv"):
                product_id = int(item["product_id"])
                order_id = int(item["order_id"])
                unit_price = Decimal(item["unit_price"])
                item_amount = Decimal(item["item_amount"])
                self.assertEqual(products[product_id], unit_price)
                self.assertEqual(unit_price * int(item["quantity"]), item_amount)
                item_totals[order_id] = item_totals.get(order_id, Decimal("0")) + item_amount

            for order in self.read_rows(output / "sales_order.csv"):
                order_id = int(order["id"])
                total = Decimal(order["total_amount"])
                discount = Decimal(order["discount_amount"])
                payment = Decimal(order["payment_amount"])
                self.assertEqual(item_totals[order_id], total)
                self.assertEqual(total - discount, payment)
                self.assertGreaterEqual(
                    datetime.fromisoformat(order["ordered_at"]),
                    registrations[int(order["user_id"])],
                )
                if order["order_status"] == "Completed":
                    self.assertTrue(order["paid_at"])
                    self.assertTrue(order["completed_at"])

    def test_invalid_scale_is_rejected(self) -> None:
        """非正数规模应被拒绝。"""
        with tempfile.TemporaryDirectory() as directory:
            config = generate_data.GeneratorConfig(
                output=Path(directory), users=0, products=1, behaviors=1, orders=1,
                seed=1, reference_time=datetime(2026, 1, 1),
            )
            with self.assertRaises(ValueError):
                generate_data.generate(config)


if __name__ == "__main__":
    unittest.main()
