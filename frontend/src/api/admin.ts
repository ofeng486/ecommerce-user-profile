import request from '@/utils/http'

/** 分页查询系统用户 */
export function fetchAdminUsers(params: { page?: number; size?: number }) {
  return request.get<{ code: number; message: string; data: any }>({ url: '/api/v1/admin/users', params })
}

/** 创建系统用户 */
export function createAdminUser(data: any) {
  return request.post<{ code: number; message: string; data: any }>({ url: '/api/v1/admin/users', data })
}

/** 更新用户状态 */
export function updateAdminUserStatus(userId: number, enabled: boolean) {
  return request.patch<{ code: number; message: string; data: any }>({
    url: `/api/v1/admin/users/${userId}/status`, data: { enabled }
  })
}

/** 更新用户信息（显示名、角色） */
export function updateAdminUser(userId: number, data: { displayName?: string; role?: string }) {
  return request.put<{ code: number; message: string; data: any }>({
    url: `/api/v1/admin/users/${userId}`, data
  })
}

/** 重置用户密码 */
export function resetAdminUserPassword(userId: number, newPassword: string) {
  return request.patch<{ code: number; message: string; data: any }>({
    url: `/api/v1/admin/users/${userId}/password`, data: { newPassword }
  })
}

/** 标签定义列表 */
export function fetchAdminTags(params: { page?: number; size?: number }) {
  return request.get<{ records: any[]; total: number }>({ url: '/api/v1/admin/tags', params })
}

/** 分析任务列表（taskType/taskStatus/keyword/orderBy 可选过滤与排序） */
export function fetchAdminTasks(params: { page?: number; size?: number; taskType?: string; taskStatus?: string; keyword?: string; orderBy?: string; orderDir?: string }) {
  return request.get<{ records: any[]; total: number }>({ url: '/api/v1/admin/analysis-tasks', params })
}

/** 创建分析任务 */
export function createAdminTask(data: { taskName: string; taskType: string; dataVersion: string }) {
  return request.post<{ code: number; message: string; data: any }>({ url: '/api/v1/admin/analysis-tasks', data })
}

// ─── 智能人群圈选 ───

/** 预估圈选人数 */
export function estimateAudience(data: { conditions: any[]; logic: string }) {
  return request.post<{ code: number; message: string; data: { count: number } }>({ url: '/api/v1/admin/audience/estimate', data })
}

/** 保存人群包（规则圈选 conditions 或 指定用户列表 userIds） */
export function saveAudiencePackage(data: { packageName: string; description?: string; conditions?: any[]; logic?: string; userIds?: number[] }) {
  return request.post<{ code: number; message: string; data: any }>({ url: '/api/v1/admin/audience/packages', data })
}

/** 更新人群包 */
export function updateAudiencePackage(id: number, data: { packageName?: string; description?: string }) {
  return request.put<{ code: number; message: string; data: any }>({ url: `/api/v1/admin/audience/packages/${id}`, data })
}

/** 删除人群包 */
export function deleteAudiencePackage(id: number) {
  return request.del<{ code: number; message: string; data: any }>({ url: `/api/v1/admin/audience/packages/${id}` })
}

/** 执行圈选查询（分页） */
export function searchAudience(data: { conditions: any[]; logic: string; page: number; size: number }) {
  return request.post<{ code: number; message: string; data: any }>({ url: '/api/v1/admin/audience/search', data })
}

/** 圈选结果导出 CSV（复用圈选条件） */
export function exportAudienceCsv(data: { conditions: any[]; logic: string }) {
  return request.post<Blob>({ url: '/api/v1/admin/audience/export', data, responseType: 'blob', showErrorMessage: false })
}

// ─── 画像对比分析 ───

/** 人群包列表（用于下拉选择） */
export function fetchAudiencePackages(params?: { page?: number; size?: number }) {
  return request.get<{ code: number; message: string; data: any }>({ url: '/api/v1/admin/audience/packages', params })
}

/** 人群包内用户 ID 列表（规则圈选包按规则重算） */
export function fetchAudiencePackageUsers(id: number) {
  return request.get<{ code: number; message: string; data: any }>({ url: `/api/v1/admin/audience/packages/${id}/users` })
}

/** 导出人群包内用户 CSV（UTF-8 BOM 供 Excel 打开） */
export function exportAudiencePackageUsers(id: number) {
  return request.get({ url: `/api/v1/admin/audience/packages/${id}/export`, responseType: 'blob' })
}

/** 画像对比分析 */
export function compareAudienceProfiles(groupAId: number, groupBId: number) {
  return request.post<{ code: number; message: string; data: any }>({
    url: '/api/v1/admin/audience/compare', data: { groupAId, groupBId }
  })
}

/** 创建标签定义 */
export function createAdminTag(data: { tagCode?: string; tagName: string; tagCategory: string; valueType: string; calculationRule?: string; sourceTable?: string; ruleExpression?: string }) {
  return request.post<{ code: number; message: string; data: any }>({ url: '/api/v1/admin/tags', data })
}

/** 更新标签定义 */
export function updateAdminTag(tagId: number, data: { tagName: string; tagCategory: string; valueType: string; calculationRule?: string; sourceTable?: string; ruleExpression?: string }) {
  return request.put<{ code: number; message: string; data: any }>({ url: `/api/v1/admin/tags/${tagId}`, data })
}

/** 启用/停用标签定义 */
export function updateAdminTagStatus(tagId: number, enabled: boolean) {
  return request.patch<{ code: number; message: string; data: any }>({ url: `/api/v1/admin/tags/${tagId}/status`, params: { enabled } })
}

/** 预览标签规则：按分档规则统计各标签值人数（不落库） */
export function previewTagRule(data: { sourceTable: string; ruleExpression: string }) {
  return request.post<{ code: number; message: string; data: any }>({ url: '/api/v1/admin/tags/preview', data })
}

/** 重算全部启用标签 */
export function recalculateTags() {
  return request.post<{ code: number; message: string; data: any }>({ url: '/api/v1/admin/tags/recalculate' })
}

/** 删除标签定义（预设标签受保护；删除连带清理画像结果） */
export function deleteAdminTag(tagId: number) {
  return request.del<{ code: number; message: string; data: any }>({ url: `/api/v1/admin/tags/${tagId}` })
}
