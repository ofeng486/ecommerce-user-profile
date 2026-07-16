import { AppRouteRecord } from '@/types/router'
import { dashboardRoutes } from './dashboard'
import { systemRoutes, importRoutes, dataGenerateRoutes, audienceRoutes } from './system'
import { profileRoutes, tagRoutes, taskRoutes } from './profile'

export const routeModules: AppRouteRecord[] = [
  // 用户功能（User + Admin 可见）
  dashboardRoutes,       // 1. 数据概览
  profileRoutes,         // 2. 用户画像
  tagRoutes,             // 3. 标签分析
  // 管理功能（Admin 可见）
  taskRoutes,            // 4. 分析任务
  dataGenerateRoutes,    // 5. 数据生成
  importRoutes,          // 6. 数据导入
  audienceRoutes,        // 7. 智能圈选
  systemRoutes,          // 8. 系统管理
]
