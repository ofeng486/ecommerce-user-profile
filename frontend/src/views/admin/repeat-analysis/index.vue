<template>
  <div class="page-body" :class="isAdminSide ? 'theme-admin' : 'theme-user'" v-loading="loading">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">复购与留存</h1>
          <span class="title-tag">REPEAT & RETENTION</span>
        </div>
        <p class="page-desc">基于有效订单分析用户复购行为与月度首购留存同期组，识别忠实用户与流失拐点。</p>
      </div>
      <div class="ph-meta">
        <span v-if="dataDate" class="header-meta">
          <span class="meta-dot"></span>
          数据截止 {{ dataDate }}
        </span>
        <span class="header-meta">
          <span class="meta-dot"></span>
          口径：有效订单
        </span>
      </div>
    </div>

    <!-- 指标卡 -->
    <div class="metric-grid">
      <div class="metric-card">
        <span class="metric-label">复购率</span>
        <span class="metric-value">{{ repeatRate.repeatRate ?? '—' }}%</span>
        <span class="metric-hint">有购用户中下单 ≥2 次占比</span>
      </div>
      <div class="metric-card">
        <span class="metric-label">多购用户</span>
        <span class="metric-value">{{ fmtNum(repeatRate.multiBuyerUsers) }}</span>
        <span class="metric-hint">累计下单 ≥2 次</span>
      </div>
      <div class="metric-card">
        <span class="metric-label">平均购买间隔</span>
        <span class="metric-value">{{ avgInterval.avgIntervalDays ?? '—' }} 天</span>
        <span class="metric-hint">多购用户相邻订单平均间隔</span>
      </div>
      <div class="metric-card">
        <span class="metric-label">有购用户</span>
        <span class="metric-value">{{ fmtNum(repeatRate.buyerUsers) }}</span>
        <span class="metric-hint">总用户 {{ fmtNum(repeatRate.totalUsers) }}</span>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-grid">
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">购买次数分布</h3>
          <span class="chart-subtitle">按用户累计订单数分档</span>
        </div>
        <div ref="distChart" style="height: 300px"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">复购率与购买间隔</h3>
          <span class="chart-subtitle">整体复购指标</span>
        </div>
        <div ref="rateChart" style="height: 300px"></div>
      </div>
      <div class="chart-card chart-card--wide">
        <div class="chart-header">
          <h3 class="chart-title">月度首购留存热力图</h3>
          <span class="chart-subtitle">行=首购月同期组 · 列=首购后第 N 月 · 颜色=留存率</span>
          <span class="chart-toolbar">
            <button v-for="opt in cohortRangeOpts" :key="opt.key" class="range-chip" :class="{ 'range-chip--active': cohortRange === opt.key }" @click="cohortRange = opt.key; renderCharts()">{{ opt.label }}</button>
          </span>
        </div>
        <div v-if="cohort.length" ref="retChart" style="height: 560px"></div>
        <div v-else class="chart-empty">暂无留存数据——需先运行画像分析任务生成有效订单数据。</div>
        <p class="heat-note">灰白色块 = 该同期组在此月无后续购买数据（数据集 stat-date 2025-12-31 之前早期同期组仅首购）</p>
      </div>
      <div class="chart-card chart-card--wide">
        <div class="chart-header">
          <h3 class="chart-title">高复购用户 Top 10</h3>
          <span class="chart-subtitle">累计订单数最多</span>
        </div>
        <ElTable :data="topRepeat" size="small" stripe class="data-table" height="360" :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 600 }" @row-click="rowClick">
          <ElTableColumn type="index" label="#" width="50" />
          <ElTableColumn prop="userCode" label="用户编码" min-width="180" />
          <ElTableColumn prop="orderCount" label="订单数" min-width="100" align="right">
            <template #default="{ row }">{{ fmtNum(row.orderCount) }}</template>
          </ElTableColumn>
          <ElTableColumn prop="amount" label="累计消费" min-width="130" align="right">
            <template #default="{ row }">¥{{ fmtNum(row.amount) }}</template>
          </ElTableColumn>
          <ElTableColumn prop="lastOrderAt" label="最近购买" min-width="160">
            <template #default="{ row }">{{ row.lastOrderAt ? String(row.lastOrderAt).slice(0, 10) : '-' }}</template>
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
import { fetchPurchaseDistribution, fetchRepeatRate, fetchAvgInterval, fetchRetentionCohort, fetchTopRepeat } from '@/api/repeat'

