/**
 * 任务类型 / 状态中文展示字典。
 * 系统面向非开发用户，界面统一展示中文描述，数据库仍存英文枚举值。
 */

/** 任务类型中文名 */
export const TASK_TYPE_LABEL: Record<string, string> = {
  PROFILE_FULL: '画像分析',
  DATA_GENERATE: '数据生成',
  DATA_IMPORT: '数据导入',
  CLUSTER_RECALC: '聚类重算',
}

/** 任务类型中文名（未知类型回退原值） */
export function taskTypeLabel(t: string) {
  return TASK_TYPE_LABEL[t] || t
}

/** 任务类型对应的 ElTag 颜色 */
export function taskTypeTagType(t: string) {
  return t === 'PROFILE_FULL' ? 'warning' : t === 'DATA_GENERATE' ? 'success' : t === 'CLUSTER_RECALC' ? 'primary' : 'info'
}

/** 任务状态中文名 */
export const TASK_STATUS_LABEL: Record<string, string> = {
  Pending: '待处理',
  Running: '运行中',
  Succeeded: '成功',
  Failed: '失败',
  Cancelled: '已取消',
}

/** 任务状态中文名（未知状态回退原值） */
export function taskStatusLabel(s: string) {
  return TASK_STATUS_LABEL[s] || s
}

/** 任务状态对应的 ElTag 颜色 */
export function taskStatusTagType(s: string) {
  return s === 'Succeeded' ? 'success' : s === 'Running' ? 'warning' : s === 'Failed' ? 'danger' : 'info'
}
