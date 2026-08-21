/**
 * 路由全局前置守卫 — 简化版
 * 所有路由已在 router/index.ts 中直接注册，无需动态注册。
 * 本守卫只负责：登录状态检查 + 角色权限拦截 + 已登录自动跳转首页。
 */
import type { Router, RouteLocationNormalized, NavigationGuardNext } from 'vue-router'
import { useUserStore } from '@/store/modules/user'

// 无需登录即可访问的公开路由
const PUBLIC_PATHS = new Set([
  '/', '/auth/login', '/auth/register',
  '/auth/forget-password', '/403', '/500'
])

export function setupBeforeEachGuard(router: Router): void {
  router.beforeEach(async (
    to: RouteLocationNormalized,
    _from: RouteLocationNormalized,
    next: NavigationGuardNext
  ) => {
    const userStore = useUserStore()

    // 1. 公开路由 → 放行
    if (PUBLIC_PATHS.has(to.path)) {
      // 已登录用户访问 / 时，根据角色跳转到对应首页
      if (to.path === '/' && userStore.isLogin) {
        const role = userStore.info?.role
        next({ path: role === 'User' ? '/user/dashboard' : '/dashboard/overview', replace: true })
        return
      }
      next()
      return
    }

    // 2. 检查登录状态
    if (!userStore.isLogin) {
      next({ path: '/', query: { login: 'true', redirect: to.fullPath } })
      return
    }

    // 3. 检查角色权限
    const routeRoles = to.meta?.roles as string[] | undefined
    const userRole = userStore.info?.role

    if (routeRoles && userRole && !routeRoles.includes(userRole)) {
      next({ name: 'Exception403', replace: true })
      return
    }

    // 4. 放行
    next()
  })
}

export function resetRouterState(): void {
  // 无需清除动态路由 — 所有路由都是静态注册的
}
