<template>
  <div class="page-body">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">用户画像列表</h1>
          <span class="title-tag">USER PROFILES</span>
        </div>
        <p class="page-desc">按关键词、分层、省份与消费金额区间筛选已画像用户，支持排序与 CSV 导出。</p>
      </div>
    </div>

    <!-- 核心指标卡（真实画像统计） -->
    <div class="metrics-grid">
      <div class="metric-card">
        <span class="metric-val">{{ fmtNum(metrics.totalUsers) }}</span>
        <span class="metric-label">已画像用户</span>
      </div>
      <div class="metric-card">
        <span class="metric-val">{{ fmtNum(metrics.totalUsers ? Math.round(metrics.totalOrders / metrics.totalUsers * 10) / 10 : 0) }}</span>
        <span class="metric-label">平均订单数</span>
      </div>
      <div class="metric-card">
        <span class="metric-val">¥{{ fmtMoney(metrics.totalUsers ? metrics.totalAmount / metrics.totalUsers : 0) }}</span>
        <span class="metric-label">人均消费</span>
      </div>
      <div class="metric-card">
        <span class="metric-val" :style="{ color: atRiskRate > 15 ? '#ef4444' : '#0f172a' }">{{ atRiskRate }}%</span>
        <span class="metric-label">流失率（流失风险用户）</span>
      </div>
    </div>

    <!-- TOP 省份消费（地域分布洞察） -->
    <div class="province-card">
      <div class="province-head">
        <h3 class="province-title">TOP 省份消费</h3>
        <span class="province-sub">TOP 5 · 按画像用户累计消费金额排序</span>
      </div>
      <div class="province-bars">
        <div v-for="p in provinces" :key="p.province" class="province-row" :class="{ 'province-row--clickable': p.province !== '其他省份' }" :title="p.province === '其他省份' ? '聚合省份不可筛选' : '点击筛选该省份用户'" @click="filterByProvince(p)">
          <span class="province-name">{{ p.province }}</span>
          <div class="province-track"><div class="province-fill" :class="{ 'province-fill--other': p.province === '其他省份' }" :style="{ width: pct(p.amount, maxProvinceAmount) + '%' }"></div></div>
          <span class="province-amount">¥{{ fmtMoney(p.amount) }}</span>
          <span class="province-pct">{{ provincePct(p.amount) }}%</span>
          <span class="province-users">{{ p.userCount }} 人</span>
        </div>
        <div v-if="!provinces.length" class="province-empty">暂无省份数据</div>
      </div>
    </div>

    <div class="section-outer"><div class="section-inner">
      <div class="search-bar">
        <ElInput v-model="keyword" placeholder="搜索用户编码..." clearable class="input-search" @keyup.enter="search">
          <template #prefix><SvgIcon icon="ri:search-line" /></template>
        </ElInput>
        <ElSelect v-model="segmentCode" placeholder="用户分层" clearable class="input-select" @change="search">
          <ElOption label="高价值用户" value="HIGH_VALUE" /><ElOption label="潜力用户" value="POTENTIAL" />
          <ElOption label="一般用户" value="GENERAL" /><ElOption label="流失风险用户" value="AT_RISK" />
          <ElOption label="低价值用户" value="LOW_VALUE" />
        </ElSelect>
        <ElSelect v-model="province" placeholder="省份" clearable class="input-select" @change="search">
          <ElOption v-for="p in provinces" :key="p.province" :label="p.province" :value="p.province" />
        </ElSelect>
        <div class="amount-range">
          <ElInput v-model.number="minAmount" placeholder="消费≥" clearable class="input-amount" @keyup.enter="search" />
          <span class="amount-sep">—</span>
          <ElInput v-model.number="maxAmount" placeholder="消费≤" clearable class="input-amount" @keyup.enter="search" />
        </div>
        <ElButton type="primary" @click="search"><SvgIcon icon="ri:search-line" class="mr-1" />搜索</ElButton>
        <ElButton plain @click="exportCsv"><SvgIcon icon="ri:download-2-line" class="mr-1" />导出 CSV</ElButton>
        <span class="result-count">共 {{ total }} 位用户</span>
      </div>

      <ElTable :data="list" stripe v-loading="loading" height="560" @row-click="goDetail" @sort-change="onSortChange" class="cursor-pointer data-table" :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 600 }">
        <ElTableColumn prop="userCode" label="用户编码" min-width="160">
          <template #default="{ row }"><span class="font-mono text-blue-600">{{ row.userCode }}</span></template>
        </ElTableColumn>
        <ElTableColumn prop="gender" label="性别" min-width="76">
          <template #default="{ row }"><ElTag size="small" :type="row.gender==='男'||row.gender==='Male'?'primary':row.gender==='女'||row.gender==='Female'?'danger':'info'">{{ genderLabel(row.gender) }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn prop="age" label="年龄" min-width="76" sortable />
        <ElTableColumn prop="province" label="省份" min-width="100" />
        <ElTableColumn prop="city" label="城市" min-width="100" />
        <ElTableColumn prop="totalOrderCount" label="订单数" min-width="130" align="center" sortable>
          <template #default="{ row }">
            <div class="bar-cell">
              <span class="bar-num">{{ row.totalOrderCount || 0 }}</span>
              <div class="bar-track"><div class="bar-fill bar-fill--blue" :style="{ width: pct(row.totalOrderCount, maxOrders) + '%' }"></div></div>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="totalPaymentAmount" label="消费金额" min-width="180" align="right" sortable>
          <template #default="{ row }">
            <div class="bar-cell">
              <span class="bar-num" style="color:#FFAE1F">¥{{ (row.totalPaymentAmount || 0).toLocaleString() }}</span>
              <div class="bar-track"><div class="bar-fill bar-fill--gold" :style="{ width: pct(row.totalPaymentAmount, maxAmt) + '%' }"></div></div>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="用户分层" min-width="120">
          <template #default="{ row }"><ElTag size="small" :type="segmentTagType(row.segmentCode)" effect="light">{{ row.segmentName||'未分层' }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn prop="segmentScore" label="评分" min-width="96" align="center" sortable>
          <template #default="{ row }"><span v-if="row.segmentScore" class="font-bold" :style="{color:scoreColor(row.segmentScore)}">{{ Number(row.segmentScore).toFixed(1) }}</span><span v-else class="text-gray-300">-</span></template>
        </ElTableColumn>
      </ElTable>
      <div class="pagination-wrap">
        <ElPagination v-model:current-page="page" :page-size="size" :total="total" layout="total,prev,pager,next,jumper" @current-change="loadData" />
      </div>
    </div></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { fetchProfileList, fetchProfileMetrics, fetchProvinceRanking, exportProfilesCsv } from '@/api/profile'
