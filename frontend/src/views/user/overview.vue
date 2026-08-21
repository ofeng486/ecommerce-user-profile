<!-- 画像概览 — 分层洞察页：KPI + 分层卡片（可下钻）+ 洞察文案 + 饼图/交叉堆叠/省份 -->
<template>
  <div class="overview" v-loading="loading">
    <!-- 页头（统一风格） -->
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">画像概览</h1>
          <span class="title-tag">OVERVIEW</span>
        </div>
        <p class="page-desc">平台用户全貌 · RFM 价值分层与画像覆盖洞察</p>
      </div>
      <div class="ph-meta">
        <span v-if="statDate" class="header-meta"><span class="meta-dot"></span>数据截止 {{ statDate }} · 版本 {{ dataVersion }}</span>
      </div>
    </div>

    <!-- 错误提示 -->
    <el-alert v-if="error" :title="error" type="error" show-icon closable @close="error=''" class="mb-4">
      <template #default><el-button size="small" @click="loadData" class="ml-2">重试</el-button></template>
    </el-alert>

    <!-- KPI — 精简卡片（点击钻取） -->
    <div class="metric-grid">
      <div v-for="(m, i) in metrics" :key="i" class="metric-shell" :title="m.tip" @click="metricClick(i)">
        <div class="metric-core">
          <div class="metric-top">
            <span class="metric-icon-svg" :style="{ color: m.color }" v-html="m.iconSvg"></span>
            <span class="metric-pct" v-if="m.pct != null" :style="{ color: m.color }">{{ m.pct }}%</span>
          </div>
          <div class="metric-val mono">{{ m.val }}</div>
          <div class="metric-label">{{ m.label }}</div>
          <div class="metric-meta">{{ m.meta }}</div>
        </div>
      </div>
    </div>

    <!-- 分层卡片（可点击下钻画像列表） -->
    <div v-if="segments.length" class="seg-cards">
      <div v-for="s in segments" :key="s.segmentCode" class="seg-card" :style="{ '--seg': rfmColors[s.segmentCode] || '#94a3b8' }" @click="drillSegment(s.segmentCode)">
        <div class="seg-head">
          <span class="seg-name">{{ s.segmentName }}</span>
          <span class="seg-count">{{ fmtN(s.userCount) }}</span>
        </div>
        <div class="seg-bar"><span class="seg-bar-fill" :style="{ width: segPct(s.userCount) + '%' }"></span></div>
        <div class="seg-foot">
          <span class="seg-ratio">{{ segPct(s.userCount).toFixed(1) }}%</span>
          <span class="seg-desc">{{ segDesc(s.segmentCode) }}</span>
        </div>
      </div>
    </div>

    <!-- 自动洞察文案 -->
    <div v-if="insight" class="insight-bar">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18h6M10 22h4M12 2a7 7 0 0 0-4 12.7c.6.5 1 1.4 1 2.3h6c0-.9.4-1.8 1-2.3A7 7 0 0 0 12 2z"/></svg>
      <span class="insight-text">{{ insight }}</span>
    </div>

    <!-- 图表区 -->
    <div class="chart-grid">
      <!-- 分层饼图 -->
      <section class="chart-card">
        <div class="chart-head">
          <h2 class="chart-title">用户价值分层</h2>
          <span class="chart-sub">RFM 五分类 · 点击扇形下钻</span>
        </div>
        <div v-if="!loading && !hasSegments" class="chart-empty">暂无分层数据，请先运行画像分析任务</div>
        <div ref="segmentChart" class="chart-body"></div>
      </section>

      <!-- 分层 × 省份 堆叠柱 -->
      <section class="chart-card">
        <div class="chart-head">
          <h2 class="chart-title">分层 × 省份分布</h2>
          <span class="chart-sub">TOP5 省份各分层构成（高价值集中在哪）</span>
        </div>
        <div v-if="!loading && !hasCross" class="chart-empty">暂无交叉数据</div>
        <div ref="crossChart" class="chart-body"></div>
      </section>

      <!-- 全宽：省份分布 -->
      <section class="chart-card chart-card--wide">
        <div class="chart-head">
          <h2 class="chart-title">用户省份分布</h2>
          <span class="chart-sub">TOP10 省份用户量 · 点击条形下钻</span>
        </div>
        <div v-if="!loading && !hasProvinces" class="chart-empty">暂无省份数据</div>
        <div ref="provinceChart" class="chart-body" style="height: 300px"></div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { fetchOverview, fetchSegmentDistribution } from '@/api/profile'
