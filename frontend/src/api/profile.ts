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
  return request.get<{ code: number; message: string; data: ProfileOverview }>({ url: `${PROFILE_BASE}/overview` })
}

/** 用户分层分布 */
export function fetchSegmentDistribution() {
  return request.get<{ code: number; message: string; data: any[] }>({ url: `${PROFILE_BASE}/segments/distribution` })
}

/** 标签分布 */
export function fetchTagDistribution(tagCode?: string) {
  return request.get<{ code: number; message: string; data: any[] }>({
    url: `${PROFILE_BASE}/tags/distribution`, params: { tagCode }
  })
}

/** 用户画像列表（分页） */
export function fetchProfileList(params: { keyword?: string; segmentCode?: string; page?: number; size?: number }) {
  return request.get<{ code: number; message: string; data: any }>({ url: `${PROFILE_BASE}/users`, params })
}

/** 用户画像详情 */
export function fetchProfileDetail(userId: number) {
  return request.get<{ code: number; message: string; data: any }>({ url: `${PROFILE_BASE}/users/${userId}` })
}
