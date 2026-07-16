<template>
  <div class="comp-page">
    <div class="page-header">
      <h2>画像对比分析</h2>
      <p class="page-desc">选择两个人群包，从年龄、性别、品类偏好等多维度对比用户画像差异</p>
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
        <div class="select-vs">VS</div>
        <div class="select-item">
          <span class="select-label">人群包 B</span>
          <el-select v-model="groupBId" placeholder="选择人群包" size="default" class="select-input" filterable>
            <el-option v-for="p in packages" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </div>
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
      <div class="charts-row-2">
        <div class="chart-card">
          <div class="chart-head">性别比例</div>
          <div ref="genderChart" class="chart-box-sm"></div>
        </div>
        <div class="chart-card">
          <div class="chart-head">品类偏好 (雷达图)</div>
          <div ref="radarChart" class="chart-box-sm"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { fetchAudiencePackages, compareAudienceProfiles } from '@/api/admin'

defineOptions({ name: 'ProfileComparison' })

const packages = ref<{ id: number; name: string }[]>([])
const groupAId = ref<number | null>(null)
const groupBId = ref<number | null>(null)
const comparing = ref(false)
const result = ref<any>(null)

const ageChart = ref(); const genderChart = ref(); const radarChart = ref(); const consumeChart = ref()
const charts: echarts.ECharts[] = []

const colorA = '#3b82f6'; const colorB = '#f97316'

// 标签映射
const genderLabelMap: Record<string,string> = { Male:'男', Female:'女', Unknown:'未知' }
const tagLabelMap: Record<string,string> = {
  '1':'数码产品','2':'服装鞋包','3':'家居生活','4':'食品饮料','5':'美妆个护',
  High:'高消费/高活跃', Medium:'中等', Low:'低消费/低活跃',
  HIGH_VALUE:'高价值用户', POTENTIAL:'潜力用户', GENERAL:'一般用户', AT_RISK:'流失风险用户', LOW_VALUE:'低价值用户',
}

const mapLabel = (m: Record<string,string>, s: string) => m[s] || s

// ── Mock 数据 ──
const mockResult = {
  groupAName: '高价值女性用户', groupBName: '新注册用户',
  groupACount: 1250, groupBCount: 3420,
  dimensions: [
    { dimension: 'age', label: '年龄段分布', items: [
      { label: '18-24岁', countA: 280, countB: 1200, ratioA: 0.224, ratioB: 0.351, diff: 0 },
      { label: '25-30岁', countA: 420, countB: 980, ratioA: 0.336, ratioB: 0.287, diff: 0 },
      { label: '31-40岁', countA: 350, countB: 720, ratioA: 0.280, ratioB: 0.211, diff: 0 },
      { label: '41-50岁', countA: 150, countB: 380, ratioA: 0.120, ratioB: 0.111, diff: 0 },
      { label: '50岁以上', countA: 50, countB: 140, ratioA: 0.040, ratioB: 0.041, diff: 0 },
    ]},
    { dimension: 'gender', label: '性别分布', items: [
      { label: '男', countA: 250, countB: 2050, ratioA: 0.20, ratioB: 0.60, diff: 0 },
      { label: '女', countA: 1000, countB: 1370, ratioA: 0.80, ratioB: 0.40, diff: 0 },
    ]},
    { dimension: 'tag', label: '品类偏好', items: [
      { label: '数码产品', countA: 380, countB: 1200, ratioA: 0.304, ratioB: 0.351, diff: 0 },
      { label: '服装鞋包', countA: 750, countB: 950, ratioA: 0.600, ratioB: 0.278, diff: 0 },
      { label: '家居生活', countA: 500, countB: 800, ratioA: 0.400, ratioB: 0.234, diff: 0 },
      { label: '食品饮料', countA: 620, countB: 1600, ratioA: 0.496, ratioB: 0.468, diff: 0 },
      { label: '美妆个护', countA: 820, countB: 1050, ratioA: 0.656, ratioB: 0.307, diff: 0 },
    ]},
  ]
}

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

