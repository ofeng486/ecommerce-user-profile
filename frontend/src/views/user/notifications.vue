<!-- 站内通知 — 用户端通知中心：三列布局（左侧分类导航 + 中间列表 + 右侧快捷面板）
     分类面向运营视角：全部/只看未读/分析任务/数据更新/系统消息 -->
<template>
  <div class="notif-view" :class="isAdmin ? 'theme-admin' : 'theme-user'">
    <!-- 全宽 header -->
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">{{ pageTitle }}</h1>
          <span class="title-tag">NOTIFICATIONS</span>
        </div>
        <p class="page-desc">{{ pageDesc }}</p>
      </div>
      <div class="ph-meta">
        <button v-if="isAdmin" class="btn-announce" @click="announceVisible = true">发布公告</button>
        <button v-if="unreadCount" class="btn-mark-all" :disabled="markingAll" @click="markAllRead">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><polyline points="20 6 9 17 4 12"/></svg>
          全部已读
        </button>
        <span v-else-if="total" class="header-meta">无未读</span>
      </div>
    </div>

    <div class="notif-body">
          <!-- 左侧：分类导航 -->
          <aside class="notif-sidebar">
            <div class="sb-title">分类</div>
            <ul class="sb-nav">
              <li v-for="t in getTypeNav()" :key="t.key"
                class="sb-item" :class="{ 'sb-item--active': isActive(t) }"
                @click="switchNav(t.key)">
                <span class="sb-label">{{ t.label }}</span>
                <span v-if="navCount(t.key) > 0" class="sb-count">{{ navCount(t.key) }}</span>
              </li>
            </ul>

            <div class="sb-divider"></div>
            <p class="sb-tip">{{ sbTip }}</p>
          </aside>

      <!-- 中间：主列表 -->
      <div class="notif-main">
        <div class="filter-strip">
          <span class="filter-chip">{{ currentLabel }}</span>
          <span class="filter-meta">共 {{ total }} 条</span>
        </div>

        <div class="notif-list" v-loading="loading">
          <div v-if="!loading && list.length === 0" class="empty-state">
            <svg class="empty-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>
            <p class="empty-text">{{ emptyText }}</p>
          </div>

          <article
            v-for="item in list" :key="item.id"
            class="notif-card"
            :class="{ 'notif-card--unread': !item.isRead }"
            @click="handleClick(item)"
          >
            <div class="notif-marker" :class="`marker--${(item.type || 'system').toLowerCase()}`"></div>
            <div class="notif-body">
              <div class="notif-row">
                <span class="notif-type">{{ typeLabel(item.type) }}</span>
                <span v-if="!item.isRead" class="unread-dot"></span>
              </div>
              <h3 class="notif-title-text">{{ friendlyTitle(item) }}</h3>
              <p class="notif-content">{{ friendlyContent(item) }}</p>
              <time class="notif-time">{{ relTime(item.createdAt) }}</time>
            </div>
            <button class="notif-del" title="删除该通知" @click.stop="removeItem(item)">✕</button>
          </article>

          <div v-if="total > size" class="page-bar">
            <button :disabled="page <= 1" @click="page--; loadData()" class="page-btn">上一页</button>
            <span class="page-info">{{ page }} / {{ Math.ceil(total / size) }}</span>
            <button :disabled="page >= Math.ceil(total / size)" @click="page++; loadData()" class="page-btn">下一页</button>
          </div>
        </div>
      </div>

      <!-- 右侧：快捷面板 -->
      <aside class="notif-aside">
        <div class="aside-card">
          <div class="aside-label">未读</div>
          <div class="aside-value" :class="{ 'aside-value--active': unreadCount > 0 }">{{ unreadCount }}</div>
          <div class="aside-sub">{{ unreadCount > 0 ? '点击上方「全部已读」快速清除' : '全部已读' }}</div>
        </div>

        <div class="aside-card">
          <div class="aside-label">常用入口</div>
          <ul class="quick-list">
            <li v-for="q in getQuickList()" :key="q.path" class="quick-item" @click="goTo(q.path)">
              <svg class="quick-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" v-html="q.icon"></svg>
              <div class="quick-meta">
                <div class="quick-title">{{ q.title }}</div>
                <div class="quick-desc">{{ q.desc }}</div>
              </div>
            </li>
          </ul>
        </div>
      </aside>
    </div>

    <!-- 平台公告弹窗（管理端） -->
    <ElDialog v-model="announceVisible" title="发布平台公告" width="520px">
      <div class="announce-form">
        <div class="af-row">
          <span class="af-label">标题</span>
          <el-input v-model="announceTitle" maxlength="50" placeholder="例如：系统维护通知 / 新功能上线" />
        </div>
        <div class="af-row">
          <span class="af-label">内容</span>
          <el-input v-model="announceContent" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="公告内容，将广播给所有用户" />
        </div>
      </div>
      <template #footer>
        <ElButton @click="announceVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="announcing" :disabled="!announceContent.trim()" @click="doAnnounce">发布</ElButton>
      </template>
    </ElDialog>

    <!-- 通知详情弹窗 -->
    <ElDialog v-model="detailVisible" title="通知详情" width="520px" class="notif-dialog">
      <div v-if="detailItem" class="detail-body">
        <div class="detail-head">
          <span class="notif-type">{{ typeLabel(detailItem.type) }}</span>
          <time class="detail-time">{{ formatTime(detailItem.createdAt) }}</time>
        </div>
        <h3 class="detail-title">{{ friendlyTitle(detailItem) }}</h3>
        <p class="detail-content">{{ friendlyContent(detailItem) }}</p>
        <div v-if="canJump" class="detail-actions">
          <ElButton type="primary" size="small" @click="goRelated">{{ jumpLabel }}</ElButton>
        </div>
      </div>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { fetchNotifications, fetchUnreadCount, markAsRead, markAllAsRead, deleteNotification } from '@/api/notification'