import request from '@/utils/http'
import * as echarts from 'echarts'

defineOptions({ name: 'UserProfileOverview' })

const router = useRouter()
const loading = ref(false)
const error = ref('')
const hasSegments = ref(false)
const hasProvinces = ref(false)
const hasCross = ref(false)
const statDate = ref(''); const dataVersion = ref('')
const overview = ref({ totalUsers: 0, profiledUsers: 0, highValueUsers: 0, totalPaymentAmount: 0 })
const segments = ref<any[]>([])
const segmentChart = ref<HTMLElement>()
const crossChart = ref<HTMLElement>()
const provinceChart = ref<HTMLElement>()
const charts: echarts.ECharts[] = []

const rfmColors: Record<string, string> = {
  HIGH_VALUE: '#059669', POTENTIAL: '#0d9488',
  GENERAL: '#94a3b8', AT_RISK: '#d97706', LOW_VALUE: '#dc2626'
}
const rfmNames: Record<string, string> = {
  HIGH_VALUE: '高价值用户', POTENTIAL: '潜力用户',
  GENERAL: '一般用户', AT_RISK: '流失风险', LOW_VALUE: '低价值'
}

const metrics = ref([
  { iconSvg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
    label: '总用户数', val: '0', meta: '电商平台注册总量', color: '#0d9488', pct: null as number | null, tip: '查看全部用户' },
  { iconSvg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
    label: '已画像', val: '0', meta: '完成 RFM 分析', color: '#059669', pct: null as number | null, tip: '画像覆盖率见下方' },
  { iconSvg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>',
    label: '高价值', val: '0', meta: '高消费 + 高频次', color: '#d97706', pct: null as number | null, tip: '查看高价值分层用户' },
  { iconSvg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>',
    label: '累计消费', val: '¥0', meta: '全平台支付总额', color: '#0d9488', pct: null as number | null, tip: '' }
])

function fmtN(v: number) { return (v ?? 0).toLocaleString('zh-CN') }
function fmtA(v: number) {
  if (!v) return '¥0'
  if (v >= 1e8) return '¥' + (v / 1e8).toFixed(2) + '亿'
  if (v >= 1e4) return '¥' + (v / 1e4).toFixed(1) + '万'
  return '¥' + v.toLocaleString()
}

/** KPI 点击钻取 */
function metricClick(i: number) {
  if (i === 0) router.push('/user/profiles')
  else if (i === 2) router.push({ path: '/user/profiles', query: { segment: 'HIGH_VALUE' } })
}

/** 分层卡片/饼图下钻 → 画像列表筛分层 */
function drillSegment(code: string) {
  router.push({ path: '/user/profiles', query: { segment: code } })
}
/** 省份下钻 → 画像列表筛省份 */
function drillProvince(p: string) {
  router.push({ path: '/user/profiles', query: { province: p } })
}

const totalSeg = computed(() => segments.value.reduce((a, b) => a + Number(b.userCount || 0), 0))
function segPct(n: number) {
  const t = totalSeg.value
  return t ? (Number(n) / t) * 100 : 0
}
/** 分层一句话运营描述 */
function segDesc(code: string) {
  const m: Record<string, string> = {
    HIGH_VALUE: '核心客群，重点运营', POTENTIAL: '转化潜力，值得培育',
    GENERAL: '中位客群，消费升级', AT_RISK: '流失倾向，建议召回', LOW_VALUE: '低价值，低成本触达'
  }
  return m[code] || ''
}

