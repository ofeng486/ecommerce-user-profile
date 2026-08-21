<!-- Admin Layout — 自定义 · 亮色蓝绿霓虹 · 玻璃拟态侧边栏 -->
<template>
  <div class="admin-shell">
    <!-- 侧边栏 -->
    <aside class="admin-sidebar">
      <div class="sidebar-glass">
        <router-link to="/dashboard/overview" class="sidebar-brand">
          <div class="brand-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>
          </div>
          <span class="brand-label">Admin Console</span>
        </router-link>

        <nav class="sidebar-nav">
          <router-link v-for="item in primaryNav" :key="item.path" :to="item.path" class="nav-item" active-class="nav-item--active">
            <span class="nav-ico" v-html="item.icon"></span>
            <span>{{ item.label }}</span>
          </router-link>

          <span class="nav-category">管理</span>
          <router-link v-for="item in adminNav" :key="item.path" :to="item.path" class="nav-item" active-class="nav-item--active">
            <span class="nav-ico" v-html="item.icon"></span>
            <span>{{ item.label }}</span>
          </router-link>

          <span class="nav-category">个人</span>
          <router-link v-for="item in systemNav" :key="item.path" :to="item.path" class="nav-item" active-class="nav-item--active">
            <span class="nav-ico" v-html="item.icon"></span>
            <span>{{ item.label }}</span>
          </router-link>
        </nav>

        <div class="sidebar-footer">
          <div class="sidebar-user">
            <span class="su-avatar">{{ initials }}</span>
            <div class="su-info">
              <span class="su-name">{{ userStore.info?.displayName || userStore.info?.username }}</span>
              <span class="su-role">管理员</span>
            </div>
            <button class="su-logout" @click="handleLogout" title="退出">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
            </button>
          </div>
        </div>
      </div>
    </aside>

    <!-- 主内容 -->
    <div class="admin-main">
      <header class="admin-topbar">
        <div class="topbar-right">
          <el-popover placement="bottom-end" :width="370" trigger="click" popper-class="notif-popover" @show="onPanelOpen">
            <template #reference>
              <div class="topbar-bell" title="通知">
                <el-badge :value="unreadCount" :max="99" :hidden="!unreadCount" class="bell-badge-wrap">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                </el-badge>
              </div>
            </template>
            <!-- 悬浮通知面板：最近 8 条 + 快捷操作，完整列表见「系统 → 通知」页 -->
            <div class="notif-panel">
              <div class="np-head">
                <span class="np-title">通知</span>
                <button v-if="!notifyOn" class="np-enable" @click="enableNotify">开启桌面通知</button>
                <span v-if="unreadCount" class="np-unread">{{ unreadCount }} 条未读</span>
                <button v-if="unreadCount" class="np-clear" @click="markAllRead">全部已读</button>
              </div>
              <div class="np-list">
                <div v-for="n in recentNotifs" :key="n.id" class="np-item" :class="{ 'np-item--unread': !n.isRead }" @click="markOneRead(n); openNotifDetail(n)">
                  <span class="np-dot" :class="'np-dot--' + (n.type || 'system').toLowerCase()"></span>
                  <div class="np-body">
                    <div class="np-row">
                      <span class="np-type">{{ typeLabel(n.type) }}</span>
                      <span class="np-time">{{ formatTime(n.createdAt) }}</span>
                    </div>
                    <div class="np-title-text">{{ n.title }}</div>
                    <div class="np-content">{{ n.content }}</div>
                  </div>
                </div>
                <div v-if="!recentNotifs.length" class="np-empty">暂无通知</div>
              </div>
              <div class="np-foot">
                <router-link to="/notifications" class="np-all">查看全部通知 →</router-link>
              </div>
            </div>
          </el-popover>
          <!-- 通知详情弹窗（悬浮面板点击条目打开，含完整内容与跳转） -->
          <ElDialog v-model="notifDetailVisible" title="通知详情" width="520px" class="np-detail-dialog">
            <div v-if="notifDetailItem" class="np-detail">
              <div class="np-detail-head">
                <span class="np-detail-type">{{ typeLabel(notifDetailItem.type) }}</span>
                <time class="np-detail-time">{{ formatTime(notifDetailItem.createdAt) }}</time>
              </div>
              <h3 class="np-detail-title">{{ notifDetailItem.title }}</h3>
              <p class="np-detail-content">{{ notifDetailItem.content }}</p>
              <div v-if="notifDetailCanJump" class="np-detail-actions">
                <ElButton type="primary" size="small" @click="goNotifDetail">{{ notifDetailJumpLabel }}</ElButton>
              </div>
            </div>
          </ElDialog>
          <span class="role-tag">管理员</span>
        </div>
      </header>
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { fetchNotifications, fetchUnreadCount, markAsRead, markAllAsRead } from '@/api/notification'
import { fetchAdminTasks } from '@/api/admin'
import { notify, isNotifyGranted, requestNotifyPermission, notifyPermission } from '@/utils/notify'

