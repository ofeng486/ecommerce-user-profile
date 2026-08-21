<!-- Admin Dashboard — 运营数据总览 · 全部数据来自真实接口（画像概览/任务/分层/活跃趋势） -->
<template>
  <div class="admin-dash" v-loading="loading">
    <!-- ═══ 页面头部（企业级统一风格） ═══ -->
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">运营数据总览</h1>
          <span class="title-tag">OPERATIONS OVERVIEW</span>
        </div>
        <p class="page-desc">画像覆盖、用户分层与任务状态一屏总览，快速定位运营要点。</p>
      </div>
    </div>

    <!-- 顶栏状态（真实） -->
    <div class="status-bar">
      <div class="status-item" v-for="s in statusItems" :key="s.label">
        <span class="status-dot" :style="{background: s.color}"></span>
        <span class="status-label">{{ s.label }}</span>
        <span class="status-value">{{ s.val }}</span>
      </div>
    </div>

    <!-- KPI 行（真实画像统计，高价值主卡突出 + 覆盖率进度） -->
    <div class="kpi-grid">
      <div v-for="(k, i) in kpis" :key="k.label" class="kpi-card">
        <div class="kpi-icon" :style="{background: k.bg, color: k.fg}">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" v-html="k.path"></svg>
        </div>
        <div class="kpi-body">
          <span class="kpi-val">{{ k.prefix }}<span :ref="el => setKpiRef(el, i)">{{ fmtNum(k.raw) }}</span>{{ k.suffix }}</span>
          <span class="kpi-label">{{ k.label }}</span>
        </div>
      </div>
      <div class="kpi-card kpi-card--action" title="将当前运营数据导出为文本快照（用于汇报/存档）">
        <button class="snapshot-btn" @click="exportSnapshot">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          <span>导出运营快照</span>
        </button>
      </div>
    </div>

    <!-- 数据解读（基于当前运营数据的经营洞察） -->
    <section class="monitor-card insight-card">
      <div class="monitor-head">
        <h3><span class="neo-dot" style="background:#2563eb"></span> 数据解读</h3>
        <button class="insight-btn" :disabled="insightLoading" @click="genInsight">
          <span v-if="!insightLoading">生成数据解读</span>
          <span v-else class="insight-loading">解读中…</span>
        </button>
      </div>
      <div v-if="insightText" class="insight-body" v-html="renderMarkdown(insightText)"></div>
      <div v-else class="insight-empty">基于当前运营数据生成经营解读：高价值用户占比、画像覆盖率、近30天活跃趋势、任务状态等，帮助快速定位运营要点。</div>
    </section>

    <!-- 图表网格（真实数据） -->
    <div class="chart-grid">
      <section class="monitor-card wide">
        <div class="monitor-head">
          <h3><span class="neo-dot"></span> 近30天活跃趋势</h3>
          <span class="card-badge green">真实数据</span>
        </div>
        <div ref="trendChart" class="monitor-body" style="height:220px"></div>
      </section>

      <section class="monitor-card">
        <div class="monitor-head">
          <h3><span class="neo-dot" style="background:#10b981"></span> 任务状态分布</h3>
          <span class="card-badge green">{{ tasks.length }} 个任务</span>
        </div>
        <div ref="taskChart" class="monitor-body" style="height:220px"></div>
      </section>

      <section class="monitor-card">
        <div class="monitor-head">
          <h3><span class="neo-dot" style="background:#2563eb"></span> 用户分层分布</h3>
          <span class="card-badge green">{{ segments.length }} 层</span>
        </div>
        <div ref="segmentChart" class="monitor-body" style="height:220px"></div>
      </section>
    </div>

    <!-- 最近任务 + 数据概况 双栏 -->
    <div class="dual-grid">
      <section class="monitor-card">
        <div class="monitor-head">
          <h3><span class="neo-dot" style="background:#2563eb"></span> 最近任务</h3>
          <router-link to="/tasks" class="card-link">查看全部 →</router-link>
        </div>
        <div class="task-list">
          <div v-for="t in recentTasks" :key="t.id" class="task-row">
            <span class="task-name" :title="t.taskName">{{ t.taskName }}</span>
            <span class="task-type">{{ taskTypeLabel(t.taskType) }}</span>
            <span class="task-status" :class="'st-' + st(t.taskStatus)">{{ taskStatusLabel(t.taskStatus) }}</span>
            <span class="task-time">{{ formatTime(t.createdAt) }}</span>
          </div>
          <div v-if="!recentTasks.length" class="empty-tip">暂无任务，可在「任务管理」创建画像分析任务</div>
        </div>
      </section>

      <section class="monitor-card">
        <div class="monitor-head">
          <h3><span class="neo-dot" style="background:#14b8a6"></span> 数据概况</h3>
          <span class="card-badge green">画像覆盖 {{ coveragePct }}%</span>
        </div>
        <div class="quality-list">
          <div class="quality-row">
            <span class="quality-label">画像覆盖率</span>
            <div class="quality-bar-wrap"><div class="quality-bar" :style="{width: coveragePct + '%', background: '#10b981'}"></div></div>
            <span class="quality-pct">{{ coveragePct }}%</span>
          </div>
          <div class="quality-row">
            <span class="quality-label">高价值占比</span>
            <div class="quality-bar-wrap"><div class="quality-bar" :style="{width: highValuePct + '%', background: '#2563eb'}"></div></div>
            <span class="quality-pct">{{ highValuePct }}%</span>
          </div>
          <div class="quality-row">
            <span class="quality-label">分层覆盖率</span>
            <div class="quality-bar-wrap"><div class="quality-bar" :style="{width: segmentedPct + '%', background: '#14b8a6'}"></div></div>
            <span class="quality-pct">{{ segmentedPct }}%</span>
          </div>
          <div class="quality-row">
            <span class="quality-label">累计消费金额</span>
            <div class="quality-bar-wrap"><div class="quality-bar" :style="{width: consumePct + '%', background: '#f59e0b'}"></div></div>
            <span class="quality-pct">¥{{ fmtNum(overview.totalPaymentAmount || 0) }}</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { fetchOverview, fetchSegmentDistribution } from '@/api/profile'
