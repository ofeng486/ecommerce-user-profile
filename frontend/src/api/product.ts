import request from '@/utils/http'

const BASE = '/api/v1/admin/product-analysis'

/** 商品总览指标 */
export function fetchProductOverview() {
  return request.get<any>({ url: `${BASE}/overview`, showErrorMessage: false })
}
/** 销售 Top10 */
export function fetchTopSales() {
  return request.get<any[]>({ url: `${BASE}/top-sales`, showErrorMessage: false })
}
/** 品类销售占比 */
export function fetchCategoryShare() {
  return request.get<any[]>({ url: `${BASE}/category-share`, showErrorMessage: false })
}
/** 价格带分布 */
export function fetchPriceBands() {
  return request.get<any[]>({ url: `${BASE}/price-bands`, showErrorMessage: false })
}
/** 头部商品贡献度 */
export function fetchConcentration() {
  return request.get<any>({ url: `${BASE}/concentration`, showErrorMessage: false })
}