defineOptions({ name: 'RepeatAnalysis' })

const router = useRouter()
const route = useRoute()
/** 管理端蓝色 / 用户端青色（共享页面按路由自动切换） */
const isAdminSide = computed(() => !route.path.startsWith('/user'))
const MAIN = computed(() => isAdminSide.value ? '#2563eb' : '#0d9488')
const MAIN_LIGHT = computed(() => isAdminSide.value ? '#60a5fa' : '#5eead4')
/** 行点击 → 跳画像详情 */
function rowClick(row: any) {
  if (row?.userId) router.push(`${route.path.startsWith('/user') ? '/user' : ''}/profiles/${row.userId}`)
}

const loading = ref(false)
const distChart = ref<HTMLElement>()
const rateChart = ref<HTMLElement>()
const retChart = ref<HTMLElement>()
const charts: echarts.ECharts[] = []

const distribution = ref<any[]>([])
const repeatRate = ref<any>({})
const avgInterval = ref<any>({})
const cohort = ref<any[]>([])
const topRepeat = ref<any[]>([])
/** 热力图范围：近 24 个月 / 全部 cohort */
const cohortRange = ref<'24' | 'all'>('24')
const cohortRangeOpts = [
  { key: '24', label: '近 24 月' },
  { key: 'all', label: '全部' }
]

const PALETTE = computed(() => isAdminSide.value
  ? ['#2563eb', '#0ea5e9', '#10b981', '#f59e0b', '#8b5cf6', '#64748b', '#f97316', '#2563eb']
  : ['#0d9488', '#14b8a6', '#5eead4', '#f59e0b', '#1e40af', '#94a3b8', '#f97316', '#2563eb'])
const GRAY = '#94a3b8'

/** 留存率 → 颜色阶梯（与 visualMap 主色阶对齐：浅→深，管理端蓝/用户端青） */
function rateColor(r: number) {
  const deep = isAdminSide.value ? '#1e3a8a' : '#134e4a'
  const main = isAdminSide.value ? '#2563eb' : '#0d9488'
  const mid = isAdminSide.value ? '#60a5fa' : '#2dd4bf'
  const light = isAdminSide.value ? '#bfdbfe' : '#99f6e4'
  const faint = isAdminSide.value ? '#eff6ff' : '#f0fdfa'
  if (r >= 60) return deep
  if (r >= 40) return main
  if (r >= 20) return mid
  if (r >= 10) return light
  return faint
}

function fmtNum(n: any) { return Number(n || 0).toLocaleString() }

/** 数据截止（有效订单 MAX(paid_at)，YYYY-MM-DD） */
const dataDate = computed(() => {
  const d = repeatRate.value?.dataDate
  return d ? String(d).slice(0, 10) : ''
})

onMounted(async () => {
  loading.value = true
  try {
    const [d, r, i, c, t] = await Promise.all([
      fetchPurchaseDistribution(), fetchRepeatRate(), fetchAvgInterval(), fetchRetentionCohort(), fetchTopRepeat()
    ])
    distribution.value = d || []; repeatRate.value = r || {}; avgInterval.value = i || {}
    cohort.value = c || []; topRepeat.value = t || []
    await nextTick()
    renderCharts()
  } catch { /* 空态 */ } finally { loading.value = false }
  window.addEventListener('resize', handleResize)
})

