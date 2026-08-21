<template>
  <div class="comp-page" :class="isAdminSide ? 'theme-admin' : 'theme-user'">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h2 class="page-title">画像对比分析</h2>
          <span class="title-tag">PROFILE COMPARISON</span>
        </div>
        <p class="page-desc">选择两个人群包，从年龄、性别、品类偏好等多维度对比用户画像差异。</p>
      </div>
    </div>

    <!-- 选择区 -->
    <div class="select-card">
      <div v-if="packages.length === 0 && !comparing" class="select-hint mb-3">
        请先在「智能圈选」中圈选并保存人群包，再回到这里进行对比分析
      </div>
      <div class="select-row">
        <div class="select-item">
          <span class="select-label">人群包 A</span>
          <el-select v-model="groupAId" placeholder="选择人群包" size="default" class="select-input" filterable>
            <el-option v-for="p in packages" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </div>
        <div class="select-vs">
          <span class="vs-text">VS</span>
          <el-tooltip content="交换 A/B 两个人群包" placement="top" :show-after="200">
            <button class="swap-btn" @click="swapGroups" :disabled="!groupAId || !groupBId" title="交换 A/B">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 16V4m0 0L3 8m4-4l4 4"/><path d="M17 8v12m0 0l4-4m-4 4l-4-4"/></svg>
            </button>
          </el-tooltip>
        </div>
        <div class="select-item">
          <span class="select-label">人群包 B</span>
          <el-select v-model="groupBId" placeholder="选择人群包" size="default" class="select-input" filterable>
            <el-option v-for="p in packages" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </div>
      </div>
      <div class="select-footer">
        <el-button type="primary" size="default" @click="doCompare" :loading="comparing" :disabled="!groupAId || !groupBId || groupAId === groupBId">
          开始对比
        </el-button>
      </div>
      <div v-if="result" class="select-summary">
        <span class="summary-tag a-tag">{{ result.groupAName }}：{{ result.groupACount }} 人</span>
        <span class="summary-vs">VS</span>
        <span class="summary-tag b-tag">{{ result.groupBName }}：{{ result.groupBCount }} 人</span>
      </div>
    </div>

    <!-- 差异洞察 -->
    <div v-if="result && topDiffs.length" class="insight-card">
      <div class="insight-head">
        <span class="insight-icon">!</span>
        <span class="insight-title">对比洞察</span>
        <span class="insight-sub">两组人群差异最明显的特征（占比差距 ≥ 5%）</span>
      </div>
      <div class="insight-list">
        <div v-for="(d, i) in topDiffs" :key="i" class="insight-item">
          <span class="insight-dim">{{ d.dimLabel }}</span>
          <span class="insight-label">{{ d.label }}</span>
          <span class="insight-diff">{{ diffText(d) }}</span>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="charts-grid" v-if="result">
      <div class="chart-card">
        <div class="chart-head">年龄段分布</div>
        <div ref="ageChart" class="chart-box"></div>
      </div>
      <div class="chart-card">
        <div class="chart-head">消费力等级分布</div>
        <div ref="consumeChart" class="chart-box"></div>
      </div>
      <div class="chart-card">
        <div class="chart-head">用户分层分布</div>
        <div ref="segmentChart" class="chart-box"></div>
      </div>
      <div class="chart-card">
        <div class="chart-head">活跃度分布</div>
        <div ref="activityChart" class="chart-box"></div>
      </div>
      <div class="chart-card">
        <div class="chart-head">性别比例</div>
        <div ref="genderChart" class="chart-box-sm"></div>
      </div>
      <div class="chart-card">
        <div class="chart-head">品类偏好分布</div>
        <div ref="radarChart" class="chart-box-tall"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { fetchAudiencePackages, compareAudienceProfiles } from '@/api/admin'

defineOptions({ name: 'ProfileComparison' })

const router = useRouter()
const route = useRoute()
/** 管理端蓝色 / 用户端青色（共享页面按路由自动切换） */
const isAdminSide = computed(() => !route.path.startsWith('/user'))