defineOptions({ name: 'AdminLayout' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 未读通知数（顶栏铃铛角标，每 20s 刷新，增量变化时弹窗提醒） */
const unreadCount = ref(0)
/** 上次轮询到的未读数（首轮不弹窗） */
let prevUnread: number | null = null
/** 悬浮面板最近通知 */
const recentNotifs = ref<any[]>([])
let notifTimer: any = null
async function loadUnread() {
  try {
    const now = Number(await fetchUnreadCount() || 0)
    // 增量检测：未读数变多（且首轮之后）→ 主动弹窗提醒（系统通知优先，未授权降级 toast）
    if (prevUnread !== null && now > prevUnread) {
      const delta = now - prevUnread
      const latest = recentNotifs.value[0] || null
      const failed = latest?.type === 'TASK_FAILED'
      notify(
        latest ? latest.title : '新通知',
        `您有 ${delta} 条新通知${latest ? '：' + latest.title : ''}，点击查看`,
        { type: failed ? 'error' : latest?.type === 'AI' ? 'info' : 'success', systemTag: 'new-notification' }
      )
      loadRecent()
    }
    prevUnread = now
    unreadCount.value = now
  } catch { /* 保留旧值 */ }
}
async function loadRecent() {
  try { const r = await fetchNotifications({ page: 0, size: 8 }); recentNotifs.value = r?.records || [] } catch { /* 保留旧值 */ }
}

/** 桌面通知开关状态（浏览器已授权则为 true） */
const notifyOn = ref(isNotifyGranted())
/** 请求桌面通知授权（浏览器要求必须由用户点击触发） */
async function enableNotify() {
  const p = await requestNotifyPermission()
  if (p === 'granted') {
    notifyOn.value = true
    ElMessage.success('桌面通知已开启，任务完成时会弹到系统通知中心')
  } else if (p === 'denied') {
    ElMessage.warning('已被浏览器拒绝，请在浏览器设置中允许本站通知后重试')
  } else {
    ElMessage.info('未开启桌面通知，将使用页面内提醒')
  }
}

/** 运行中任务集合（用于检测任务完成 → 即时触发通知，布局层轮询不依赖具体页面） */
const runningTaskIds = ref<Set<number>>(new Set())
let taskTimer: any = null
async function checkRunningTasks() {
  try {
    const res = await fetchAdminTasks({ page: 0, size: 50, taskStatus: 'Running' })
    const nowIds = new Set((res?.records || []).map((t: any) => t.id))
    const prev = runningTaskIds.value
    // 有任务从「运行中」消失 → 刚完成 → 立即刷新未读（内部触发增量弹窗）
    if (prev.size > 0 && [...prev].some(id => !nowIds.has(id))) {
      loadUnread()
      loadRecent()
    }
    runningTaskIds.value = nowIds
  } catch { /* 忽略 */ }
}
/** 打开面板时刷新列表 */
function onPanelOpen() { loadRecent() }
/** 单条标记已读（点击即读，本地即时更新） */
async function markOneRead(n: any) {
  if (n.isRead) return
  n.isRead = true
  unreadCount.value = Math.max(0, unreadCount.value - 1)
  try { await markAsRead(n.id) } catch { /* 失败不阻塞交互 */ }
}
/** 全部已读 */
async function markAllRead() {
  try { await markAllAsRead() } catch { /* 忽略 */ }
  unreadCount.value = 0
  recentNotifs.value.forEach(n => { n.isRead = true })
}
/** 通知类型中文化 */
function typeLabel(t: string) {
  return ({ TASK_SUCCESS: '任务', TASK_FAILED: '任务', TASK: '任务', TAG_RECALC: '标签', DATA: '数据更新', AI: 'AI 分析', SYSTEM: '系统' } as Record<string, string>)[t] || '系统'
}

/** ═══ 悬浮面板通知详情弹窗 ═══ */
const notifDetailVisible = ref(false)
const notifDetailItem = ref<any>(null)
/** 点击悬浮条目：标记已读 + 打开详情弹窗 */
function openNotifDetail(n: any) {
  notifDetailItem.value = n
  notifDetailVisible.value = true
}
/** 可跳转：任务通知→任务详情；AI 通知→运营数据总览（AI 解读所在地） */
const notifDetailCanJump = computed(() => {
  const it = notifDetailItem.value
  return !!it && ((it.refType === 'TASK' && it.refId) || it.refType === 'AI')
})
const notifDetailJumpLabel = computed(() => notifDetailItem.value?.refType === 'AI' ? '前往 AI 分析 →' : '查看任务详情 →')
function goNotifDetail() {
  const it = notifDetailItem.value
  if (!it) return
  notifDetailVisible.value = false
  if (it.refType === 'TASK') router.push(`/tasks?taskId=${it.refId}`)
  else if (it.refType === 'AI') router.push('/dashboard/overview')
}
/** 相对时间：刚刚 / N分钟前 / N小时前 / MM-DD */
function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  const diff = Date.now() - d.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return Math.floor(diff / 60_000) + '分钟前'
  if (diff < 86_400_000) return Math.floor(diff / 3_600_000) + '小时前'
  return `${d.getMonth() + 1}-${String(d.getDate()).padStart(2, '0')}`
}
onMounted(() => { loadUnread(); notifTimer = setInterval(loadUnread, 20000); taskTimer = setInterval(checkRunningTasks, 10000) })
onUnmounted(() => { if (notifTimer) clearInterval(notifTimer); if (taskTimer) clearInterval(taskTimer) })

const initials = computed(() =>
  (userStore.info?.displayName || userStore.info?.username || 'A').charAt(0).toUpperCase()
)

const primaryNav = [
  { path: '/dashboard/overview', label: '运营数据总览',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></svg>' },
]

const adminNav = [
  { path: '/tasks', label: '任务管理',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>' },
  { path: '/admin/data-generate', label: '数据生成',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polyline points="16 3 21 3 21 8"/><line x1="4" y1="20" x2="21" y2="3"/><polyline points="21 16 21 21 16 21"/><line x1="15" y1="15" x2="21" y2="21"/><line x1="4" y1="4" x2="9" y2="9"/></svg>' },
  { path: '/admin/import', label: '数据导入',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>' },
  { path: '/system/users', label: '系统用户',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>' },
  { path: '/system/tag-definition', label: '标签体系管理',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>' },
  // 聚类重算：数据生产动作（Spark 作业），仅管理端入口（页面内部按角色显示重算按钮）
  { path: '/admin/cluster-analysis', label: '聚类重算',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="5" cy="6" r="3"/><circle cx="19" cy="5" r="2"/><circle cx="12" cy="12" r="4"/><circle cx="6" cy="19" r="2"/><circle cx="19" cy="18" r="3"/><path d="M7.5 8.5L10 10M14 10l3-3.5M9 15l-1.5 2.5M15 15l2.5 1.5"/></svg>' },
]

const systemNav = [
  { path: '/system/user-center', label: '个人中心',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>' },
  { path: '/notifications', label: '通知',
    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>' },
]

const analysisNav: any[] = []

const opsNav: any[] = []

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(() => userStore.logOut()).catch(() => {})
}
</script>

<style scoped>
.admin-shell {
  display: flex; min-height: 100vh;
  background: var(--admin-grad-page, linear-gradient(180deg, #f4f6fa 0%, #eef2f8 100%));
  font-family: var(--admin-font-body, 'Plus Jakarta Sans', 'Inter', 'PingFang SC', system-ui, sans-serif);
}

/* ─── Sidebar ─── */
.admin-sidebar { width: 240px; flex-shrink: 0; position: sticky; top: 0; height: 100vh; padding: 12px; }

.sidebar-glass {
  height: 100%; display: flex; flex-direction: column;
  background: var(--admin-glass-bg, rgba(255,255,255,0.82));
  backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 18px;
  box-shadow: var(--admin-shadow-md, 0 8px 28px rgba(15, 23, 42, 0.06));
  padding: 20px 12px;
}

/* ─── Brand ─── */
.sidebar-brand {
  display: flex; align-items: center; gap: 10px;
  padding: 0 8px 20px; text-decoration: none;
  border-bottom: 1px solid #f1f5f9; margin-bottom: 16px;
}

.brand-icon {
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #2563eb, #1e40af);
  color: #fff; border-radius: 8px;
}

.brand-label { font-size: 16px; font-weight: 700; color: #0f172a; font-family: 'Plus Jakarta Sans', sans-serif; }

/* ─── Nav ─── */
.sidebar-nav { flex: 1; display: flex; flex-direction: column; gap: 2px; overflow-y: auto; }

.nav-category { font-size: 10px; font-weight: 600; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.8px; padding: 12px 8px 4px; }

.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 12px; border-radius: 8px;
  text-decoration: none; color: #64748b; font-size: 13px; font-weight: 500;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  position: relative;
}
.nav-item:hover { background: #f1f5f9; color: #334155; }
.nav-item--active {
  background: linear-gradient(90deg, rgba(37,99,235,0.10), rgba(37,99,235,0.04));
  color: #1e40af; font-weight: 600;
}
.nav-item--active::before {
  content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  width: 3px; height: 16px; border-radius: 2px;
  background: linear-gradient(180deg, #2563eb, #60a5fa);
}
.nav-ico { display: flex; align-items: center; width: 18px; height: 18px; flex-shrink: 0; }

/* ─── Footer ─── */
.sidebar-footer { padding-top: 12px; border-top: 1px solid #f1f5f9; margin-top: 8px; }

.sidebar-user { display: flex; align-items: center; gap: 10px; }
.su-avatar {
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #2563eb, #1e40af); color: #fff;
  border-radius: 8px; font-size: 12px; font-weight: 700; font-family: 'JetBrains Mono', monospace; flex-shrink: 0;
}
.su-info { flex: 1; min-width: 0; }
.su-name { font-size: 12px; font-weight: 600; color: #334155; display: block; }
.su-role { font-size: 10px; color: #94a3b8; }
.su-logout {
  width: 28px; height: 28px; display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; border-radius: 6px; cursor: pointer;
  color: #94a3b8; transition: all 0.15s; flex-shrink: 0;
}
.su-logout:hover { background: #fef2f2; color: #ef4444; }

/* ─── Main ─── */
.admin-main { flex: 1; display: flex; flex-direction: column; min-width: 0; padding: 12px 24px 24px 12px; }

.admin-topbar {
  display: flex; align-items: center; justify-content: flex-end;
  margin-bottom: 20px; height: 44px;
}

.topbar-right { display: flex; align-items: center; gap: 12px; }
.topbar-bell { position: relative; display: flex; align-items: center; color: #64748b; transition: color .15s; cursor: pointer; }
.topbar-bell:hover { color: #1e40af; }
.bell-badge-wrap { display: flex; }
/* ─── 悬浮通知面板（el-popover 内容，popper 挂 body 下需全局样式） ─── */
:global(.notif-popover) { padding: 0 !important; border-radius: 12px; box-shadow: 0 12px 32px rgba(15, 23, 42, .12) !important; }
.notif-panel { display: flex; flex-direction: column; max-height: 460px; }
.np-head { display: flex; align-items: center; gap: 8px; padding: 12px 14px; border-bottom: 1px solid #f1f5f9; }
.np-title { font-size: 14px; font-weight: 600; color: #0f172a; }
.np-unread { font-size: 11px; color: #1e40af; background: #eff6ff; border-radius: 10px; padding: 1px 8px; }
.np-enable { font-size: 11.5px; color: #7c3aed; background: #f5f3ff; border: 1px solid #ddd6fe; border-radius: 6px; padding: 2px 8px; cursor: pointer; font-family: inherit; }
.np-enable:hover { background: #ede9fe; }
.np-clear { margin-left: auto; font-size: 11.5px; color: #64748b; background: none; border: none; cursor: pointer; font-family: inherit; }
.np-clear:hover { color: #1e40af; }
.np-list { overflow-y: auto; max-height: 330px; }
.np-item { display: flex; gap: 10px; padding: 10px 14px; cursor: pointer; border-bottom: 1px solid #f8fafc; transition: background .12s; }
.np-item:hover { background: #f8fafc; }
.np-item--unread { background: #f0f9ff; }
.np-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 5px; flex-shrink: 0; background: #94a3b8; }
.np-dot--task, .np-dot--task_success, .np-dot--task_failed { background: #1e40af; }
.np-dot--data { background: #10b981; }
.np-dot--ai { background: #8b5cf6; }
.np-dot--system { background: #f59e0b; }
.np-body { flex: 1; min-width: 0; }
.np-row { display: flex; align-items: center; gap: 8px; }
.np-type { font-size: 10.5px; color: #64748b; background: #f1f5f9; border-radius: 4px; padding: 0 6px; }
.np-time { margin-left: auto; font-size: 10.5px; color: #94a3b8; }
.np-title-text { font-size: 12.5px; font-weight: 600; color: #334155; margin-top: 2px; }

/* ═══ 悬浮面板通知详情弹窗 ═══ */
.np-detail { padding: 4px 2px; }
.np-detail-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.np-detail-type { font-size: 12px; font-weight: 600; color: #7c3aed; }
.np-detail-time { font-size: 12px; color: #94a3b8; }
.np-detail-title { font-size: 15px; font-weight: 600; color: #0f172a; margin: 0 0 10px; }
.np-detail-content { font-size: 13px; line-height: 1.9; color: #475569; white-space: pre-wrap; word-break: break-word; max-height: 50vh; overflow-y: auto; margin: 0; }
.np-detail-actions { margin-top: 14px; text-align: right; }
.np-item--unread .np-title-text { color: #0f172a; }
.np-content { font-size: 11.5px; color: #64748b; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.np-empty { padding: 28px 0; text-align: center; color: #94a3b8; font-size: 12.5px; }
.np-foot { padding: 10px 14px; border-top: 1px solid #f1f5f9; text-align: center; }
.np-all { font-size: 12px; color: #1e40af; text-decoration: none; }
.np-all:hover { text-decoration: underline; }

.topbar-link {
  display: flex; align-items: center; gap: 6px; font-size: 12.5px; color: #64748b;
  text-decoration: none; padding: 6px 12px; border-radius: 8px; transition: all 0.15s;
}
.topbar-link:hover { background: #f1f5f9; color: #1e40af; }
.role-tag {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 600; color: #1e40af;
  background: rgba(30,64,175,0.08);
  padding: 5px 12px; border-radius: 9999px;
}

.admin-content { flex: 1; overflow-y: auto; }
</style>
