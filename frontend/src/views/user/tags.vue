<!-- 标签分析 — frontend-design: 图表驱动、数据叙事、排版层次 -->
<template>
  <div class="tanalysis" v-loading="loading">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">标签分析</h1>
          <span class="title-tag">TAGS</span>
        </div>
        <p class="page-desc">用户行为与价值的多维度标签分布</p>
      </div>
    </div>

    <div class="chart-grid">
      <!-- 消费 × 活跃度交叉 -->
      <section class="chart-card">
        <div class="chart-head">
          <h2 class="chart-ttl">消费 × 活跃度交叉</h2>
          <span class="chart-sub">各活跃档下消费能力构成</span>
        </div>
        <div ref="crossChart" class="chart-body"></div>
      </section>

      <!-- 消费能力 -->
      <section class="chart-card">
        <div class="chart-head">
          <h2 class="chart-ttl">消费能力等级</h2>
          <span class="chart-sub">基于消费金额分层</span>
        </div>
        <div ref="consumeChart" class="chart-body"></div>
      </section>
    </div>

    <section class="chart-card wide">
      <div class="chart-head">
        <h2 class="chart-ttl">用户偏好品类</h2>
        <span class="chart-sub">按浏览行为加权评分排名</span>
      </div>
      <div ref="favChart" class="chart-body chart-body--tall"></div>
    </section>

    <section class="chart-card wide">
      <div class="chart-head">
        <h2 class="chart-ttl">用户活跃度分布</h2>
        <span class="chart-sub">基于近30日登录和浏览行为</span>
      </div>
      <div ref="activeChart" class="chart-body"></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { fetchTagDistribution, fetchTagCross } from '@/api/profile'
import * as echarts from 'echarts'

defineOptions({ name: 'UserTagAnalysis' })

const router = useRouter()

const loading = ref(false)
const crossChart = ref<HTMLElement>()
const consumeChart = ref<HTMLElement>()
const favChart = ref<HTMLElement>()
const activeChart = ref<HTMLElement>()
const charts: echarts.ECharts[] = []
const tags = ref<any[]>([])
const crossData = ref<any[]>([]) // 活跃档 × 消费档交叉矩阵

const chartColors = ['#0d9488','#059669','#d97706','#dc2626','#14b8a6','#0891b2','#94a3b8']
// 消费/活跃度按"额度从高到低"排序显示（high/mid/low）；映射任意 tag_value，回退原值
const consumeMap: Record<string,string> = { High: '高消费', Medium: '中等消费', Low: '低消费' }
const activeMap:  Record<string,string> = { High: '高活跃', Medium: '中活跃', Low: '低活跃' }

function groupTags(code: string) { return tags.value.filter((t: any) => t.tagCode === code) }
/** 智能分类名：映射表 → 原值 → "未命名类别" 兜底（避免全显示"未分类"） */
function className(m: Record<string,string>, v: string, kind: 'tier' | 'category' = 'tier') {
  return m[v] || (v && v !== 'Unknown' ? v : (kind === 'category' ? '未命名类别' : '未分类'))
}

const tooltip = {
  backgroundColor: '#fff', borderColor: '#e2e8f0', textStyle: { color: '#334155', fontSize: 12 },
  extraCssText: 'border-radius:8px;padding:8px 12px;box-shadow:0 4px 12px rgba(0,0,0,0.08);'
}

function ic(el: any, opts: any) {
  if (!el.value) return null
  const c = echarts.init(el.value)
  charts.push(c)
  c.setOption(opts)
  return c
}

/** 下钻：跳画像列表并筛选该标签（tagValue 可为逗号分隔原始值，tagName 用于提示条显示） */
function drillTag(tagCode: string, tagValue: string, tagName: string) {
  router.push({ path: '/user/profiles', query: { tagCode, tagValue, tagName } })
}

