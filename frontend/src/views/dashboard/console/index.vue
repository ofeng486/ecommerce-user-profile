<template>
  <div class="min-dash" v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon closable @close="error = ''" class="mb-5">
      <template #default><el-button type="primary" size="small" @click="loadData" class="ml-3">重试</el-button></template>
    </el-alert>
    <div class="stats-grid">
      <div class="stat-card" v-for="(s,i) in stats" :key="i">
        <div class="stat-top"><div class="stat-dot" :style="{background:s.color}"></div><span class="stat-label">{{ s.label }}</span></div>
        <div class="stat-value">{{ s.val }}</div><div class="stat-meta">{{ s.meta }}</div>
      </div>
    </div>
    <div class="charts-grid">
      <div class="chart-card chart-wide">
        <div class="chart-head"><h3>用户价值分层概览</h3><span class="chart-sub">RFM 五分类</span></div>
        <div v-if="!loading && !hasSegments" class="chart-empty">暂无分层数据，请先运行画像分析任务</div>
        <div ref="segmentChart" class="chart-box"></div>
      </div>
      <div class="chart-card">
        <div class="chart-head"><h3>画像覆盖率</h3><span class="chart-sub">已分析 / 总用户</span></div>
        <div ref="coverageChart" class="chart-box"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { fetchOverview, fetchSegmentDistribution } from '@/api/profile'
import * as echarts from 'echarts'
defineOptions({ name: 'DashboardConsole' })
const loading=ref(false); const error=ref(''); const hasSegments=ref(false)
const overview=ref({totalUsers:0,profiledUsers:0,highValueUsers:0,totalPaymentAmount:0})
const segmentChart=ref(); const coverageChart=ref()
const charts:echarts.ECharts[]=[]
const rfmNames:Record<string,string>={HIGH_VALUE:'高价值用户',POTENTIAL:'潜力用户',GENERAL:'一般用户',AT_RISK:'流失风险用户',LOW_VALUE:'低价值用户'}
const rfmColors:Record<string,string>={HIGH_VALUE:'#13DEB9',POTENTIAL:'#5D87FF',GENERAL:'#949eb7',AT_RISK:'#FFAE1F',LOW_VALUE:'#FF4D4F'}
const stats=ref([{label:'总用户数',val:'0',meta:'电商用户总量',color:'#5D87FF'},{label:'已画像用户',val:'0',meta:'完成 RFM 分析',color:'#A0C0FF'},{label:'高价值用户',val:'0',meta:'高消费+高频次',color:'#13DEB9'},{label:'累计消费金额',val:'¥0',meta:'全平台支付总额',color:'#FFAE1F'}])
function fmtN(v:number){return (v??0).toLocaleString('zh-CN')}
function fmtA(v:number){return v?(v>=1e8?(v/1e8).toFixed(2)+'亿':v>=1e4?(v/1e4).toFixed(1)+'万':v.toLocaleString()):'—'}
async function loadData(){
  loading.value=true; error.value=''
  try{
    const [ovR,sgR]=await Promise.allSettled([fetchOverview(),fetchSegmentDistribution()])
    const fl:string[]=[]; if(ovR.status==='rejected') fl.push('概览'); if(sgR.status==='rejected') fl.push('分层')
    if(fl.length){error.value='加载失败：'+fl.join('、')+'，请检查后端服务'}
    if(ovR.status==='fulfilled'&&ovR.value){const o=ovR.value as any; overview.value=o; stats.value[0].val=fmtN(o.totalUsers); stats.value[1].val=fmtN(o.profiledUsers); stats.value[2].val=fmtN(o.highValueUsers); stats.value[3].val='¥'+fmtA(o.totalPaymentAmount)}
    const segs=sgR.status==='fulfilled'?(sgR.value as any[])||[]:[]; hasSegments.value=segs.length>0
    loading.value=false; await nextTick(); renderCharts(segs)
  }catch(e:any){loading.value=false; error.value='加载异常：'+(e?.message||'未知错误')}
}
function renderCharts(segs:any[]){charts.forEach(c=>c.dispose());charts.length=0
  if(segmentChart.value&&segs.length){const c=echarts.init(segmentChart.value);charts.push(c);c.setOption({tooltip:{trigger:'item',formatter:'{b}<br/>人数：{c} 人 ({d}%)'},legend:{orient:'horizontal',bottom:0,itemWidth:8,itemHeight:8,itemGap:16,textStyle:{fontSize:12,color:'#949eb7'}},series:[{type:'pie',radius:['50%','75%'],center:['50%','45%'],itemStyle:{borderRadius:6,borderColor:'#fff',borderWidth:3},label:{formatter:'{b}\n{d}%',fontSize:11,color:'#949eb7'},emphasis:{itemStyle:{shadowBlur:8,shadowColor:'rgba(0,0,0,0.06)'},label:{fontSize:14,fontWeight:'bold'}},data:segs.map((s:any)=>({name:rfmNames[s.segmentCode]||s.segmentName,value:s.userCount,itemStyle:{color:rfmColors[s.segmentCode]||'#949eb7'}}))}]})}
  if(coverageChart.value){const c=echarts.init(coverageChart.value);charts.push(c);const t=overview.value.totalUsers||0;const p=overview.value.profiledUsers||0;const r=t>0?Math.round((p/t)*100):0;c.setOption({series:[{type:'gauge',startAngle:200,endAngle:-20,min:0,max:100,progress:{show:true,width:22,roundCap:true,itemStyle:{color:'#5D87FF'}},axisLine:{lineStyle:{width:22,color:[[1,'#e2e8ee']]}},pointer:{show:false},axisTick:{show:false},splitLine:{show:false},axisLabel:{show:false},data:[{value:r}],detail:{valueAnimation:true,fontSize:32,fontWeight:'bold',color:'#323251',offsetCenter:[0,'10%'],formatter:'{value}%'},title:{show:true,fontSize:13,color:'#949eb7',offsetCenter:[0,'40%']}}],graphic:[{type:'text',left:'center',top:'65%',style:{text:`${p} / ${t} 人`,fontSize:13,fill:'#949eb7'}}]})}
}
const handleResize=()=>charts.forEach(c=>c.resize())
onMounted(()=>{loadData();window.addEventListener('resize',handleResize)})
onUnmounted(()=>{window.removeEventListener('resize',handleResize);charts.forEach(c=>c.dispose())})
</script>

