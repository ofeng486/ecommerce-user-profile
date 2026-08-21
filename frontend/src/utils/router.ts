/**
 * 路由工具函数
 *
 * @module utils/router
 */
import { RouteLocationNormalized, RouteRecordRaw } from 'vue-router'

/** 扩展的路由配置类型 */
export type AppRouteRecordRaw = RouteRecordRaw & {
  hidden?: boolean
}

/** 系统名称（用于页面标题） */
const SYSTEM_NAME = '电商用户画像分析系统'

/**
 * 设置页面标题
 * @param to 当前路由对象
 */
export const setPageTitle = (to: RouteLocationNormalized): void => {
  const { title } = to.meta
  if (title) {
    setTimeout(() => {
      document.title = `${String(title)} - ${SYSTEM_NAME}`
    }, 150)
  }
}
