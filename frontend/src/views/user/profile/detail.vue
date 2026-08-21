<!-- 画像详情 — frontend-design: 卡片式信息分层、数据对得起排版 -->
<template>
  <div class="pdetail" v-loading="loading">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">{{ profile?.userCode || '用户画像' }}</h1>
          <span class="title-tag">USER DETAIL</span>
          <el-tag v-if="profile" size="small" :type="segType(profile.segmentCode)" effect="light" class="title-tag-seg">{{ profile.segmentName || '未分层' }}</el-tag>
        </div>
        <p class="page-desc">画像详情 · RFM 分层、行为指标、价值分析</p>
      </div>
      <div class="ph-meta">
        <button class="back-btn" @click="$router.push('/user/profiles')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          返回列表
        </button>
      </div>
    </div>

    <template v-if="profile">
      <!-- 用户身份卡 -->
      <section class="identity-card">
        <div class="identity-avatar">
          <span class="avatar-letter">{{ (profile.userCode || 'U')[0] }}</span>
        </div>
        <div class="identity-info">
          <div class="identity-meta">
            <span class="meta-item"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg> {{ genderLabel(profile.gender) }}</span>
            <span class="meta-item"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 16V8a2 4 0 0 0-2-2H5a2 4 0 0 0-2 2v8a2 4 0 0 0 2 2h14a2 4 0 0 0 2-2z"/><line x1="3" y1="12" x2="21" y2="12"/></svg> {{ profile.age ?? '—' }} 岁</span>
            <span class="meta-item"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg> {{ profile.province || '—' }} {{ profile.city || '' }}</span>
            <span class="meta-item"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg> {{ profile.registeredAt ? formatDate(profile.registeredAt) : '—' }} 注册</span>
            <span class="meta-item" v-if="profile.lastActiveAt"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg> 最近活跃 {{ relActiveTime(profile.lastActiveAt) }}</span>
          </div>
        </div>
        <div class="identity-score" v-if="profile.segmentScore != null">
          <span class="score-val" :style="{color: scoreColor(profile.segmentScore)}">{{ Number(profile.segmentScore).toFixed(1) }}</span>
          <span class="score-label">综合评分</span>
        </div>
      </section>

      <!-- 指标网格 -->
      <div class="kpi-strip">
        <div class="kpi-cell">
          <span class="kpi-val mono">{{ profile.totalOrderCount || 0 }}</span>
          <span class="kpi-label">订单总数</span>
        </div>
        <div class="kpi-cell">
          <span class="kpi-val mono gold">¥{{ (profile.totalPaymentAmount || 0).toLocaleString() }}</span>
          <span class="kpi-label">消费金额</span>
        </div>
        <div class="kpi-cell">
          <span class="kpi-val mono">{{ profile.averageOrderAmount ? '¥' + Number(profile.averageOrderAmount).toLocaleString() : '—' }}</span>
          <span class="kpi-label">平均客单价</span>
        </div>
        <div class="kpi-cell">
          <span class="kpi-val mono">{{ profile.browseCount30d || 0 }}</span>
          <span class="kpi-label">30日浏览</span>
        </div>
        <div class="kpi-cell">
          <span class="kpi-val mono">{{ profile.loginCount30d || 0 }}</span>
          <span class="kpi-label">30日登录</span>
        </div>
      </div>

      <!-- RFM 分层 -->
      <section class="detail-card" v-if="profile.rScore != null">
        <h2 class="card-title">RFM 价值分析</h2>
        <div class="rfm-strip">
          <div class="rfm-item">
            <span class="rfm-label">R 最近消费</span>
            <div class="rfm-bar"><div class="rfm-fill" :style="{width: rfmPct(profile.rScore), background: rfmBarColor(profile.rScore)}"></div></div>
            <span class="rfm-score">{{ profile.rScore }}/5</span>
          </div>
          <div class="rfm-item">
            <span class="rfm-label">F 消费频率</span>
            <div class="rfm-bar"><div class="rfm-fill" :style="{width: rfmPct(profile.fScore), background: rfmBarColor(profile.fScore)}"></div></div>
            <span class="rfm-score">{{ profile.fScore }}/5</span>
          </div>
          <div class="rfm-item">
            <span class="rfm-label">M 消费金额</span>
            <div class="rfm-bar"><div class="rfm-fill" :style="{width: rfmPct(profile.mScore), background: rfmBarColor(profile.mScore)}"></div></div>
            <span class="rfm-score">{{ profile.mScore }}/5</span>
          </div>
        </div>
      </section>

      <!-- 标签列表（按维度分组：活跃度/消费能力/偏好品类/RFM 分层） -->
      <section class="detail-card" v-if="profile.tags?.length">
        <h2 class="card-title">用户标签</h2>
        <div v-for="group in tagGroups" :key="group.code" class="tag-group">
          <div class="tag-group-head">
            <span class="tag-group-label">{{ group.label }}</span>
          </div>
          <div class="tags-strip">
            <span v-for="tag in group.tags" :key="tag.tagId || tag.tagName" class="tag-chip" :style="tagStyle(tag)">
              {{ tag.tagName }} · {{ tagValueLabel(tag) }}
            </span>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { fetchProfileDetail } from '@/api/profile'

