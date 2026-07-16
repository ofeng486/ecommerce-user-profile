"""ADS 到 MySQL 同步脚本静态测试。"""

import ast
import unittest
from pathlib import Path

FILE = Path(__file__).with_name("sync_ads_to_mysql.py")


class SyncAdsToMySqlTest(unittest.TestCase):
    def setUp(self) -> None:
        self.source = FILE.read_text(encoding="utf-8")
        self.tree = ast.parse(self.source)

    def test_password_only_comes_from_environment(self) -> None:
        self.assertIn('os.environ.get("MYSQL_PASSWORD")', self.source)
        self.assertNotIn('add_argument("--mysql-password"', self.source)

    def test_three_result_tables_are_synced(self) -> None:
        for table in ("user_profile_summary", "user_segment", "user_profile_tag"):
            self.assertIn(f'"{table}"', self.source)

    def test_truncate_preserves_mysql_schema(self) -> None:
        self.assertIn('.option("truncate", "true")', self.source)


if __name__ == "__main__":
    unittest.main()
