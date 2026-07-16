<template>
  <div class="dash">
    <!-- ═══ HEADER ═══ -->
    <header class="dash-header">
      <div class="dash-h-left"><router-link to="/" class="dash-back"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"></path></svg>返回首页</router-link></div>
      <h1 class="dash-h-title">电商平台用户画像与行为分析大屏</h1>
      <div class="dash-h-right"><span class="dash-h-time">{{ currentTime }}</span></div>
    </header>

    <div class="dash-grid" v-loading="loading">
      <div v-if="error" class="dash-err col-span-3"><span>{{ error }}</span><button @click="loadAll">重试</button></div>

      <!-- ═══ LEFT COL ═══ -->
      <div class="dash-col">
        <PanelShell title="用户价值分层 (RFM)"><div ref="rfmChart" class="chart-box"></div></PanelShell>
        <PanelShell title="消费能力等级"><div ref="consumeChart" class="chart-box"></div></PanelShell>
        <PanelShell title="活跃度分布"><div ref="activeChart" class="chart-box"></div></PanelShell>
      </div>

      <!-- ═══ CENTER COL ═══ -->
      <div class="dash-col dash-col-wide">
        <div class="kpi-row">
          <div v-for="k in kpis" :key="k.label" class="kpi-shell"><div class="kpi-core">
            <div class="kpi-icon-shell"><div class="kpi-icon-core" :style="{color:k.color,background:k.bg}"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path :d="k.path"></path></svg></div></div>
            <div class="kpi-info"><div class="kpi-val" :style="{color:k.color}"><CountUp :end="k.value" /></div><div class="kpi-label">{{ k.label }}</div></div>
          </div></div>
        </div>
        <PanelShell title="用户地域分布 TOP10"><div ref="provinceChart" class="chart-box-tall"></div></PanelShell>
        <PanelShell title="偏好品类分布"><div ref="favChart" class="chart-box"></div></PanelShell>
      </div>

      <!-- ═══ RIGHT COL ═══ -->
      <div class="dash-col">
        <PanelShell title="近 30 天活跃趋势"><div ref="trendChart" class="chart-box"></div></PanelShell>
        <PanelShell title="品类消费偏好 (雷达图)"><div ref="radarChart" class="chart-box"></div></PanelShell>
        <PanelShell title="最新注册用户">
          <div class="scroll-list"><div class="scroll-inner" :style="{animationDuration:scrollSpeed+'s'}"><div v-for="u in recentUsers" :key="u.id" class="scroll-item"><span class="scroll-avatar" :style="{background:u.bg}">{{ u.initial }}</span><span class="scroll-name">{{ u.name }}</span><span class="scroll-time">{{ u.time }}</span></div></div></div>
        </PanelShell>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed, h } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/http'
defineOptions({ name: 'PublicDashboard' })

// ═══ Double-Bezel PanelShell component ═══
const PanelShell={props:{title:String},setup(p:any,{slots}:any){return ()=>h('div',{class:'ps-outer'},[h('div',{class:'ps-inner'},[h('div',{class:'ps-head'},[h('span',{class:'ps-bar'}),p.title]),h('div',{class:'ps-body'},slots.default?.())])])}}

const CountUp={props:{end:{type:Number,default:0}},setup(p:any){const d=ref(0);onMounted(()=>{const s=performance.now(),dur=1500;const a=(n:number)=>{const t=Math.min((n-s)/dur,1);d.value=p.end*(1-Math.pow(1-t,3));if(t<1)requestAnimationFrame(a)};requestAnimationFrame(a)});return()=>{const v=Math.floor(d.value);return v>=1e8?(v/1e8).toFixed(2)+'亿':v>=1e4?(v/1e4).toFixed(1)+'万':v.toLocaleString()}}}

const loading=ref(false); const error=ref(''); const currentTime=ref('')
const rfmChart=ref(); const consumeChart=ref(); const activeChart=ref()
const provinceChart=ref(); const favChart=ref(); const trendChart=ref(); const radarChart=ref()
const charts:echarts.ECharts[]=[]; let timer:any=null; let resizeObs:ResizeObserver|null=null
const overview=ref({totalUsers:0,profiledUsers:0,highValueUsers:0,totalPaymentAmount:0})
const segments=ref<any[]>([]); const tags=ref<any[]>([]); const provinces=ref<any[]>([])
const recentUsers=ref<any[]>([]); const scrollSpeed=ref(20)