defineOptions({ name: 'UserProfileDetail' })

const route = useRoute()
const loading = ref(false)
const profile = ref<any>(null)

function genderLabel(g: string) {
  if (g === '男' || g === 'Male') return '男'
  if (g === '女' || g === 'Female') return '女'
  return '未知'
}
function segType(c: string) {
  const m: Record<string, string> = { HIGH_VALUE: 'success', POTENTIAL: 'primary', GENERAL: 'info', AT_RISK: 'warning', LOW_VALUE: 'danger' }
  return (m[c] || 'info') as any
}
function scoreColor(s: number) {
  if (s >= 4) return '#059669'; if (s >= 3) return '#0d9488'; if (s >= 2) return '#d97706'
  return '#dc2626'
}
function rfmPct(s: number) { return Math.min((s || 0) / 5 * 100, 100) + '%' }
function rfmBarColor(s: number) {
  if (s >= 4) return '#059669'; if (s >= 3) return '#0d9488'; if (s >= 2) return '#d97706'
  return '#dc2626'
}
const tagColors = ['#0d9488','#059669','#d97706','#14b8a6','#dc2626','#0891b2']
function tagStyle(tag: any) {
  const i = (tag.tagId || 0) % tagColors.length
  return { background: tagColors[i] + '12', color: tagColors[i], borderColor: tagColors[i] + '30' }
}
/** 标签值中文化映射（后端存英文枚举，展示用中文） */
const TAG_VALUE_LABELS: Record<string, Record<string, string>> = {
  ACTIVE_LEVEL: { High: '高活跃', Medium: '中活跃', Low: '低活跃' },
  CONSUMPTION_LEVEL: { High: '高消费', Medium: '中等消费', Low: '低消费' },
  FAVORITE_CATEGORY: { '1': '数码产品', '2': '服装鞋包', '3': '家居生活', '4': '食品饮料', '5': '美妆个护' },
  RFM_SEGMENT: { HIGH_VALUE: '高价值', POTENTIAL: '潜力用户', GENERAL: '一般用户', AT_RISK: '待挽留', LOW_VALUE: '低价值' }
}
function tagValueLabel(tag: any) {
  return TAG_VALUE_LABELS[tag.tagCode]?.[tag.tagValue] || tag.tagValue || ''
}
/** 标签按维度分组（活跃度/消费能力/偏好品类/RFM） */
const TAG_GROUP_LABELS: Record<string, string> = {
  ACTIVE_LEVEL: '活跃度',
  CONSUMPTION_LEVEL: '消费能力',
  FAVORITE_CATEGORY: '偏好品类',
  RFM_SEGMENT: 'RFM 分层'
}
const tagGroups = computed(() => {
  const groups: { code: string; label: string; tags: any[] }[] = []
  for (const t of profile.value?.tags || []) {
    let g = groups.find(x => x.code === t.tagCode)
    if (!g) { g = { code: t.tagCode, label: TAG_GROUP_LABELS[t.tagCode] || t.tagCode, tags: [] }; groups.push(g) }
    g.tags.push(t)
  }
  return groups.filter(g => g.tags.length > 0)
})
/** 最近活跃相对时间 */
function relActiveTime(t: string) {
  if (!t) return '—'
  const d = new Date(t)
  if (isNaN(d.getTime())) return '—'
  const day = Math.floor((Date.now() - d.getTime()) / 86400000)
  if (day <= 0) return '今日活跃'
  if (day < 7) return `${day} 天前`
  return formatDate(t)
}
function formatDate(d: string) {
  if (!d) return '—'
  const dt = new Date(d)
  return `${dt.getFullYear()}-${String(dt.getMonth()+1).padStart(2,'0')}-${String(dt.getDate()).padStart(2,'0')}`
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const res = await fetchProfileDetail(id)
    if (res) profile.value = res
  } catch {} finally { loading.value = false }
})
</script>

