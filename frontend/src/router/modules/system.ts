import { AppRouteRecord } from '@/types/router'

/** 系统管理路由 */
export const systemRoutes: AppRouteRecord = {
  name: 'System',
  path: '/system',
  component: '/index/index',
  meta: {
    title: '系统管理',
    icon: 'ri:settings-3-line',
    roles: ['Admin']
  },
  children: [
    {
      path: 'users',
      name: 'SystemUsers',
      component: '/system/user',
      meta: { title: '用户管理', keepAlive: true }
    },
    {
      path: 'user-center',
      name: 'UserCenter',
      component: '/system/user-center',
      meta: { title: '个人中心', isHide: true }
    }
  ]
}

/** 数据导入路由 */
export const importRoutes: AppRouteRecord = {
  name: 'DataImport',
  path: '/admin/import',
  component: '/index/index',
  meta: {
    title: '数据导入',
    icon: 'ri:upload-cloud-2-line',
    roles: ['Admin']
  },
  children: [
    {
      path: '',
      name: 'ImportData',
      component: '/admin/import',
      meta: { title: 'CSV 文件导入', keepAlive: true }
    }
  ]
}

/** 数据生成路由 */
export const dataGenerateRoutes: AppRouteRecord = {
  name: 'DataGenerate',
  path: '/admin/data-generate',
  component: '/index/index',
  meta: {
    title: '数据生成',
    icon: 'ri:database-2-line',
    roles: ['Admin']
  },
  children: [
    {
      path: '',
      name: 'GenerateData',
      component: '/admin/data-generate',
      meta: { title: 'Python 批量生成', keepAlive: true }
    }
  ]
}

/** 智能人群圈选路由 */
export const audienceRoutes: AppRouteRecord = {
  name: 'Audience',
  path: '/admin/audience',
  component: '/index/index',
  meta: {
    title: '人群圈选',
    icon: 'ri:user-search-line',
    roles: ['Admin']
  },
  children: [
    {
      path: 'index',
      name: 'AudienceSegmentation',
      component: '/admin/audience',
      meta: { title: '智能圈选', keepAlive: true }
    },
    {
      path: 'comparison',
      name: 'AudienceComparison',
      component: '/admin/audience/comparison',
      meta: { title: '画像对比', keepAlive: true }
    },
    {
      path: 'packages',
      name: 'AudiencePackages',
      component: '/admin/audience/packages',
      meta: { title: '人群包管理', keepAlive: true }
    }
  ]
}
