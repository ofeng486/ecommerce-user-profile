/**
 * 通知工具：优先浏览器系统级通知（Web Notification API），
 * 未授权或不支持时自动降级为页面内 toast（ElNotification）。
 */

/** 权限状态缓存（session 级，刷新后重新查询） */
let cachedPermission: NotificationPermission | '' = ''

/** 查询当前权限状态（'granted' 已授权 / 'denied' 拒绝 / 'default' 未询问） */
export function notifyPermission(): NotificationPermission {
  if ('Notification' in window) {
    return Notification.permission
  }
  return 'denied'
}

/** 请求授权（浏览器要求必须由用户手势触发，如点击按钮） */
export async function requestNotifyPermission(): Promise<NotificationPermission> {
  if (!('Notification' in window)) return 'denied'
  try {
    cachedPermission = await Notification.requestPermission()
  } catch {
    cachedPermission = 'denied'
  }
  return cachedPermission
}

/** 是否已授权系统通知 */
export function isNotifyGranted(): boolean {
  return 'Notification' in window && Notification.permission === 'granted'
}

/**
 * 弹一条通知：已授权 → 系统级弹窗；否则 → 页面右下角 toast。
 * @param title 标题（系统通知中加粗显示）
 * @param body 正文
 * @param opts.type toast 类型（仅降级时生效）；systemTag 用于系统通知去重（同 tag 只保留最新）
 */
export function notify(
  title: string,
  body: string,
  opts: { type?: 'success' | 'warning' | 'info' | 'error'; systemTag?: string } = {}
): void {
  // 系统级通知
  if (isNotifyGranted()) {
    try {
      const n = new Notification(title, {
        body,
        tag: opts.systemTag || 'default',
        icon: '/favicon.svg'
      })
      // 点击系统通知 → 聚焦页面
      n.onclick = () => {
        window.focus()
        n.close()
      }
      return
    } catch {
      /* 系统通知失败 → 降级 toast */
    }
  }
  // 降级：页面内 toast
  import('element-plus').then(({ ElNotification }) => {
    ElNotification({
      title,
      message: body,
      type: opts.type || 'info',
      position: 'bottom-right',
      duration: 6000
    })
  })
}