/** 自动洞察文案（数据驱动的一句话结论） */
const insight = computed(() => {
  if (!segments.value.length) return ''
  const t = totalSeg.value
  const get = (code: string) => {
    const hit = segments.value.find(s => s.segmentCode === code)
    return hit ? { n: Number(hit.userCount || 0), pct: t ? (Number(hit.userCount) / t) * 100 : 0 } : { n: 0, pct: 0 }
  }
  const hv = get('HIGH_VALUE')
  const risk = get('AT_RISK')
  const parts: string[] = []
  if (hv.n > 0) parts.push(`高价值用户 ${fmtN(hv.n)} 人，占比 ${hv.pct.toFixed(1)}%，是平台核心价值客群`)
  if (risk.n > 0) parts.push(`流失风险用户 ${fmtN(risk.n)} 人（${risk.pct.toFixed(1)}%），建议优先召回`)
  if (!parts.length) return ''
  return parts.join('；') + '。'
})

async function loadData() {
  loading.value = true; error.value = ''
  try {
    const [ovR, sgR, pvR, crR] = await Promise.allSettled([
      fetchOverview(), fetchSegmentDistribution(),
      request.get<any[]>({ url: '/api/v1/public/provinces', showErrorMessage: false }),
      request.get<any[]>({ url: '/api/v1/public/segment-provinces', showErrorMessage: false })
    ])
    const fails: string[] = []
    if (ovR.status === 'rejected') fails.push('概览')
    if (sgR.status === 'rejected') fails.push('分层')
    if (pvR.status === 'rejected') fails.push('省份')
    if (crR.status === 'rejected') fails.push('交叉')
    if (fails.length) error.value = '加载失败：' + fails.join('、')

    if (ovR.status === 'fulfilled' && ovR.value) {
      const o = ovR.value as any
      overview.value = o
      metrics.value[0].val = fmtN(o.totalUsers)
      metrics.value[1].val = fmtN(o.profiledUsers)
      metrics.value[2].val = fmtN(o.highValueUsers)
      metrics.value[3].val = fmtA(o.totalPaymentAmount)
      if (o.totalUsers > 0) {
        metrics.value[1].pct = Math.round((o.profiledUsers / o.totalUsers) * 100)
        metrics.value[2].pct = o.profiledUsers > 0 ? Math.round((o.highValueUsers / o.profiledUsers) * 100) : 0
      }
      dataVersion.value = o.dataVersion || ''
      statDate.value = o.calculatedAt ? String(o.calculatedAt).slice(0, 10) : ''
    }

    const segs = sgR.status === 'fulfilled' ? (sgR.value as any[]) || [] : []
    hasSegments.value = segs.length > 0
    // 分层按业务价值排序展示（高价值 → 低价值）
    const SEG_ORDER: Record<string, number> = { HIGH_VALUE: 0, POTENTIAL: 1, GENERAL: 2, AT_RISK: 3, LOW_VALUE: 4 }
    segments.value = [...segs].sort((a, b) =>
      (SEG_ORDER[a.segmentCode] ?? 9) - (SEG_ORDER[b.segmentCode] ?? 9) || (Number(b.userCount) - Number(a.userCount)))
    const provinces = pvR.status === 'fulfilled' ? (pvR.value as any[]) || [] : []
    hasProvinces.value = provinces.length > 0
    const cross = crR.status === 'fulfilled' ? (crR.value as any[]) || [] : []
    hasCross.value = cross.length > 0

    loading.value = false
    await nextTick()
    renderCharts(segs, provinces, cross)
  } catch (e: any) {
    loading.value = false
    error.value = '加载异常：' + (e?.message || '未知错误')
  }
}

