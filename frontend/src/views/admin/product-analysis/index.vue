<template>
  <div class="page-body" :class="isAdminSide ? 'theme-admin' : 'theme-user'" v-loading="loading">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">商品分析</h1>
          <span class="title-tag">PRODUCT INSIGHT</span>
        </div>
        <p class="page-desc">商品销售表现、品类结构与价格带分布，辅助选品与库存决策。</p>
      </div>
      <div class="ph-meta">
        <span v-if="dataDate" class="meta-pill">
          <span class="meta-dot"></span>
          数据截止 {{ dataDate }}
        </span>
        <span class="meta-pill">
          <span class="meta-dot"></span>
          口径：有效订单
        </span>
      </div>
    </div>

    <!-- 指标卡 -->
    <div class="metric-grid">
      <div class="metric-card" v-for="m in metrics" :key="m.label">
        <span class="metric-label">{{ m.label }}</span>
        <span class="metric-value">{{ m.value }}</span>
        <span class="metric-hint" v-if="m.hint">{{ m.hint }}</span>
      </div>
    </div>

    <!-- 贡献度横幅 -->
    <div class="concentration-bar" v-if="conc">
      <span class="conc-dot"></span>
      <span class="conc-text">头部商品（Top {{ conc.topCount }}）贡献 <b>{{ conc.ratio }}%</b> 的销售额，共 ¥{{ fmtMoney(conc.topAmount) }}</span>
      <span class="conc-sub">长尾分散度 {{ conc.ratio > 50 ? '偏低' : '良好' }}（占比越高越依赖头部）</span>
    </div>

    <!-- 图表区 -->
    <div class="chart-grid">
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">销售 Top 10</h3>
          <span class="chart-subtitle">按销售额排序</span>
        </div>
        <div ref="topChart" style="height: 360px"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">品类销售占比</h3>
          <span class="chart-subtitle">点击下钻 · 按销售额占比</span>
        </div>
        <div ref="catChart" style="height: 360px"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">价格带分布</h3>
          <span class="chart-subtitle">商品数与销售额双指标</span>
        </div>
        <div ref="bandChart" style="height: 300px"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">热销商品明细</h3>
          <span class="chart-subtitle">Top 10 销量与销售额</span>
        </div>
        <ElTable :data="topSales" size="small" stripe class="data-table" height="360" :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 600 }">
          <ElTableColumn type="index" label="#" width="50" />
          <ElTableColumn prop="productName" label="商品名称" min-width="200" show-overflow-tooltip />
          <ElTableColumn prop="quantity" label="销量" min-width="100" align="right">
            <template #default="{ row }">{{ fmtNum(row.quantity) }}</template>
          </ElTableColumn>
          <ElTableColumn prop="orderCount" label="订单数" min-width="100" align="right">
            <template #default="{ row }">{{ fmtNum(row.orderCount) }}</template>
          </ElTableColumn>
          <ElTableColumn prop="amount" label="销售额" min-width="160" align="right">
            <template #default="{ row }">
              <div class="bar-cell">
                <span class="bar-num">¥{{ fmtMoney(row.amount) }}</span>
                <div class="bar-track"><div class="bar-fill" :style="{ width: amountPct(row.amount) + '%' }"></div></div>
              </div>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { fetchProductOverview, fetchTopSales, fetchCategoryShare, fetchPriceBands, fetchConcentration } from '@/api/product'

defineOptions({ name: 'ProductAnalysis' })

const router = useRouter()
const route = useRoute()
/** 管理端（/admin 前缀）蓝色主题；用户端（/user 前缀）青色主题——共享页面按端自动切换 */
const isAdminSide = computed(() => !route.path.startsWith('/user'))
const MAIN = computed(() => isAdminSide.value ? '#2563eb' : '#0d9488')
const MAIN_LIGHT = computed(() => isAdminSide.value ? '#60a5fa' : '#5eead4')
const PALETTE = computed(() => isAdminSide.value
  ? ['#2563eb', '#0ea5e9', '#10b981', '#f59e0b', '#8b5cf6', '#64748b']
  : ['#0d9488', '#14b8a6', '#5eead4', '#f59e0b', '#1e40af', '#94a3b8'])
const GRAY = '#94a3b8'

const loading = ref(false)
const topChart = ref<HTMLElement>()
const catChart = ref<HTMLElement>()
const bandChart = ref<HTMLElement>()
const charts: echarts.ECharts[] = []

const overview = ref<any>({})
const topSales = ref<any[]>([])
const categoryShare = ref<any[]>([])
const priceBands = ref<any[]>([])
const conc = ref<any>(null)

