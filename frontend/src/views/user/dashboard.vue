<!-- User Dashboard — 数据分析控制台：简洁问候+KPI+趋势+快捷+最近+AI辅助 -->
<template>
  <div class="dash" v-loading="loading">
    <!-- 问候区 + 数据新鲜度（合并一行） -->
    <header class="dash-greeting">
      <div class="greet-left">
        <h1 class="greeting-text">{{ greetingText }}，{{ userStore.info?.displayName || userStore.info?.username }}</h1>
        <p class="greeting-sub">
          欢迎回到用户画像平台。
          <template v-if="statDate">画像数据统计截止 <strong>{{ statDate }}</strong><span class="greet-sep">·</span>版本 {{ dataVersion }}</template>
        </p>
      </div>
      <span class="greet-date">{{ todayStr }}</span>
    </header>

    <!-- KPI 行（白卡，可点击钻取；覆盖率带进度环、日活跃带环比） -->
    <div class="kpi-grid">
      <!-- 3 个普通 KPI：图标 + 数字 + 环比 -->
      <div v-for="(k, i) in otherKpis" :key="k.label" class="kpi-card kpi-card--click" :title="k.tip" @click="kpiClick(k.idx)">
        <div class="kpi-icon" :style="{background:k.bg,color:k.fg}" v-html="k.icon"></div>
        <div class="kpi-body">
          <span class="kpi-val">
            {{ k.val }}
            <span v-if="k.delta != null" class="kpi-delta" :class="k.delta >= 0 ? 'delta-up' : 'delta-down'">
              {{ k.delta >= 0 ? '↑' : '↓' }}{{ Math.abs(k.delta).toFixed(1) }}%
            </span>
          </span>
          <span class="kpi-label">{{ k.label }}</span>
          <span class="kpi-meta">{{ k.meta }}</span>
        </div>
      </div>

      <!-- 覆盖率用环形进度（差异化视觉，节省空间） -->
      <div class="kpi-card kpi-card--click kpi-card--ring" :title="kpis[1].tip" @click="kpiClick(1)">
        <div class="kpi-ring" :style="`--pct:${kpis[1].pct ?? 0}%`">
          <span class="ring-num">{{ kpis[1].val }}</span>
        </div>
        <div class="kpi-body">
          <span class="kpi-val">
            已分析 <strong>{{ profiledUsers.toLocaleString() }}</strong> / {{ totalUsers.toLocaleString() }}
          </span>
          <span class="kpi-label">{{ kpis[1].label }}</span>
          <span class="kpi-meta">{{ kpis[1].meta }}</span>
        </div>
      </div>
    </div>

    <!-- 增长趋势（全宽，时间范围可切换；活跃数 + 高价值对比线） -->
    <section class="panel trend-card">
      <div class="card-hd">
        <h2>用户增长趋势</h2>
        <div class="hd-right">
          <div class="range-chips">
            <button v-for="r in trendRanges" :key="r.days" class="range-chip" :class="{ 'range-chip--active': trendDays === r.days }" @click="setTrendDays(r.days)">{{ r.label }}</button>
          </div>
          <div class="legend-inline"><span class="legend-dot dot--teal"></span>活跃用户<span class="legend-dot dot--blue"></span>高价值用户</div>
        </div>
      </div>
      <div ref="lineChart" class="chart-box" style="height:260px"></div>
    </section>

    <!-- 三栏：快捷 + 待办预警 + AI -->
    <div class="triple-row">
      <!-- 快捷入口 -->
      <section class="panel">
        <div class="card-hd"><h2>快捷入口</h2></div>
        <div class="quick-grid">
          <router-link v-for="q in quickLinks" :key="q.to" :to="q.to" class="quick-cell">
            <span class="quick-icon" v-html="q.icon"></span>
            <span class="quick-body">
              <span class="quick-label">{{ q.label }}</span>
              <span class="quick-desc">{{ q.desc }}</span>
            </span>
          </router-link>
        </div>
      </section>

      <!-- 待办 / 预警（替代原"最新注册"，行动导向） -->
      <section class="panel">
        <div class="card-hd"><h2>待办与预警</h2><router-link to="/user/notifications" class="card-link">全部</router-link></div>
        <div class="todo-strip">
          <router-link v-if="unreadCount > 0" to="/user/notifications" class="todo-row todo-row--amber">
            <span class="todo-icon">!</span>
            <span class="todo-body"><span class="todo-title">{{ unreadCount }} 条未读通知</span><span class="todo-desc">点击查看详情</span></span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </router-link>
          <router-link v-if="atRiskUsers > 0" to="/user/profiles?segment=AT_RISK" class="todo-row todo-row--red">
            <span class="todo-icon">⚠</span>
            <span class="todo-body"><span class="todo-title">{{ fmtNum(atRiskUsers) }} 位流失风险用户</span><span class="todo-desc">RFM 分层 AT_RISK，建议召回</span></span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </router-link>
          <router-link v-if="statDate" to="/user/overview" class="todo-row todo-row--teal">
            <span class="todo-icon">✓</span>
            <span class="todo-body"><span class="todo-title">画像数据已更新</span><span class="todo-desc">截止 {{ statDate }} · 版本 {{ dataVersion }}</span></span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </router-link>
          <div v-if="unreadCount === 0 && atRiskUsers === 0" class="empty-tip">暂无待办事项</div>
        </div>
      </section>

      <!-- AI 辅助（紧凑，非主视觉） -->
      <section class="panel ai-card">
        <div class="card-hd">
          <h2>AI 分析助手</h2>
          <span class="badge badge--blue">辅助</span>
        </div>
        <p class="ai-tip">输入问题，AI 将结合画像数据为您分析。</p>
        <div class="ai-input-line">
          <input v-model="aiInput" class="ai-input" placeholder="如：高价值用户特征？" @keydown.enter.prevent="goAi" />
          <button class="ai-send" :disabled="!aiInput.trim()" @click="goAi">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
          </button>
        </div>
        <router-link to="/user/ai" class="ai-link">进入 AI 分析页 →</router-link>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import * as echarts from 'echarts'