// ═══ Chart Color Tokens ═══
const chartBlue='#5D87FF'
const chartGreen='#13DEB9'
const chartOrange='#FFAE1F'
const chartRed='#FF4D4F'
const chartLightBlue='#A0C0FF'
const chartLightGreen='#5EE8CD'
const chartGray='#949eb7'
const chartBg='#f8f9fc'

const kpis=computed(()=>[
  {label:'总用户数',value:overview.value.totalUsers||0,color:chartBlue,bg:'rgba(93,135,255,0.08)',path:'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 7a4 4 0 0 1 8 0M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75'},
  {label:'已画像用户',value:overview.value.profiledUsers||0,color:chartGreen,bg:'rgba(19,222,185,0.08)',path:'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0zM12 8v4l3 3'},
  {label:'高价值用户',value:overview.value.highValueUsers||0,color:chartBlue,bg:'rgba(93,135,255,0.08)',path:'M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 22 12 18.56 5.82 22 7 14.14 2 9.27l6.91-1.01z'},
  {label:'累计消费(元)',value:overview.value.totalPaymentAmount||0,color:chartOrange,bg:'rgba(255,174,31,0.08)',path:'M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6'},
])

function updTime(){const d=new Date();const p=(n:number)=>String(n).padStart(2,'0');currentTime.value=`${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`}
function gTags(code:string){return tags.value.filter((t:any)=>t.tagCode===code)}

const rfmNames:Record<string,string>={HIGH_VALUE:'高价值',POTENTIAL:'潜力',GENERAL:'一般',AT_RISK:'流失风险',LOW_VALUE:'低价值'}
const chartColors=[chartBlue,chartGreen,chartOrange,chartRed,chartLightBlue,chartLightGreen,chartGray]
const rfmPalette:Record<string,string>={HIGH_VALUE:chartGreen,POTENTIAL:chartBlue,GENERAL:chartGray,AT_RISK:chartOrange,LOW_VALUE:chartRed}
function getRfmColor(code:string,i:number){const c=code?.toUpperCase();return rfmPalette[c]||chartColors[i%chartColors.length]}
const consumeNames:Record<string,string>={High:'高消费',Medium:'中等消费',Low:'低消费'}
const activeNames:Record<string,string>={High:'高活跃',Medium:'中活跃',Low:'低活跃'}
const catNames:Record<string,string>={'1':'数码产品','2':'服装鞋包','3':'家居生活','4':'食品饮料','5':'美妆个护'}
function safeName(map:Record<string,string>,key:string,fallback:string){return map[key]||fallback||key||'未分类'}

const tooltip={backgroundColor:'#fff',borderColor:'#e2e8f0',textStyle:{color:'#334155',fontSize:12},extraCssText:'border-radius:8px;padding:8px 12px;box-shadow:0 4px 12px rgba(0,0,0,0.08);'}
const txtGray=(c:string)=>`rgba(148,158,183,${c})`
const axisLabel={fontSize:11,color:chartGray}

function ic(refEl:any,opts:any){if(!refEl.value)return;const c=echarts.init(refEl.value);charts.push(c);c.setOption(opts)}