import request from '@/utils/http'

defineOptions({ name: 'UserNotifications' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
/** 管理端（角色 Admin）显示"发布公告" */
const isAdmin = computed(() => (userStore.info?.role || '').toUpperCase() === 'ADMIN')
/** 可跳转：聚类/画像任务→聚类页；AI 通知→AI 分析页 */
const canJump = computed(() => {
  const it = detailItem.value
  if (!it) return false
  if (it.refType === 'TASK' && /聚类重算|画像分析/.test(it.content || '')) return true
  if (it.refType === 'AI') return true
  return false
})
const jumpLabel = computed(() => {
  const it = detailItem.value
  return it?.refType === 'AI' ? '前往 AI 分析 →' : '前往用户聚类查看 →'
})
function goRelated() {
  if (!canJump.value) return
  const it = detailItem.value
  detailVisible.value = false
  if (it.refType === 'TASK') {
    router.push('/user/cluster-analysis')
  } else if (it.refType === 'AI') {
    router.push(route.path.startsWith('/user') ? '/user/ai' : '/dashboard/overview')
  }
}

const loading = ref(false)
const markingAll = ref(false)
const list = ref<any[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const unreadCount = ref(0)
/** 当前导航：all | unread | TASK | DATA | SYSTEM */
const nav = ref('all')

/** 常用入口（按角色） */
const QUICK_LIST_ADMIN = [
  { path: '/system/users', title: '用户管理', desc: '系统用户与权限', icon: '<path stroke-linecap="round" stroke-linejoin="round" d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zm14 10v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>' },
  { path: '/tasks', title: '任务管理', desc: '查看任务执行结果', icon: '<path stroke-linecap="round" stroke-linejoin="round" d="M9 11l3 3L22 4M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/>' },
  { path: '/admin/cluster-analysis', title: '聚类重算', desc: '重算 K-Means 分组', icon: '<rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>' },
  { path: '/admin/import', title: '数据导入', desc: '导入原始数据并重建画像', icon: '<path stroke-linecap="round" stroke-linejoin="round" d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/>' },
  { path: '/dashboard/overview', title: '运营总览', desc: '核心指标一览', icon: '<rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/>' }
]
const QUICK_LIST_USER = [
  { path: '/user/cluster-analysis', title: '用户聚类', desc: '查看最新分组结果', icon: '<path stroke-linecap="round" stroke-linejoin="round" d="M8 13v-1m4 1v-3m4 3V8m2 13H6a2 2 0 01-2-2V6a2 2 0 012-2h12a2 2 0 012 2v12a2 2 0 01-2 2z"/>' },
  { path: '/user/overview', title: '画像概览', desc: '核心指标一览', icon: '<path stroke-linecap="round" stroke-linejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>' },
  { path: '/user/ai', title: 'AI 数据分析', desc: '自然语言查询数据', icon: '<path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z"/>' }
]
/** 用户端分类（去 AI/PUSH） */
const TYPE_NAV_USER = [
  { key: 'all', label: '全部通知' },
  { key: 'unread', label: '只看未读' },
  { key: 'TASK', label: '分析任务' },
  { key: 'DATA', label: '数据更新' },
  { key: 'SYSTEM', label: '系统消息' }
]
/** 管理端分类（"任务结果"统一入口） */
const TYPE_NAV_ADMIN = [
  { key: 'all', label: '全部通知' },
  { key: 'unread', label: '只看未读' },
  { key: 'TASK', label: '任务结果' },
  { key: 'DATA', label: '数据更新' },
  { key: 'SYSTEM', label: '系统消息' }
]
/** 模板 getter：按角色返回对应配置（响应式依赖 isAdmin） */
function getTypeNav() { return isAdmin.value ? TYPE_NAV_ADMIN : TYPE_NAV_USER }
function getQuickList() { return isAdmin.value ? QUICK_LIST_ADMIN : QUICK_LIST_USER }

/** 页面标题与描述（按角色） */
const pageTitle = computed(() => isAdmin.value ? '系统通知' : '通知')
const pageDesc = computed(() => isAdmin.value
  ? '系统消息、任务结果、数据更新的集中收件箱'
  : '分析任务、数据更新、系统消息的集中收件箱')
const sbTip = computed(() => isAdmin.value
  ? '可发布全站公告、查看任务结果与系统消息'
  : '通知仅保留任务结果与系统重要消息')

function isActive(t: { key: string }) {
  return nav.value === t.key
}

/** 切换导航：只看未读 单独处理，其余按 type 过滤 */
function switchNav(key: string) {
  nav.value = key
  page.value = 1
  loadData()
}

/** 当前导航对应的后端参数 */
function currentParams(): { type?: string; unread?: boolean } {
  if (nav.value === 'unread') return { unread: true }
  if (nav.value === 'all') return {}
  return { type: nav.value }
}

/** 徽章计数：只看未读 → 未读总数；类型 → 当前页该类型数 */
function navCount(key: string): number {
  if (key === 'unread') return unreadCount.value
  if (key === 'all') return 0
  return list.value.filter(i => (i.type || '').toUpperCase().startsWith(key.toUpperCase())).length
}

const currentLabel = computed(() => {
  const t = getTypeNav().find(n => n.key === nav.value)
  return t ? t.label : '全部通知'
})

const emptyText = computed(() => {
  if (nav.value === 'unread') return '没有未读通知'
  if (nav.value === 'all') return '暂无通知，分析任务完成时会自动出现'
  return '该分类暂无通知'
})

function typeLabel(t: string) {
  const m: Record<string, string> = { SYSTEM: '系统消息', TASK: '分析任务', TASK_SUCCESS: '分析任务', TASK_FAILED: '分析任务', DATA: '数据更新', AI: 'AI 分析', PUSH: '推送' }
  return m[t] || t || '通知'
}

/** 聚类重算/RFM画像分析等任务通知转为友好文案（隐藏 K=8/版本号等技术细节） */
function friendlyContent(item: any): string {
  const raw = item.content || ''
  if (item.refType === 'TASK' && /聚类重算|画像分析/.test(raw)) {
    return '画像与聚类数据已更新，前往「用户聚类」查看最新分组结果。'
  }
  return raw
}
function friendlyTitle(item: any): string {
  const raw = item.title || ''
  if (item.refType === 'TASK' && /聚类重算|画像分析/.test(item.content || '')) {
    return '画像数据已更新'
  }
  return raw
}

function relTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return ''
  const diff = Date.now() - d.getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return `${m} 分钟前`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} 小时前`
  const day = Math.floor(h / 24)
  if (day < 7) return `${day} 天前`
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`
}

function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value - 1, size: size.value, ...currentParams() }
    const res = await fetchNotifications(params)
    if (res) { list.value = res.records || []; total.value = res.total || 0 }
  } catch {} finally { loading.value = false }
}