const packages = ref<{ id: number; name: string }[]>([])
const groupAId = ref<number | null>(null)
const groupBId = ref<number | null>(null)
const comparing = ref(false)
const result = ref<any>(null)

const ageChart = ref(); const genderChart = ref(); const radarChart = ref(); const consumeChart = ref()
const segmentChart = ref(); const activityChart = ref()
const charts: echarts.ECharts[] = []


/** 人群 A 主色：用户端青 / 管理端蓝（B 固定橙色形成对比） */
const colorA = computed(() => isAdminSide.value ? '#2563eb' : '#0d9488')
const colorB = '#f97316'
const colorASoft = computed(() => isAdminSide.value ? 'rgba(37,99,235,0.08)' : 'rgba(13,148,136,0.08)')
const colorBSoft = 'rgba(249,115,22,0.08)'

/** 性别英文 → 中文（后端 compareGender 返回 u.gender 原值 Male/Female） */
const mapGender = (s: string) => ({ Male: '男', Female: '女', Unknown: '未知' } as Record<string, string>)[s] || s

// ── Load packages ──
onMounted(async () => {
  try {
    const res = await fetchAudiencePackages({ page: 0, size: 100 }) as any
    const realList = Array.isArray(res) ? res : (res?.records ?? res?.list ?? res?.data ?? [])
    packages.value = realList.filter((p: any) => p.status !== 0).map((p: any) => ({ id: p.id ?? p.packageId, name: p.packageName ?? p.name }))
  } catch { packages.value = [] }
})

// ── Compare ──
async function doCompare() {
  if (!groupAId.value || !groupBId.value) return
  comparing.value = true
  try {
    const res = await compareAudienceProfiles(groupAId.value, groupBId.value) as any
    result.value = res?.data ?? res
  } catch (e: any) {
    ElMessage.error('对比失败：' + (e?.message || '请先保存人群包并确保后端服务正常'))
  } finally { comparing.value = false }
  await nextTick(); renderCharts()
}

/** 交换 A/B 人群包并重新对比 */
function swapGroups() {
  const t = groupAId.value
  groupAId.value = groupBId.value
  groupBId.value = t
  if (result.value) doCompare()
}

/** 人群包选择变化（A/B 任一）→ 清旧对比结果，避免图表显示"上一次"残留 */
watch([groupAId, groupBId], () => {
  result.value = null
  charts.forEach(c => c.dispose())
  charts.length = 0
})

/** 差异洞察：各维度内 |diff| 最大的项，取全维度 Top3（diff = ratioA - ratioB） */
const topDiffs = computed(() => {
  if (!result.value?.dimensions) return []
  const all: { dimLabel: string; label: string; diff: number; ratioA: number; ratioB: number }[] = []
  const toNum = (v: any) => v == null ? 0 : (typeof v === 'object' ? Number(v.value ?? v) : Number(v)) || 0
  result.value.dimensions.forEach((d: any) => {
    ;(d.items || []).forEach((it: any) => {
      const diff = toNum(it.diff)
      if (Math.abs(diff) < 0.05) return // 差异不足 5% 不提示
      all.push({ dimLabel: d.label, label: d.dimension === 'gender' ? mapGender(it.label) : it.label, diff, ratioA: toNum(it.ratioA), ratioB: toNum(it.ratioB) })
    })
  })
  return all.sort((a, b) => Math.abs(b.diff) - Math.abs(a.diff)).slice(0, 3)
})

/** 差异文案：A 显著 → "A 更高"，负值 → "B 更高" */
function diffText(d: any) {
  return d.diff > 0 ? `A 组更高（${(d.ratioA * 100).toFixed(0)}% vs ${(d.ratioB * 100).toFixed(0)}%）`
                    : `B 组更高（${(d.ratioB * 100).toFixed(0)}% vs ${(d.ratioA * 100).toFixed(0)}%）`
}