function renderCharts() {
  // 购买次数分布
  if (distChart.value && distribution.value.length) {
    const existing = echarts.getInstanceByDom(distChart.value)
    if (existing) existing.dispose()
    const c = echarts.init(distChart.value); charts.push(c)
    const bands = [...distribution.value].sort((a, b) => a.userCount - b.userCount)
    c.setOption({
      grid: { left: 8, right: 40, top: 8, bottom: 8, containLabel: true },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: (p: any) => `${p[0].name}<br/>用户数：${p[0].value.toLocaleString()}` },
      xAxis: { type: 'value', axisLabel: { fontSize: 11, color: GRAY }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
      yAxis: { type: 'category', data: bands.map(d => d.band), axisLabel: { fontSize: 11, color: '#334155' }, axisLine: { show: false }, axisTick: { show: false } },
      series: [{
        // 分布图单主色：0 次灰色、其余主色渐变（克制统一，替代多彩条）
        type: 'bar', data: bands.map((d, i) => ({ value: d.userCount, itemStyle: { color: d.band === '0 次' ? '#cbd5e1' : { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: MAIN_LIGHT.value }, { offset: 1, color: MAIN.value }] }, borderRadius: [0, 4, 4, 0] } })), barWidth: 16,
        label: { show: true, position: 'right', fontSize: 11, color: '#64748b' }
      }]
    })
  }
  // 复购率环形图
  if (rateChart.value) {
    const existing = echarts.getInstanceByDom(rateChart.value)
    if (existing) existing.dispose()
    const c = echarts.init(rateChart.value); charts.push(c)
    const buyers = Number(repeatRate.value.buyerUsers || 0)
    const multi = Number(repeatRate.value.multiBuyerUsers || 0)
    const single = Math.max(0, buyers - multi)
    c.setOption({
      tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 人（{d}%）' },
      legend: { bottom: 0, itemWidth: 10, itemHeight: 8, textStyle: { fontSize: 11, color: '#64748b' } },
      series: [{
        type: 'pie', radius: ['52%', '74%'], center: ['50%', '46%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
        // 统一 label：只显示人数（名字由底部图例承担），避免小扇区 label 被卡片边距遮挡
        label: { fontSize: 11, color: '#334155', formatter: (p: any) => `${p.value} 人` },
        labelLayout: { hideOverlap: true },
        labelLine: { show: true, length: 8, length2: 8 },
        data: [
          { name: '复购用户(≥2次)', value: multi, itemStyle: { color: MAIN.value } },
          { name: '仅购 1 次', value: single, itemStyle: { color: '#e2e8f0' } }
        ]
      }]
    })
  }
  // 月度首购留存 cohort 热力图（行=首购月，列=首购后第 N 月，颜色=留存率）
  if (retChart.value && cohort.value.length) {
    // 1. 每月 cohort 基数（monthIndex=0）
    const base: Record<string, number> = {}
    cohort.value.forEach((r: any) => { if (Number(r.monthIndex) === 0) base[r.firstMonth] = Number(r.retentionUsers) })
    const allMonths = Object.keys(base).sort()
    // 2. 范围过滤：近 24 月 / 全部
    const months = cohortRange.value === '24' ? allMonths.slice(-24) : allMonths
    const maxIdx = Math.max(...cohort.value.map((r: any) => Number(r.monthIndex)))
    // 3. 热力图数据：rate=null 的格子用浅灰色 itemStyle 标记「无数据」，与「低留存率」视觉区分
    const data: any[] = []
    months.forEach((m, yi) => {
      for (let xi = 0; xi <= maxIdx; xi++) {
        const hit = cohort.value.find((r: any) => r.firstMonth === m && Number(r.monthIndex) === xi)
        const rate = hit && base[m] ? Math.round(Number(hit.retentionUsers) / base[m] * 1000) / 10 : null
        if (rate !== null) {
          data.push({ value: [xi, yi, rate], itemStyle: { color: rateColor(rate) } })
        } else {
          // 无数据格子：浅灰斜纹
          data.push({ value: [xi, yi, '-'], itemStyle: { color: '#f1f5f9' } })
        }
      }
    })
    // 4. 动态高度（每行 24px + 顶部 20 + 底部 66 + visualMap）
    const heatHeight = Math.min(1320, Math.max(560, months.length * 24 + 86))
    retChart.value.style.height = heatHeight + 'px'
    // 5. 防重复 init
    const existing = echarts.getInstanceByDom(retChart.value)
    if (existing) existing.dispose()
    const c = echarts.init(retChart.value); charts.push(c)
    c.setOption({
      tooltip: {
        position: 'top',
        formatter: (p: any) => {
          const m = months[p.value[1]]
          if (p.value[2] === '-') return `${m} 首购同期组<br/>首购后第 ${p.value[0]} 月<br/><span style="color:#94a3b8">无后续购买数据</span>`
          const rate = p.value[2]
          const total = base[m] || 0
          return `${m} 首购同期组<br/>首购后第 ${p.value[0]} 月<br/>留存率 <b>${rate}%</b><br/>留存用户 ${total ? Math.round(total * rate / 100) : '-'} 人 / 基数 ${total} 人`
        }
      },
      grid: { left: 90, right: 100, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: Array.from({ length: maxIdx + 1 }, (_, i) => (i === 0 ? '首购月' : i + '月')), splitArea: { show: true }, axisLabel: { fontSize: 11, color: '#334155', rotate: 45 }, axisLine: { show: false }, axisTick: { show: false } },
      yAxis: { type: 'category', data: months, splitArea: { show: true }, axisLabel: { fontSize: 11, color: '#334155' }, axisLine: { show: false }, axisTick: { show: false } },
      visualMap: {
        type: 'continuous', min: 0, max: 100, calculable: true, orient: 'vertical', right: 10, top: 'middle',
        itemHeight: 130, itemWidth: 14, textStyle: { fontSize: 11, color: '#64748b' },
        inRange: { color: isAdminSide.value
          ? ['#eff6ff', '#bfdbfe', '#60a5fa', '#2563eb', '#1e3a8a']
          : ['#f0fdfa', '#99f6e4', '#2dd4bf', '#0d9488', '#134e4a'] },
        text: ['留存率 %', '低']
      },
      series: [{
        type: 'heatmap', data,
        label: { show: months.length <= 26, fontSize: 10, color: '#334155',
          formatter: (p: any) => (p.value[2] === '-' ? '' : p.value[2] + '%') },
        emphasis: { itemStyle: { borderColor: '#0f172a', borderWidth: 1 } }
      }]
    })
  }
}

