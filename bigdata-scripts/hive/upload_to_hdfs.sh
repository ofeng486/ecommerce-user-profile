#!/usr/bin/env bash
set -euo pipefail

# 将生成器输出的 CSV 按 Hive ODS 外部表目录上传至 HDFS。
LOCAL_DATA_DIR="${1:-generated-data/million}"
HDFS_RAW_BASE="${2:-/warehouse/ecommerce_profile/ods/raw}"

if [[ ! -d "${LOCAL_DATA_DIR}" ]]; then
  echo "本地数据目录不存在：${LOCAL_DATA_DIR}" >&2
  exit 1
fi

FILES=(
  product_category
  product
  ecommerce_user
  user_browse_behavior
  user_login_behavior
  sales_order
  sales_order_item
)

for name in "${FILES[@]}"; do
  source_file="${LOCAL_DATA_DIR}/${name}.csv"
  target_dir="${HDFS_RAW_BASE}/${name}"
  if [[ ! -f "${source_file}" ]]; then
    echo "缺少数据文件：${source_file}" >&2
    exit 1
  fi
  hdfs dfs -mkdir -p "${target_dir}"
  hdfs dfs -rm -f "${target_dir}/${name}.csv" >/dev/null 2>&1 || true
  hdfs dfs -put "${source_file}" "${target_dir}/${name}.csv"
  echo "已上传 ${source_file} -> ${target_dir}"
done
