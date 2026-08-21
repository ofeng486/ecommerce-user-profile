import request from '@/utils/http'

/**
 * 用户画像 API
 */
export interface ProfileOverview {
  totalUsers: number; profiledUsers: number; highValueUsers: number; totalPaymentAmount: number;
}

const PROFILE_BASE = '/api/v1/profiles'

/** 画像概览 */
export function fetchOverview() {
  return request.get<ProfileOverview>({ url: `${PROFILE_BASE}/overview` })
}

/** 用户分层分布 */
export function fetchSegmentDistribution() {
  return request.get<any[]>({ url: `${PROFILE_BASE}/segments/distribution` })
}

/** 标签分布 */
export function fetchTagDistribution(tagCode?: string) {
  return request.get<any[]>({
    url: `${PROFILE_BASE}/tags/distribution`, params: { tagCode }
  })
}

/** 标签交叉矩阵（活跃档 × 消费档） */
export function fetchTagCross() {
  return request.get<any[]>({ url: `${PROFILE_BASE}/tags/cross` })
}

/** 用户画像列表（分页） */
/** 画像核心指标（总数/订单/消费/流失风险） */
export function fetchProfileMetrics() {
  return request.get<{ totalUsers: number; totalOrders: number; totalAmount: number; atRiskUsers: number }>({ url: `${PROFILE_BASE}/metrics` })
}

/** TOP 省份消费排名 */
export function fetchProvinceRanking() {
  return request.get<Array<{ province: string; amount: number; userCount: number }>>({ url: `${PROFILE_BASE}/province-ranking` })
}

export function fetchProfileList(params: { keyword?: string; segmentCode?: string; province?: string; minAmount?: number; maxAmount?: number; tagCode?: string; tagValue?: string; orderBy?: string; orderDir?: string; page?: number; size?: number }) {
  return request.get<{ records: any[]; total: number }>({ url: `${PROFILE_BASE}/users`, params })
}

/** 画像列表导出 CSV（复用筛选条件） */
export function exportProfilesCsv(params: { keyword?: string; segmentCode?: string; province?: string; minAmount?: number; maxAmount?: number; tagCode?: string; tagValue?: string; orderBy?: string; orderDir?: string }) {
  return request.get<Blob>({ url: `${PROFILE_BASE}/users/export`, params, responseType: 'blob' })
}

/** 用户画像详情 */
export function fetchProfileDetail(userId: number) {
  return request.get<any>({ url: `${PROFILE_BASE}/users/${userId}` })
}
