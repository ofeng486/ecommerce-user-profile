import request from '@/utils/http'

const BASE = '/api/v1/admin/repeat-analysis'

export function fetchPurchaseDistribution() {
  return request.get<any[]>({ url: `${BASE}/purchase-distribution`, showErrorMessage: false })
}
export function fetchRepeatRate() {
  return request.get<any>({ url: `${BASE}/repeat-rate`, showErrorMessage: false })
}
export function fetchAvgInterval() {
  return request.get<any>({ url: `${BASE}/avg-interval`, showErrorMessage: false })
}
export function fetchRetentionCohort() {
  return request.get<any[]>({ url: `${BASE}/retention-cohort`, showErrorMessage: false })
}
export function fetchTopRepeat() {
  return request.get<any[]>({ url: `${BASE}/top-repeat`, showErrorMessage: false })
}