import { useUserStore } from '@/store/modules/user'
import request from '@/utils/http'
import { taskTypeLabel, taskStatusLabel } from '@/utils/taskDict'
import { renderMarkdown } from '@/utils/markdown'

defineOptions({ name: 'AdminDashboard' })

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const healthOk = ref(false)
const unreadCount = ref(0)
const overview = ref<any>({})
const segments = ref<any[]>([])
const tasks = ref<any[]>([])
const trend = ref<any[]>([])

const trendChart = ref<HTMLElement>()
const taskChart = ref<HTMLElement>()
const segmentChart = ref<HTMLElement>()
const charts: echarts.ECharts[] = []

/* ─── 状态栏（真实） ─── */
const runningCount = computed(() => tasks.value.filter(t => t.taskStatus === 'Running' || t.taskStatus === 'Pending').length)
const statusItems = computed(() => [
  { label: '系统状态', val: healthOk.value ? '运行中' : '异常', color: healthOk.value ? '#10b981' : '#ef4444' },
  { label: 'MySQL', val: healthOk.value ? '已连接' : '连接失败', color: healthOk.value ? '#10b981' : '#ef4444' },
  { label: '运行中任务', val: String(runningCount.value), color: runningCount.value ? '#2563eb' : '#94a3b8' },
  { label: '未读通知', val: String(unreadCount.value), color: unreadCount.value ? '#f59e0b' : '#94a3b8' }
])

