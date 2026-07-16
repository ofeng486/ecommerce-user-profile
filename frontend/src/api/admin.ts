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

/** 标签定义列表 */
export function fetchAdminTags(params: { page?: number; size?: number }) {
  return request.get<{ code: number; message: string; data: any }>({ url: '/api/v1/admin/tags', params })
}

/** 分析任务列表 */
export function fetchAdminTasks(params: { page?: number; size?: number }) {
  return request.get<{ code: number; message: string; data: any }>({ url: '/api/v1/admin/analysis-tasks', params })
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

/** 保存人群包 */
export function saveAudiencePackage(data: { packageName: string; description?: string; conditions: any[]; logic: string }) {
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

// ─── 画像对比分析 ───

/** 人群包列表（用于下拉选择） */
export function fetchAudiencePackages(params?: { page?: number; size?: number }) {
  return request.get<{ code: number; message: string; data: any }>({ url: '/api/v1/admin/audience/packages', params })
}

/** 画像对比分析 */
export function compareAudienceProfiles(groupAId: number, groupBId: number) {
  return request.post<{ code: number; message: string; data: any }>({
    url: '/api/v1/admin/audience/compare', data: { groupAId, groupBId }
  })
}