import { fetchOverview } from '@/api/profile'
import { fetchNotifications } from '@/api/notification'
import request from '@/utils/http'

defineOptions({ name: 'UserDashboard' })

const router = useRouter(); const userStore = useUserStore()
const loading = ref(false); const unreadCount = ref(0)
const aiInput = ref('')
const lineChart = ref<HTMLElement>()
const charts: echarts.ECharts[] = []; const notifications = ref<any[]>([])
/** 数据新鲜度 + 待办预警 */
const statDate = ref(''); const dataVersion = ref(''); const atRiskUsers = ref(0)
/** 覆盖率 KPI 数字明细（用于环内/卡内展示） */
const totalUsers = ref(0); const profiledUsers = ref(0)
/** 趋势范围：7 / 30 / 90 天 */
const trendDays = ref(30)
const trendRanges = [
  { days: 7, label: '近7天' },
  { days: 30, label: '近30天' },
  { days: 90, label: '近90天' }
]

/** 按时间段问候 */
const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 5) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const todayStr = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

function fmtNum(n: any) { return Number(n || 0).toLocaleString() }

/** AI 输入 → 跳转 AI 分析页并自动提问 */
function goAi() {
  const q = aiInput.value.trim()
  if (!q) return
  router.push({ path: '/user/ai', query: { q } })
}

const kpis = ref([
  { label:'总用户数', val:'—', meta:'电商平台注册总量', tip:'查看全部画像用户', delta: null as number | null,
    icon:'<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>', bg:'#f0fdfa', fg:'#0d9488' },
  { label:'画像覆盖率', val:'—', meta:'已分析/总用户', tip:'前往画像概览', pct: null as number | null,
    icon:'<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>', bg:'#f0fdf4', fg:'#059669' },
  { label:'高价值用户', val:'—', meta:'RFM 高价值分层', tip:'查看高价值分层用户', delta: null as number | null,
    icon:'<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>', bg:'#fffbeb', fg:'#d97706' },
  { label:'日活跃用户', val:'—', meta:'今日登录用户', tip:'查看用户增长趋势', delta: null as number | null,
    icon:'<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>', bg:'#f0fdfa', fg:'#0d9488' }
])