function fmtNum(n: any) {
  return Number(n || 0).toLocaleString()
}
function fmtMoney(n: any) {
  const v = Number(n || 0)
  return v >= 10000 ? (v / 10000).toFixed(1) + '万' : v.toLocaleString()
}
/** 销售额 mini 进度条：相对 Top1 的占比 */
function amountPct(v: any) {
  const max = Math.max(...topSales.value.map(x => Number(x.amount || 0)), 1)
  return Math.max(4, Math.round(Number(v || 0) / max * 100))
}

/** 品类下钻：跳画像列表筛"偏好该品类"的用户（FAVORITE_CATEGORY；tag_value 已数据治理统一） */
function drillCategory(name: string, id: number) {
  if (!id) return
  router.push({ path: '/user/profiles', query: { tagCode: 'FAVORITE_CATEGORY', tagValue: String(id), tagName: name } })
}

/** 数据截止（有效订单 MAX(paid_at)，YYYY-MM-DD） */
const dataDate = computed(() => {
  const d = overview.value?.dataDate
  return d ? String(d).slice(0, 10) : ''
})

const metrics = computed<{ label: string; value: string; hint?: string }[]>(() => [
  { label: '在售商品', value: fmtNum(overview.value.productCount) },
  { label: '累计销量', value: fmtNum(overview.value.totalQuantity) },
  { label: '累计销售额', value: '¥' + fmtMoney(overview.value.totalAmount) },
  { label: '平均单价', value: '¥' + fmtNum(overview.value.avgPrice) }
])

onMounted(async () => {
  loading.value = true
  try {
    const [ov, top, cat, band, cc] = await Promise.all([
      fetchProductOverview(), fetchTopSales(), fetchCategoryShare(), fetchPriceBands(), fetchConcentration()
    ])
    overview.value = ov || {}; topSales.value = top || []; categoryShare.value = cat || []
    priceBands.value = band || []; conc.value = cc || null
    await nextTick()
    renderCharts()
  } catch { /* 保留空态 */ } finally { loading.value = false }
  window.addEventListener('resize', handleResize)
})

function renderCharts() {
  const palette = PALETTE.value
  // 销售 Top10 横向条形
  if (topChart.value && topSales.value.length) {
    const c = echarts.init(topChart.value); charts.push(c)
    const data = [...topSales.value].sort((a, b) => a.amount - b.amount)
    c.setOption({
      grid: { left: 8, right: 56, top: 8, bottom: 8, containLabel: true },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: (p: any) => `${p[0].name}<br/>销售额：¥${fmtMoney(p[0].value)}` },
      xAxis: { type: 'value', axisLabel: { fontSize: 11, color: GRAY, formatter: (v: number) => fmtMoney(v) }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
      yAxis: { type: 'category', data: data.map(d => d.productName), axisLabel: { fontSize: 11, color: '#334155', width: 90, overflow: 'truncate' }, axisLine: { show: false }, axisTick: { show: false } },
      series: [{
        type: 'bar', data: data.map(d => d.amount), barWidth: 14,
        itemStyle: { color: MAIN.value, borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', fontSize: 11, color: '#64748b', formatter: (p: any) => fmtMoney(p.value) }
      }]
    })
  }
  // 品类占比环形（点击下钻：跳画像列表筛偏好该品类的用户）
  if (catChart.value && categoryShare.value.length) {
    const c = echarts.init(catChart.value); charts.push(c)
    c.setOption({
      tooltip: { trigger: 'item', formatter: '{b}<br/>销售额：¥{c}（{d}%）' },
      legend: { bottom: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11, color: '#64748b' } },
      series: [{
        type: 'pie', radius: ['38%', '68%'], center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
        label: { fontSize: 11, color: '#334155', formatter: '{b} {d}%' },
        data: categoryShare.value.map((d, i) => ({ name: d.categoryName, value: Number(d.amount), categoryId: d.categoryId, itemStyle: { color: palette[i % palette.length] } }))
      }]
    })
    c.on('click', (p: any) => { if (p.data?.categoryId) drillCategory(p.name, Number(p.data.categoryId)) })
  }
  // 价格带柱状：双指标（商品数 + 销售额，横向条形双 x 轴）
  if (bandChart.value && priceBands.value.length) {
    const c = echarts.init(bandChart.value); charts.push(c)
    const bands = [...priceBands.value].sort((a, b) => Number(a.productCount || 0) - Number(b.productCount || 0))
    c.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' },
        formatter: (p: any) => {
          const d = priceBands.value.find(x => x.band === p[0].name)
          return `${p[0].name}<br/>商品数：${(d?.productCount || 0)} 个<br/>销售额：¥${fmtMoney(d?.amount || 0)}`
        } },
      legend: { bottom: 0, itemWidth: 12, itemHeight: 6, textStyle: { fontSize: 11, color: '#64748b' } },
      grid: { left: 8, right: 56, top: 8, bottom: 24, containLabel: true },
      xAxis: [
        { type: 'value', axisLabel: { fontSize: 10, color: GRAY, formatter: (v: number) => v >= 1000 ? (v / 1000).toFixed(0) + 'k' : v }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
        { type: 'value', axisLabel: { fontSize: 10, color: GRAY, formatter: (v: number) => fmtMoney(v) }, splitLine: { show: false } }
      ],
      yAxis: { type: 'category', data: bands.map(d => d.band), axisLabel: { fontSize: 11, color: '#334155' }, axisLine: { show: false }, axisTick: { show: false } },
      series: [
        { name: '商品数', type: 'bar', data: bands.map(d => Number(d.productCount || 0)), barWidth: 8,
          itemStyle: { color: MAIN.value, borderRadius: [0, 3, 3, 0] },
          label: { show: true, position: 'right', fontSize: 10, color: '#475569' } },
        { name: '销售额', type: 'bar', xAxisIndex: 1, data: bands.map(d => Number(d.amount || 0)), barWidth: 8,
          itemStyle: { color: '#f59e0b', borderRadius: [0, 3, 3, 0] },
          label: { show: true, position: 'right', fontSize: 10, color: '#b45309', formatter: (p: any) => fmtMoney(p.value) } }
      ]
    })
  }
}