// ── Charts ──
function renderCharts() {
  charts.forEach(c => c.dispose()); charts.length = 0
  if (!result.value) return

  const dims: Record<string, any> = {}
  result.value.dimensions?.forEach((d: any) => { dims[d.dimension] = d })

  // Age bar
  if (ageChart.value && dims.age) {
    const c = echarts.init(ageChart.value); charts.push(c)
    const items = dims.age.items
    c.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: [result.value.groupAName, result.value.groupBName], bottom: 0, textStyle: { fontSize: 11 } },
      grid: { left: 8, right: 8, top: 20, bottom: 35 },
      xAxis: { type: 'category', data: items.map((i: any) => i.label), axisLabel: { fontSize: 10, color: '#6b7280' } },
      yAxis: { type: 'value', axisLabel: { fontSize: 10, color: '#9ca3af' }, splitLine: { lineStyle: { color: '#f3f4f6' } } },
      series: [
        { name: result.value.groupAName, type: 'bar', data: items.map((i: any) => i.countA), barWidth: 16, itemStyle: { color: colorA, borderRadius: [4, 4, 0, 0] } },
        { name: result.value.groupBName, type: 'bar', data: items.map((i: any) => i.countB), barWidth: 16, itemStyle: { color: colorB, borderRadius: [4, 4, 0, 0] } },
      ]
    })
  }

  // Consumption level bar
  if (consumeChart.value && dims.consumption) {
    const c = echarts.init(consumeChart.value); charts.push(c)
    const items = dims.consumption.items
    c.setOption({
      tooltip: { trigger:'axis', axisPointer:{ type:'shadow' } },
      legend: { data:[result.value.groupAName,result.value.groupBName], bottom:0, textStyle:{fontSize:11} },
      grid: { left:8, right:8, top:20, bottom:35 },
      xAxis: { type:'category', data:items.map((i:any)=>i.label), axisLabel:{fontSize:11,color:'#6b7280'} },
      yAxis: { type:'value', axisLabel:{fontSize:10,color:'#9ca3af'}, splitLine:{lineStyle:{color:'#f3f4f6'}} },
      series: [
        { name:result.value.groupAName, type:'bar', data:items.map((i:any)=>i.countA), barWidth:16, itemStyle:{color:colorA,borderRadius:[4,4,0,0]} },
        { name:result.value.groupBName, type:'bar', data:items.map((i:any)=>i.countB), barWidth:16, itemStyle:{color:colorB,borderRadius:[4,4,0,0]} },
      ]
    })
  }

  // Gender
  if (genderChart.value && dims.gender) {
    const c = echarts.init(genderChart.value); charts.push(c)
    const items = dims.gender.items
    const ma = items.find((i:any)=>i.label==='Male')?.countA||0
    const fa = items.find((i:any)=>i.label==='Female')?.countA||0
    const mb = items.find((i:any)=>i.label==='Male')?.countB||0
    const fb = items.find((i:any)=>i.label==='Female')?.countB||0
    const na = result.value.groupAName; const nb = result.value.groupBName
    const labelA = na.length > 6 ? na.substring(0,5)+'…' : na
    const labelB = nb.length > 6 ? nb.substring(0,5)+'…' : nb
    c.setOption({
      tooltip: { trigger:'item', formatter:'{b}<br/>人数：{c} 人 ({d}%)' },
      legend: { orient:'horizontal', bottom:0, itemWidth:8, itemHeight:8, itemGap:14, textStyle:{fontSize:11,color:'#6b7280'}},
      series: [{ type:'pie', radius:['45%','72%'], center:['50%','43%'],
        itemStyle:{ borderRadius:6, borderColor:'#fff', borderWidth:3 },
        label:{ formatter:'{d}%', fontSize:12, color:'#6b7280' },
        emphasis:{ itemStyle:{ shadowBlur:8, shadowColor:'rgba(0,0,0,0.06)' }, label:{fontSize:14,fontWeight:'bold'} },
        data: [
          { name:na+'·男', value:ma, itemStyle:{color:'#3b82f6'} },
          { name:na+'·女', value:fa, itemStyle:{color:'#ec4899'} },
          { name:nb+'·男', value:mb, itemStyle:{color:'#93c5fd'} },
          { name:nb+'·女', value:fb, itemStyle:{color:'#f9a8d4'} },
        ]
      }]
    })
  }

  // Radar
  if (radarChart.value && dims.tag) {
    const c = echarts.init(radarChart.value); charts.push(c)
    const items = dims.tag.items
    const indicator = items.map((i: any) => ({ name: mapLabel(tagLabelMap, i.label), max: Math.max(i.countA, i.countB) * 1.3 || 100 }))
    c.setOption({
      tooltip: {},
      legend: { data: [result.value.groupAName, result.value.groupBName], bottom: 0, textStyle: { fontSize: 10, color: '#6b7280' }, itemWidth: 10, itemHeight: 10, itemGap: 16 },
      radar: { indicator, center: ['50%', '48%'], radius: '65%', axisName: { fontSize: 9, color: '#6b7280' }, splitArea: { areaStyle: { color: ['rgba(59,130,246,0.02)', 'rgba(59,130,246,0.02)'] } }, splitLine: { lineStyle: { color: '#e5e7eb' } }, axisLine: { lineStyle: { color: '#d1d5db' } } },
      series: [{
        type: 'radar', data: [
          { name: result.value.groupAName, value: items.map((i: any) => i.countA), itemStyle: { color: colorA }, lineStyle: { color: colorA, width: 2 }, areaStyle: { color: 'rgba(59,130,246,0.08)' }, symbol: 'circle', symbolSize: 4 },
          { name: result.value.groupBName, value: items.map((i: any) => i.countB), itemStyle: { color: colorB }, lineStyle: { color: colorB, width: 2 }, areaStyle: { color: 'rgba(249,115,22,0.08)' }, symbol: 'circle', symbolSize: 4 },
        ]
      }]
    })
  }

  window.addEventListener('resize', handleResize)
}