defineOptions({ name: 'ProfileList' })
const router = useRouter()
const route = useRoute()
const keyword = ref(''); const segmentCode = ref(''); const province = ref('')
const minAmount = ref<number | undefined>(undefined); const maxAmount = ref<number | undefined>(undefined)
const orderBy = ref(''); const orderDir = ref('')
const metrics = ref({ totalUsers: 0, totalOrders: 0, totalAmount: 0, atRiskUsers: 0 })
const atRiskRate = ref(0)
const provinces = ref<Array<{ province: string; amount: number; userCount: number }>>([])
const list = ref<any[]>([]); const loading = ref(false)
const page = ref(1); const size = ref(20); const total = ref(0)
function fmtNum(n: number) { return n >= 10000 ? (n / 10000).toFixed(1) + '万' : (n || 0).toLocaleString() }
function fmtMoney(n: number) { return n >= 10000 ? (n / 10000).toFixed(1) + '万' : (n || 0).toLocaleString() }
/** 当前页最大值（mini bar 相对比例基准） */
const maxAmt = computed(() => Math.max(1, ...list.value.map(r => r.totalPaymentAmount || 0)))
const maxOrders = computed(() => Math.max(1, ...list.value.map(r => r.totalOrderCount || 0)))
const maxProvinceAmount = computed(() => Math.max(1, ...provinces.value.map(p => p.amount || 0)))
function pct(v: number, max: number) { return Math.max(0, Math.min(100, Math.round(((v || 0) / max) * 100))) }
function genderLabel(g: string) { if (g === '男' || g === 'Male') return '男'; if (g === '女' || g === 'Female') return '女'; return '未知' }
function segmentTagType(c: string) { const m: Record<string, string> = { HIGH_VALUE: 'success', POTENTIAL: 'primary', GENERAL: 'info', AT_RISK: 'warning', LOW_VALUE: 'danger' }; return (m[c] || 'info') as any }
function scoreColor(s: number) { if (s >= 4) return '#13DEB9'; if (s >= 3) return '#5D87FF'; if (s >= 2) return '#FFAE1F'; return '#FF4D4F' }
/** 省份占比（占全部画像用户消费总额） */
function provincePct(amount: number) {
  const t = metrics.value.totalAmount || 0
  return t > 0 ? Math.round((amount / t) * 1000) / 10 : 0
}
/** 点击省份条形 → 表格按该省筛选（"其他省份"为聚合行，不可筛） */
function filterByProvince(p: { province: string }) {
  if (p.province === '其他省份') return
  province.value = p.province
  search()
}
function search() { page.value = 1; loadData() }
/** 导出 CSV：按当前筛选条件导出全部匹配用户 */
function exportCsv() {
  exportProfilesCsv({
    keyword: keyword.value || undefined,
    segmentCode: segmentCode.value || undefined,
    province: province.value || undefined,
    minAmount: minAmount.value || undefined,
    maxAmount: maxAmount.value || undefined,
    orderBy: orderBy.value || undefined,
    orderDir: orderDir.value || undefined
  }).then((blob: any) => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `画像列表_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }).catch((e: any) => { console.warn('导出失败', e) })
}
function goDetail(row: any) { router.push(`/profiles/${row.userId}`) }
/** 表格排序（服务端）：orderBy/orderDir 重新请求 */
function onSortChange({ prop, order }: any) {
  orderBy.value = order ? (prop || '') : ''
  orderDir.value = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : ''
  page.value = 1
  loadData()
}
async function loadMetrics() {
  try {
    const m = await fetchProfileMetrics()
    if (m) {
      metrics.value = m
      atRiskRate.value = m.totalUsers ? Math.round(m.atRiskUsers / m.totalUsers * 1000) / 10 : 0
    }
  } catch { /* 保留旧值 */ }
}
async function loadProvinces() {
  try { const p = await fetchProvinceRanking(); if (p) provinces.value = p } catch { /* 保留旧值 */ }
}
async function loadData() {
  loading.value = true
  try {
    const res = await fetchProfileList({
      keyword: keyword.value || undefined,
      segmentCode: segmentCode.value || undefined,
      province: province.value || undefined,
      minAmount: minAmount.value,
      maxAmount: maxAmount.value,
      orderBy: orderBy.value || undefined,
      orderDir: orderDir.value || undefined,
      page: page.value - 1, size: size.value
    })
    if (res) { list.value = res.records || []; total.value = res.total || 0 }
  } catch {} finally { loading.value = false }
}
onMounted(() => {
  // 支持从运营总览分层图钻取进入（?segment=xxx 自动筛选）
  const s = route.query.segment
  if (s) segmentCode.value = String(s)
  loadMetrics()
  loadProvinces()
  loadData()
})
</script>

<style scoped>
.page-body { font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; }
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
  background: linear-gradient(180deg, #2563eb 0%, #60a5fa 100%);
}
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
  max-width: 560px;
}

.section-outer { padding: 1.5px; border-radius: 14px; background: rgba(0,0,0,.025); }
.section-inner { border-radius: calc(14px - 1.5px); background: var(--default-box-color); padding: 20px 24px 24px; border: 1px solid var(--default-border); }

/* ─── 核心指标卡 ─── */
.metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 14px; margin-bottom: 16px; }
.metric-card { display: flex; flex-direction: column; gap: 4px; background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px 16px; transition: all .2s; }
.metric-card:hover { box-shadow: 0 4px 14px rgba(15,23,42,.06); transform: translateY(-1px); }
.metric-val { font-size: 22px; font-weight: 700; color: #0f172a; line-height: 1.2; font-variant-numeric: tabular-nums; }
.metric-label { font-size: 11.5px; color: #94a3b8; }

/* ─── TOP 省份消费 ─── */
.province-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px 16px; margin-bottom: 16px; }
.province-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 10px; }
.province-title { font-size: 13.5px; font-weight: 600; color: #334155; margin: 0; }
.province-sub { font-size: 11px; color: #94a3b8; }
.province-bars { display: flex; flex-direction: column; gap: 7px; }
.province-row { display: flex; align-items: center; gap: 10px; }
.province-row--clickable { cursor: pointer; padding: 1px 4px; border-radius: 6px; transition: background .15s; }
.province-row--clickable:hover { background: #f1f5f9; }
.province-name { width: 60px; font-size: 12px; color: #475569; text-align: right; flex-shrink: 0; }
.province-track { flex: 1; height: 8px; border-radius: 4px; background: #f1f5f9; overflow: hidden; }
.province-fill { height: 100%; border-radius: 4px; background: linear-gradient(90deg, #5D87FF, #13DEB9); transition: width .6s ease; }
.province-fill--other { background: #cbd5e1; }
.province-amount { width: 70px; font-size: 11.5px; font-weight: 600; color: #0f172a; text-align: right; font-variant-numeric: tabular-nums; flex-shrink: 0; }
.province-pct { width: 42px; font-size: 11px; font-weight: 600; color: #5D87FF; text-align: right; font-variant-numeric: tabular-nums; flex-shrink: 0; }
.province-users { width: 46px; font-size: 10.5px; color: #94a3b8; text-align: right; flex-shrink: 0; }
.province-empty { font-size: 12px; color: #94a3b8; padding: 10px 0; text-align: center; }

/* ─── 表格 mini bar ─── */
.bar-cell { display: flex; flex-direction: column; gap: 3px; align-items: flex-end; }
.bar-num { font-size: 12.5px; font-weight: 600; font-variant-numeric: tabular-nums; }
.bar-track { width: 100%; height: 4px; border-radius: 2px; background: #f1f5f9; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 2px; transition: width .5s ease; }
.bar-fill--blue { background: #5D87FF; }
.bar-fill--gold { background: #FFAE1F; }

.search-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
.input-search { width: 240px; }
.input-select { width: 160px; }
.amount-range { display: flex; align-items: center; gap: 6px; }
.input-amount { width: 110px; }
.amount-sep { color: #94a3b8; font-size: 12px; }
.result-count { margin-left: auto; font-size: 13px; color: #949eb7; }

.data-table { width: 100%; }
.data-table :deep(.el-table__row:hover > td) { background: #f5f9ff !important; }
.data-table :deep(.score) { font-size: 15px; font-variant-numeric: tabular-nums; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: center; }

/* ─── system/users 风格：筛选区覆写 ─── */
.search-bar :deep(.el-input__wrapper),
.search-bar :deep(.el-select__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1.5px var(--default-border) inset;
  transition: box-shadow .2s;
}
.search-bar :deep(.el-input__wrapper:hover),
.search-bar :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1.5px #c7d7fe inset;
}
.search-bar :deep(.el-input__wrapper.is-focus),
.search-bar :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1.5px #5D87FF inset, 0 0 0 3px rgba(37,99,235,.1);
}
.search-bar :deep(.el-button) { border-radius: 8px; }
</style>