function renderAll() {
  charts.forEach(c => c.dispose()); charts.length = 0
  const emptyData = [{ name: '暂无数据', value: 1, itemStyle: { color: '#e2e8f0' } }]

  // 消费 × 活跃度交叉堆叠柱（数据源：/api/v1/profiles/tags/cross 用户级交叉矩阵）
  // X 轴 = 活跃档，堆叠段 = 消费档 → 直观看出"高消费人群集中在哪档活跃"
  // 视觉语义："高=顶"——堆叠柱顶端是高消费段（深青），符合业务直觉
  const cross = crossData.value
  const actOrder = ['High', 'Medium', 'Low']
  // 堆叠顺序：低→中→高（ECharts 默认 array[0] 画在最底，所以 Low 先画、High 最后画在最顶）
  const stackOrder = ['Low', 'Medium', 'High']
  // 图例顺序：高→中→低（按业务重要度，与堆叠顺序解耦）
  const legendOrder = ['High', 'Medium', 'Low']
  const consTone: Record<string, string> = { High: '#0d9488', Medium: '#f59e0b', Low: '#cbd5e1' }
  const matrix: Record<string, Record<string, number>> = {}
  cross.forEach((c: any) => {
    (matrix[c.activeLevel] ??= {})[c.consumeLevel] = (matrix[c.activeLevel]?.[c.consumeLevel] || 0) + Number(c.userCount || 0)
  })
  const consSeries = stackOrder.map(lv => ({
    name: className(consumeMap, lv, 'tier'),
    type: 'bar', stack: 'total', barWidth: 52,
    itemStyle: { color: consTone[lv] || '#94a3b8', borderRadius: [0, 0, 0, 0] },
    data: actOrder.map(a => matrix[a]?.[lv] || 0)
  }))
  // 堆叠柱顶端圆角：最后一段（stackOrder 末尾）= 高消费，画在最顶，需要顶部圆角
  if (consSeries.length) {
    const last = consSeries[consSeries.length - 1]
    last.itemStyle = { ...last.itemStyle, borderRadius: [6, 6, 0, 0] }
  }
  const cc = ic(crossChart, {
    tooltip: { ...tooltip, trigger: 'axis', axisPointer: { type: 'shadow' },
      formatter: (p: any) => {
        const act = p[0]?.axisValue || ''
        const lines = p.map((s: any) => `${s.marker}${s.seriesName}：${s.value.toLocaleString()} 人`)
        return `${act}<br/>${lines.join('<br/>')}`
      } },
    legend: {
      bottom: 0,
      // 显式 data 让图例顺序按业务重要度 [高→中→低]，与堆叠顺序解耦
      data: legendOrder.map(lv => ({ name: className(consumeMap, lv, 'tier') })),
      textStyle: { fontSize: 11, color: '#94a3b8' }, itemWidth: 12, itemHeight: 6
    },
    grid: { left: 8, right: 16, top: 8, bottom: 24, containLabel: true },
    xAxis: { type: 'category', data: actOrder.map(a => className(activeMap, a, 'tier')),
      axisLabel: { fontSize: 12, color: '#64748b' }, axisLine: { show: false }, axisTick: { show: false } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } },
      axisLabel: { fontSize: 10, color: '#94a3b8', formatter: (v: number) => v >= 1000 ? (v / 1000).toFixed(1) + 'k' : v } },
    series: cross.length ? consSeries : []
  })
  // 点击 X 轴活跃档 → 下钻画像列表筛该活跃档（如"低活跃"）
  if (cc) {
    const actLabelToRaw: Record<string, string> = {}
    actOrder.forEach(a => { actLabelToRaw[className(activeMap, a, 'tier')] = a })
    cc.on('click', (p: any) => {
      if (p.componentType === 'series' && actLabelToRaw[p.name]) drillTag('ACTIVE_LEVEL', actLabelToRaw[p.name], p.name)
    })
  }

  // 消费能力（数据源：user_profile_tag /CONSUMPTION_LEVEL）
  const consume = groupTags('CONSUMPTION_LEVEL').filter(t => t.tagValue !== 'Unknown')
  const consumeChart_ = ic(consumeChart, {
    tooltip: { ...tooltip, trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 11, color: '#94a3b8' }, itemWidth: 6, itemHeight: 6 },
    series: [{ type: 'pie', radius: ['40%', '68%'], center: ['50%', '42%'],
      itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 },
      label: { fontSize: 12, color: '#64748b', formatter: '{d}%' },
      data: consume.length ? consume.map((t: any, i: number) => ({ name: className(consumeMap, t.tagValue, 'tier'), value: t.userCount, raw: t.tagValue, itemStyle: { color: chartColors[i % chartColors.length] } })) : emptyData }]
  })
  // 点击消费档扇形 → 下钻画像列表筛该消费档（如"高消费"）
  if (consumeChart_) consumeChart_.on('click', (p: any) => { if (p.data?.raw) drillTag('CONSUMPTION_LEVEL', p.data.raw, p.name) })

  // 品类偏好（数据源：user_profile_tag /FAVORITE_CATEGORY；tagValue 为合并类目名）
  // 双指标：用户数（青条）+ 人均消费（橙条，右轴）→ 看"哪类人群更值钱"
  const fav = groupTags('FAVORITE_CATEGORY').filter(t => t.tagValue !== 'Unknown')
    .map(t => ({
      name: className({}, t.tagValue, 'category'),
      value: t.userCount,
      avg: Number(t.avgAmount || 0),
      raw: t.filterTagValue || t.tagValue
    }))
    .sort((a, b) => b.value - a.value)
  const favNames = fav.map(d => d.name).reverse()
  const favChart_ = ic(favChart, {
    tooltip: { ...tooltip, trigger: 'axis', axisPointer: { type: 'shadow' },
      formatter: (p: any) => {
        const d = fav.find(f => f.name === p[0].name)
        return `${p[0].name}<br/>用户数：${p[0].value.toLocaleString()} 人<br/>人均消费：¥${(d?.avg || 0).toLocaleString()}`
      } },
    legend: { bottom: 0, textStyle: { fontSize: 11, color: '#94a3b8' }, itemWidth: 12, itemHeight: 6 },
    grid: { left: 3, right: 52, top: 5, bottom: 24 },
    xAxis: [
      { type: 'value', show: false },   // 左轴：用户数
      { type: 'value', show: false }    // 右轴：人均消费
    ],
    yAxis: { type: 'category', data: favNames, axisLine: { show: false }, axisTick: { show: false }, axisLabel: { fontSize: 12, color: '#64748b' } },
    series: [
      { name: '用户数', type: 'bar', data: fav.map(d => d.value).reverse(), barWidth: 7,
        itemStyle: { borderRadius: [0, 3, 3, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#0d9488' }, { offset: 1, color: '#5eead4' }]) },
        label: { show: true, position: 'right', fontSize: 10, color: '#475569', formatter: (p: any) => p.value >= 1000 ? (p.value / 1000).toFixed(1) + 'k' : p.value } },
      { name: '人均消费', type: 'bar', xAxisIndex: 1, data: fav.map(d => d.avg).reverse(), barWidth: 7,
        itemStyle: { borderRadius: [0, 3, 3, 0], color: '#f59e0b' },
        label: { show: true, position: 'right', fontSize: 10, color: '#b45309', formatter: (p: any) => '¥' + (p.value >= 1000 ? (p.value / 1000).toFixed(1) + 'k' : p.value) } }
    ]
  })
  // 点击品类条 → 下钻画像列表筛该品类（filterTagValue 为标签原始值，已数据治理统一）
  if (favChart_) favChart_.on('click', (p: any) => {
    const d = fav.find(f => f.name === p.name)
    if (d) drillTag('FAVORITE_CATEGORY', d.raw, d.name)
  })

  // 活跃度（数据源：user_profile_tag /ACTIVE_LEVEL，独立数据源）
  const active = groupTags('ACTIVE_LEVEL').filter(t => t.tagValue !== 'Unknown')
  const activeChart_ = ic(activeChart, {
    tooltip: { ...tooltip, trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 11, color: '#94a3b8' }, itemWidth: 6, itemHeight: 6 },
    series: [{ type: 'pie', radius: ['40%', '68%'], center: ['50%', '42%'],
      itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 6 },
      label: { fontSize: 12, color: '#64748b', formatter: '{d}%' },
      data: active.length ? active.map((t: any, i: number) => ({ name: className(activeMap, t.tagValue, 'tier'), value: t.userCount, raw: t.tagValue, itemStyle: { color: chartColors[i % chartColors.length] } })) : emptyData }]
  })
  // 点击活跃档扇形 → 下钻画像列表筛该活跃档（如"低活跃"）
  if (activeChart_) activeChart_.on('click', (p: any) => { if (p.data?.raw) drillTag('ACTIVE_LEVEL', p.data.raw, p.name) })
}