function handleResize() { charts.forEach(c => c.resize()) }
onUnmounted(() => { window.removeEventListener('resize', handleResize); charts.forEach(c => c.dispose()) })
</script>

<style scoped>
.comp-page { padding: 28px 32px; max-width: 1100px; font-size: 15px; }
.page-header { margin-bottom: 22px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: var(--art-gray-900); margin: 0 0 6px; }
.page-desc { font-size: 14px; color: var(--art-gray-500); margin: 0; }
.page-desc { font-size: 13px; color: var(--art-gray-500); margin: 0; }

.select-card { background: var(--default-box-color); border: 1px solid var(--default-border); border-radius: 12px; padding: 20px 24px; margin-bottom: 20px; box-shadow: 0 1px 2px rgba(0,0,0,.03); }
.select-row { display: flex; align-items: flex-end; gap: 14px; flex-wrap: wrap; }
.select-item { display: flex; flex-direction: column; gap: 6px; }
.select-label { font-size: 12px; color: var(--art-gray-500); font-weight: 500; }
.select-input { width: 200px; }
.select-hint{color:var(--art-gray-500);font-size:13px;padding:6px 0}
.select-vs { font-size: 14px; font-weight: 800; color: var(--art-gray-300); padding-bottom: 8px; }
.select-summary { display: flex; align-items: center; gap: 10px; margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--art-gray-200); }
.summary-tag { font-size: 12px; padding: 4px 12px; border-radius: 6px; font-weight: 600; }
.a-tag { background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
.b-tag { background: #fff8e6; color: #FFAE1F; }
.summary-vs { font-size: 12px; color: var(--art-gray-300); font-weight: 700; }

.charts-grid { display: flex; flex-direction: column; gap: 16px; }
.charts-row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-card { background: var(--default-box-color); border: 1px solid var(--default-border); border-radius: 12px; padding: 20px; box-shadow: 0 1px 2px rgba(0,0,0,.03); }
.chart-head { font-size: 14px; font-weight: 700; color: var(--art-gray-800); margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid var(--art-gray-200); }
.chart-box { height: 320px; }
.chart-box-sm { height: 280px; }

@media (max-width: 768px) { .charts-row-2 { grid-template-columns: 1fr; } .select-input { width: 140px; } }
</style>
