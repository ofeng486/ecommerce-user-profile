/**
 * 用户状态管理模块
 *
 * 管理当前登录用户的登录状态、用户信息与访问令牌。
 *
 * ## 主要功能
 *
 * - 用户登录状态管理
 * - 用户信息存储（含角色 role）
 * - 访问令牌管理
 * - 退出登录清理
 *
 * ## 持久化
 *
 * - 使用 localStorage 存储（Pinia persist 插件）
 * - 存储键：sys-v{version}-user
 *
 * @module store/modules/user
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { router } from '@/router'

/**
 * 用户状态管理
 * 管理用户登录状态、个人信息、访问令牌等
 */
export const useUserStore = defineStore(
  'userStore',
  () => {
    // 登录状态
    const isLogin = ref(false)
    // 用户信息
    const info = ref<Partial<Api.Auth.UserInfo>>({})
    // 访问令牌
    const accessToken = ref('')

    // 计算属性：获取用户信息
    const getUserInfo = computed(() => info.value)

    /**
     * 设置用户信息
     * @param newInfo 新的用户信息
     */
    const setUserInfo = (newInfo: Api.Auth.UserInfo) => {
      info.value = newInfo
    }

    /**
     * 设置登录状态
     * @param status 登录状态
     */
    const setLoginStatus = (status: boolean) => {
      isLogin.value = status
    }

    /**
     * 设置访问令牌
     * @param token 访问令牌
     */
    const setToken = (token: string) => {
      accessToken.value = token
      isLogin.value = !!token
    }

    /**
     * 退出登录
     * 清空所有用户相关状态并跳转到首页
     */
    const logOut = () => {
      // 清除会话级缓存（如运营总览的「数据解读」），退出登录后不再恢复
      try {
        const uid = (info.value as any)?.userId
        localStorage.removeItem(`ai_insight_cache_${uid ?? 'anon'}`)
      } catch { /* 忽略 */ }
      // 清空用户信息
      info.value = {}
      // 重置登录状态
      isLogin.value = false
      // 清空访问令牌
      accessToken.value = ''
      // 跳转到首页
      router.push({ path: '/', query: { login: 'true' } })
    }

    return {
      isLogin,
      info,
      accessToken,
      getUserInfo,
      setUserInfo,
      setLoginStatus,
      setToken,
      logOut
    }
  },
  {
    persist: {
      key: 'user',
      storage: localStorage
    }
  }
)
