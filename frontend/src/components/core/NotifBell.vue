<!-- 顶栏通知铃铛悬浮窗 — 通用组件：未读角标 + 最近 8 条 + 全部已读 + 查看全部
     支持 admin / user 两种模式（跳转路径与详情跳转不同） -->
<template>
  <el-popover placement="bottom-end" :width="370" trigger="click" popper-class="notif-popover" @show="onPanelOpen">
    <template #reference>
      <div class="topbar-bell" title="通知">
        <el-badge :value="unreadCount" :max="99" :hidden="!unreadCount" class="bell-badge-wrap">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
        </el-badge>
      </div>
    </template>

    <div class="notif-panel">
      <div class="np-head">
        <span class="np-title">通知</span>
        <span v-if="unreadCount" class="np-unread">{{ unreadCount }} 条未读</span>
        <button v-if="unreadCount" class="np-clear" @click="markAllRead">全部已读</button>
      </div>
      <div class="np-list">
        <div v-for="n in recentNotifs" :key="n.id" class="np-item" :class="{ 'np-item--unread': !n.isRead }" @click="openDetail(n)">
          <span class="np-dot" :class="'np-dot--' + (n.type || 'system').toLowerCase()"></span>
          <div class="np-body">
            <div class="np-row">
              <span class="np-type">{{ typeLabel(n.type) }}</span>
              <span class="np-time">{{ relTime(n.createdAt) }}</span>
            </div>
            <div class="np-title-text">{{ n.title }}</div>
            <div class="np-content">{{ n.content }}</div>
          </div>
        </div>
        <div v-if="!recentNotifs.length" class="np-empty">暂无通知</div>
      </div>
      <div class="np-foot">
        <router-link :to="allPath" class="np-all">查看全部通知 →</router-link>
      </div>
    </div>
  </el-popover>

  <!-- 通知详情弹窗 -->
  <ElDialog v-model="detailVisible" title="通知详情" width="520px" class="np-detail-dialog">
    <div v-if="detailItem" class="np-detail">
      <div class="np-detail-head">
        <span class="np-detail-type">{{ typeLabel(detailItem.type) }}</span>
        <time class="np-detail-time">{{ formatTime(detailItem.createdAt) }}</time>
      </div>
      <h3 class="np-detail-title">{{ detailItem.title }}</h3>
      <p class="np-detail-content">{{ detailItem.content }}</p>
      <div v-if="canJump" class="np-detail-actions">
        <ElButton type="primary" size="small" @click="goDetail">{{ jumpLabel }}</ElButton>
      </div>
    </div>
  </ElDialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchNotifications, fetchUnreadCount, markAsRead, markAllAsRead } from '@/api/notification'

defineOptions({ name: 'NotifBell' })

const props = defineProps<{
  /** 通知模式：'admin' | 'user'，决定跳转路径 */
  mode: 'admin' | 'user'
}>()

const router = useRouter()

/** 模式相关路径 */
const allPath = computed(() => props.mode === 'admin' ? '/notifications' : '/user/notifications')
const aiJumpPath = computed(() => props.mode === 'admin' ? '/dashboard/overview' : '/user/ai')

/** 通知类型中文化（按 mode） */
const TYPE_LABELS_ADMIN: Record<string, string> = { TASK_SUCCESS: '任务', TASK_FAILED: '任务', TASK: '任务', TAG_RECALC: '标签', DATA: '数据更新', AI: 'AI 分析', SYSTEM: '系统' }
const TYPE_LABELS_USER: Record<string, string> = { TASK_SUCCESS: '分析任务', TASK_FAILED: '分析任务', TASK: '分析任务', TAG_RECALC: '标签重算', DATA: '数据更新', SYSTEM: '系统消息', AI: 'AI 分析' }
function typeLabel(t: string) {
  const m = props.mode === 'admin' ? TYPE_LABELS_ADMIN : TYPE_LABELS_USER
  return m[t] || t || '通知'
}

/** TASK 跳转：admin 跳任务详情；user 跳聚类页统一查看画像/聚类结果 */
function taskJumpPath(refId: number, content: string): string | null {
  if (props.mode === 'admin') {
    if (/聚类重算|画像分析/.test(content || '')) return '/user/cluster-analysis'
    return `/tasks?taskId=${refId}`
  }
  return '/user/cluster-analysis'
}

/** 数据 */
const unreadCount = ref(0)
const recentNotifs = ref<any[]>([])
const detailVisible = ref(false)
const detailItem = ref<any>(null)

/** 桌面通知（未启用，预留扩展） */
const notifyOn = ref(false)
async function enableNotify() { /* 暂未启用 */ }

/** 加载未读数 */
let notifTimer: any = null
async function loadUnread() {
  try {
    const now = Number(await fetchUnreadCount() || 0)
    unreadCount.value = now
  } catch {}
}
async function loadRecent() {
  try { const r = await fetchNotifications({ page: 0, size: 8 }); recentNotifs.value = r?.records || [] } catch {}
}

/** 打开面板时刷新 */
function onPanelOpen() { loadRecent() }

