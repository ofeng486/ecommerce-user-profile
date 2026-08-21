import type { RouteRecordRaw } from 'vue-router'

/**
 * Admin 路由 — 标准 Vue Router 配置，不再依赖动态注册
 */
export const adminRoutes: RouteRecordRaw = {
  path: '/',
  component: () => import('@views/admin/index.vue'),
  redirect: '/dashboard/overview',
  children: [
    {
      path: 'dashboard/overview',
      name: 'AdminDashboard',
      component: () => import('@views/dashboard/console/index.vue'),
      meta: { title: '运营数据总览', roles: ['Admin'] }
    },
    {
      path: 'profiles',
      name: 'AdminProfiles',
      component: () => import('@views/profile/list/index.vue'),
      meta: { title: '画像列表', roles: ['Admin'] }
    },
    {
      path: 'profiles/:id',
      name: 'AdminProfileDetail',
      component: () => import('@views/profile/detail/index.vue'),
      meta: { title: '画像详情', roles: ['Admin'], hideInMenu: true }
    },
    /* ─── 管理 ─── */
    {
      path: 'tasks',
      name: 'AdminTasks',
      component: () => import('@views/task/list/index.vue'),
      meta: { title: '任务管理', roles: ['Admin'] }
    },
    {
      path: 'system/users',
      name: 'AdminSystemUsers',
      component: () => import('@views/system/user/index.vue'),
      meta: { title: '系统用户', roles: ['Admin'] }
    },
    {
      path: 'system/tag-definition',
      name: 'AdminTagDefinition',
      component: () => import('@views/system/tag-definition/index.vue'),
      meta: { title: '标签体系管理', roles: ['Admin'] }
    },
    {
      path: 'admin/data-generate',
      name: 'AdminDataGenerate',
      component: () => import('@views/admin/data-generate/index.vue'),
      meta: { title: '数据生成', roles: ['Admin'] }
    },
    {
      path: 'admin/import',
      name: 'AdminDataImport',
      component: () => import('@views/admin/import/index.vue'),
      meta: { title: '数据导入', roles: ['Admin'] }
    },
    /* ─── 运营 ─── */
    {
      path: 'admin/audience',
      name: 'AdminAudience',
      component: () => import('@views/admin/audience/index.vue'),
      meta: { title: '人群圈选', roles: ['Admin'] }
    },
    {
      path: 'admin/audience/comparison',
      name: 'AdminAudienceComparison',
      component: () => import('@views/admin/audience/comparison.vue'),
      meta: { title: '画像对比', roles: ['Admin'] }
    },
    {
      path: 'admin/audience/packages',
      name: 'AdminAudiencePackages',
      component: () => import('@views/admin/audience/packages.vue'),
      meta: { title: '人群包管理', roles: ['Admin'] }
    },
    {
      path: 'admin/product-analysis',
      name: 'AdminProductAnalysis',
      component: () => import('@views/admin/product-analysis/index.vue'),
      meta: { title: '商品分析', roles: ['Admin'] }
    },
    {
      path: 'admin/repeat-analysis',
      name: 'AdminRepeatAnalysis',
      component: () => import('@views/admin/repeat-analysis/index.vue'),
      meta: { title: '复购与留存', roles: ['Admin'] }
    },
    {
      path: 'admin/churn-analysis',
      name: 'AdminChurnAnalysis',
      component: () => import('@views/admin/churn-analysis/index.vue'),
      meta: { title: '流失预警', roles: ['Admin'] }
    },
    {
      path: 'admin/cluster-analysis',
      name: 'AdminClusterAnalysis',
      component: () => import('@views/admin/cluster-analysis/index.vue'),
      meta: { title: '用户聚类', roles: ['Admin'] }
    },
    {
      path: 'system/user-center',
      name: 'AdminUserCenter',
      component: () => import('@views/system/user-center/index.vue'),
      meta: { title: '个人中心', roles: ['Admin'] }
    },
    {
      path: 'notifications',
      name: 'AdminNotifications',
      component: () => import('@views/user/notifications.vue'),
      meta: { title: '通知', roles: ['Admin'] }
    }
  ]
}