<style scoped>
.pdetail {
  font-family: var(--font-body, 'Inter', system-ui);
  max-width: 880px; margin: 0 auto;
}

/* ─── Back ─── */
.back-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 12px; border: 1px solid #e2e8f0; background: #fff;
  border-radius: 6px; font-size: 13px; color: #64748b; cursor: pointer;
  margin-bottom: 20px; transition: all 0.15s; font-family: inherit;
}
.back-btn:hover { border-color: #0d9488; color: #0d9488; }

/* ─── Identity ─── */
.identity-card {
  display: flex; align-items: center; gap: 20px;
  padding: 24px; background: #fff; border: 1px solid #e2e8f0;
  border-radius: 12px; margin-bottom: 16px;
}

.identity-avatar {
  width: 52px; height: 52px; display: flex; align-items: center; justify-content: center;
  background: #eff6ff; border-radius: 14px; flex-shrink: 0;
}

.avatar-letter { font-size: 22px; font-weight: 700; color: #0d9488; font-family: var(--font-mono, monospace); }

.identity-info { flex: 1; min-width: 0; }
.identity-meta { display: flex; gap: 16px; flex-wrap: wrap; font-size: 13px; color: #64748b; }
.meta-item { display: inline-flex; align-items: center; gap: 5px; }
.meta-item svg { color: #94a3b8; }

/* page-header 内 SEG 标签的间距微调 */
.title-tag-seg { margin-left: 4px; }
.back-btn { padding: 6px 12px; }

.identity-score { text-align: center; flex-shrink: 0; }
.score-val { font-size: 28px; font-weight: 700; display: block; font-family: var(--font-mono, monospace); }
.score-label { font-size: 11px; color: #94a3b8; font-weight: 500; }

/* ─── KPI Strip ─── */
.kpi-strip {
  display: flex; gap: 1px; background: #e2e8f0; border-radius: 10px;
  overflow: hidden; margin-bottom: 16px; border: 1px solid #e2e8f0;
}
.kpi-cell {
  flex: 1; background: #fff; padding: 16px; text-align: center;
}
.kpi-val { font-size: 20px; font-weight: 700; color: #1e293b; display: block; line-height: 1.2; }
.kpi-val.gold { color: #d97706; }
.mono { font-family: var(--font-mono, monospace); letter-spacing: -0.3px; }
.kpi-label { font-size: 11px; color: #94a3b8; font-weight: 500; margin-top: 4px; display: block; }

/* ─── Cards ─── */
.detail-card {
  background: #fff; border: 1px solid rgba(15,23,42,0.06); border-radius: 14px;
  padding: 20px; margin-bottom: 16px;
  box-shadow: 0 6px 22px rgba(13,148,136,0.04), 0 1px 3px rgba(15,23,42,0.02);
}
.card-title { font-size: 15px; font-weight: 600; color: #1e293b; margin: 0 0 16px; }

/* ─── RFM ─── */
.rfm-strip { display: flex; flex-direction: column; gap: 14px; }
.rfm-item { display: flex; align-items: center; gap: 12px; }
.rfm-label { font-size: 13px; color: #64748b; min-width: 80px; }
.rfm-bar { flex: 1; height: 8px; background: #f1f5f9; border-radius: 4px; overflow: hidden; }
.rfm-fill { height: 100%; border-radius: 4px; transition: width 0.6s ease; }
.rfm-score { font-size: 13px; font-weight: 600; color: #1e293b; min-width: 32px; text-align: right; font-family: var(--font-mono, monospace); }

/* ─── Tags ─── */
.tag-group { margin-bottom: 14px; }
.tag-group:last-child { margin-bottom: 0; }
.tag-group-head { margin-bottom: 8px; }
.tag-group-label {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 600; color: #64748b;
}
.tag-group-label::before {
  content: ''; width: 8px; height: 8px; border-radius: 2px;
  background: var(--acc, #0d9488);
}
.tags-strip { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-chip {
  padding: 4px 12px; border-radius: 20px; font-size: 12px;
  font-weight: 500; border: 1px solid;
}
</style>