const handleResize = () => charts.forEach(c => c.resize())

onMounted(async () => {
  loading.value = true
  try {
    const allTags: any[] = []
    const [t1, t2, t3, cxR] = await Promise.allSettled([
      fetchTagDistribution('ACTIVE_LEVEL'), fetchTagDistribution('CONSUMPTION_LEVEL'),
      fetchTagDistribution('FAVORITE_CATEGORY'), fetchTagCross()
    ])
    if (t1.status === 'fulfilled' && t1.value) t1.value.forEach((t: any) => allTags.push({ ...t, tagCode: 'ACTIVE_LEVEL' }))
    if (t2.status === 'fulfilled' && t2.value) t2.value.forEach((t: any) => allTags.push({ ...t, tagCode: 'CONSUMPTION_LEVEL' }))
    if (t3.status === 'fulfilled' && t3.value) t3.value.forEach((t: any) => allTags.push({ ...t, tagCode: 'FAVORITE_CATEGORY' }))
    tags.value = allTags
    // 活跃 × 消费交叉矩阵
    if (cxR.status === 'fulfilled' && cxR.value) crossData.value = cxR.value as any[]
  } catch {} finally { loading.value = false }
  await nextTick()
  renderAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
})
</script>

<style scoped>
.tanalysis {
  font-family: var(--font-body, 'Inter', system-ui);
  max-width: 1200px; margin: 0 auto;
}

/* ─── Glass Card ─── */
.chart-card { background: #fff; border: 1px solid rgba(15,23,42,0.06); border-radius: 14px; box-shadow: 0 8px 24px rgba(13,148,136,0.05), 0 1px 3px rgba(15,23,42,0.03); padding: 24px; transition: box-shadow .35s cubic-bezier(0.32,0.72,0,1); }

.ta-head { margin-bottom: 24px; }
.ta-title { font-size: 20px; font-weight: 600; color: #1e293b; margin: 0 0 4px; }
.ta-desc { font-size: 13px; color: #94a3b8; margin: 0; }

.chart-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;
}
@media (max-width: 768px) { .chart-grid { grid-template-columns: 1fr; } }

.chart-card {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 20px;
}
.chart-card.wide { margin-bottom: 16px; }

.chart-head {
  display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 12px;
}

.chart-ttl {
  font-size: 15px; font-weight: 600; color: #1e293b; margin: 0;
}

.chart-sub { font-size: 12px; color: #94a3b8; }

.chart-body { height: 340px; }

.chart-body--tall { height: 300px; }
</style>
