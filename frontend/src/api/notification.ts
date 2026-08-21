import request from '@/utils/http'

/**
 * 获取通知列表
 */
export function fetchNotifications(params?: { page?: number; size?: number; type?: string; unread?: boolean }) {
  return request.get<{ records: any[]; total: number; current: number; size: number }>({
    url: '/api/v1/notifications',
    params: params || { page: 0, size: 20 }
  })
}

/**
 * 获取未读通知数
 */
export function fetchUnreadCount() {
  return request.get<number>({
    url: '/api/v1/notifications/unread-count'
  })
}

/**
 * 标记通知为已读
 */
export function markAsRead(id: number) {
  return request.patch({
    url: `/api/v1/notifications/${id}/read`
  })
}

/**
 * 标记全部为已读
 */
export function markAllAsRead() {
  return request.patch({
    url: '/api/v1/notifications/read-all'
  })
}

/**
 * 删除单条通知
 */
export function deleteNotification(id: number) {
  return request.del({
    url: `/api/v1/notifications/${id}`
  })
}