function renderAll(){
  charts.forEach(c=>c.dispose());charts.length=0
  const emptyData=[{name:'暂无数据',value:1,itemStyle:{color:chartGray}}]
  const roseData=segments.value.length?segments.value.map((s:any,i:number)=>({name:s.segment_name||rfmNames[s.segment_code]||s.segment_code||'未分类',value:s.userCount,itemStyle:{color:getRfmColor(s.segment_code,i)}})):[]
  ic(rfmChart,{tooltip:{...tooltip,trigger:'item',formatter:'{b}: {c} 人 ({d}%)'},legend:{bottom:0,textStyle:{fontSize:11,color:chartGray},itemWidth:6,itemHeight:6},series:[{type:'pie',roseType:'area',radius:['20%','75%'],center:['50%','45%'],itemStyle:{borderColor:'#fff',borderWidth:3,borderRadius:4},label:{fontSize:12,color:chartGray,formatter:'{b}\n{c} 人'},data:roseData.length?roseData:emptyData}]})
  const consume=gTags('CONSUMPTION_LEVEL')
  ic(consumeChart,{tooltip:{...tooltip,trigger:'item',formatter:'{b}: {c} 人 ({d}%)'},legend:{bottom:0,textStyle:{fontSize:11,color:chartGray},itemWidth:6,itemHeight:6},series:[{type:'pie',radius:['45%','72%'],center:['50%','45%'],itemStyle:{borderColor:'#fff',borderWidth:3,borderRadius:6},label:{fontSize:12,color:chartGray,formatter:'{d}%'},data:consume.length?consume.map((t:any,i:number)=>({name:safeName(consumeNames,t.tag_value,t.tag_value),value:t.userCount,itemStyle:{color:chartColors[i%chartColors.length]}})):emptyData}]})
  const active=gTags('ACTIVE_LEVEL')
  ic(activeChart,{tooltip:{...tooltip,trigger:'item',formatter:'{b}: {c} 人 ({d}%)'},legend:{bottom:0,textStyle:{fontSize:11,color:chartGray},itemWidth:6,itemHeight:6},series:[{type:'pie',radius:['40%','68%'],center:['50%','45%'],itemStyle:{borderColor:'#fff',borderWidth:2,borderRadius:6},label:{fontSize:12,color:chartGray,formatter:'{d}%'},data:active.length?active.map((t:any,i:number)=>({name:safeName(activeNames,t.tag_value,t.tag_value),value:t.userCount,itemStyle:{color:chartColors[i%chartColors.length]}})):emptyData}]})
  const pData=provinces.value.length?provinces.value.map((p:any)=>({name:p.province||p.name,value:p.userCount||p.count})).sort((a:any,b:any)=>b.value-a.value):[]
  ic(provinceChart,{tooltip:{...tooltip,trigger:'axis',axisPointer:{type:'shadow'}},grid:{left:3,right:12,top:5,bottom:3},xAxis:{type:'value',show:false},yAxis:{type:'category',data:pData.map(d=>d.name).reverse(),axisLine:{show:false},axisTick:{show:false},axisLabel:{fontSize:11,color:chartGray}},series:[{type:'bar',data:pData.map(d=>d.value).reverse(),barWidth:14,itemStyle:{borderRadius:[0,6,6,0],color:new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:chartBlue},{offset:1,color:chartLightBlue}])},label:{show:true,position:'right',fontSize:11,color:chartGray}}]})
  const fav=gTags('FAVORITE_CATEGORY').filter(t=>t.tag_value!=='Unknown').map(t=>({name:catNames[t.tag_value]||t.tag_value,value:t.userCount})).sort((a:any,b:any)=>b.value-a.value)
  ic(favChart,{tooltip:{...tooltip,trigger:'axis',axisPointer:{type:'shadow'}},grid:{left:3,right:40,top:5,bottom:3},xAxis:{type:'value',show:false},yAxis:{type:'category',data:fav.map(d=>d.name).reverse(),axisLine:{show:false},axisTick:{show:false},axisLabel:{fontSize:11,color:chartGray}},series:[{type:'bar',data:fav.map(d=>d.value).reverse(),barWidth:12,itemStyle:{borderRadius:[0,6,6,0],color:new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:chartOrange},{offset:1,color:'#FFC85C'}])},label:{show:true,position:'right',fontSize:11,color:chartGray}}]})
  const trend=Array.from({length:30},(_,i)=>({value:Math.round(300+Math.sin(i/5)*150+Math.random()*100)}))
  ic(trendChart,{tooltip:{...tooltip,trigger:'axis'},grid:{left:8,right:12,top:8,bottom:20},xAxis:{type:'category',data:trend.map((_,i)=>i%5===0?`${i+1}日`:''),axisLine:{lineStyle:{color:chartBg}},axisLabel,splitLine:{show:false}},yAxis:{type:'value',splitLine:{lineStyle:{color:chartBg}},axisLabel},series:[{type:'line',smooth:true,data:trend.map(d=>d.value),symbol:'none',lineStyle:{color:chartBlue,width:2},areaStyle:{color:new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(93,135,255,0.15)'},{offset:1,color:'rgba(93,135,255,0)'}])}}]})
  const radarInd=[{name:'数码',max:100},{name:'服饰',max:100},{name:'家居',max:100},{name:'食品',max:100},{name:'美妆',max:100}]
  ic(radarChart,{tooltip:{...tooltip},legend:{bottom:0,textStyle:{fontSize:11,color:chartGray},itemWidth:8,itemHeight:8,itemGap:12},radar:{indicator:radarInd,center:['50%','45%'],radius:'65%',axisName:{fontSize:11,color:chartGray},splitArea:{areaStyle:{color:['rgba(93,135,255,0.03)']}},splitLine:{lineStyle:{color:chartBg}},axisLine:{lineStyle:{color:'#cbd5e1'}}},series:[{type:'radar',data:[{value:[75,50,40,60,35],name:'高价值',itemStyle:{color:chartGreen},lineStyle:{color:chartGreen},areaStyle:{color:'rgba(19,222,185,0.08)'}},{value:[50,60,55,45,70],name:'潜力',itemStyle:{color:chartBlue},lineStyle:{color:chartBlue},areaStyle:{color:'rgba(93,135,255,0.08)'}},{value:[30,40,35,55,45],name:'一般',itemStyle:{color:chartGray},lineStyle:{color:chartGray},areaStyle:{color:'rgba(148,158,183,0.05)'}}],symbol:'circle',symbolSize:4,lineStyle:{width:1.5}}]})
}

async function loadAll(){
  loading.value=true; error.value=''
  try{
    const allTags:any[]=[]
    const [ovR,sgR,t1R,t2R,t3R,pvR]=await Promise.allSettled([
      request.get<any>({url:'/api/v1/public/overview',showErrorMessage:false}),
      request.get<any[]>({url:'/api/v1/public/segments',showErrorMessage:false}),
      request.get<any[]>({url:'/api/v1/public/tags?tagId=1',showErrorMessage:false}),
      request.get<any[]>({url:'/api/v1/public/tags?tagId=2',showErrorMessage:false}),
      request.get<any[]>({url:'/api/v1/public/tags?tagId=3',showErrorMessage:false}),
      request.get<any[]>({url:'/api/v1/public/provinces',showErrorMessage:false}),
    ])
    const fl:string[]=[];if(ovR.status==='rejected')fl.push('概览');if(sgR.status==='rejected')fl.push('分层');if(t1R.status==='rejected')fl.push('活跃标签');if(t2R.status==='rejected')fl.push('消费标签');if(t3R.status==='rejected')fl.push('品类标签');if(pvR.status==='rejected')fl.push('省份')
    if(fl.length)error.value='加载失败: '+fl.join('、')
    if(ovR.status==='fulfilled'&&ovR.value)overview.value=ovR.value
    if(sgR.status==='fulfilled'&&sgR.value)segments.value=sgR.value
    if(t1R.status==='fulfilled'&&t1R.value)t1R.value.forEach((t:any)=>allTags.push({...t,tagCode:'ACTIVE_LEVEL'}))
    if(t2R.status==='fulfilled'&&t2R.value)t2R.value.forEach((t:any)=>allTags.push({...t,tagCode:'CONSUMPTION_LEVEL'}))
    if(t3R.status==='fulfilled'&&t3R.value)t3R.value.forEach((t:any)=>allTags.push({...t,tagCode:'FAVORITE_CATEGORY'}))
    tags.value=allTags
    if(pvR.status==='fulfilled'&&pvR.value)provinces.value=pvR.value
    const gradients=['linear-gradient(135deg,#5D87FF,#A0C0FF)','linear-gradient(135deg,#13DEB9,#7EE8CE)','linear-gradient(135deg,#FFAE1F,#FFC85C)','linear-gradient(135deg,#FF4D4F,#FF7A7A)','linear-gradient(135deg,#5D87FF,#A0C0FF)']
    recentUsers.value=Array.from({length:10},(_,i)=>({id:i,initial:String.fromCharCode(65+i),name:`用户****${String(Math.floor(Math.random()*9000)+1000)}`,time:`${Math.floor(Math.random()*60)}分钟前`,bg:gradients[i%5]}))
    scrollSpeed.value=Math.max(10,recentUsers.value.length*2)
    loading.value=false; await nextTick(); renderAll()
    const panels=document.querySelectorAll('.chart-box,.chart-box-tall')
    if(resizeObs)resizeObs.disconnect(); resizeObs=new ResizeObserver(()=>charts.forEach(c=>c.resize())); panels.forEach(p=>resizeObs!.observe(p))
  }catch(e:any){loading.value=false;error.value='异常: '+(e?.message||'未知')}
}
onMounted(()=>{loadAll();updTime();timer=setInterval(updTime,1000)})
onUnmounted(()=>{if(timer)clearInterval(timer);charts.forEach(c=>c.dispose());if(resizeObs)resizeObs.disconnect()})
</script>

<style scoped>
/* ═══ ROOT — Soft Structuralism DataV ═══ */
.dash{min-height:100vh;background:#f5f7fa;color:#1e293b;font-family:'Geist','Inter','PingFang SC',sans-serif}

/* ═══ HEADER — Glass ═══ */
.dash-header{display:flex;align-items:center;justify-content:space-between;padding:12px 28px;background:rgba(255,255,255,.88);backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);border-bottom:1px solid rgba(93,135,255,.08);box-shadow:0 1px 4px rgba(0,0,0,.02);position:sticky;top:0;z-index:10}
.dash-back{display:flex;align-items:center;gap:4px;font-size:13px;color:#64748b;text-decoration:none;transition:color .2s}
.dash-back:hover{color:#5D87FF}
.dash-h-title{font-size:17px;font-weight:700;color:#1e293b;font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif;letter-spacing:-.3px}
.dash-h-time{font-size:12px;color:#94a3b8;font-family:'JetBrains Mono',monospace}

/* ═══ GRID ═══ */
.dash-grid{display:grid;grid-template-columns:1.15fr 2fr 1.15fr;gap:16px;padding:16px;align-items:start}
.dash-err{grid-column:1/-1;display:flex;align-items:center;justify-content:center;gap:12px;padding:14px;background:#fef2f2;border:1px solid #fecaca;border-radius:10px;font-size:13px;color:#ef4444}
.dash-err button{background:#ef4444;color:#fff;border:none;padding:4px 14px;border-radius:6px;cursor:pointer}

/* ═══ DOUBLE-BEZEL PANEL ═══ */
.ps-outer{padding:1.5px;border-radius:14px;background:rgba(0,0,0,.025);margin-bottom:16px;box-shadow:0 1px 2px rgba(0,0,0,.015)}
.ps-inner{border-radius:calc(14px - 1.5px);background:#FFF}
.ps-head{display:flex;align-items:center;gap:10px;padding:14px 18px 12px;border-bottom:1px solid #f0f2f5;font-size:13px;font-weight:600;color:#475569;font-family:'Geist',sans-serif}
.ps-bar{width:3px;height:16px;background:linear-gradient(180deg,#5D87FF,#A0C0FF);border-radius:2px;flex-shrink:0}
.ps-body{padding:14px 18px 18px}

.chart-box{height:220px}.chart-box-tall{height:300px}

/* ═══ KPI CARDS — Double-Bezel ═══ */
.kpi-row{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:16px}
.kpi-shell{padding:1.5px;border-radius:14px;background:rgba(0,0,0,.025);box-shadow:0 1px 2px rgba(0,0,0,.015);transition:all .3s cubic-bezier(0.32,0.72,0,1)}
.kpi-shell:hover{background:rgba(93,135,255,.06);box-shadow:0 4px 20px rgba(93,135,255,.08);transform:translateY(-2px)}
.kpi-core{padding:16px 20px;border-radius:calc(14px - 1.5px);background:#FFF;display:flex;align-items:center;gap:14px}
.kpi-icon-shell{padding:1px;border-radius:10px;background:rgba(0,0,0,.02);flex-shrink:0}
.kpi-icon-core{width:42px;height:42px;border-radius:calc(10px - 1px);display:flex;align-items:center;justify-content:center;font-size:18px}
.kpi-info{flex:1;min-width:0}
.kpi-val{font-size:22px;font-weight:700;font-family:'JetBrains Mono','Space Grotesk',monospace;letter-spacing:-.3px}
.kpi-label{font-size:11px;color:#94a3b8;margin-top:2px;font-family:'Geist',sans-serif}

/* ═══ SCROLL LIST ═══ */
.scroll-list{height:180px;overflow:hidden;padding:0 4px}
.scroll-inner{animation:scrollUp linear infinite}
.scroll-item{display:flex;align-items:center;gap:10px;padding:9px 0;border-bottom:1px solid #f0f2f5}
.scroll-item:last-child{border-bottom:none}
.scroll-avatar{width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:600;color:#fff;flex-shrink:0}
.scroll-name{flex:1;font-size:12px;color:#475569;font-family:'Geist',sans-serif}
.scroll-time{font-size:11px;color:#94a3b8}
@keyframes scrollUp{0%{transform:translateY(0)}100%{transform:translateY(-50%)}}

/* ═══ RESPONSIVE ═══ */
@media(max-width:1200px){.dash-grid{grid-template-columns:1fr 1fr}.dash-col-wide{grid-column:span 2}}
@media(max-width:768px){.dash-grid{grid-template-columns:1fr}.dash-col-wide{grid-column:span 1}.kpi-row{grid-template-columns:repeat(2,1fr)}.dash-header{padding:10px 16px}.dash-h-title{font-size:14px}}
</style>