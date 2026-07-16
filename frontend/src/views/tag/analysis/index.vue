<template>
  <div class="p-5" v-loading="loading">
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-5">
      <!-- RFM 用户分层 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title"><ArtSvgIcon icon="ri:bar-chart-grouped-line" class="mr-1" />RFM 用户价值分层</h3>
          <span class="chart-subtitle">综合消费频率、金额、最近消费</span>
        </div>
        <div ref="rfmChart" style="height: 360px"></div>
      </div>

      <!-- 消费能力等级 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title"><ArtSvgIcon icon="ri:cup-line" class="mr-1" />消费能力等级分布</h3>
          <span class="chart-subtitle">基于消费金额分层</span>
        </div>
        <div ref="consumeChart" style="height: 360px"></div>
      </div>
    </div>

    <!-- 偏好品类 -->
    <div class="chart-card mb-5">
      <div class="chart-header">
        <h3 class="chart-title"><ArtSvgIcon icon="ri:store-2-line" class="mr-1" />用户偏好品类分布</h3>
        <span class="chart-subtitle">按浏览行为加权评分排名</span>
      </div>
      <div ref="favChart" style="height: 340px"></div>
    </div>

    <!-- 活跃度分布 -->
    <div class="chart-card">
      <div class="chart-header">
        <h3 class="chart-title"><ArtSvgIcon icon="ri:pulse-line" class="mr-1" />用户活跃度分布</h3>
        <span class="chart-subtitle">基于近30日登录和浏览行为</span>
      </div>
      <div ref="activeChart" style="height: 300px"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { fetchTagDistribution } from '@/api/profile'
import * as echarts from 'echarts'

defineOptions({ name: 'TagAnalysis' })
const loading = ref(false)
const rfmChart = ref()
const consumeChart = ref()
const favChart = ref()
const activeChart = ref()
const charts: echarts.ECharts[] = []

function groupTags(tags: any[], code: string) {
  return tags.filter((t: any) => t.tagCode === code)
}

const rfmLabelMap: Record<string, string> = {
  HIGH_VALUE: '高价值用户', POTENTIAL: '潜力用户', GENERAL: '一般用户',
  AT_RISK: '流失风险用户', LOW_VALUE: '低价值用户'
}
const rfmColorMap: Record<string, string> = {
  HIGH_VALUE: '#13DEB9', POTENTIAL: '#5D87FF', GENERAL: '#949eb7',
  AT_RISK: '#FFAE1F', LOW_VALUE: '#FF4D4F'
}
const consumeLabelMap: Record<string, string> = {
  High: '高消费', Medium: '中等消费', Low: '低消费'
}
const consumeColorMap: Record<string, string> = {
  High: '#FFAE1F', Medium: '#5D87FF', Low: '#13DEB9'
}
const categoryLabelMap: Record<string, string> = {
  '1': '数码产品', '2': '服装鞋包', '3': '家居生活', '4': '食品饮料', '5': '美妆个护'
}
const activeLabelMap: Record<string, string> = {
  High: '高活跃', Medium: '中活跃', Low: '低活跃'
}
const activeColorMap: Record<string, string> = {
  High: '#13DEB9', Medium: '#5D87FF', Low: '#949eb7'
}

