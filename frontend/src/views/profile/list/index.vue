<template>
  <div class="page-body">
    <div class="page-header">
      <h1 class="page-title">用户画像列表</h1>
    </div>

    <div class="section-outer"><div class="section-inner">
      <div class="search-bar">
        <ElInput v-model="keyword" placeholder="搜索用户编码..." clearable class="input-search" @keyup.enter="search">
          <template #prefix><ArtSvgIcon icon="ri:search-line" /></template>
        </ElInput>
        <ElSelect v-model="segmentCode" placeholder="用户分层" clearable class="input-select" @change="search">
          <ElOption label="高价值用户" value="HIGH_VALUE" /><ElOption label="潜力用户" value="POTENTIAL" />
          <ElOption label="一般用户" value="GENERAL" /><ElOption label="流失风险用户" value="AT_RISK" />
          <ElOption label="低价值用户" value="LOW_VALUE" />
        </ElSelect>
        <ElButton type="primary" @click="search"><ArtSvgIcon icon="ri:search-line" class="mr-1" />搜索</ElButton>
        <span class="result-count">共 {{ total }} 位用户</span>
      </div>

      <ElTable :data="list" stripe v-loading="loading" @row-click="goDetail" class="cursor-pointer data-table">
        <ElTableColumn prop="userCode" label="用户编码" width="140">
          <template #default="{ row }"><span class="font-mono text-blue-600">{{ row.userCode }}</span></template>
        </ElTableColumn>
        <ElTableColumn prop="gender" label="性别" width="70">
          <template #default="{ row }"><ElTag size="small" :type="row.gender==='男'||row.gender==='Male'?'primary':row.gender==='女'||row.gender==='Female'?'danger':'info'">{{ genderLabel(row.gender) }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn prop="age" label="年龄" width="70" sortable />
        <ElTableColumn prop="province" label="省份" width="90" />
        <ElTableColumn prop="city" label="城市" width="90" />
        <ElTableColumn prop="totalOrderCount" label="订单数" width="90" align="center" sortable>
          <template #default="{ row }"><span class="font-medium">{{ row.totalOrderCount||0 }}</span></template>
        </ElTableColumn>
        <ElTableColumn prop="totalPaymentAmount" label="消费金额" width="130" align="right" sortable>
          <template #default="{ row }"><span class="font-medium" style="color:#FFAE1F">¥{{ (row.totalPaymentAmount||0).toLocaleString() }}</span></template>
        </ElTableColumn>
        <ElTableColumn label="用户分层" width="120">
          <template #default="{ row }"><ElTag size="small" :type="segmentTagType(row.segmentCode)" effect="light">{{ row.segmentName||'未分层' }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn prop="segmentScore" label="评分" width="80" align="center" sortable>
          <template #default="{ row }"><span v-if="row.segmentScore" class="font-bold" :style="{color:scoreColor(row.segmentScore)}">{{ Number(row.segmentScore).toFixed(1) }}</span><span v-else class="text-gray-300">-</span></template>
        </ElTableColumn>
      </ElTable>
      <div class="pagination-wrap">
        <ElPagination v-model:current-page="page" :page-size="size" :total="total" layout="total,prev,pager,next,jumper" @current-change="loadData" />
      </div>
    </div></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchProfileList } from '@/api/profile'
defineOptions({ name: 'ProfileList' })
const router = useRouter()
const keyword = ref(''); const segmentCode = ref('')
const list = ref<any[]>([]); const loading = ref(false)
const page = ref(1); const size = ref(20); const total = ref(0)
function genderLabel(g:string) { if(g==='男'||g==='Male')return'男'; if(g==='女'||g==='Female')return'女'; return'未知' }
function segmentTagType(c:string){ const m:Record<string,string>={HIGH_VALUE:'success',POTENTIAL:'primary',GENERAL:'info',AT_RISK:'warning',LOW_VALUE:'danger'}; return (m[c]||'info') as any }
function scoreColor(s:number) { if(s>=4)return'#13DEB9'; if(s>=3)return'#5D87FF'; if(s>=2)return'#FFAE1F'; return'#FF4D4F' }
function search(){ page.value=1; loadData() }
function goDetail(row:any){ router.push(`/profiles/${row.userId}`) }
async function loadData(){ loading.value=true; try { const res=await fetchProfileList({keyword:keyword.value||undefined,segmentCode:segmentCode.value||undefined,page:page.value-1,size:size.value}); if(res){list.value=res.records||[];total.value=res.total||0} } catch {} finally { loading.value=false } }
onMounted(loadData)
</script>

<style scoped>
.page-body { font-family: 'Geist','Inter','PingFang SC',sans-serif; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--art-gray-900); margin: 0; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; letter-spacing: -.3px; }

.section-outer { padding: 1.5px; border-radius: 14px; background: rgba(0,0,0,.025); }
.section-inner { border-radius: calc(14px - 1.5px); background: var(--default-box-color); padding: 20px 24px 24px; border: 1px solid var(--default-border); }

.search-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; }
.input-search { width: 240px; }
.input-select { width: 160px; }
.result-count { margin-left: auto; font-size: 13px; color: var(--art-gray-500); }

.data-table { width: 100%; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: center; }
</style>