// ── Charts ──
/** 防御性占比→百分比（后端 BigDecimal 序列化为对象/字符串时 Number() 变 NaN，这里统一兜底） */
const pct = (v: any) => {
  if (v == null || v === '') return 0
  const n = typeof v === 'object' ? Number(v.value ?? v) : Number(v)
  return isNaN(n) ? 0 : Math.round(n * 100)
}

/** 占比→显示字符串（保留 1 位小数，避免 0.47% 显示成 0% 误导） */
const pctStr = (v: any, decimal = 1) => {
  if (v == null || v === '') return (0).toFixed(decimal) + '%'
  const n = typeof v === 'object' ? Number(v.value ?? v) : Number(v)
  if (isNaN(n)) return (0).toFixed(decimal) + '%'
  return (n * 100).toFixed(decimal) + '%'
}

/** 通用双占比柱状图（年龄/消费力/分层/活跃度）——两组各自归一化 100%，避免人数差异误导 */
function renderBar(el: any, dim: any, nameA: string, nameB: string) {
  if (!el || !dim) return
  const c = echarts.init(el); charts.push(c)
  const items = dim.items || []
  c.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' },
      formatter: (p: any) => {
        const it = items.find((i: any) => i.label === p[0]?.name)
        const lines = p.map((s: any) => {
          const isA = s.seriesName === nameA
          const cnt = isA ? it?.countA : it?.countB
          const r = isA ? it?.ratioA : it?.ratioB
          return `${s.marker}${s.seriesName}：${pctStr(r)}（${(cnt || 0).toLocaleString()} 人）`
        })
        return `${p[0]?.name}<br/>${lines.join('<br/>')}`
      } },
    legend: { data: [nameA, nameB], bottom: 0, textStyle: { fontSize: 11, color: '#64748b' }, itemWidth: 12, itemHeight: 6 },
    grid: { left: 8, right: 16, top: 24, bottom: 56 },
    xAxis: { type: 'category', data: items.map((i: any) => i.label), axisLabel: { fontSize: 11, color: '#64748b' }, axisLine: { show: false }, axisTick: { show: false } },
    yAxis: { type: 'value', max: 100, axisLabel: { fontSize: 10, color: '#94a3b8', formatter: '{value}%' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: [
      { name: nameA, type: 'bar', data: items.map((i: any) => pct(i.ratioA)), barWidth: 14, itemStyle: { color: colorA.value, borderRadius: [4, 4, 0, 0] } },
      { name: nameB, type: 'bar', data: items.map((i: any) => pct(i.ratioB)), barWidth: 14, itemStyle: { color: colorB, borderRadius: [4, 4, 0, 0] } },
    ]
  })
}

/** 100% 堆叠柱（消费力/用户分层等结构对比）——A/B 各一根柱，柱内分段显示各档占比，看"结构差异" */
function renderStack(el: any, dim: any, nameA: string, nameB: string) {
  if (!el || !dim) return
  const c = echarts.init(el); charts.push(c)
  const items = dim.items || []
  // 堆叠顺序：ECharts series 数组第一个在底、最后一个在顶
  // 后端消费力 items=[高,中,低]、分层按占比降序——反转后"业务高位"（高消费/高价值）落在柱顶
  const order = [...items].reverse()
  // 档位色（从底到顶）：灰 → 橙 → 主色 → 紫 → 蓝（顶段用主色突出"高"）
  const palette = ['#cbd5e1', '#f59e0b', colorA.value, '#8b5cf6', '#0ea5e9']
  c.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' },
      formatter: (p: any) => {
        const gi = p[0]?.dataIndex || 0  // 0=A / 1=B
        const gName = gi === 0 ? nameA : nameB
        const lines = p.map((s: any) => `${s.marker}${s.seriesName}：${s.value}%`)
        return `${gName}<br/>${lines.join('<br/>')}`
      } },
    legend: { data: order.map((i: any) => i.label), bottom: 0, textStyle: { fontSize: 10, color: '#64748b' }, itemWidth: 10, itemHeight: 6, itemGap: 8 },
    grid: { left: 8, right: 16, top: 24, bottom: 40 },
    xAxis: { type: 'category', data: [nameA, nameB], axisLabel: { fontSize: 11, color: '#64748b', overflow: 'truncate', width: 90 }, axisLine: { show: false }, axisTick: { show: false } },
    yAxis: { type: 'value', max: 100, axisLabel: { fontSize: 10, color: '#94a3b8', formatter: '{value}%' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: order.map((it: any, i: number) => ({
      name: it.label, type: 'bar', stack: 'total', barWidth: 56,
      itemStyle: { color: palette[i % palette.length], borderRadius: i === order.length - 1 ? [6, 6, 0, 0] : [0, 0, 0, 0] },
      data: [pct(it.ratioA), pct(it.ratioB)]
    }))
  })
}