onMounted(async () => {
  loading.value = true
  let tags: any[] = []
  try { const res = await fetchTagDistribution(); tags = (res as any[]) || [] } catch {} finally { loading.value = false }
  await nextTick()

  // RFM 分层 —— 环形图
  const rfm = groupTags(tags, 'RFM_SEGMENT')
  if (rfmChart.value && rfm.length) {
    const chart = echarts.init(rfmChart.value)
    charts.push(chart)
    const data = rfm.map(t => ({
      name: rfmLabelMap[t.tagValue] || t.tagValue,
      value: t.userCount,
      itemStyle: { color: rfmColorMap[t.tagValue] || '#8c8c8c' }
    }))
    chart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}<br/>人数：{c} 人 ({d}%)' },
      legend: { orient: 'horizontal', bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12 } },
      series: [{
        type: 'pie', radius: ['45%', '72%'], center: ['50%', '42%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{d}%', fontSize: 12 },
        emphasis: { label: { fontSize: 14, fontWeight: 'bold' }, itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' } },
        data
      }]
    })
  }

  // 消费能力 —— 玫瑰图
  const consume = groupTags(tags, 'CONSUMPTION_LEVEL')
  if (consumeChart.value && consume.length) {
    const chart = echarts.init(consumeChart.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}<br/>人数：{c} 人 ({d}%)' },
      legend: { orient: 'horizontal', bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12 } },
      series: [{
        type: 'pie', roseType: 'area', radius: ['25%', '75%'], center: ['50%', '42%'],
        itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c} 人', fontSize: 12 },
        data: consume.map(t => ({
          name: consumeLabelMap[t.tagValue] || t.tagValue,
          value: t.userCount,
          itemStyle: { color: consumeColorMap[t.tagValue] || '#8c8c8c' }
        }))
      }]
    })
  }

  // 偏好品类 —— 横向渐变柱状图
  const fav = groupTags(tags, 'FAVORITE_CATEGORY')
  if (favChart.value && fav.length) {
    const chart = echarts.init(favChart.value)
    charts.push(chart)
    const data = fav.filter((t: any) => t.tagValue !== 'Unknown')
      .map((t: any) => ({ name: categoryLabelMap[t.tagValue] || `品类${t.tagValue}`, value: t.userCount }))
      .sort((a: any, b: any) => b.value - a.value)
    chart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: (p: any) => `${p[0].name}：${p[0].value} 人` },
      grid: { left: '3%', right: '10%', bottom: '3%', top: '5%', containLabel: true },
      xAxis: { type: 'value', show: false },
      yAxis: { type: 'category', data: data.map(d => d.name).reverse(), axisLine: { show: false }, axisTick: { show: false }, axisLabel: { fontSize: 13 } },
      series: [{
        type: 'bar', data: data.map(d => d.value).reverse(), barWidth: 28,
        itemStyle: { borderRadius: [0, 8, 8, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#5D87FF' }, { offset: 1, color: '#A0C0FF' }]) },
        label: { show: true, position: 'right', formatter: '{c} 人', fontSize: 12, color: '#949eb7' }
      }]
    })
  }

  // 活跃度 —— 横向柱状图
  const active = groupTags(tags, 'ACTIVE_LEVEL')
  if (activeChart.value && active.length) {
    const chart = echarts.init(activeChart.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}<br/>人数：{c} 人 ({d}%)' },
      legend: { orient: 'horizontal', bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12 } },
      series: [{
        type: 'pie', radius: ['40%', '68%'], center: ['50%', '42%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c} 人 ({d}%)', fontSize: 12 },
        data: active.map(t => ({
          name: activeLabelMap[t.tagValue] || t.tagValue,
          value: t.userCount,
          itemStyle: { color: activeColorMap[t.tagValue] || '#8c8c8c' }
        }))
      }]
    })
  }

  window.addEventListener('resize', handleResize)
})

function handleResize() { charts.forEach(c => c.resize()) }
onUnmounted(() => { window.removeEventListener('resize', handleResize); charts.forEach(c => c.dispose()) })
</script>

<style scoped>
.chart-card {
  background: var(--default-box-color);
  border: 1px solid var(--default-border);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: none;
  transition: all .25s ease;
}
.chart-card:hover {
  box-shadow: 0 4px 20px rgba(93,135,255,0.10);
  transform: translateY(-2px);
}
.chart-header {
  display: flex; align-items: center; justify-content: space-between;
  padding-bottom: 14px; margin-bottom: 14px;
  border-bottom: 1px solid var(--art-gray-200);
}
.chart-title { font-size: 15px; font-weight: 700; color: var(--art-gray-900); display: flex; align-items: center; }
.chart-subtitle { font-size: 13px; color: var(--art-gray-500); }
</style>
