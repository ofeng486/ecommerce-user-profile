import request from '@/utils/http'

const BASE = '/api/v1/admin/churn-analysis'

export function fetchChurnLevels() {
  return request.get<any[]>({ url: `${BASE}/levels`, showErrorMessage: false })
}
export function fetchChurnUsers(params: { level?: string; page?: number; size?: number; orderBy?: string; orderDir?: string }) {
  return request.get<any>({ url: `${BASE}/users`, params, showErrorMessage: false })
}
export function fetchChurnVersion() {
  return request.get<any>({ url: `${BASE}/version`, showErrorMessage: false })
}
export function exportChurnCsv(level?: string) {
  return request.get<Blob>({ url: `${BASE}/export`, params: level ? { level } : {}, responseType: 'blob' })
}