async function loadUnread() {
  try { unreadCount.value = (await fetchUnreadCount() as any) || 0 } catch { unreadCount.value = 0 }
}

const detailVisible = ref(false)
const detailItem = ref<any>(null)
async function handleClick(item: any) {
  if (!item.isRead) {
    try { await markAsRead(item.id); item.isRead = true; if (unreadCount.value > 0) unreadCount.value-- } catch {}
  }
  detailItem.value = item
  detailVisible.value = true
}

async function markAllRead() {
  markingAll.value = true
  try { await markAllAsRead(); list.value.forEach(i => i.isRead = true); unreadCount.value = 0 } catch {} finally { markingAll.value = false }
}

async function removeItem(item: any) {
  try {
    await deleteNotification(item.id)
  } catch {}
  list.value = list.value.filter(i => i.id !== item.id)
  total.value = Math.max(0, total.value - 1)
  if (!item.isRead && unreadCount.value > 0) unreadCount.value--
  if (detailItem.value?.id === item.id) detailVisible.value = false
}

function goTo(path: string) { router.push(path) }

/** ─── 平台公告（管理端） ─── */
const announceVisible = ref(false)
const announcing = ref(false)
const announceTitle = ref('平台公告')
const announceContent = ref('')
async function doAnnounce() {
  if (!announceContent.value.trim() || announcing.value) return
  announcing.value = true
  try {
    await request.post({ url: '/api/v1/notifications/broadcast', data: { title: announceTitle.value.trim() || '平台公告', content: announceContent.value.trim() } })
    ElMessage.success('公告已发布')
    announceVisible.value = false
    announceContent.value = ''
    announceTitle.value = '平台公告'
    page.value = 1
    loadData()
    loadUnread()
  } catch {
    ElMessage.error('发布失败')
  } finally { announcing.value = false }
}