/** KPI 钻取 */
function kpiClick(i: number) {
  if (i === 0) router.push('/user/profiles')                       // 总用户 → 画像列表
  else if (i === 1) router.push('/user/overview')                  // 覆盖率 → 概览
  else if (i === 2) router.push({ path: '/user/profiles', query: { segment: 'HIGH_VALUE' } }) // 高价值 → 筛分层
  else if (i === 3) window.scrollTo({ top: 400, behavior: 'smooth' }) // 日活跃 → 滚动到趋势图
}

/** 除"覆盖率"外的 3 个 KPI（用 idx 保持 kpiClick 一致） */
const otherKpis = computed(() => [
  { ...kpis.value[0], idx: 0 },
  { ...kpis.value[2], idx: 2 },
  { ...kpis.value[3], idx: 3 }
])

const quickLinks = [
  { label:'画像概览', desc:'全平台数据全貌', to:'/user/overview',
    icon:'<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>' },
  { label:'画像列表', desc:'查看用户画像明细', to:'/user/profiles',
    icon:'<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>' },
  { label:'标签洞察', desc:'标签分布与占比', to:'/user/tags',
    icon:'<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>' },
  { label:'AI 分析', desc:'自然语言查询数据', to:'/user/ai',
    icon:'<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 2a4 4 0 0 1 4 4c0 2-2 4-4 4a4 4 0 0 1-4-4c0-2.21 1.79-4 4-4z"/><path d="M2 22v-2a6 6 0 0 1 6-6h8a6 6 0 0 1 6 6v2"/><circle cx="17" cy="9" r="2"/><path d="M21 22v-2a4 4 0 0 0-3-3.87"/><path d="M7 22v-2a4 4 0 0 1 3-3.87"/></svg>' },
]

/** 趋势范围切换：重拉数据并重绘 */
function setTrendDays(days: number) {
  trendDays.value = days
  loadTrend()
}
async function loadTrend() {
  const res = await request.get<any[]>({ url: `/api/v1/public/active-trend?days=${trendDays.value}`, showErrorMessage: false }).catch(() => [])
  const trend = (res as any[]) || []
  await nextTick(); renderCharts(trend)
}