function renderCharts() {
  charts.forEach(c => c.dispose()); charts.length = 0
  if (!result.value) return

  const dims: Record<string, any> = {}
  result.value.dimensions?.forEach((d: any) => { dims[d.dimension] = d })
  const nameA = result.value.groupAName
  const nameB = result.value.groupBName

  // 年龄段 / 活跃度：连续分布 → 分组柱状；消费力 / 用户分层：结构对比 → 100% 堆叠柱
  renderBar(ageChart.value, dims.age, nameA, nameB)
  renderStack(consumeChart.value, dims.consumption, nameA, nameB)
  renderStack(segmentChart.value, dims.segment, nameA, nameB)
  renderBar(activityChart.value, dims.activity, nameA, nameB)

  // 性别（环形：A·男/A·女/B·男/B·女）—— 过滤 value=0 段避免空位
  if (genderChart.value && dims.gender) {
    const c = echarts.init(genderChart.value); charts.push(c)
    const items = dims.gender.items
    const pick = (lbl: string) => items.find((i: any) => i.label === lbl)
    const ma = pick('Male')?.countA || 0, fa = pick('Female')?.countA || 0
    const mb = pick('Male')?.countB || 0, fb = pick('Female')?.countB || 0
    const labelA = nameA.length > 6 ? nameA.substring(0, 5) + '…' : nameA
    const labelB = nameB.length > 6 ? nameB.substring(0, 5) + '…' : nameB
    const segs: { name: string; value: number; color: string }[] = [
      { name: labelA + '·男', value: ma, color: colorA.value },
      { name: labelA + '·女', value: fa, color: '#ec4899' },
      { name: labelB + '·男', value: mb, color: colorA.value + '55' },
      { name: labelB + '·女', value: fb, color: '#f9a8d4' },
    ].filter(s => s.value > 0)  // 空段不画
    c.setOption({
      tooltip: { trigger: 'item', formatter: '{b}<br/>人数：{c} 人 ({d}%)' },
      legend: segs.length > 1 ? { orient: 'horizontal', bottom: 0, itemWidth: 8, itemHeight: 8, itemGap: 14, textStyle: { fontSize: 11, color: '#64748b' } } : { show: false },
      series: [{ type: 'pie', radius: ['45%', '72%'], center: ['50%', '43%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 3 },
        label: { formatter: '{d}%', fontSize: 12, color: '#64748b' },
        emphasis: { itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,0.06)' }, label: { fontSize: 14, fontWeight: 'bold' } },
        data: segs.map(s => ({ name: s.name, value: s.value, itemStyle: { color: s.color } }))
      }]
    })
  }

  // 品类偏好（双侧条形图 / Population Pyramid）——0 在中间，左 A 右 B，差异一眼看出
  if (radarChart.value && dims.tag) {
    const c = echarts.init(radarChart.value); charts.push(c)
    const items = [...dims.tag.items].sort((a: any, b: any) => (b.ratioA - b.ratioB) - (a.ratioA - a.ratioB))  // 按 A-B 差值降序（差异大靠上）
    // X 轴自适应：max = 两组最大占比 ×1.15 圆整到 5（避免固定 100 导致普通对比柱长浪费）
    const allPctNum = items.flatMap((i: any) => [Number(i.ratioA || 0), Number(i.ratioB || 0)])
    const maxRatio = Math.max(...allPctNum, 0.05)  // 至少 5%
    const xMax = Math.ceil(maxRatio * 1.15 * 100 / 5) * 5
    c.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' },
        formatter: (p: any) => {
          const it = items.find((i: any) => i.label === p[0]?.axisValue)
          const lines = p.map((s: any) => {
            const isA = s.seriesName === nameA
            const r = Math.abs(s.value)
            return `${s.marker}${s.seriesName}：${r}%${it ? `（${(isA ? it.countA : it.countB).toLocaleString()} 人）` : ''}`
          })
          return `${p[0]?.axisValue}<br/>${lines.join('<br/>')}`
        } },
      legend: { data: [nameA, nameB], bottom: 0, textStyle: { fontSize: 11, color: '#64748b' }, itemWidth: 12, itemHeight: 6 },
      grid: { left: 8, right: 16, top: 16, bottom: 56, containLabel: true },
      xAxis: { type: 'value', min: -xMax, max: xMax, axisLabel: { fontSize: 10, color: '#94a3b8', formatter: (v: number) => Math.abs(v) + '%' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
      yAxis: { type: 'category', data: items.map((i: any) => i.label), axisLabel: { fontSize: 11, color: '#64748b' }, axisLine: { show: false }, axisTick: { show: false } },
      series: [
        // 左半：A 占比（用负数向左延伸）
        { name: nameA, type: 'bar', stack: 'pp',
          data: items.map((i: any) => -pct(i.ratioA)),
          barWidth: 16, itemStyle: { color: colorA.value, borderRadius: [6, 0, 0, 6] },
          label: { show: true, position: 'left', fontSize: 10, color: '#475569', formatter: (p: any) => `${Math.abs(p.value)}%` } },
        // 右半：B 占比（正数向右延伸）
        { name: nameB, type: 'bar', stack: 'pp',
          data: items.map((i: any) => pct(i.ratioB)),
          barWidth: 16, itemStyle: { color: colorB, borderRadius: [0, 6, 6, 0] },
          label: { show: true, position: 'right', fontSize: 10, color: '#b45309', formatter: (p: any) => `${p.value}%` } }
      ]
    })
  }

  window.addEventListener('resize', handleResize)
}