<style scoped>
.min-dash{padding:28px 32px;background:var(--default-bg-color);min-height:100vh}
.stats-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:24px}
.stat-card{background:var(--default-box-color);border:1px solid var(--default-border);border-radius:10px;padding:22px 24px;transition:all .25s ease;cursor:default}
.stat-card:hover{box-shadow:0 4px 20px rgba(93,135,255,0.12);transform:translateY(-2px)}
.stat-top{display:flex;align-items:center;gap:8px;margin-bottom:10px}
.stat-dot{width:8px;height:8px;border-radius:2px;flex-shrink:0}
.stat-label{font-size:13px;color:var(--art-gray-500);font-weight:500}
.stat-value{font-size:28px;font-weight:700;color:var(--art-gray-900);font-variant-numeric:tabular-nums}
.stat-meta{font-size:11px;color:var(--art-gray-400);margin-top:4px}
.charts-grid{display:grid;grid-template-columns:2fr 1fr;gap:20px}
.chart-card{background:var(--default-box-color);border:1px solid var(--default-border);border-radius:12px;padding:24px;transition:all .25s ease}
.chart-card:hover{box-shadow:0 4px 20px rgba(93,135,255,0.12);transform:translateY(-2px)}
.chart-head{display:flex;align-items:center;justify-content:space-between;padding-bottom:16px;margin-bottom:8px;border-bottom:1px solid var(--art-gray-200)}
.chart-head h3{font-size:15px;font-weight:600;color:var(--art-gray-900)}
.chart-sub{font-size:12px;color:var(--art-gray-400)}
.chart-empty{display:flex;align-items:center;justify-content:center;height:370px;color:var(--art-gray-300);font-size:13px}
.chart-box{height:370px}
@media(max-width:768px){.min-dash{padding:16px}.stats-grid{grid-template-columns:repeat(2,1fr)}.charts-grid{grid-template-columns:1fr}}
</style>
