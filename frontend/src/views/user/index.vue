<!-- UserLayout — 现代SaaS：左导航 + 顶栏搜索 + 毛玻璃 -->
<template>
  <div class="portal">
    <!-- 左侧导航 -->
    <aside class="sidebar">
      <div class="sidebar-inner">
        <!-- Logo -->
        <router-link to="/user/dashboard" class="sidebar-logo">
          <div class="logo-mark">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
              <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
              <line x1="12" y1="22.08" x2="12" y2="12"/>
            </svg>
          </div>
          <span class="logo-text">Profile</span>
        </router-link>

        <!-- 导航 -->
        <nav class="side-nav">
          <template v-for="(group, gi) in navGroups" :key="gi">
            <span v-if="group.title" class="nav-category">{{ group.title }}</span>
            <router-link
              v-for="item in group.items"
              :key="item.path"
              :to="item.path"
              class="nav-item"
              active-class="nav-item--active"
            >
              <span class="nav-icon" v-html="item.icon"></span>
              <span class="nav-label">{{ item.label }}</span>
            </router-link>
          </template>
        </nav>

        <!-- 底部用户 -->
        <div class="sidebar-user">
          <div class="user-avatar">{{ initials }}</div>
          <div class="user-info">
            <span class="user-name">{{ userStore.info?.displayName || userStore.info?.username }}</span>
            <span class="user-role">运营分析员</span>
          </div>
          <button class="user-logout" @click="handleLogout" title="退出登录">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          </button>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="main-area">
      <!-- 顶栏 -->
      <header class="topbar">
        <div class="topbar-right">
          <NotifBell mode="user" />
          <router-link to="/user/settings" class="avatar-ring">
            <span class="avatar-sm">{{ initials }}</span>
          </router-link>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="content">
        <router-view />
      </main>
    </div>

    <!-- AI 聊天组件 -->
    <AIChatWidget />
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue'
  import { ElMessageBox } from 'element-plus'
  import { useUserStore } from '@/store/modules/user'
  import AIChatWidget from '@/components/core/layouts/AIChatWidget.vue'
  import NotifBell from '@/components/core/NotifBell.vue'

  defineOptions({ name: 'UserPortalLayout' })

  const userStore = useUserStore()

  const initials = computed(() =>
    (userStore.info?.displayName || userStore.info?.username || 'U').charAt(0).toUpperCase()
  )

  const navGroups = [
    {
      title: '',
      items: [
        { path: '/user/dashboard', label: '工作台',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/></svg>' },
      ]
    },
    {
      title: '画像',
      items: [
        { path: '/user/overview', label: '画像概览',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>' },
        { path: '/user/profiles', label: '画像列表',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>' },
        { path: '/user/tags', label: '标签分析',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>' },
      ]
    },
    {
      title: '分析',
      items: [
        { path: '/user/product-analysis', label: '商品分析',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>' },
        { path: '/user/cluster-analysis', label: '用户聚类',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="5" cy="6" r="3"/><circle cx="19" cy="5" r="2"/><circle cx="12" cy="12" r="4"/><circle cx="6" cy="19" r="2"/><circle cx="19" cy="18" r="3"/><path d="M7.5 8.5L10 10M14 10l3-3.5M9 15l-1.5 2.5M15 15l2.5 1.5"/></svg>' },
        { path: '/user/repeat-analysis', label: '复购与留存',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>' },
        { path: '/user/churn-analysis', label: '流失预警',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>' },
      ]
    },
    {
      title: '运营',
      items: [
        { path: '/user/audience', label: '人群圈选',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>' },
        { path: '/user/audience/comparison', label: '画像对比',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>' },
        { path: '/user/audience/packages', label: '人群包',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>' },
        { path: '/user/ai', label: 'AI 分析',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 2a4 4 0 0 1 4 4c0 2-2 4-4 4a4 4 0 0 1-4-4c0-2.21 1.79-4 4-4z"/><path d="M2 22v-2a6 6 0 0 1 6-6h8a6 6 0 0 1 6 6v2"/><circle cx="17" cy="9" r="2"/><path d="M21 22v-2a4 4 0 0 0-3-3.87"/><path d="M7 22v-2a4 4 0 0 1 3-3.87"/></svg>' },
      ]
    },
    {
      title: '消息',
      items: [
        { path: '/user/notifications', label: '通知',
          icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>' }
      ]
    }
  ]

  function handleLogout() {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    }).then(() => userStore.logOut()).catch(() => {})
  }

</script>

<style scoped>
/* ─── 布局容器 ─── */
.portal {
  display: flex; min-height: 100vh;
  background: var(--grad-page, linear-gradient(180deg, #f4fbf9 0%, #eaf5f2 100%));
  font-family: var(--font-body, 'Plus Jakarta Sans', 'Inter', 'PingFang SC', system-ui, sans-serif);
}

/* ══════════════════ 侧边栏 ══════════════════ */
.sidebar {
  width: 220px; flex-shrink: 0; position: sticky; top: 0; height: 100vh;
  padding: 16px 12px;
}

.sidebar-inner {
  height: 100%; display: flex; flex-direction: column;
  background: #ffffff;
  border: 1px solid var(--bezel-border, rgba(15, 23, 42, 0.06));
  border-radius: var(--bezel-radius, 20px);
  box-shadow: var(--bezel-shadow, 0 8px 28px rgba(13, 148, 136, 0.06));
  padding: 20px 12px;
  transition: box-shadow 0.35s cubic-bezier(0.32, 0.72, 0, 1);
}

/* ─── Logo ─── */
.sidebar-logo {
  display: flex; align-items: center; gap: 10px;
  padding: 0 8px 24px; text-decoration: none;
  border-bottom: 1px solid rgba(0,0,0,0.04);
  margin-bottom: 16px;
}

.logo-mark {
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
  background: var(--grad-purple, linear-gradient(135deg, #2dd4bf, #0d9488));
  color: #fff; border-radius: var(--radius-sm, 10px);
}

.logo-text {
  font-family: var(--font-display, 'Plus Jakarta Sans', sans-serif);
  font-size: 17px; font-weight: 700; color: var(--text-primary, #18181b);
  letter-spacing: -0.4px;
}

/* ─── Nav ─── */
.side-nav { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.nav-category { font-size: 10px; font-weight: 600; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.8px; padding: 12px 8px 4px; }

.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: var(--radius-sm, 10px);
  text-decoration: none; color: var(--text-secondary, #71717a);
  font-size: 13.5px; font-weight: 500;
  transition: all 0.2s cubic-bezier(0.32,0.72,0,1);
}

.nav-item:hover {
  background: rgba(13,148,136,0.06);
  color: var(--text-primary, #18181b);
}

.nav-item--active {
  background: linear-gradient(90deg, rgba(13,148,136,0.10), rgba(13,148,136,0.04));
  color: var(--purple-600, #0d9488);
  font-weight: 600;
  position: relative;
}
.nav-item--active::before {
  content: ''; position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  width: 3px; height: 16px; border-radius: 2px;
  background: linear-gradient(180deg, #2dd4bf, #0d9488);
}

.nav-icon { display: flex; align-items: center; width: 18px; height: 18px; flex-shrink: 0; }

/* ─── Bottom User ─── */
.sidebar-user {
  display: flex; align-items: center; gap: 10px;
  padding: 16px 8px 0; margin-top: auto;
  border-top: 1px solid rgba(0,0,0,0.04);
}

.user-avatar {
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
  background: var(--grad-purple); color: #fff;
  border-radius: var(--radius-sm, 10px);
  font-size: 13px; font-weight: 700;
  font-family: var(--font-mono, monospace); flex-shrink: 0;
}

.user-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.user-name { font-size: 13px; font-weight: 600; color: var(--text-primary); }
.user-role { font-size: 11px; color: var(--text-tertiary); }

.user-logout {
  width: 30px; height: 30px; display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; border-radius: 8px; cursor: pointer;
  color: var(--text-tertiary); transition: all 0.15s; flex-shrink: 0;
}
.user-logout:hover { background: rgba(239,68,68,0.08); color: #ef4444; }

/* ══════════════════ 主内容区 ══════════════════ */
.main-area {
  flex: 1; display: flex; flex-direction: column; min-width: 0;
  padding: 16px 24px 24px 12px;
}

/* ─── Topbar ─── */
.topbar {
  display: flex; align-items: center; justify-content: flex-end;
  margin-bottom: 24px; height: 48px;
}

.topbar-right { display: flex; align-items: center; gap: 12px; }

.avatar-ring {
  width: 40px; height: 40px; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #2dd4bf, #0d9488); color: #fff; border-radius: 9999px;
  text-decoration: none; font-size: 14px; font-weight: 700;
  font-family: var(--font-mono, monospace);
  box-shadow: 0 0 0 2px rgba(13,148,136,0.15);
  transition: box-shadow 0.2s;
}
.avatar-ring:hover { box-shadow: 0 0 0 3px rgba(13,148,136,0.25); }

.avatar-sm { line-height: 1; }

/* ─── Content ─── */
.content {
  flex: 1; overflow-y: auto;
}
</style>
