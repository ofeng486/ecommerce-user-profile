import { AppRouteRecord } from '@/types/router'

/** 仪表盘——画像概览 */
export const dashboardRoutes: AppRouteRecord = {
  name: 'Dashboard',
  path: '/dashboard',
  component: '/index/index',
  redirect: '/dashboard/overview',
  meta: {
    title: '数据概览',
    icon: 'ri:dashboard-line',
    roles: ['User', 'Admin']
  },
  children: [
    {
      path: 'overview',
      name: 'ProfileOverview',
      component: '/dashboard/console',
      meta: {
        title: '画像仪表盘',
        keepAlive: false,
        fixedTab: true
      }
    }
  ]
}
