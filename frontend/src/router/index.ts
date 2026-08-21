import type { App } from 'vue'
import { createRouter, createWebHashHistory } from 'vue-router'
import { staticRoutes } from './routes/staticRoutes'
import { adminRoutes } from './routes/adminRoutes'

// User 门户路由 — 独立 Layout，支持 /user/* 路径
const userRoutes = {
  path: '/user',
  component: () => import('@views/user/index.vue'),
  redirect: '/user/dashboard',
  children: [
    { path: 'dashboard', name: 'UserDashboard', component: () => import('@views/user/dashboard.vue'), meta: { title: '工作台', roles: ['User'] } },
    { path: 'overview', name: 'UserOverview', component: () => import('@views/user/overview.vue'), meta: { title: '画像概览', roles: ['User'] } },
    { path: 'profiles', name: 'UserProfiles', component: () => import('@views/user/profiles.vue'), meta: { title: '画像列表', roles: ['User'] } },
    { path: 'profiles/:id', name: 'UserProfileDetail', component: () => import('@views/user/profile/detail.vue'), meta: { title: '画像详情', roles: ['User'], hideInMenu: true } },
    { path: 'tags', name: 'UserTags', component: () => import('@views/user/tags.vue'), meta: { title: '标签分析', roles: ['User'] } },
    { path: 'ai', name: 'UserAi', component: () => import('@views/user/ai.vue'), meta: { title: 'AI 分析', roles: ['User'] } },
    // 业务分析（复用管理端页面组件，User 角色可访问）
    { path: 'product-analysis', name: 'UserProductAnalysis', component: () => import('@views/admin/product-analysis/index.vue'), meta: { title: '商品分析', roles: ['User'] } },
    { path: 'repeat-analysis', name: 'UserRepeatAnalysis', component: () => import('@views/admin/repeat-analysis/index.vue'), meta: { title: '复购与留存', roles: ['User'] } },
    { path: 'churn-analysis', name: 'UserChurnAnalysis', component: () => import('@views/admin/churn-analysis/index.vue'), meta: { title: '流失预警', roles: ['User'] } },
    { path: 'cluster-analysis', name: 'UserClusterAnalysis', component: () => import('@views/admin/cluster-analysis/index.vue'), meta: { title: '用户聚类', roles: ['User'] } },
    // 人群运营（复用管理端页面组件）
    { path: 'audience', name: 'UserAudience', component: () => import('@views/admin/audience/index.vue'), meta: { title: '人群圈选', roles: ['User'] } },
    { path: 'audience/comparison', name: 'UserAudienceComparison', component: () => import('@views/admin/audience/comparison.vue'), meta: { title: '画像对比', roles: ['User'] } },
    { path: 'audience/packages', name: 'UserAudiencePackages', component: () => import('@views/admin/audience/packages.vue'), meta: { title: '人群包', roles: ['User'] } },
    { path: 'notifications', name: 'UserNotifications', component: () => import('@views/user/notifications.vue'), meta: { title: '通知', roles: ['User'] } },
    { path: 'settings', name: 'UserSettings', component: () => import('@views/user/settings.vue'), meta: { title: '个人中心', roles: ['User'] } }
  ]
}

// 创建路由实例
export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    ...staticRoutes,
    adminRoutes,
    userRoutes
  ]
})

import { setupBeforeEachGuard } from './guards/beforeEach'

// 初始化路由
export function initRouter(app: App<Element>): void {
  setupBeforeEachGuard(router)
  app.use(router)
}

export const HOME_PAGE_PATH = '/dashboard/overview'
