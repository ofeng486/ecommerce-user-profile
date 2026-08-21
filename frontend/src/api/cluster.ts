import request from '@/utils/http'

const BASE = '/api/v1/admin/cluster-analysis'

export function fetchClusterOverview() {
  return request.get<any[]>({ url: `${BASE}/overview`, showErrorMessage: false })
}
export function fetchClusterUsers(params: { cluster: number; page?: number; size?: number }) {
  return request.get<any>({ url: `${BASE}/users`, params, showErrorMessage: false })
}
export function fetchClusterVersion() {
  return request.get<any>({ url: `${BASE}/version`, showErrorMessage: false })
}
export function exportClusterUsersCsv(cluster: number) {
  return request.get<Blob>({ url: `${BASE}/users/export`, params: { cluster }, responseType: 'blob' })
}
export function recalcCluster(k: number, mergeSimilar: boolean = true) {
  return request.post<any>({ url: `${BASE}/recalc`, data: { k, mergeSimilar } })
}
