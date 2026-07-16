<template>
  <div class="p-5" v-loading="loading">
    <template v-if="profile">
      <!-- 用户头部 -->
      <div class="bg-white dark:bg-dark-box p-6 rounded-xl shadow-sm mb-5 border border-gray-200 dark:border-gray-700">
        <div class="flex items-center gap-5">
          <div class="user-avatar">
            <ArtSvgIcon icon="ri:user-3-fill" class="text-3xl" />
          </div>
          <div class="flex-1">
            <div class="flex items-center gap-3">
              <h2 class="text-xl font-bold">{{ profile.userCode }}</h2>
              <ElTag :type="segmentTagType(profile.segmentCode)" effect="dark" size="small">
                {{ profile.segmentName || '未分层' }}
              </ElTag>
            </div>
            <div class="flex gap-4 mt-2 text-sm text-gray-500">
              <span><ArtSvgIcon icon="ri:user-line" class="mr-1" />{{ genderLabel(profile.gender) }}</span>
              <span><ArtSvgIcon icon="ri:calendar-line" class="mr-1" />{{ profile.age || '-' }} 岁</span>
              <span><ArtSvgIcon icon="ri:map-pin-line" class="mr-1" />{{ profile.province }} {{ profile.city }}</span>
            </div>
          </div>
          <div class="text-right">
            <div class="text-xs text-gray-400">综合评分</div>
            <div class="text-3xl font-bold" :style="{ color: scoreColor(profile.segmentScore) }">
              {{ profile.segmentScore ? Number(profile.segmentScore).toFixed(1) : '-' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 消费指标 -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-5">
        <div class="metric-card metric-blue">
          <div class="metric-icon"><ArtSvgIcon icon="ri:shopping-cart-line" /></div>
          <div class="metric-label">累计订单</div>
          <div class="metric-value">{{ profile.totalOrderCount || 0 }}</div>
        </div>
        <div class="metric-card metric-orange">
          <div class="metric-icon"><ArtSvgIcon icon="ri:money-cny-circle-line" /></div>
          <div class="metric-label">累计消费</div>
          <div class="metric-value">¥{{ (profile.totalPaymentAmount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
        </div>
        <div class="metric-card metric-green">
          <div class="metric-icon"><ArtSvgIcon icon="ri:price-tag-3-line" /></div>
          <div class="metric-label">客单价</div>
          <div class="metric-value">¥{{ (profile.averageOrderAmount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
        </div>
        <div class="metric-card metric-purple">
          <div class="metric-icon"><ArtSvgIcon icon="ri:eye-line" /></div>
          <div class="metric-label">30日浏览</div>
          <div class="metric-value">{{ profile.browseCount30d || 0 }}</div>
        </div>
      </div>

      <!-- 行为指标 -->
      <div class="bg-white dark:bg-dark-box p-6 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700">
        <h3 class="text-base font-bold mb-4 flex items-center">
          <ArtSvgIcon icon="ri:bar-chart-line" class="mr-1" />行为指标
        </h3>
        <div class="grid grid-cols-2 md:grid-cols-3 gap-4">
          <div class="behavior-item">
            <div class="behavior-label"><ArtSvgIcon icon="ri:eye-line" class="mr-1" />近30日浏览次数</div>
            <div class="behavior-value">{{ profile.browseCount30d || 0 }} 次</div>
          </div>
          <div class="behavior-item">
            <div class="behavior-label"><ArtSvgIcon icon="ri:login-circle-line" class="mr-1" />近30日登录次数</div>
            <div class="behavior-value">{{ profile.loginCount30d || 0 }} 次</div>
          </div>
          <div class="behavior-item">
            <div class="behavior-label"><ArtSvgIcon icon="ri:time-line" class="mr-1" />最近活跃时间</div>
            <div class="behavior-value">{{ profile.lastActiveAt ? new Date(profile.lastActiveAt).toLocaleString('zh-CN') : '暂无记录' }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { fetchProfileDetail } from '@/api/profile'

defineOptions({ name: 'ProfileDetail' })
const route = useRoute()
const profile = ref<any>(null)
const loading = ref(true)

function genderLabel(g: string) {
  if (g === '男' || g === 'Male') return '男'
  if (g === '女' || g === 'Female') return '女'
  return '未知'
}

function segmentTagType(code: string) {
  const map: Record<string, string> = {
    HIGH_VALUE: 'success', POTENTIAL: 'primary', GENERAL: 'info',
    AT_RISK: 'warning', LOW_VALUE: 'danger'
  }
  return (map[code] || 'info') as any
}

function scoreColor(score: number) {
  if (!score) return '#949eb7'
  if (score >= 4) return '#13DEB9'
  if (score >= 3) return '#5D87FF'
  if (score >= 2) return '#FFAE1F'
  return '#FF4D4F'
}

onMounted(async () => {
  try {
    const res = await fetchProfileDetail(Number(route.params.id))
    profile.value = res
  } catch {} finally { loading.value = false }
})
</script>

<style scoped>
.user-avatar {
  width: 64px; height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #5D87FF, #A0C0FF);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 28px;
  flex-shrink: 0;
}
.metric-card {
  background: var(--default-box-color);
  border: 1px solid var(--default-border);
  border-radius: 12px;
  padding: 16px 20px;
  display: flex; flex-direction: column;
  gap: 4px;
  transition: all 0.25s ease;
}
.metric-card:hover { transform: translateY(-2px); box-shadow: 0 4px 20px rgba(93,135,255,0.12); }
.metric-icon {
  width: 36px; height: 36px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 18px; margin-bottom: 4px;
}
.metric-label { font-size: 13px; color: var(--art-gray-500); }
.metric-value { font-size: 20px; font-weight: 700; color: var(--art-gray-900); }
.metric-blue { border-top: 3px solid #5D87FF; }
.metric-blue .metric-icon { background: linear-gradient(135deg, #5D87FF, #3B6CE0); }
.metric-orange { border-top: 3px solid #FFAE1F; }
.metric-orange .metric-icon { background: linear-gradient(135deg, #FFAE1F, #E09100); }
.metric-green { border-top: 3px solid #13DEB9; }
.metric-green .metric-icon { background: linear-gradient(135deg, #13DEB9, #0EA88A); }
.metric-purple { border-top: 3px solid #5D87FF; }
.metric-purple .metric-icon { background: linear-gradient(135deg, #5D87FF, #3B6CE0); }
.behavior-item {
  padding: 12px 16px; background: var(--art-gray-100); border-radius: 8px;
}
.behavior-label { font-size: 13px; color: var(--art-gray-500); margin-bottom: 6px; display: flex; align-items: center; }
.behavior-value { font-size: 16px; font-weight: 600; color: var(--art-gray-900); }
</style>