/* ─── KPI（真实画像统计，raw 供数字滚动动画） ─── */
function fmtNum(n: number) { return n >= 10000 ? (n / 10000).toFixed(1) + '万' : n.toLocaleString() }
const kpiEls: (HTMLElement | null)[] = []
function setKpiRef(el: any, i: number) { if (el) kpiEls[i] = el as HTMLElement }
/** 数字滚动动画（轻量动效） */
function animateNumber(el: HTMLElement, to: number) {
  const dur = 700, t0 = performance.now()
  const step = (t: number) => {
    const p = Math.min(1, (t - t0) / dur)
    el.textContent = fmtNum(Math.round(to * (1 - Math.pow(1 - p, 3))))
    if (p < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}
/** 导出运营快照：当前 KPI + 分层 + 任务统计 + 活跃趋势摘要 → 文本文件（汇报/存档用） */
function exportSnapshot() {
  const seg = segments.value.map(s => `${s.segmentName || s.segmentCode} ${s.userCount}人`).join('、')
  const tr = trend.value
  const trendInfo = tr && tr.length
    ? `近30日活跃趋势：${tr[0]?.day} 活跃 ${tr[0]?.activeCount} 人 → ${tr[tr.length - 1]?.day} 活跃 ${tr[tr.length - 1]?.activeCount} 人`
    : '近30日活跃趋势：暂无数据'
  const okTasks = tasks.value.filter(t => t.taskStatus === 'Succeeded').length
  const failTasks = tasks.value.filter(t => t.taskStatus === 'Failed').length
  const lines = [
    '=== 运营数据快照 ===',
    `导出时间：${new Date().toLocaleString()}`,
    '',
    `总用户数：${overview.value.totalUsers || 0}`,
    `已画像用户：${overview.value.profiledUsers || 0}（覆盖率 ${coveragePct.value}%）`,
    `高价值用户：${overview.value.highValueUsers || 0}`,
    `累计消费金额：¥${fmtNum(overview.value.totalPaymentAmount || 0)}`,
    `用户分层：${seg || '暂无数据'}`,
    trendInfo,
    `任务统计：共 ${tasks.value.length} 个（成功 ${okTasks}，失败 ${failTasks}）`,
    '',
    '（由运营数据总览自动生成）'
  ]
  const blob = new Blob(['\uFEFF' + lines.join('\n')], { type: 'text/plain;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `运营快照_${new Date().toISOString().slice(0, 10)}.txt`
  a.click()
  URL.revokeObjectURL(a.href)
}

const kpis = computed(() => [
  { label: '总用户数', raw: overview.value.totalUsers || 0, prefix: '', suffix: '', bg: '#eff6ff', fg: '#2563eb', path: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>' },
  { label: '已画像用户', raw: overview.value.profiledUsers || 0, prefix: '', suffix: '', bg: '#eff6ff', fg: '#1e40af', path: '<circle cx="12" cy="12" r="10"/><path d="M12 8v4l3 3"/>' },
  { label: '高价值用户', raw: overview.value.highValueUsers || 0, prefix: '', suffix: '', bg: '#fdf4ff', fg: '#a21caf', path: '<path d="M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 22 12 18.56 5.82 22 7 14.14 2 9.27l6.91-1.01z"/>' },
  { label: '累计消费金额', raw: overview.value.totalPaymentAmount || 0, prefix: '¥', suffix: '', bg: '#fefce8', fg: '#ca8a04', path: '<rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/>' },
  { label: '画像覆盖率', raw: coveragePct.value, prefix: '', suffix: '%', bg: '#f0fdf4', fg: '#16a34a', path: '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="M22 4L12 14.01l-3-3"/>' }
])

/* ─── AI 经营解读（洞察引导） ─── */
const insightText = ref('')
const insightLoading = ref(false)
/** 解读结果会话级缓存：key 绑定登录用户，退出登录即清除（见 userStore.logOut） */
const INSIGHT_CACHE_KEY = () => {
  const uid = userStore.info?.userId
  return `ai_insight_cache_${uid ?? 'anon'}`
}
async function genInsight() {
  insightLoading.value = true
  const segTop = segments.value.slice(0, 3).map(s => `${s.segmentName || s.segmentCode} ${s.userCount}人`).join('、')
  const tr = trend.value
  const trendInfo = tr && tr.length
    ? `近30日活跃趋势：${tr[0]?.day} 活跃 ${tr[0]?.activeCount} 人 → ${tr[tr.length - 1]?.day} 活跃 ${tr[tr.length - 1]?.activeCount} 人`
    : '近30日活跃趋势：暂无数据'
  const okTasks = tasks.value.filter(t => t.taskStatus === 'Succeeded').length
  const failTasks = tasks.value.filter(t => t.taskStatus === 'Failed').length
  const prompt = '你是电商运营数据分析助手。请基于以下运营数据给出 3-5 条简明经营解读与行动建议（面向运营人员，避免术语堆砌，每条一行，以「。」结尾）：\n'
    + `- 总用户数 ${overview.value.totalUsers || 0}，已画像 ${overview.value.profiledUsers || 0}（覆盖率 ${coveragePct.value}%）\n`
    + `- 高价值用户 ${overview.value.highValueUsers || 0}（占比 ${highValuePct.value}%）\n`
    + `- 累计消费金额 ¥${overview.value.totalPaymentAmount || 0}\n`
    + `- ${trendInfo}\n`
    + `- 用户分层：${segTop || '暂无数据'}\n`
    + `- 任务统计：共 ${tasks.value.length} 个（成功 ${okTasks}，失败 ${failTasks}）`
  try {
    // DeepSeek 流式响应总时长可能超过 2 分钟（后端 readTimeout 为读间隔不受限），前端总时长放宽到 300s
    const resp = await request.post<any>({ url: '/api/v1/ai/chat', data: { messages: [{ role: 'user', content: prompt }] }, timeout: 300000, showErrorMessage: false })
    insightText.value = resp?.answer || '解读生成完成，但未返回内容。'
    try { localStorage.setItem(INSIGHT_CACHE_KEY(), JSON.stringify({ text: insightText.value })) } catch { /* 忽略 */ }
  } catch {
    insightText.value = '⚠️ 解读生成失败（可能超时或服务暂时不可用），请稍后重试。'
  } finally { insightLoading.value = false }
}
/** 恢复上次解读（仅同一登录用户会话内恢复；不同用户/退出登录后不显示） */
function restoreInsight() {
  try {
    const raw = localStorage.getItem(INSIGHT_CACHE_KEY())
    if (raw) {
      const cache = JSON.parse(raw)
      if (cache?.text) insightText.value = cache.text
    }
  } catch { /* 忽略 */ }
}

/* ─── 数据概况（真实占比） ─── */
const coveragePct = computed(() => {
  const t = overview.value.totalUsers || 0
  return t > 0 ? Math.round(((overview.value.profiledUsers || 0) / t) * 100) : 0
})
const highValuePct = computed(() => {
  const t = overview.value.totalUsers || 0
  return t > 0 ? Math.round(((overview.value.highValueUsers || 0) / t) * 100) : 0
})
const segmentedPct = computed(() => {
  const total = segments.value.reduce((s: number, x: any) => s + (x.userCount || 0), 0)
  const t = overview.value.totalUsers || 0
  return t > 0 ? Math.round((Math.min(total, t) / t) * 100) : 0
})
const consumePct = computed(() => {
  const t = overview.value.totalPaymentAmount || 0
  return t > 0 ? Math.min(100, Math.round(t / 10000)) : 0
})

/* ─── 最近任务 ─── */
const recentTasks = computed(() => tasks.value.slice(0, 8))
function st(s: string) { return s === 'Succeeded' ? 'success' : s === 'Running' ? 'running' : s === 'Failed' ? 'failed' : s === 'Cancelled' ? 'cancelled' : 'pending' }
function formatTime(t: string) {
  if (!t) return '—'
  const d = new Date(t)
  return `${d.getMonth() + 1}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

/* ─── 数据加载 ─── */
async function loadOverview() {
  try { const r = await fetchOverview(); overview.value = r || {} } catch { /* 保留旧值 */ }
  // KPI 数字滚动动画（仅数值变化时播放，避免轮询时反复闪烁）
  nextTick(() => kpis.value.forEach((k, i) => {
    const el = kpiEls[i]
    if (el && el.textContent !== fmtNum(k.raw)) animateNumber(el, k.raw)
  }))
}
async function loadSegments() {
  try { const r = await fetchSegmentDistribution(); segments.value = r || [] } catch { /* 保留旧值 */ }
}
async function loadTrend() {
  try { const r = await request.get<any[]>({ url: '/api/v1/public/active-trend', showErrorMessage: false }); trend.value = r || [] } catch { /* 保留旧值 */ }
}
async function loadTasks() {
  try {
    const r = await request.get<any>({ url: '/api/v1/admin/analysis-tasks', params: { page: 0, size: 20 }, showErrorMessage: false })
    tasks.value = r?.records || []
    checkAndStopPolling()
  } catch { /* 保留旧值 */ }
}
async function loadMeta() {
  try { const h = await request.get<any>({ url: '/api/v1/system/health', showErrorMessage: false }); healthOk.value = !!(h && h.status === 'UP') } catch { healthOk.value = false }
  try { const n = await request.get<any>({ url: '/api/v1/notifications/unread-count', showErrorMessage: false }); unreadCount.value = Number(n || 0) } catch { /* 忽略 */ }
}

/* ─── 任务轮询：有运行/待处理任务时每 10s 刷新 ─── */
let pollTimer: any = null
function startPolling() { stopPolling(); pollTimer = setInterval(loadTasks, 10000) }
function stopPolling() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }
function checkAndStopPolling() { if (runningCount.value === 0 && pollTimer) stopPolling() }

/* ─── 图表渲染 ─── */
const CHART_GRAY = '#94a3b8'
function initCharts() {
  charts.forEach(c => c.dispose()); charts.length = 0
  const tooltip = { trigger: 'axis' as const }
  const grid = { left: 40, right: 12, top: 10, bottom: 24 }

  // 近30天活跃趋势（真实）
  if (trendChart.value) {
    const c = echarts.init(trendChart.value); charts.push(c)
    const days = trend.value.map((d: any) => String(d.day || '').slice(5))
    const counts = trend.value.map((d: any) => Number(d.activeCount || 0))
    c.setOption({
      tooltip,
      grid,
      xAxis: { type: 'category', data: days, axisLabel: { fontSize: 10, color: CHART_GRAY }, axisLine: { show: false } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { fontSize: 10, color: CHART_GRAY } },
      series: [{
        name: '活跃用户', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5, data: counts,
        lineStyle: { color: '#2563eb', width: 2 },
        itemStyle: { color: '#2563eb' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(37,99,235,0.18)' }, { offset: 1, color: 'rgba(37,99,235,0.02)' }]) }
      }]
    })
  }

  // 任务状态分布（真实统计）
  if (taskChart.value) {
    const c = echarts.init(taskChart.value); charts.push(c)
    const count = (s: string) => tasks.value.filter(t => t.taskStatus === s).length
    const labels = ['成功', '失败', '运行中', '待处理', '已取消']
    const colors = ['#10b981', '#ef4444', '#2563eb', '#f59e0b', '#94a3b8']
    const statusCodes = ['Succeeded', 'Failed', 'Running', 'Pending', 'Cancelled']
    const values = statusCodes.map(s => count(s))
    const maxV = Math.max(...values, 1)
    c.setOption({
      tooltip: { trigger: 'axis', formatter: (p: any) => `${p[0].name}：${p[0].value} 个任务` },
      grid: { left: 40, right: 12, top: 30, bottom: 24 },
      xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 10, color: CHART_GRAY }, axisLine: { show: false } },
      yAxis: { type: 'value', max: Math.ceil(maxV * 1.2), splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { fontSize: 10, color: CHART_GRAY } },
      series: [{
        name: '任务数', type: 'bar', barWidth: 26, data: values.map((v, i) => ({ value: v, itemStyle: { color: colors[i], borderRadius: [4, 4, 0, 0] } })),
        label: { show: true, position: 'top', fontSize: 11, color: '#334155', formatter: (p: any) => p.value > 0 ? String(p.value) : '0' }
      }]
    })
    // 点击状态条 → 跳转任务列表并预选该状态
    c.on('click', (p: any) => { router.push(`/tasks?taskStatus=${statusCodes[p.dataIndex]}`) })
  }

  // 用户分层分布（真实）
  if (segmentChart.value) {
    const c = echarts.init(segmentChart.value); charts.push(c)
    const data = segments.value.map((s: any, i: number) => ({
      name: s.segmentName || s.segmentCode || '未分类',
      value: s.userCount || 0,
      itemStyle: { color: ['#8b5cf6', '#2563eb', '#10b981', '#f59e0b', '#ef4444', '#94a3b8'][i % 6] }
    }))
    c.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
      legend: { bottom: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 10, color: CHART_GRAY } },
      series: [{
        type: 'pie', radius: ['35%', '68%'], center: ['50%', '44%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
        label: { fontSize: 11, color: '#334155', formatter: '{b} {d}%' },
        data: data.length ? data : [{ name: '暂无数据', value: 1, itemStyle: { color: '#e2e8f0' } }]
      }]
    })
    // 画像列表查看归属用户门户（User 端），管理端运营总览不再提供分层钻取跳转
  }
}

const handleResize = () => charts.forEach(c => c.resize())

onMounted(async () => {
  loading.value = true
  await Promise.allSettled([loadMeta(), loadOverview(), loadSegments(), loadTrend(), loadTasks()])
  await nextTick()
  initCharts()
  loading.value = false
  restoreInsight()
  // 有进行中任务时开启轮询
  if (runningCount.value > 0) startPolling()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  stopPolling()
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
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

/* ─── Variables ─── */
.admin-dash {
  --c-cyan: #2563eb; --c-teal: #14b8a6; --c-green: #10b981;
  --c-amber: #f59e0b; --c-red: #ef4444;
  font-family: 'Inter', 'PingFang SC', system-ui, sans-serif;
  padding: 4px;
}

/* ─── Status Bar ─── */
.status-bar { display: flex; gap: 24px; margin-bottom: 20px; padding: 13px 20px; background: #fff; border: 1px solid rgba(15,23,42,0.06); border-radius: 14px; flex-wrap: wrap; box-shadow: 0 2px 10px rgba(15,23,42,0.03); }
.status-item { display: flex; align-items: center; gap: 6px; font-size: 12.5px; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; }
.status-label { color: #94a3b8; }
.status-value { color: #334155; font-weight: 600; }

/* ─── KPI ─── */
.kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-bottom: 14px; }
.kpi-card { display: flex; align-items: center; gap: 12px; background: #fff; border: 1px solid rgba(15,23,42,0.06); border-radius: 14px; padding: 16px 18px; transition: all .35s cubic-bezier(0.32,0.72,0,1); box-shadow: 0 4px 16px rgba(15,23,42,0.04); }
.kpi-card--action { justify-content: center; border-style: dashed; background: rgba(255,255,255,0.6); }
.snapshot-btn { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 600; color: #475569; background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 10px 16px; cursor: pointer; transition: all .2s; }
.snapshot-btn:hover { color: #2563eb; border-color: #2563eb; box-shadow: 0 4px 14px rgba(37,99,235,.12); }
.kpi-card:hover { box-shadow: 0 12px 30px rgba(15,23,42,0.08); transform: translateY(-2px); }
.kpi-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.kpi-body { display: flex; flex-direction: column; min-width: 0; }
.kpi-val { font-size: 20px; font-weight: 700; color: #0f172a; line-height: 1.2; }
.kpi-label { font-size: 11.5px; color: #94a3b8; margin-top: 2px; }

/* ─── AI 经营解读 ─── */
.insight-card { margin-bottom: 14px; }
.insight-btn { display: inline-flex; align-items: center; gap: 6px; height: 30px; padding: 0 14px; border-radius: 8px; border: none; background: linear-gradient(135deg, #8b5cf6, #7c3aed); color: #fff; font-size: 12px; font-weight: 600; cursor: pointer; font-family: inherit; transition: all .2s; box-shadow: 0 2px 8px rgba(139,92,246,.25); }
.insight-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(139,92,246,.35); }
.insight-btn:disabled { opacity: .6; cursor: not-allowed; }
.insight-loading { display: inline-flex; align-items: center; gap: 6px; }
.insight-loading::before { content: ''; width: 11px; height: 11px; border-radius: 50%; border: 2px solid rgba(255,255,255,.4); border-top-color: #fff; animation: insight-spin .7s linear infinite; }
@keyframes insight-spin { to { transform: rotate(360deg); } }
.insight-body { font-size: 12.5px; line-height: 1.9; color: #475569; background: #faf8ff; border: 1px solid #ede9fe; border-radius: 8px; padding: 12px 14px; white-space: pre-wrap; max-height: 260px; overflow-y: auto; }
.insight-empty { font-size: 12px; color: #94a3b8; background: #f8fafc; border: 1px dashed #e2e8f0; border-radius: 8px; padding: 12px 14px; }

/* ─── Chart Grid ─── */
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 14px; }
.monitor-card { background: #fff; border: 1px solid rgba(15,23,42,0.06); border-radius: 16px; padding: 18px 20px; box-shadow: 0 6px 22px rgba(15,23,42,0.04); transition: box-shadow .35s cubic-bezier(0.32,0.72,0,1), transform .35s cubic-bezier(0.32,0.72,0,1); }
.monitor-card.wide { grid-column: 1 / -1; }
.monitor-card:hover { box-shadow: 0 14px 36px rgba(15,23,42,0.07); }
.monitor-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.monitor-head h3 { font-size: 13.5px; font-weight: 600; color: #334155; margin: 0; display: flex; align-items: center; gap: 8px; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; }
.neo-dot { width: 8px; height: 8px; border-radius: 2px; background: #8b5cf6; display: inline-block; }
.card-badge { font-size: 10.5px; padding: 2px 8px; border-radius: 20px; font-weight: 500; }
.card-badge.green { background: #f0fdf4; color: #10b981; }
.card-badge.red { background: #fef2f2; color: #ef4444; }
.card-link { font-size: 11.5px; color: #2563eb; text-decoration: none; }
.card-link:hover { text-decoration: underline; }

/* ─── Task List ─── */
.task-list { display: flex; flex-direction: column; }
.task-row { display: flex; align-items: center; gap: 10px; padding: 8px 2px; border-bottom: 1px solid #f1f5f9; font-size: 12.5px; }
.task-row:last-child { border-bottom: none; }
.task-row:hover { background: #f8fafc; border-radius: 6px; }
.task-name { flex: 1; color: #334155; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-type { font-size: 10.5px; color: #94a3b8; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 4px; padding: 1px 6px; }
.task-status { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 20px; flex-shrink: 0; }
.st-success { background: #f0fdf4; color: #10b981; }
.st-failed { background: #fef2f2; color: #ef4444; }
.st-running { background: #eff6ff; color: #2563eb; }
.st-pending { background: #fffbeb; color: #f59e0b; }
.st-cancelled { background: #f8fafc; color: #94a3b8; }
.task-time { font-size: 11px; color: #94a3b8; font-family: 'JetBrains Mono', monospace; flex-shrink: 0; }
.empty-tip { padding: 18px 0; text-align: center; color: #94a3b8; font-size: 12.5px; }

/* ─── Dual Grid ─── */
.dual-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }

/* ─── Quality ─── */
.quality-list { display: flex; flex-direction: column; gap: 12px; }
.quality-row { display: flex; align-items: center; gap: 10px; font-size: 12.5px; }
.quality-label { width: 90px; color: #475569; flex-shrink: 0; }
.quality-bar-wrap { flex: 1; height: 8px; background: #f1f5f9; border-radius: 4px; overflow: hidden; }
.quality-bar { height: 100%; border-radius: 4px; transition: width .4s; }
.quality-pct { width: 110px; text-align: right; color: #475569; font-weight: 600; font-size: 12px; flex-shrink: 0; }

@media (max-width: 1100px) {
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-grid, .dual-grid { grid-template-columns: 1fr; }
}
</style>