/** 标记单条已读 */
async function markOneRead(n: any) {
  if (n.isRead) return
  n.isRead = true
  unreadCount.value = Math.max(0, unreadCount.value - 1)
  try { await markAsRead(n.id) } catch {}
}
/** 全部已读 */
async function markAllRead() {
  try { await markAllAsRead() } catch {}
  unreadCount.value = 0
  recentNotifs.value.forEach(n => { n.isRead = true })
}

/** 点击条目：标记已读 + 打开详情 */
function openDetail(n: any) {
  markOneRead(n)
  detailItem.value = n
  detailVisible.value = true
}

/** 详情跳转规则 */
const canJump = computed(() => {
  const it = detailItem.value
  return !!it && ((it.refType === 'TASK' && taskJumpPath(it.refId, it.content)) || it.refType === 'AI')
})
const jumpLabel = computed(() => {
  const it = detailItem.value
  return it?.refType === 'AI' ? '前往 AI 分析 →' : '查看任务详情 →'
})
function goDetail() {
  const it = detailItem.value
  if (!it) return
  detailVisible.value = false
  if (it.refType === 'TASK' && it.refId) {
    const p = taskJumpPath(it.refId, it.content)
    if (p) router.push(p)
  } else if (it.refType === 'AI') {
    router.push(aiJumpPath.value)
  }
}

/** 相对时间（刚刚/N分钟前/N小时前/MM-DD） */
function relTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return ''
  const diff = Date.now() - d.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return Math.floor(diff / 60_000) + '分钟前'
  if (diff < 86_400_000) return Math.floor(diff / 3_600_000) + '小时前'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
/** 绝对时间（详情用） */
function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

defineExpose({ loadUnread, loadRecent, unreadCount })

onMounted(() => { loadUnread(); notifTimer = setInterval(loadUnread, 20000) })
onUnmounted(() => { if (notifTimer) clearInterval(notifTimer) })
</script>

<style scoped>
.topbar-bell { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #64748b; cursor: pointer; transition: all 0.15s; }
.topbar-bell:hover { background: #f1f5f9; color: #334155; }
:deep(.bell-badge-wrap) .el-badge__content { background: #ef4444; border: none; font-size: 10px; height: 16px; line-height: 16px; padding: 0 4px; min-width: 16px; }

/* 悬浮面板 */
.notif-panel { font-family: 'Inter', 'PingFang SC', system-ui, sans-serif; }
.np-head { display: flex; align-items: center; justify-content: space-between; padding: 0 4px 10px; border-bottom: 1px solid #f1f5f9; }
.np-title { font-size: 14px; font-weight: 600; color: #1e293b; }
.np-unread { font-size: 12px; color: #94a3b8; }
.np-enable, .np-clear { font-size: 12px; padding: 4px 10px; border: 1px solid #e2e8f0; background: #fff; border-radius: 6px; color: #64748b; cursor: pointer; font-family: inherit; transition: all 0.15s; }
.np-enable:hover, .np-clear:hover { border-color: var(--acc); color: var(--acc); }

.np-list { max-height: 360px; overflow-y: auto; padding: 4px 0; }
.np-item { display: flex; gap: 8px; padding: 10px 6px; border-radius: 8px; cursor: pointer; transition: background 0.12s; }
.np-item:hover { background: #f8fafc; }
.np-item--unread { background: #f8fafc; }
.np-dot { width: 4px; height: 32px; border-radius: 2px; flex-shrink: 0; margin-top: 2px; }
.np-dot--task { background: #059669; } .np-dot--task_success { background: #059669; } .np-dot--task_failed { background: #dc2626; }
.np-dot--data { background: #d97706; } .np-dot--ai { background: #14b8a6; } .np-dot--system { background: #0d9488; } .np-dot--push { background: #dc2626; }

.np-body { flex: 1; min-width: 0; }
.np-row { display: flex; align-items: center; gap: 6px; margin-bottom: 3px; }
.np-type { font-size: 10px; font-weight: 600; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }
.np-time { font-size: 11px; color: #94a3b8; }
.np-title-text { font-size: 13px; font-weight: 500; color: #1e293b; margin-bottom: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.np-content { font-size: 12px; color: #64748b; line-height: 1.5; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.np-empty { padding: 30px 0; text-align: center; color: #94a3b8; font-size: 13px; }

.np-foot { padding: 8px 0 0; border-top: 1px solid #f1f5f9; text-align: center; }
.np-all { font-size: 12px; color: var(--acc); text-decoration: none; }
.np-all:hover { color: var(--acc-dark); }

/* 详情弹窗 */
.np-detail { padding: 4px 2px; }
.np-detail-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.np-detail-type { font-size: 11px; font-weight: 600; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }
.np-detail-time { font-size: 12px; color: #94a3b8; }
.np-detail-title { font-size: 15px; font-weight: 600; color: #0f172a; margin: 0 0 10px; }
.np-detail-content { font-size: 13px; line-height: 1.9; color: #475569; white-space: pre-wrap; word-break: break-word; max-height: 50vh; overflow-y: auto; margin: 0; }
.np-detail-actions { margin-top: 14px; text-align: right; }
</style>