function handleResize() { charts.forEach(c => c.resize()) }
onUnmounted(() => { window.removeEventListener('resize', handleResize); charts.forEach(c => c.dispose()) })
</script>

<style scoped>
.comp-page { padding: 28px 32px; width: 100%; font-size: 15px; }
/* 主题变量族：管理端蓝 / 用户端青 */
.theme-admin.comp-page { --acc: #2563eb; --acc-dark: #1d4ed8; --acc-soft: rgba(37,99,235,.08); --acc-line: #93c5fd; }
.theme-user.comp-page { --acc: #0d9488; --acc-dark: #0f766e; --acc-soft: rgba(13,148,136,.08); --acc-line: #5eead4; }
/* 用户端 Element 组件主色（开始对比按钮/下拉选中态等） */
.theme-user.comp-page { --el-color-primary: #0d9488; --el-color-primary-dark-2: #0f766e; }
/* ═══ 页面头部（企业级统一风格） ═══ */
.page-header {
  display: flex; align-items: flex-end; justify-content: space-between; gap: 20px;
  margin-bottom: 22px; padding-bottom: 18px;
  border-bottom: 1px solid #eef2f6;
}
.ph-left { min-width: 0; }
.ph-title-row { display: flex; align-items: center; gap: 10px; }
.title-accent {
  width: 4px; height: 20px; border-radius: 2px; flex-shrink: 0;
  background: linear-gradient(180deg, #2563eb 0%, #60a5fa 100%);
}
.theme-user .title-accent { background: linear-gradient(180deg, #0d9488 0%, #5eead4 100%); }
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

.select-card { background: var(--default-box-color); border: 1px solid rgba(15,23,42,0.06); border-radius: 16px; padding: 22px 24px; margin-bottom: 16px; box-shadow: 0 6px 22px rgba(15,23,42,0.04); transition: box-shadow .3s cubic-bezier(0.32,0.72,0,1); }
.select-row { display: grid; grid-template-columns: 1fr auto 1fr; gap: 24px; align-items: end; }
.select-item { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.select-label { font-size: 12px; color: #949eb7; font-weight: 500; }
.select-input { width: 100%; }
.select-hint{color:#949eb7;font-size:13px;padding:6px 0}
.select-vs { display: flex; flex-direction: column; align-items: center; gap: 6px; padding-bottom: 4px; min-width: 60px; }
.vs-text { font-size: 15px; font-weight: 800; color: #e6eaeb; letter-spacing: 1px; }
.swap-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; border-radius: 6px;
  border: 1px solid var(--default-border); background: var(--default-box-color);
  color: #dbdfe1; cursor: pointer; transition: all .15s;
}
.swap-btn:hover:not(:disabled) { color: var(--acc); border-color: var(--acc-line); transform: rotate(180deg); }
.swap-btn:disabled { opacity: .4; cursor: not-allowed; }
.select-footer { display: flex; justify-content: flex-end; margin-top: 16px; padding-top: 14px; border-top: 1px dashed #f2f4f5; }
.select-summary { display: flex; align-items: center; gap: 10px; margin-top: 14px; padding-top: 12px; border-top: 1px solid #f2f4f5; }
.summary-tag { font-size: 12px; padding: 4px 12px; border-radius: 6px; font-weight: 600; }
.a-tag { background: var(--acc-soft); color: var(--acc); }
.b-tag { background: #fff8e6; color: #FFAE1F; }
.summary-vs { font-size: 12px; color: #e6eaeb; font-weight: 700; }

/* ─── 对比洞察横幅 ─── */
.insight-card {
  background: var(--default-box-color); border: 1px solid var(--default-border);
  border-left: 3px solid var(--acc); border-radius: 12px;
  padding: 14px 20px; margin-bottom: 16px; box-shadow: 0 1px 2px rgba(0,0,0,.03);
}
.insight-head { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.insight-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; border-radius: 50%;
  background: var(--acc); color: #fff; font-size: 12px; font-weight: 700;
}
.insight-title { font-size: 13.5px; font-weight: 700; color: #0f172a; }
.insight-sub { font-size: 11.5px; color: #94a3b8; margin-left: 4px; }
.insight-list { display: flex; flex-wrap: wrap; gap: 10px 24px; }
.insight-item { display: flex; align-items: center; gap: 8px; font-size: 12.5px; }
.insight-dim { color: #94a3b8; background: #f8fafc; border: 1px solid #eef2f6; border-radius: 4px; padding: 1px 6px; font-size: 11px; }
.insight-label { color: #334155; font-weight: 600; }
.insight-diff { color: var(--acc); font-weight: 600; }

/* ─── 图表区：2 列布局 ─── */
.charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-card { background: var(--default-box-color); border: 1px solid var(--default-border); border-radius: 12px; padding: 20px; box-shadow: 0 1px 2px rgba(0,0,0,.03); }
.chart-head { font-size: 14px; font-weight: 700; color: #383853; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid #f2f4f5; }
.chart-box { height: 280px; }
.chart-box-sm { height: 260px; }
.chart-box-tall { height: 380px; } /* 横向条形（9 个品类）专用 */

@media (max-width: 900px) { .charts-grid { grid-template-columns: 1fr; } .select-input { width: 140px; } }

/* ─── system/users 风格：人群包选择区覆写 ─── */
.select-input :deep(.el-select__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1.5px var(--default-border) inset;
  transition: box-shadow .2s;
}
.select-input :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1.5px var(--acc-line) inset;
}
.select-input :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1.5px var(--acc) inset, 0 0 0 3px var(--acc-soft);
}
/* 开始对比按钮：主色 + 微阴影 */
.select-row :deep(.el-button--primary) { box-shadow: 0 2px 8px var(--acc-soft); }
</style>
