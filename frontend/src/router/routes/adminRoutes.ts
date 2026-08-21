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
    {
      path: 'admin/cluster-analysis',
      name: 'AdminClusterAnalysis',
      component: () => import('@views/admin/cluster-analysis/index.vue'),
      meta: { title: '聚类重算', roles: ['Admin'] }
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
