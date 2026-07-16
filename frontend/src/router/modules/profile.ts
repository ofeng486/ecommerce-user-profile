import { AppRouteRecord } from '@/types/router'

/** 用户画像路由 */
export const profileRoutes: AppRouteRecord = {
  name: 'Profile',
  path: '/profiles',
  component: '/index/index',
  meta: {
    title: '用户画像',
    icon: 'ri:user-search-line',
    roles: ['User', 'Admin']
  },
  children: [
    {
      path: '',
      name: 'ProfileList',
      component: '/profile/list',
      meta: { title: '画像列表', keepAlive: true }
    },
    {
      path: ':id',
      name: 'ProfileDetail',
      component: '/profile/detail',
      meta: { title: '画像详情', isHide: true }
    }
  ]
}

/** 标签分析路由 */
export const tagRoutes: AppRouteRecord = {
  name: 'Tags',
  path: '/tags',
  component: '/index/index',
  meta: {
    title: '标签分析',
    icon: 'ri:price-tag-3-line',
    roles: ['User', 'Admin']
  },
  children: [
    {
      path: '',
      name: 'TagAnalysis',
      component: '/tag/analysis',
      meta: { title: '标签分布', keepAlive: true }
    }
  ]
}

/** 分析任务路由（Admin） */
export const taskRoutes: AppRouteRecord = {
  name: 'Tasks',
  path: '/tasks',
  component: '/index/index',
  meta: {
    title: '分析任务',
    icon: 'ri:rocket-2-line',
    roles: ['Admin']
  },
  children: [
    {
      path: '',
      name: 'TaskList',
      component: '/task/list',
      meta: { title: 'Spark 画像任务', keepAlive: true }
    }
  ]
}
