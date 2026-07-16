"""Hive 数仓脚本静态结构测试。

当前开发机未安装 Hive，通过关键表、变量、语句顺序和基础括号匹配尽早发现脚本缺失。
"""

from __future__ import annotations

import re
import unittest
from pathlib import Path

HIVE_DIR = Path(__file__).parent


class HiveScriptTest(unittest.TestCase):
    """验证 Hive SQL 文件的核心结构。"""

    def read(self, name: str) -> str:
        return (HIVE_DIR / name).read_text(encoding="utf-8")

    def test_all_layers_define_expected_tables(self) -> None:
        ods = self.read("01_ods_tables.sql")
        dwd = self.read("02_dwd_tables.sql")
        dws = self.read("03_dws_tables.sql")
        ads = self.read("04_ads_tables.sql")

        for table in (
            "ods_product_category", "ods_product", "ods_ecommerce_user",
            "ods_user_browse_behavior", "ods_user_login_behavior",
            "ods_sales_order", "ods_sales_order_item",
        ):
            self.assertIn(table, ods)
        for table in (
            "dwd_ecommerce_user", "dwd_product", "dwd_user_behavior",
            "dwd_user_login", "dwd_valid_order", "dwd_valid_order_item",
        ):
            self.assertIn(table, dwd)
        self.assertIn("dws_user_profile_metrics", dws)
        for table in (
            "ads_user_profile_summary", "ads_user_value_segment", "ads_user_tag",
            "ads_tag_distribution", "ads_segment_distribution",
        ):
            self.assertIn(table, ads)

    def test_required_hive_variables_are_used(self) -> None:
        self.assertIn("${hivevar:raw_base_path}", self.read("01_ods_tables.sql"))
        self.assertIn("${hivevar:stat_date}", self.read("03_dws_tables.sql"))
        self.assertIn("${hivevar:data_version}", self.read("04_ads_tables.sql"))

    def test_sql_parentheses_are_balanced(self) -> None:
        """忽略注释和字符串后检查基础括号配对。"""
        for name in ("01_ods_tables.sql", "02_dwd_tables.sql", "03_dws_tables.sql", "04_ads_tables.sql"):
            sql = self.read(name)
            sql = re.sub(r"--.*", "", sql)
            sql = re.sub(r"'[^']*'", "''", sql)
            self.assertEqual(sql.count("("), sql.count(")"), name)

    def test_each_create_or_insert_statement_ends_with_semicolon(self) -> None:
        for name in ("01_ods_tables.sql", "02_dwd_tables.sql", "03_dws_tables.sql", "04_ads_tables.sql"):
            sql = self.read(name).strip()
            self.assertTrue(sql.endswith(";"), name)
            self.assertNotIn("CREATE TABLE IF NOT EXISTS ;", sql)


if __name__ == "__main__":
    unittest.main()