function handleResize() { charts.forEach(c => c.resize()) }
onUnmounted(() => { window.removeEventListener('resize', handleResize); charts.forEach(c => c.dispose()) })
</script>

<style scoped>
/* ═══ 页面头部（企业级克制风） ═══ */
.page-header {
  display: flex; align-items: flex-end; justify-content: space-between; gap: 20px;
  margin-bottom: 20px; padding-bottom: 18px;
  border-bottom: 1px solid #eef2f6;
}
.ph-left { min-width: 0; }
.ph-title-row { display: flex; align-items: center; gap: 10px; }
/* 主题化：管理端蓝色 / 用户端青色（共享页面按路由自动切换） */
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
  max-width: 560px;
}
.ph-meta { flex-shrink: 0; display: flex; gap: 8px; }
.meta-pill {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; color: #475569; background: #f8fafc;
  border: 1px solid #e2e8f0; border-radius: 999px;
  padding: 5px 12px; white-space: nowrap;
}
.meta-dot { width: 6px; height: 6px; border-radius: 50%; background: #10b981; }

.metric-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 16px; }
.metric-card { background: var(--default-box-color); border: 1px solid rgba(15,23,42,0.06); border-radius: 14px; padding: 20px 24px; display: flex; flex-direction: column; gap: 6px; box-shadow: 0 4px 16px rgba(15,23,42,0.03); }
.metric-label { font-size: 12.5px; color: #64748b; }
.metric-value { font-size: 26px; font-weight: 700; color: #0f172a; font-variant-numeric: tabular-nums; letter-spacing: -0.5px; }
.metric-hint { font-size: 12px; color: #94a3b8; }
.concentration-bar { display: flex; align-items: center; gap: 10px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 14px 20px; margin-bottom: 16px; font-size: 13px; color: #475569; }
.conc-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.theme-admin .conc-dot { background: #2563eb; }
.theme-user  .conc-dot { background: #0d9488; }
.conc-text b { font-weight: 700; }
.theme-admin .conc-text b { color: #2563eb; }
.theme-user  .conc-text b { color: #0d9488; }
.conc-sub { margin-left: auto; font-size: 12px; color: #94a3b8; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-card { background: var(--default-box-color); border: 1px solid rgba(15,23,42,0.06); border-radius: 14px; padding: 20px 24px; box-shadow: 0 4px 16px rgba(15,23,42,0.03); transition: box-shadow .3s cubic-bezier(0.32,0.72,0,1); }
.chart-header { display: flex; align-items: baseline; justify-content: space-between; padding-bottom: 12px; margin-bottom: 8px; border-bottom: 1px solid #f1f5f9; }
.chart-title { font-size: 15px; font-weight: 600; color: #0f172a; }
.chart-subtitle { font-size: 12px; color: #94a3b8; }
.data-table :deep(.el-table__row:hover > td) { background: #f5f9ff !important; }
.theme-user .data-table :deep(.el-table__row:hover > td) { background: #f0fdfa !important; }
/* 销售额 mini 进度条 */
.bar-cell { display: flex; align-items: center; gap: 8px; justify-content: flex-end; }
.bar-num { font-size: 12.5px; color: #334155; font-variant-numeric: tabular-nums; white-space: nowrap; }
.bar-track { width: 64px; height: 5px; border-radius: 3px; background: #f1f5f9; overflow: hidden; flex-shrink: 0; }
.bar-fill { height: 100%; border-radius: 3px; }
.theme-admin .bar-fill { background: linear-gradient(90deg, #60a5fa, #2563eb); }
.theme-user  .bar-fill { background: linear-gradient(90deg, #5eead4, #0d9488); }
@media (max-width: 1100px) { .chart-grid { grid-template-columns: 1fr; } }
</style>
