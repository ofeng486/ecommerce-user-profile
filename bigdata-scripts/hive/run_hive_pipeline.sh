#!/usr/bin/env bash
set -euo pipefail

# 顺序执行 ODS -> DWD -> DWS -> ADS Hive SQL。
RAW_BASE_PATH="${1:-/warehouse/ecommerce_profile/ods/raw}"
STAT_DATE="${2:-2026-01-01}"
DATA_VERSION="${3:-$(date +%Y%m%d%H%M%S)}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

beeline -u "${HIVE_JDBC_URL:-jdbc:hive2://localhost:10000/default}" \
  --hivevar raw_base_path="${RAW_BASE_PATH}" \
  -f "${SCRIPT_DIR}/01_ods_tables.sql"

beeline -u "${HIVE_JDBC_URL:-jdbc:hive2://localhost:10000/default}" \
  -f "${SCRIPT_DIR}/02_dwd_tables.sql"

beeline -u "${HIVE_JDBC_URL:-jdbc:hive2://localhost:10000/default}" \
  --hivevar stat_date="${STAT_DATE}" \
  -f "${SCRIPT_DIR}/03_dws_tables.sql"

beeline -u "${HIVE_JDBC_URL:-jdbc:hive2://localhost:10000/default}" \
  --hivevar stat_date="${STAT_DATE}" \
  --hivevar data_version="${DATA_VERSION}" \
  -f "${SCRIPT_DIR}/04_ads_tables.sql"

echo "Hive 用户画像流水线执行完成：stat_date=${STAT_DATE}, data_version=${DATA_VERSION}"