function renderCharts(trend: any[]) {
  charts.forEach(c => c.dispose()); charts.length = 0
  if (lineChart.value) {
    const c = echarts.init(lineChart.value); charts.push(c)
    c.setOption({
      tooltip: { trigger:'axis', formatter: (params: any) => {
        const p = Array.isArray(params) ? params : [params]
        const d = trend[p[0]?.dataIndex]?.day
        const lines = p.map((x: any) => `${x.marker}${x.seriesName}：${x.value}`).join('<br/>')
        return d ? `${d}<br/>${lines}` : lines
      } },
      grid: { left:40, right:12, top:8, bottom:20 },
      xAxis: { type:'category', data:trend.map((d: any) => {
        if (d?.day) {
          const date = String(d.day)
          return date.length >= 10 ? `${date.slice(5, 7)}/${date.slice(8, 10)}` : date
        }
        return `${trend.indexOf(d) + 1}日`
      }), axisLabel:{ fontSize:10, color:'#94a3b8' }, axisLine:{ show:false }, axisTick:{ show:false } },
      yAxis: { type:'value', splitLine:{ lineStyle:{ color:'#f1f5f9' } }, axisLabel:{ fontSize:10, color:'#94a3b8' } },
      series: [
        { name:'活跃用户', type:'line', smooth:true, data:trend.map((d:any)=>Number(d.activeCount||0)), symbol:'none',
          lineStyle:{ color:'#0d9488', width:2 },
          areaStyle:{ color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(13,148,136,0.12)'},{offset:1,color:'rgba(13,148,136,0)'}]) } },
        { name:'高价值用户', type:'line', smooth:true, data:trend.map((d:any)=>Number(d.highValueCount||0)), symbol:'none',
          lineStyle:{ color:'#3b82f6', width:2, type:'dashed' } }
      ]
    })
  }
}

async function loadData() {
  loading.value = true
  try {
    const [ovR, trR, ntR] = await Promise.allSettled([
      fetchOverview(),
      request.get<any[]>({ url:`/api/v1/public/active-trend?days=${trendDays.value}`, showErrorMessage:false }),
      fetchNotifications({ page:0, size:5 })
    ])
    if (ovR.status === 'fulfilled' && ovR.value) {
      const o = ovR.value as any; const t = o.totalUsers||0; const p = o.profiledUsers||0
      kpis.value[0].val = t.toLocaleString(); kpis.value[1].val = t > 0 ? Math.round(p/t*100)+'%' : '0%'
      kpis.value[2].val = (o.highValueUsers||0).toLocaleString()
      // 覆盖率进度环（0-100）+ 数字明细（环外展示已分析/总）
      kpis.value[1].pct = t > 0 ? Math.min(100, Math.round(p/t*100)) : 0
      totalUsers.value = t; profiledUsers.value = p
      // 待办预警：流失风险用户规模
      atRiskUsers.value = Number(o.atRiskUsers || 0)
      // 数据新鲜度：画像数据版本与统计截止
      dataVersion.value = o.dataVersion || ''
      statDate.value = o.calculatedAt ? String(o.calculatedAt).slice(0, 10) : ''
    }
    // 日活跃 = active-trend 最后一天；环比 = 最后两天对比；高价值占比/环比
    if (trR.status === 'fulfilled' && Array.isArray(trR.value) && trR.value.length) {
      const tr = trR.value
      const last = tr[tr.length - 1]
      kpis.value[3].val = (Number(last?.activeCount) || 0).toLocaleString()
      const prev = tr.length >= 2 ? tr[tr.length - 2] : null
      const cur = Number(last?.activeCount) || 0
      const old = Number(prev?.activeCount) || 0
      kpis.value[3].delta = old > 0 ? ((cur - old) / old * 100) : null
      // 高价值用户环比：近30天最后一周均值 vs 前一周均值（避免单日波动）
      if (tr.length >= 14) {
        const hvArr = tr.map((d: any) => Number(d.highValueCount || 0))
        const sum = (arr: number[]) => arr.reduce((a, b) => a + b, 0)
        const w2 = sum(hvArr.slice(-14, -7)) / 7
        const w1 = sum(hvArr.slice(-7)) / 7
        kpis.value[2].delta = w2 > 0 ? ((w1 - w2) / w2 * 100) : null
      }
    }
    if (ntR.status === 'fulfilled' && ntR.value) {
      notifications.value = (ntR.value as any).records || []
      unreadCount.value = notifications.value.filter((n:any)=>!n.isRead).length
    }
    const trend = trR.status === 'fulfilled' ? (trR.value as any[])||[] : []
    await nextTick(); renderCharts(trend)
  } catch {} finally { loading.value = false }
}
const handleResize = () => charts.forEach(c => c.resize())
onMounted(() => { loadData(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); charts.forEach(c => c.dispose()) })
</script>

<style scoped>
.dash { font-family: 'Inter','PingFang SC',system-ui,sans-serif; }

/* ─── 问候区（含数据新鲜度） ─── */
.dash-greeting { display:flex; align-items:flex-end; justify-content:space-between; gap:16px; margin-bottom:22px; }
.greet-left { min-width:0; }
.greeting-text { font-size:21px; font-weight:700; color:#0f172a; margin:0 0 5px; letter-spacing:-.3px; }
.greeting-sub { font-size:13px; color:#64748b; margin:0; line-height:1.6; }
.greeting-sub strong { color:#0d9488; font-weight:600; }
.greet-sep { color:#d4d8dd; margin:0 4px; }
.greet-date { font-size:12px; color:#94a3b8; flex-shrink:0; }

/* ─── KPI ─── */
.kpi-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:14px; margin-bottom:18px; }
@media(max-width:900px){ .kpi-grid{grid-template-columns:repeat(2,1fr)} }

.kpi-card {
  display:flex; align-items:center; gap:13px;
  background:#fff; border:1px solid rgba(15,23,42,0.06); border-radius:16px;
  box-shadow:0 8px 24px rgba(13,148,136,0.05), 0 1px 3px rgba(15,23,42,0.03);
  padding:17px 18px; transition:box-shadow .35s cubic-bezier(0.32,0.72,0,1), transform .35s cubic-bezier(0.32,0.72,0,1);
}
.kpi-card--click { cursor:pointer; }
.kpi-card--click:hover { border-color:#99f6e4; box-shadow:0 14px 34px rgba(13,148,136,.12), 0 2px 6px rgba(15,23,42,.03); transform:translateY(-2px); }

.kpi-icon { width:42px; height:42px; display:flex; align-items:center; justify-content:center; border-radius:10px; flex-shrink:0; }
.kpi-val { font-size:20px; font-weight:700; color:#0f172a; font-family:'JetBrains Mono',monospace; letter-spacing:-.3px; display:block; line-height:1.2; }
.kpi-label { font-size:12.5px; font-weight:500; color:#475569; }
.kpi-meta { font-size:11px; color:#94a3b8; }

/* KPI 环比涨跌 */
.kpi-delta { font-size:10.5px; font-weight:600; font-family:'Inter','PingFang SC',sans-serif; margin-left:6px; letter-spacing:0; vertical-align:1px; }
.delta-up { color:#0d9488; }
.delta-down { color:#dc2626; }

/* KPI 覆盖率环形进度（conic-gradient 画环，节省横向空间） */
.kpi-card--ring { padding:16px 18px; }
.kpi-card--ring .kpi-ring {
  --pct: 0%;
  width:64px; height:64px; flex-shrink:0;
  border-radius:50%;
  background:
    conic-gradient(#0d9488 calc(var(--pct) * 1), #f1f5f9 0);
  display:flex; align-items:center; justify-content:center;
  position:relative;
}
.kpi-card--ring .kpi-ring::before {
  content:''; position:absolute; inset:5px; border-radius:50%; background:#fff;
}
.kpi-card--ring .ring-num {
  position:relative; font-size:14px; font-weight:700; color:#0f172a;
  font-family:'JetBrains Mono',monospace; letter-spacing:-.3px;
}
.kpi-card--ring .kpi-body { min-width:0; }
.kpi-card--ring .kpi-val { font-size:14px; font-weight:500; color:#475569; font-family:inherit; letter-spacing:0; line-height:1.5; }
.kpi-card--ring .kpi-val strong { color:#0f172a; font-weight:700; }

/* ─── Panel 卡片（白底细边框） ─── */
.panel {
  background:#fff; border:1px solid rgba(15,23,42,0.06); border-radius:16px;
  box-shadow:0 8px 24px rgba(13,148,136,0.05), 0 1px 3px rgba(15,23,42,0.03); padding:18px 20px;
  transition:box-shadow .35s cubic-bezier(0.32,0.72,0,1);
}
.panel:hover { box-shadow:0 14px 34px rgba(13,148,136,0.09), 0 2px 6px rgba(15,23,42,0.03); }
.card-hd { display:flex; justify-content:space-between; align-items:center; margin-bottom:14px; }
.card-hd h2 { font-size:14px; font-weight:600; color:#0f172a; margin:0; }
.hd-right { display:flex; align-items:center; gap:10px; }
.range-chips { display:flex; align-items:center; gap:4px; background:#f1f5f9; border-radius:8px; padding:2px; }
.range-chip {
  font-size:11.5px; font-weight:500; color:#64748b; background:transparent;
  border:none; border-radius:6px; padding:4px 10px; cursor:pointer; transition:all .15s;
  font-family:inherit;
}
.range-chip:hover { color:#0d9488; }
.range-chip--active { color:#fff; background:#0d9488; }
.badge { font-size:11px; font-weight:500; color:#64748b; background:#f1f5f9; padding:2px 10px; border-radius:999px; }
.badge--blue { color:#0d9488; background:#f0fdfa; }
.hd-hint { font-size:11px; color:#94a3b8; }
.card-link { font-size:12px; color:#0d9488; text-decoration:none; }

/* ─── 趋势图例 ─── */
.legend-inline { display:flex; align-items:center; gap:5px; font-size:11px; color:#64748b; }
.legend-dot { width:10px; height:3px; border-radius:2px; display:inline-block; }
.legend-dot + .legend-dot { margin-left:8px; }
.dot--teal { background:#0d9488; }
.dot--blue { background:#3b82f6; }

/* ─── 趋势 ─── */
.trend-card { margin-bottom:18px; }
.chart-box { width:100%; }

/* ─── 三栏 ─── */
.triple-row { display:grid; grid-template-columns:1.4fr 1fr 1fr; gap:14px; }
@media(max-width:1100px){ .triple-row{grid-template-columns:1fr 1fr} }
@media(max-width:600px){ .triple-row{grid-template-columns:1fr} }

/* ─── 快捷入口 ─── */
.quick-grid { display:grid; grid-template-columns:1fr 1fr; gap:8px; }
.quick-cell {
  display:flex; align-items:center; gap:10px; padding:11px 12px;
  background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px;
  text-decoration:none; transition:all .15s;
}
.quick-cell:hover { border-color:#5eead4; background:#f0fdfa; }
.quick-icon { width:30px; height:30px; display:flex; align-items:center; justify-content:center; border-radius:8px; background:#fff; border:1px solid #e2e8f0; color:#0d9488; flex-shrink:0; }
.quick-body { display:flex; flex-direction:column; gap:1px; min-width:0; }
.quick-label { font-size:12.5px; font-weight:600; color:#0f172a; }
.quick-desc { font-size:10.5px; color:#94a3b8; }

/* ─── 待办 / 预警 ─── */
.todo-strip { display:flex; flex-direction:column; gap:8px; }
.todo-row {
  display:flex; align-items:center; gap:10px; padding:10px 12px;
  border:1px solid #e2e8f0; border-radius:10px; text-decoration:none;
  transition:all .15s;
}
.todo-row:hover { border-color:#5eead4; box-shadow:0 1px 4px rgba(15,23,42,.05); }
.todo-row--amber { background:#fffbeb; border-color:#fde68a; }
.todo-row--red { background:#fef2f2; border-color:#fecaca; }
.todo-row--teal { background:#f0fdfa; border-color:#99f6e4; }
.todo-icon {
  width:26px; height:26px; display:flex; align-items:center; justify-content:center;
  border-radius:8px; font-size:13px; font-weight:600; flex-shrink:0;
}
.todo-row--amber .todo-icon { background:#fef3c7; color:#d97706; }
.todo-row--red .todo-icon { background:#fee2e2; color:#dc2626; }
.todo-row--teal .todo-icon { background:#ccfbf1; color:#0d9488; }
.todo-body { display:flex; flex-direction:column; gap:1px; min-width:0; }
.todo-title { font-size:12.5px; font-weight:600; color:#0f172a; }
.todo-desc { font-size:10.5px; color:#64748b; }
.todo-row svg:last-child { margin-left:auto; color:#94a3b8; flex-shrink:0; }
.empty-tip { text-align:center; padding:16px 0; font-size:12.5px; color:#94a3b8; }

/* ─── AI 辅助（紧凑） ─── */
.ai-card { display:flex; flex-direction:column; }
.ai-tip { font-size:12px; color:#64748b; margin:0 0 10px; line-height:1.6; }
.ai-input-line { display:flex; gap:6px; align-items:center; background:#fff; border:1px solid #e2e8f0; border-radius:10px; padding:3px 3px 3px 12px; transition:border-color .15s; }
.ai-input-line:focus-within { border-color:#5eead4; }
.ai-input { flex:1; border:none; outline:none; font-size:12.5px; padding:6px 0; font-family:inherit; color:#0f172a; }
.ai-send { width:30px; height:30px; display:flex; align-items:center; justify-content:center; border:none; background:#0d9488; color:#fff; border-radius:8px; cursor:pointer; transition:background .15s; }
.ai-send:hover:not(:disabled) { background:#0f766e; }
.ai-send:disabled { background:#5eead4; cursor:not-allowed; }
.ai-link { font-size:11.5px; color:#0d9488; text-decoration:none; margin-top:10px; }
</style>
