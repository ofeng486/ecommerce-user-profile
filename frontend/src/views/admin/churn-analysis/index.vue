<template>
  <div class="page-body" :class="isAdminSide ? 'theme-admin' : 'theme-user'" v-loading="loading">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">流失预警</h1>
          <span class="title-tag">CHURN ALERT</span>
        </div>
        <p class="page-desc">按距最近购买天数（recency）评估流失风险：&gt;180 天高流失、90-180 天中流失、30-90 天低流失，支持名单导出。</p>
      </div>
      <span class="header-meta">
        <span class="meta-dot"></span>
        统计基准：画像数据版本 {{ dataVersion }}<template v-if="statDate"> · 截止 {{ statDate }}</template>
      </span>
    </div>

    <!-- 等级分布 -->
    <div class="level-grid">
      <div v-for="lv in levels" :key="lv.level" class="level-card" :class="['level--' + levelKey(lv.level), { 'level-card--active': levelFilter === lv.level }]" @click="selectLevel(lv.level)">
        <span class="level-name">{{ lv.level }}</span>
        <span class="level-count">{{ fmtNum(lv.userCount) }}</span>
        <span class="level-ratio">{{ levelRatio(lv.level) }}% 占比</span>
        <span class="level-desc">{{ LEVEL_DESC[lv.level] }}</span>
      </div>
    </div>

    <!-- 名单 -->
    <div class="section-outer"><div class="section-inner">
      <div class="list-toolbar">
        <ElSelect v-model="levelFilter" placeholder="全部等级" clearable class="input-level" @change="reload">
          <ElOption v-for="lv in levels" :key="lv.level" :label="lv.level" :value="lv.level" />
        </ElSelect>
        <span class="result-count">共 {{ total }} 位用户</span>
        <span class="toolbar-spacer"></span>
        <ElButton plain size="small" @click="doExport"><SvgIcon icon="ri:download-2-line" class="mr-1" />导出 CSV</ElButton>
      </div>
      <ElTable :data="list" stripe size="small" class="data-table" height="460" :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 600 }" @row-click="rowClick" @sort-change="onSortChange" empty-text="该等级暂无用户">
        <ElTableColumn type="index" label="#" width="50" />
        <ElTableColumn prop="userCode" label="用户编码" min-width="150" />
        <ElTableColumn label="流失等级" min-width="96">
          <template #default="{ row }"><ElTag size="small" :type="tagType(row.level)">{{ row.level }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn prop="segmentName" label="用户分层" min-width="110">
          <template #default="{ row }">{{ row.segmentName || '-' }}</template>
        </ElTableColumn>
        <ElTableColumn prop="rfmGroupName" label="RFM 分组" min-width="120">
          <template #default="{ row }">
            <ElTag size="small" :type="rfmTagType(row.rfmGroupName)" effect="light" disable-transitions>{{ row.rfmGroupName || '-' }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="gender" label="性别" min-width="60">
          <template #default="{ row }">{{ genderLabel(row.gender) }}</template>
        </ElTableColumn>
        <ElTableColumn prop="age" label="年龄" min-width="60" align="right" />
        <ElTableColumn prop="recencyDays" label="距最近购买" min-width="104" align="right" sortable="custom">
          <template #default="{ row }">
            <ElTooltip :content="churnHint(row)" placement="left" :disabled="Number(row.recencyDays) >= 9999">
              <span class="recency-cell" :style="{ color: recencyColor(row.recencyDays), fontWeight: Number(row.recencyDays) > 180 ? 600 : 400 }">{{ Number(row.recencyDays) >= 9999 ? '无订单' : row.recencyDays + ' 天' }}</span>
            </ElTooltip>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="orderCount" label="订单数" min-width="80" align="right" sortable="custom" />
        <ElTableColumn prop="totalPaymentAmount" label="累计消费" min-width="120" align="right" sortable="custom">
          <template #default="{ row }">¥{{ fmtNum(row.totalPaymentAmount) }}</template>
        </ElTableColumn>
      </ElTable>
      <div class="pager-row">
        <ElPagination layout="prev, pager, next" :total="total" :page-size="pageSize" :current-page="page + 1" @current-change="p => { page = p - 1; load() }" background small />
      </div>
    </div></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { fetchChurnLevels, fetchChurnUsers, fetchChurnVersion, exportChurnCsv } from '@/api/churn'

defineOptions({ name: 'ChurnAnalysis' })

const router = useRouter()
const route = useRoute()
/** 管理端蓝色 / 用户端青色（共享页面按路由自动切换） */
const isAdminSide = computed(() => !route.path.startsWith('/user'))
/** 行点击 → 跳画像详情 */
function rowClick(row: any) {
  if (row?.userId) router.push(`${route.path.startsWith('/user') ? '/user' : ''}/profiles/${row.userId}`)
}

const loading = ref(false)
const levels = ref<any[]>([])
const list = ref<any[]>([])
const total = ref(0)
const page = ref(0)
const pageSize = 20
const levelFilter = ref('')
const dataVersion = ref('')
const statDate = ref('')
/** 排序状态（后端全量排序，白名单防注入） */
let sortOrderBy = ''
let sortOrderDir = ''

const LEVEL_COLOR: Record<string, string> = {
  高流失: '#ef4444', 中流失: '#f59e0b', 低流失: '#eab308', 活跃: '#10b981', 从未购买: '#94a3b8'
}

/** 各等级风险口径说明 */
const LEVEL_DESC: Record<string, string> = {
  高流失: '距最近购买超 180 天，优先召回',
  中流失: '距最近购买 90~180 天，需关注',
  低流失: '距最近购买 30~90 天，及早干预',
  活跃: '距最近购买 ≤30 天，正常活跃',
  从未购买: '无有效订单，未产生购买'
}

/** 距最近购买列 hover 解读：流失原因画像 */
function churnHint(row: any) {
  const d = Number(row.recencyDays)
  if (d >= 9999) return '该用户无有效订单（从未购买）'
  const rec = `已 ${d} 天未购买`
  if (d > 180) return `${rec}，超过 180 天警戒线，高流失风险`
  if (d > 90) return `${rec}，处于 90~180 天中风险区间`
  if (d > 30) return `${rec}，处于 30~90 天低风险区间，建议触达`
  return `${rec}，活跃用户`
}

/** RFM 分组 → ElTag 颜色（按客户价值分级） */
function rfmTagType(g: string): 'success' | 'primary' | 'warning' | 'danger' | 'info' {
  return ({ '重要价值客户': 'success', '一般价值客户': 'success', '重要保持客户': 'primary',
    '重要发展客户': 'primary', '一般发展客户': 'warning', '重要挽留客户': 'warning',
    '流失客户': 'danger' } as Record<string, 'success' | 'primary' | 'warning' | 'danger' | 'info'>)[g || ''] || 'info'
}

/** 距最近购买 → 风险色阶（>180红 / 90-180橙 / 30-90黄 / <30绿 / 无订单灰） */
function recencyColor(d: any) {
  const v = Number(d)
  if (v >= 9999) return '#94a3b8'
  if (v > 180) return '#ef4444'
  if (v > 90) return '#f59e0b'
  if (v > 30) return '#eab308'
  return '#10b981'
}

function fmtNum(n: any) { return Number(n || 0).toLocaleString() }
function levelKey(lv: string) { return { 高流失: 'high', 中流失: 'mid', 低流失: 'low', 活跃: 'active', 从未购买: 'none' }[lv] || 'none' }
function levelRatio(lv: string) {
  const totalN = levels.value.reduce((a, b) => a + Number(b.userCount || 0), 0)
  const hit = levels.value.find(x => x.level === lv)
  return totalN && hit ? ((Number(hit.userCount) / totalN) * 100).toFixed(1) : '0.0'
}
function tagType(lv: string): 'danger' | 'warning' | 'success' | 'info' {
  return ({ 高流失: 'danger', 中流失: 'warning', 低流失: 'warning', 活跃: 'success', 从未购买: 'info' } as Record<string, 'danger' | 'warning' | 'success' | 'info'>)[lv] || 'info'
}
function genderLabel(g: string) { return g === 'Male' ? '男' : g === 'Female' ? '女' : (g || '-') }
function selectLevel(lv: string) { levelFilter.value = lv; sortOrderBy = ''; sortOrderDir = ''; page.value = 0; load() }
function reload() { page.value = 0; load() }
/** 列排序 → 后端全量排序（orderBy 白名单防注入） */
function onSortChange({ prop, order }: any) {
  if (order === null) { sortOrderBy = ''; sortOrderDir = '' }
  else {
    sortOrderBy = prop === 'recencyDays' ? 'recencyDays' : prop === 'orderCount' ? 'orderCount' : 'totalPaymentAmount'
    sortOrderDir = order === 'ascending' ? 'asc' : 'desc'
  }
  page.value = 0
  load()
}

async function load() {
  loading.value = true
  try {
    const params: any = { level: levelFilter.value || undefined, page: page.value, size: pageSize }
    if (sortOrderBy) { params.orderBy = sortOrderBy; params.orderDir = sortOrderDir }
    const res = await fetchChurnUsers(params)
    list.value = res?.records || []; total.value = res?.total || 0
  } catch { /* 空态 */ } finally { loading.value = false }
}

function doExport() {
  exportChurnCsv(levelFilter.value || undefined).then((blob: any) => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const scope = levelFilter.value || '全部等级'
    a.download = `流失预警_${scope}_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }).catch((e: any) => { console.warn('导出失败', e) })
}

onMounted(async () => {
  const [lv, ver] = await Promise.all([fetchChurnLevels().catch(() => []), fetchChurnVersion().catch(() => null)])
  levels.value = lv || []
  dataVersion.value = ver?.dataVersion || ''
  statDate.value = ver?.calculatedAt ? String(ver.calculatedAt).slice(0, 10) : ''
  await load()
})
</script>

<style scoped>
/* ═══ 页面头部（企业级统一风格） ═══ */
.page-header {
  display: flex; align-items: flex-end; justify-content: space-between; gap: 20px;
  margin-bottom: 20px; padding-bottom: 18px;
  border-bottom: 1px solid #eef2f6;
}
.ph-left { min-width: 0; }
.ph-title-row { display: flex; align-items: center; gap: 10px; }
.title-accent {
  width: 4px; height: 20px; border-radius: 2px; flex-shrink: 0;
}
.theme-admin .title-accent { background: linear-gradient(180deg, #2563eb 0%, #60a5fa 100%); }
.theme-user  .title-accent { background: linear-gradient(180deg, #0d9488 0%, #5eead4 100%); }
.page-title {
  font-size: 22px; font-weight: 700; color: #0f172a; margin: 0;
  font-family: 'Plus Jakarta Sans', 'Inter', 'PingFang SC', sans-serif;
  letter-spacing: -0.3px; line-height: 1.2;
}
.title-tag {
  font-size: 10px; font-weight: 600; letter-spacing: 1.2px; color: #94a3b8;
  background: #f1f5f9; border-radius: 4px; padding: 2px 6px;
  font-family: 'JetBrains Mono', monospace; text-transform: uppercase;
}
.page-desc {
  font-size: 13px; color: #64748b; margin: 8px 0 0 14px; line-height: 1.6;
  max-width: 600px;
}
.header-meta {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; color: #475569; background: #f8fafc;
  border: 1px solid #e2e8f0; border-radius: 999px;
  padding: 5px 12px; white-space: nowrap; flex-shrink: 0;
}
.meta-dot { width: 6px; height: 6px; border-radius: 50%; background: #10b981; }
.level-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-bottom: 16px; }
.level-card { background: var(--default-box-color); border: 1px solid var(--default-border); border-left: 4px solid #94a3b8; border-radius: 10px; padding: 18px 20px; cursor: pointer; transition: all .2s; display: flex; flex-direction: column; gap: 4px; }
.level-card:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(15,23,42,.07); }
.level--high { border-left-color: #ef4444; } .level--mid { border-left-color: #f59e0b; }
.level--low { border-left-color: #eab308; } .level--active { border-left-color: #10b981; } .level--none { border-left-color: #94a3b8; }
.level-name { font-size: 13px; color: #64748b; }
.level-count { font-size: 24px; font-weight: 700; color: #0f172a; font-variant-numeric: tabular-nums; }
.level-ratio { font-size: 12px; color: #94a3b8; }
.level-desc { font-size: 11.5px; color: #94a3b8; line-height: 1.5; margin-top: 2px; }
/* 选中态：纯光圈高亮（不覆盖 border-color，保留左侧风险色条语义色） */
.theme-admin .level-card--active { box-shadow: 0 0 0 2px rgba(37,99,235,.18); transform: translateY(-2px); }
.theme-user  .level-card--active { box-shadow: 0 0 0 2px rgba(13,148,136,.18); transform: translateY(-2px); }
.recency-cell { border-bottom: 1px dashed #cbd5e1; cursor: help; }
.list-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.input-level { width: 160px; }
.result-count { font-size: 12.5px; color: #94a3b8; }
.toolbar-spacer { flex: 1; }
.pager-row { display: flex; justify-content: flex-end; margin-top: 12px; }
.data-table :deep(.el-table__row) { cursor: pointer; }
.theme-admin .data-table :deep(.el-table__row:hover > td) { background: #f5f9ff !important; }
.theme-user  .data-table :deep(.el-table__row:hover > td) { background: #f0fdfa !important; }
/* 用户端容器内 Element 组件主色 → 青色（按钮/分页/下拉），管理端保持默认 */
.theme-user { --el-color-primary: #0d9488; --el-color-primary-light-3: #14b8a6; --el-color-primary-light-5: #5eead4; --el-color-primary-light-8: #ccfbf1; }
</style>