function handleResize() { charts.forEach(c => c.resize()) }
onUnmounted(() => { window.removeEventListener('resize', handleResize); charts.forEach(c => c.dispose()) })
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
.ph-meta { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.meta-dot { width: 6px; height: 6px; border-radius: 50%; background: #10b981; }
.metric-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 16px; }
.metric-card { background: var(--default-box-color); border: 1px solid rgba(15,23,42,0.06); border-radius: 14px; padding: 20px 24px; display: flex; flex-direction: column; gap: 6px; box-shadow: 0 4px 16px rgba(15,23,42,0.03); }
.metric-label { font-size: 12.5px; color: #64748b; }
.metric-value { font-size: 26px; font-weight: 700; color: #0f172a; font-variant-numeric: tabular-nums; letter-spacing: -0.5px; }
.metric-hint { font-size: 12px; color: #94a3b8; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-card { background: var(--default-box-color); border: 1px solid rgba(15,23,42,0.06); border-radius: 14px; padding: 20px 24px; box-shadow: 0 4px 16px rgba(15,23,42,0.03); transition: box-shadow .3s cubic-bezier(0.32,0.72,0,1); }
.chart-card--wide { grid-column: 1 / -1; }
.chart-header { display: flex; align-items: baseline; justify-content: space-between; padding-bottom: 12px; margin-bottom: 8px; border-bottom: 1px solid #f1f5f9; }
.chart-title { font-size: 15px; font-weight: 600; color: #0f172a; }
.chart-subtitle { font-size: 12px; color: #94a3b8; }
.chart-toolbar { display: flex; align-items: center; gap: 6px; }
.range-chip {
  font-size: 11.5px; color: #64748b; background: #f8fafc;
  border: 1px solid #e2e8f0; border-radius: 999px;
  padding: 3px 10px; cursor: pointer; transition: all .15s;
  font-family: inherit;
}
.range-chip:hover { color: #2563eb; border-color: #93c5fd; }
.theme-user .range-chip:hover { color: #0d9488; border-color: #5eead4; }
.range-chip--active { color: #fff; background: #2563eb; border-color: #2563eb; }
.theme-user .range-chip--active { background: #0d9488; border-color: #0d9488; }
.heat-note { font-size: 11.5px; color: #94a3b8; margin: 8px 0 0; line-height: 1.6; }
.chart-empty {
  background: #f8fafc; border: 1px dashed var(--default-border); border-radius: 10px;
  padding: 40px 20px; text-align: center; color: #94a3b8; font-size: 13px;
}
.data-table :deep(.el-table__row) { cursor: pointer; }
.theme-admin .data-table :deep(.el-table__row:hover > td) { background: #f5f9ff !important; }
.theme-user  .data-table :deep(.el-table__row:hover > td) { background: #f0fdfa !important; }
@media (max-width: 1100px) { .chart-grid { grid-template-columns: 1fr; } }
</style>
