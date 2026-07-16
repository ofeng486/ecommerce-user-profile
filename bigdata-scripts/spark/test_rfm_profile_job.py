"""PySpark 画像任务静态测试。

开发环境未安装 PySpark，通过 AST 检查任务入口、目标表和核心函数是否完整。
"""

import ast
import unittest
from pathlib import Path

JOB_FILE = Path(__file__).with_name("rfm_profile_job.py")


class SparkProfileJobTest(unittest.TestCase):
    def setUp(self) -> None:
        self.source = JOB_FILE.read_text(encoding="utf-8")
        self.tree = ast.parse(self.source)
        self.functions = {node.name for node in ast.walk(self.tree) if isinstance(node, ast.FunctionDef)}

    def test_required_functions_exist(self) -> None:
        self.assertTrue({
            "build_profile_summary", "build_segments", "build_user_tags",
            "build_distribution", "build_segment_distribution", "run", "main",
        }.issubset(self.functions))

    def test_all_ads_targets_exist(self) -> None:
        for table in (
            "ads_user_profile_summary", "ads_user_value_segment", "ads_user_tag",
            "ads_tag_distribution", "ads_segment_distribution",
        ):
            self.assertIn(table, self.source)

    def test_five_segments_and_four_tags_exist(self) -> None:
        for code in ("HIGH_VALUE", "POTENTIAL", "GENERAL", "AT_RISK", "LOW_VALUE"):
            self.assertIn(code, self.source)
        for tag in ("ACTIVE_LEVEL", "CONSUMPTION_LEVEL", "FAVORITE_CATEGORY", "RFM_SEGMENT"):
            self.assertIn(tag, self.source)


if __name__ == "__main__":
    unittest.main()
