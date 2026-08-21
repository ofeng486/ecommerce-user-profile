import request from '@/utils/http'

/**
 * 登录
 * @param params 登录参数（username + password）
 */
export function fetchLogin(params: { username: string; password: string }) {
  return request.post<{
    accessToken: string; tokenType: string; expiresIn: number;
    userId: number; username: string; displayName: string; role: string;
  }>({ url: '/api/v1/auth/login', data: params })
}

/**
 * 注册
 */
export function fetchRegister(params: { username: string; password: string; displayName: string }) {
  return request.post<{ userId: number; username: string; displayName: string; role: string }>({
    url: '/api/v1/auth/register', data: params
  })
}

/**
 * 获取当前用户信息
 */
export function fetchGetUserInfo() {
  return request.get<{ userId: number; username: string; displayName: string; role: string }>({
    url: '/api/v1/auth/me'
  })
}