function renderCharts(segs: any[], provinces: any[], cross: any[]) {
  charts.forEach(c => c.dispose()); charts.length = 0

  // 分层饼图（点击扇形下钻）
  if (segmentChart.value && segs.length) {
    const c = echarts.init(segmentChart.value); charts.push(c)
    c.setOption({
      tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 人 ({d}%)' },
      legend: { orient: 'horizontal', bottom: 0, itemWidth: 8, itemHeight: 8, itemGap: 16, textStyle: { fontSize: 12, color: '#94a3b8' } },
      series: [{
        type: 'pie', radius: ['48%', '72%'], center: ['50%', '42%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 3 },
        label: { formatter: '{b}\n{d}%', fontSize: 11, color: '#64748b', lineHeight: 16 },
        emphasis: { itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,0.06)' }, label: { fontSize: 13, fontWeight: 'bold' } },
        data: segs.map((s: any) => ({
          name: rfmNames[s.segmentCode] || s.segmentName || s.segmentCode,
          value: s.userCount,
          itemStyle: { color: rfmColors[s.segmentCode] || '#94a3b8' }
        }))
      }]
    })
    c.on('click', (p: any) => {
      const code = segs.find((s: any) => (rfmNames[s.segmentCode] || s.segmentName) === p.name)?.segmentCode
      if (code) drillSegment(code)
    })
  }

  // 分层 × 省份 堆叠柱（TOP5 省份）
  if (crossChart.value && cross.length) {
    const c = echarts.init(crossChart.value); charts.push(c)
    // 省份按总人数 TOP5
    const provTotal = new Map<string, number>()
    cross.forEach((x: any) => provTotal.set(x.province, (provTotal.get(x.province) || 0) + Number(x.userCount || 0)))
    const topProv = [...provTotal.entries()].sort((a, b) => b[1] - a[1]).slice(0, 5).map(e => e[0])
    const segList = ['HIGH_VALUE', 'POTENTIAL', 'GENERAL', 'AT_RISK', 'LOW_VALUE']
    const activeSegs = segList.filter(code => cross.some((x: any) => x.segmentCode === code))
    const seriesData = activeSegs.map(code => ({
      name: rfmNames[code] || code,
      type: 'bar',
      stack: 'total',
      barWidth: 22,
      itemStyle: { color: rfmColors[code] || '#94a3b8' },
      data: topProv.map(p => cross.filter((x: any) => x.province === p && x.segmentCode === code)
        .reduce((a, x: any) => a + Number(x.userCount || 0), 0))
    }))
    c.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { bottom: 0, itemWidth: 8, itemHeight: 8, itemGap: 12, textStyle: { fontSize: 11, color: '#94a3b8' } },
      grid: { left: 8, right: 16, top: 8, bottom: 40, containLabel: true },
      xAxis: { type: 'category', data: topProv, axisLabel: { fontSize: 11, color: '#334155' }, axisLine: { show: false }, axisTick: { show: false } },
      yAxis: { type: 'value', axisLabel: { fontSize: 11, color: '#94a3b8' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
      series: seriesData
    })
  }

  // 省份分布横向条形图（TOP10，点击下钻；最大值在上）
  if (provinceChart.value && provinces.length) {
    const c = echarts.init(provinceChart.value); charts.push(c)
    const sorted = [...provinces].sort((a, b) => (Number(b.userCount) || 0) - (Number(a.userCount) || 0)).slice(0, 10)
    // 范围随数据动态变化（maxV × 1.05 留 5% 头部空间，splitNumber=4 控制刻度数量）
    const maxV = Math.max(...sorted.map(x => Number(x.userCount) || 0), 1)
    c.setOption({
      grid: { left: 8, right: 40, top: 8, bottom: 8, containLabel: true },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: (p: any) => `${p[0].name}<br/>用户数：${p[0].value.toLocaleString()}` },
      xAxis: { type: 'value', max: Math.ceil(maxV * 1.05), splitNumber: 4, axisLabel: { fontSize: 11, color: '#94a3b8', formatter: (v: number) => v >= 1000 ? (v/1000).toFixed(1) + 'k' : String(v) }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
      yAxis: { type: 'category', data: sorted.map(x => x.province), inverse: true, axisLabel: { fontSize: 11, color: '#334155' }, axisLine: { show: false }, axisTick: { show: false } },
      series: [{
        type: 'bar', barWidth: 14, data: sorted.map(x => ({
          value: Number(x.userCount) || 0,
          itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#2dd4bf' }, { offset: 1, color: '#0d9488' }]), borderRadius: [0, 4, 4, 0] }
        })),
        label: { show: true, position: 'right', fontSize: 11, color: '#64748b' }
      }]
    })
    c.on('click', (p: any) => { if (p.name) drillProvince(p.name) })
  }
}

const handleResize = () => charts.forEach(c => c.resize())
onMounted(() => { loadData(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize); charts.forEach(c => c.dispose()) })
</script>

<style scoped>
.overview {
  font-family: var(--font-body, 'Inter', 'PingFang SC', system-ui, sans-serif);
  max-width: 1200px; margin: 0 auto;
}

/* ─── 页头 ───（样式已抽到 user-portal.css 全局） */

/* ─── KPI ─── */
.metric-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 18px; }
@media (max-width: 900px) { .metric-grid { grid-template-columns: repeat(2, 1fr); } }
.metric-shell {
  background: #fff; border: 1px solid rgba(15, 23, 42, 0.06); border-radius: 16px;
  box-shadow: 0 8px 24px rgba(13, 148, 136, 0.05), 0 1px 3px rgba(15, 23, 42, 0.03); padding: 1.5px;
  transition: transform 0.35s cubic-bezier(0.32, 0.72, 0, 1), box-shadow 0.35s cubic-bezier(0.32, 0.72, 0, 1); cursor: pointer;
}
.metric-shell:hover { transform: translateY(-2px); box-shadow: 0 14px 34px rgba(13, 148, 136, 0.1), 0 2px 6px rgba(15, 23, 42, 0.03); }
.metric-core { padding: 18px 20px; }
.metric-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.metric-icon-svg { display: inline-flex; }
.metric-pct { font-size: 13px; font-weight: 600; font-family: 'JetBrains Mono', monospace; }
.metric-val { font-size: 22px; font-weight: 700; color: #0f172a; font-family: 'JetBrains Mono', monospace; letter-spacing: -0.3px; }
.metric-label { font-size: 12.5px; font-weight: 500; color: #475569; margin-top: 2px; }
.metric-meta { font-size: 11px; color: #94a3b8; }

/* ─── 分层卡片 ─── */
.seg-cards { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 14px; }
@media (max-width: 1100px) { .seg-cards { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 700px) { .seg-cards { grid-template-columns: repeat(2, 1fr); } }
.seg-card {
  --seg: #94a3b8;
  background: #fff; border: 1px solid rgba(15, 23, 42, 0.06); border-radius: 14px;
  padding: 14px 16px; cursor: pointer; transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.03);
}
.seg-card:hover { transform: translateY(-2px); box-shadow: 0 10px 26px rgba(15, 23, 42, 0.07); border-color: var(--seg); }
.seg-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 10px; }
.seg-name { font-size: 13px; font-weight: 600; color: #0f172a; }
.seg-count { font-size: 17px; font-weight: 700; color: var(--seg); font-family: 'JetBrains Mono', monospace; }
.seg-bar { height: 6px; background: #f1f5f9; border-radius: 3px; overflow: hidden; margin-bottom: 8px; }
.seg-bar-fill { display: block; height: 100%; background: var(--seg); border-radius: 3px; }
.seg-foot { display: flex; align-items: center; justify-content: space-between; }
.seg-ratio { font-size: 11px; font-weight: 600; color: var(--seg); }
.seg-desc { font-size: 10.5px; color: #94a3b8; }

/* ─── 洞察文案 ─── */
.insight-bar {
  display: flex; align-items: flex-start; gap: 10px;
  background: #f0fdfa; border: 1px solid #99f6e4; border-radius: 10px;
  padding: 12px 16px; margin-bottom: 16px;
}
.insight-bar svg { color: #0d9488; flex-shrink: 0; margin-top: 1px; }
.insight-text { font-size: 13px; color: #134e4a; line-height: 1.7; }

/* ─── 图表区 ─── */
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-card--wide { grid-column: 1 / -1; }
@media (max-width: 1100px) { .chart-grid { grid-template-columns: 1fr; } }
.chart-card { background: #fff; border: 1px solid rgba(15,23,42,0.06); border-radius: 14px; box-shadow: 0 8px 24px rgba(13,148,136,0.05), 0 1px 3px rgba(15,23,42,0.03); padding: 20px 24px; transition: box-shadow .35s cubic-bezier(0.32,0.72,0,1); }
.chart-head { display: flex; align-items: baseline; justify-content: space-between; padding-bottom: 12px; margin-bottom: 8px; border-bottom: 1px solid #f1f5f9; }
.chart-title { font-size: 15px; font-weight: 600; color: #0f172a; }
.chart-sub { font-size: 12px; color: #94a3b8; }
.chart-empty { text-align: center; padding: 40px 0; color: #94a3b8; font-size: 13px; }
.chart-body { width: 100%; height: 320px; }
</style>