onMounted(() => { loadData(); loadUnread() })
</script>

<style scoped>
.notif-view {
  font-family: 'Inter', 'PingFang SC', system-ui, sans-serif;
}

/* ─── 三列布局 ─── */
.notif-body {
  display: grid;
  grid-template-columns: 200px 1fr 230px;
  gap: 16px;
  align-items: start;
}
@media (max-width: 1200px) {
  .notif-body { grid-template-columns: 200px 1fr; }
  .notif-aside { display: none; }
}
@media (max-width: 900px) {
  .notif-body { grid-template-columns: 1fr; }
  .notif-sidebar { order: 2; }
  .notif-main { order: 1; }
  .notif-aside { display: none; }
}

/* ─── 左侧分类导航 ─── */
.notif-sidebar {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px;
  position: sticky; top: 16px;
}
.sb-title {
  font-size: 11px; font-weight: 600; color: #94a3b8;
  text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 10px;
}
.sb-nav { list-style: none; padding: 0; margin: 0; }
.sb-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 8px;
  cursor: pointer; transition: all 0.12s; font-size: 13px; color: #475569;
}
.sb-item:hover { background: #f8fafc; }
.sb-item--active { background: var(--acc-soft); color: var(--acc); font-weight: 500; }
.sb-label { flex: 1; }
.sb-count {
  min-width: 18px; padding: 0 6px; height: 18px; line-height: 18px;
  background: #f1f5f9; color: #64748b; border-radius: 9px;
  font-size: 11px; text-align: center; font-weight: 500;
}
.sb-item--active .sb-count { background: var(--acc); color: #fff; }
.sb-divider { height: 1px; background: #f1f5f9; margin: 16px 0 14px; }
.sb-tip { font-size: 12px; color: #94a3b8; line-height: 1.6; margin: 0; }

/* ─── 中间主区 ─── */
.notif-main { min-width: 0; }

.notif-action {
  font-size: 13px; color: var(--acc); background: none; border: 1px solid var(--acc-line);
  padding: 6px 14px; border-radius: 6px; cursor: pointer; transition: all 0.15s;
  font-family: inherit;
}
.notif-action:hover:not(:disabled) { background: var(--acc-soft); }
.notif-action:disabled { opacity: 0.4; cursor: not-allowed; }

/* ─── 主题变量（按角色：管理端蓝 / 用户端青） ─── */
.theme-admin.notif-view { --acc: #2563eb; --acc-dark: #1d4ed8; --acc-soft: rgba(37,99,235,.08); --acc-line: #93c5fd; }
.theme-user.notif-view { --acc: #0d9488; --acc-dark: #0f766e; --acc-soft: rgba(13,148,136,.08); --acc-line: #5eead4; }

/* 页头主题色跟随（覆盖全局 .page-header .title-accent 的青色渐变） */
.notif-view .title-accent {
  background: linear-gradient(180deg, var(--acc) 0%, var(--acc-dark) 100%);
}

/* 发布公告主按钮（管理端显眼）—— 使用主题变量，管理端蓝 / 用户端青 */
.btn-announce {
  font-size: 13px; font-weight: 500; color: #ffffff;
  background: var(--acc); border: 1px solid var(--acc);
  padding: 7px 16px; border-radius: 6px; cursor: pointer;
  transition: all 0.15s; font-family: inherit;
  display: inline-flex; align-items: center; gap: 4px;
}
.btn-announce::before {
  content: '+'; font-size: 15px; font-weight: 400; line-height: 1;
}
.btn-announce:hover { background: var(--acc-dark); border-color: var(--acc-dark); }

/* 全部已读主按钮（仅在有未读时显示，醒目）—— 使用主题变量 */
.btn-mark-all {
  font-size: 13px; font-weight: 500; color: #ffffff;
  background: var(--acc); border: 1px solid var(--acc);
  padding: 7px 16px; border-radius: 6px; cursor: pointer;
  transition: all 0.15s; font-family: inherit;
  display: inline-flex; align-items: center; gap: 5px;
}
.btn-mark-all:hover:not(:disabled) { background: var(--acc-dark); border-color: var(--acc-dark); }
.btn-mark-all:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-mark-all svg { stroke: #ffffff; }

.filter-strip { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.filter-chip {
  padding: 5px 14px; border: 1px solid var(--acc); background: var(--acc-soft);
  border-radius: 20px; font-size: 12.5px; color: var(--acc); font-family: inherit; font-weight: 500;
}
.filter-meta { font-size: 12px; color: #94a3b8; margin-left: auto; }

/* ─── 列表 ─── */
.notif-list {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;
}
.empty-state { text-align: center; padding: 60px 20px; }
.empty-ico { width: 40px; height: 40px; color: #cbd5e1; display: block; margin: 0 auto 10px; }
.empty-text { font-size: 14px; color: #94a3b8; margin: 0; }

.notif-card {
  display: flex; gap: 14px; padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9; cursor: pointer; transition: background 0.15s;
}
.notif-card:last-child { border-bottom: none; }
.notif-card:hover { background: #fafbfc; }
.notif-card--unread { background: #f8fafc; }
.notif-card--unread:hover { background: #f1f5f9; }

.notif-marker { width: 4px; border-radius: 2px; flex-shrink: 0; margin-top: 4px; }
.marker--system { background: var(--acc); }
.marker--task { background: #059669; }
.marker--task_success { background: #059669; }
.marker--task_failed { background: #dc2626; }
.marker--data { background: #d97706; }
.marker--ai { background: #14b8a6; }
.marker--push { background: #dc2626; }

.notif-body { flex: 1; min-width: 0; }
.notif-row { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.notif-type { font-size: 11px; font-weight: 600; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }
.unread-dot { width: 6px; height: 6px; background: var(--acc); border-radius: 50%; }
.notif-title-text { font-size: 14px; font-weight: 600; color: #1e293b; margin: 0 0 4px; line-height: 1.4; }
.notif-content { font-size: 13px; color: #64748b; line-height: 1.5; margin: 0 0 6px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.notif-time { font-size: 12px; color: #94a3b8; }

.notif-del {
  width: 22px; height: 22px; flex-shrink: 0; margin-top: 2px;
  display: none; align-items: center; justify-content: center;
  border: none; background: transparent; border-radius: 6px;
  color: #94a3b8; font-size: 12px; cursor: pointer; transition: all 0.15s;
}
.notif-card:hover .notif-del { display: inline-flex; }
.notif-del:hover { background: #fee2e2; color: #dc2626; }

/* ─── 右侧快捷面板 ─── */
.notif-aside { display: flex; flex-direction: column; gap: 12px; position: sticky; top: 16px; }
.aside-card {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px;
}
.aside-label { font-size: 11px; font-weight: 600; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 10px; }
.aside-value { font-size: 32px; font-weight: 700; color: #cbd5e1; line-height: 1; font-variant-numeric: tabular-nums; }
.aside-value--active { color: var(--acc); }
.aside-sub { font-size: 11px; color: #94a3b8; margin-top: 6px; }
.quick-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 4px; }
.quick-item {
  display: flex; align-items: center; gap: 10px; padding: 8px 4px; border-radius: 8px;
  cursor: pointer; transition: background 0.12s;
}
.quick-item:hover { background: #f8fafc; }
.quick-ico { width: 18px; height: 18px; color: var(--acc); flex-shrink: 0; }
.quick-meta { flex: 1; min-width: 0; }
.quick-title { font-size: 13px; font-weight: 500; color: #334155; line-height: 1.3; }
.quick-desc { font-size: 11px; color: #94a3b8; margin-top: 2px; }

/* ─── 详情弹窗 ─── */
.announce-form { display: flex; flex-direction: column; gap: 14px; padding: 4px 2px; }
.af-row { display: flex; align-items: flex-start; gap: 12px; }
.af-label { width: 40px; font-size: 13px; color: #475569; line-height: 32px; flex-shrink: 0; }
.detail-body { padding: 4px 2px; }
.detail-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.detail-time { font-size: 12px; color: #94a3b8; }
.detail-title { font-size: 15px; font-weight: 600; color: #0f172a; margin: 0 0 10px; }
.detail-content { font-size: 13px; line-height: 1.9; color: #475569; white-space: pre-wrap; word-break: break-word; max-height: 50vh; overflow-y: auto; margin: 0; }
.detail-actions { margin-top: 14px; text-align: right; }

/* ─── 分页 ─── */
.page-bar { display: flex; justify-content: center; align-items: center; gap: 16px; padding: 16px; }
.page-btn { padding: 6px 14px; border: 1px solid #e2e8f0; background: #fff; border-radius: 6px; font-size: 13px; color: #64748b; cursor: pointer; transition: all 0.15s; font-family: inherit; }
.page-btn:hover:not(:disabled) { border-color: var(--acc); color: var(--acc); }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 13px; color: #94a3b8; }
</style>
