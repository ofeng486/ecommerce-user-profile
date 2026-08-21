<template>
  <div class="page-body" :class="isAdminSide ? 'theme-admin' : 'theme-user'" v-loading="loading">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">{{ isAdminSide ? '聚类重算' : '用户聚类' }}</h1>
          <span class="title-tag">{{ isAdminSide ? 'CLUSTER RECALC' : 'USER CLUSTERING' }}</span>
        </div>
        <p v-if="!isAdminSide" class="page-desc">基于消费金额、订单数、浏览与登录活跃四维特征，由 Spark K-Means 自动划分用户群体，识别高价值、潜力与沉睡用户。</p>
        <p v-else class="page-desc">调整簇数 K 并重算用户聚类（Spark K-Means 作业）。重算完成后请在用户端「用户聚类」页查看分析结果。</p>
      </div>
      <div class="ph-meta">
        <span v-if="clusters.length" class="header-meta header-meta--k" :title="kExplain">
          <span class="k-badge">K</span>
          <template v-if="requestedK != null && requestedK !== clusters.length">
            请求 {{ requestedK }} → 实际 {{ clusters.length }} 簇
          </template>
          <template v-else>
            当前数据 {{ clusters.length }} 簇
          </template>
        </span>
        <span class="header-meta">
          <span class="meta-dot"></span>
          数据版本 {{ version.dataVersion || '-' }} · {{ version.calculatedAt ? String(version.calculatedAt).slice(0, 16).replace('T', ' ') : '' }}
        </span>
      </div>
    </div>

    <!-- ═══ 管理端：重算工作台（只做数据生产，不重复展示分析结果） ═══ -->
    <div v-if="isAdminSide" class="recalc-workbench">
      <div class="wb-card wb-card--main">
        <div class="wb-title">聚类参数</div>
        <div class="wb-controls">
          <div class="wb-field">
            <span class="wb-label">簇数 K</span>
            <ElSelect v-model="recalcK" size="default" class="recalc-select" :disabled="recalcing">
              <ElOption v-for="n in [3, 4, 5, 6, 7, 8]" :key="n" :label="String(n)" :value="n" />
            </ElSelect>
          </div>
          <button class="btn-recalc btn-recalc--lg" :disabled="recalcing" @click="doRecalc">
            <svg v-if="!recalcing" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 4v6h-6M1 20v-6h6"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
            <span v-if="recalcing" class="recalc-spinner"></span>
            <span>{{ recalcing ? '重算中…' : '开始重算' }}</span>
          </button>
        </div>
        <label class="wb-check">
          <el-checkbox v-model="recalcMerge" :disabled="recalcing">自动合并相似簇（推荐）</el-checkbox>
          <span class="wb-check-hint">勾选后合并特征相近的重复簇，实际簇数可能少于 K；不勾选严格按 K 输出</span>
        </label>
      </div>
      <div class="wb-card">
        <div class="wb-title">重算说明</div>
        <ul class="wb-list">
          <li>基于画像表四维特征（消费/订单/浏览/登录）重跑聚类</li>
          <li>勾选"自动合并"：合并特征相近重复簇，实际簇数 ≤ K</li>
          <li>取消勾选：严格按 K 输出原始 K-Means 结果（可能含重复簇）</li>
          <li>完成后可在用户端「用户聚类」查看分析结果</li>
        </ul>
      </div>
    </div>

    <!-- ═══ 用户端：完整分析页 ═══ -->
    <template v-if="!isAdminSide">
      <div v-if="!clusters.length" class="empty-tip">暂无聚类数据——请先运行一次画像分析任务（管线会自动执行 K-Means 聚类）。</div>

      <template v-else>
      <!-- 簇分布 -->
      <div class="cluster-grid">
        <div v-for="(cl, i) in clusters" :key="cl.clusterId" class="cluster-card" :class="{ 'cluster-card--active': activeCluster === cl.clusterId }" @click="selectCluster(cl.clusterId)">
          <div class="cluster-head">
            <span class="cluster-dot" :style="{ background: palette[i % palette.length] }"></span>
            <span class="cluster-name">{{ clusterName(cl) }}</span>
            <svg class="cluster-enter" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </div>
          <div class="cluster-amount">¥{{ fmtMoney(cl.avgAmount) }}</div>
          <div class="cluster-meta">{{ fmtNum(cl.userCount) }} 人 · {{ clusterRatio(cl) }}%</div>
          <div class="cluster-fingerprint">
            <span v-for="(f, fi) in fingerprint(cl)" :key="fi" class="fp-chip" :class="'fp--' + f.tone">{{ f.label }}</span>
          </div>
          <div class="cluster-desc">{{ clusterProfiles.get(cl.clusterId) }}</div>
          <div class="cluster-hint" @click.stop="viewClusterUsers(cl.clusterId)">点击查看簇内用户 →</div>
        </div>
      </div>

      <!-- 雷达 + 特征表 -->
      <div class="chart-grid">
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">簇特征雷达</h3>
            <span class="chart-subtitle">各簇特征均值（归一化对比）</span>
          </div>
          <div ref="radarChart" style="height: 340px"></div>
        </div>
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">簇特征明细</h3>
            <span class="chart-subtitle">各簇均值指标</span>
          </div>
          <ElTable :data="clusters" size="small" stripe class="data-table">
            <ElTableColumn label="簇" width="110">
              <template #default="{ row }"><span class="cluster-dot" :style="{ background: palette[clusters.indexOf(row) % palette.length] }"></span>{{ clusterName(row) }}</template>
            </ElTableColumn>
            <ElTableColumn prop="userCount" label="人数" width="90" align="right" />
            <ElTableColumn label="占比" width="70" align="right">
              <template #default="{ row }">{{ clusterRatio(row) }}%</template>
            </ElTableColumn>
            <ElTableColumn label="人均消费" align="right">
              <template #default="{ row }">
                <span :style="{ color: amtColor(row.avgAmount), fontWeight: 600 }">¥{{ fmtMoney(row.avgAmount) }}</span>
              </template>
            </ElTableColumn>
            <ElTableColumn label="人均订单" align="right">
              <template #default="{ row }">{{ row.avgOrders }}</template>
            </ElTableColumn>
            <ElTableColumn label="人均浏览" align="right">
              <template #default="{ row }">{{ row.avgBrowse }}</template>
            </ElTableColumn>
            <ElTableColumn label="人均登录" align="right">
              <template #default="{ row }">{{ row.avgLogin }}</template>
            </ElTableColumn>
          </ElTable>
        </div>
      </div>

      <!-- 簇内用户 -->
      <div class="section-outer"><div class="section-inner">
        <div class="list-toolbar">
          <span class="list-title">{{ activeClusterName }}用户列表</span>
          <span class="result-count">共 {{ total }} 位用户</span>
          <span class="toolbar-spacer"></span>
          <button class="btn-export" :disabled="!total" @click="exportCsv">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            <span>导出 CSV</span>
          </button>
        </div>
        <ElTable :data="list" stripe size="small" class="data-table" height="460" :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 600 }" @row-click="rowClick" @sort-change="onSortChange">
          <ElTableColumn type="index" label="#" width="50" />
          <ElTableColumn prop="userCode" label="用户编码" min-width="160" />
          <ElTableColumn prop="gender" label="性别" min-width="64">
            <template #default="{ row }">{{ genderLabel(row.gender) }}</template>
          </ElTableColumn>
          <ElTableColumn prop="age" label="年龄" min-width="64" align="right" />
          <ElTableColumn prop="segmentName" label="用户分层" min-width="120">
            <template #default="{ row }">{{ row.segmentName || '-' }}</template>
          </ElTableColumn>
          <ElTableColumn prop="totalOrderCount" label="订单数" min-width="96" align="right" sortable="custom" />
          <ElTableColumn prop="totalPaymentAmount" label="累计消费" min-width="140" align="right" sortable="custom">
            <template #default="{ row }">¥{{ fmtNum(row.totalPaymentAmount) }}</template>
          </ElTableColumn>
        </ElTable>
        <div class="pager-row">
          <ElPagination layout="prev, pager, next" :total="total" :page-size="pageSize" :current-page="page + 1" @current-change="p => { page = p - 1; loadUsers() }" background small />
        </div>
      </div></div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { fetchClusterOverview, fetchClusterUsers, fetchClusterVersion, exportClusterUsersCsv, recalcCluster } from '@/api/cluster'
import { fetchAdminTasks } from '@/api/admin'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'ClusterAnalysis' })

const router = useRouter()
const route = useRoute()
/** 管理端可见重算入口（/user 前缀为运营分析员端，只读） */
const isAdminSide = computed(() => !route.path.startsWith('/user'))
/** 主题：管理端蓝色 / 用户端青色（共享页面按路由自动切换） */
const MAIN = computed(() => isAdminSide.value ? '#2563eb' : '#0d9488')
const MAIN_LIGHT = computed(() => isAdminSide.value ? '#60a5fa' : '#5eead4')
const palette = computed(() => isAdminSide.value
  ? ['#2563eb', '#0ea5e9', '#10b981', '#f59e0b', '#8b5cf6']
  : ['#0d9488', '#14b8a6', '#5eead4', '#f59e0b', '#1e40af'])
const GRAY = '#94a3b8'
/** 从 data_version 解析原始 K（如 K_5_DD516D36 → 5） */
const requestedK = computed(() => {
  const v = String(version.value?.dataVersion || '')
  const m = v.match(/^K_(\d+)_/)
  return m ? Number(m[1]) : null
})
/** 自动合并说明（hover tooltip） */
const kExplain = computed(() => {
  if (requestedK.value == null || requestedK.value === clusters.value.length) return ''
  const merged = requestedK.value - clusters.value.length
  return `K-Means 自动合并了 ${merged} 对特征相近的重复簇（原始 K=${requestedK.value}）` +
    (merged > 0 ? '\n实际产出簇数 = 原始 K - 合并对数' : '')
})

const loading = ref(false)
const clusters = ref<any[]>([])
const list = ref<any[]>([])
const total = ref(0)
const page = ref(0)
const pageSize = 20
const activeCluster = ref<number | null>(null)
const version = ref<any>({})
const radarChart = ref<HTMLElement>()
const charts: echarts.ECharts[] = []

/** 重算聚类：K 选择 + 异步任务轮询 */
const recalcK = ref(5)
const recalcMerge = ref(true) // 默认自动合并相似簇（可关闭：严格按 K 输出）
const recalcing = ref(false)
let recalcTimer: any = null
async function doRecalc() {
  if (recalcing.value) return
  recalcing.value = true
  try {
    await recalcCluster(recalcK.value, recalcMerge.value)
    ElMessage.success(`已提交聚类重算任务（K=${recalcK.value}${recalcMerge.value ? '' : '，不合并'}），完成后自动刷新`)
    // 轮询任务：检测最新 CLUSTER_RECALC 任务结束
    let poll = 0
    const tick = async () => {
      poll++
      const res = await fetchAdminTasks({ page: 0, size: 5, taskType: 'CLUSTER_RECALC' }).catch(() => null)
      const tasks = res?.records || []
      const latest = tasks[0]
      if (latest && latest.taskStatus !== 'Running' && latest.taskStatus !== 'Pending') {
        clearInterval(recalcTimer)
        recalcing.value = false
        if (latest.taskStatus === 'Succeeded') {
          ElMessage.success(`聚类重算完成（K=${recalcK.value}）`)
          await loadAll()
        } else {
          ElMessage.error(`聚类重算失败：${latest.errorMessage || '未知原因'}`)
        }
        return
      }
      if (poll > 90) { // 15 分钟超时保护（10s 一次）
        clearInterval(recalcTimer)
        recalcing.value = false
        ElMessage.warning('聚类重算超时，请到任务管理查看状态')
      }
    }
    recalcTimer = setInterval(tick, 10000)
  } catch (e: any) {
    recalcing.value = false
    ElMessage.error('提交失败：' + (e?.message || '未知错误'))
  }
}

/** 全量刷新：概览 + 版本 + 重绘雷达 + 用户列表 */
async function loadAll() {
  try {
    const [ov, ver] = await Promise.all([fetchClusterOverview(), fetchClusterVersion()])
    clusters.value = ov || []; version.value = ver || {}
    if (clusters.value.length) {
      activeCluster.value = clusters.value[0].clusterId
      await nextTick()
      renderRadar()
      await loadUsers()
    } else {
      activeCluster.value = null; list.value = []; total.value = 0
    }
  } catch { /* 保留空态 */ }
}

function fmtNum(n: any) { return Number(n || 0).toLocaleString() }
function fmtMoney(n: any) {
  const v = Number(n || 0)
  return v >= 10000 ? (v / 10000).toFixed(1) + '万' : v.toLocaleString()
}
/** 人均消费 → 主色阶（相对最高簇深浅，管理端蓝/用户端青） */
function amtColor(v: any) {
  const max = Math.max(...clusters.value.map(x => Number(x.avgAmount || 0)), 1)
  const r = Number(v || 0) / max
  const deep = isAdminSide.value ? '#1e3a8a' : '#0f766e'
  const main = isAdminSide.value ? '#2563eb' : '#0d9488'
  const light = isAdminSide.value ? '#60a5fa' : '#5eead4'
  if (r >= 0.75) return deep
  if (r >= 0.5) return main
  if (r >= 0.25) return light
  return '#94a3b8'
}
function genderLabel(g: string) { return g === 'Male' ? '男' : g === 'Female' ? '女' : (g || '-') }

/** 簇名语义化：优先用簇内 RFM 分层众数（业务主导，画像重算后自动跟随）；
 *  无众数/分层分散时 fallback 到四维特征（消费/订单/浏览/登录）相对水平规则命名 */
const clusterNames = computed<Map<number, string>>(() => {
  const m = new Map<number, string>()
  const list = clusters.value
  if (!list.length) return m

  // ── 主路径：RFM 分层众数 → 业务簇名（动态跟随画像；同名簇加序号后缀去重） ──
  const SEG_TO_NAME: Record<string, string> = {
    HIGH_VALUE: '高价值簇', POTENTIAL: '潜力簇', GENERAL: '一般簇',
    AT_RISK: '流失风险簇', LOW_VALUE: '沉睡簇'
  }
  const used = new Set<number>()
  const nameCount = new Map<string, number>() // 业务名出现次数（同名加序号）
  const pending: { id: number; base: string }[] = []
  let namedBySeg = 0
  for (const cl of list) {
    const code = cl.dominantSegment
    const segName = SEG_TO_NAME[code]
    if (code && segName) {
      pending.push({ id: cl.clusterId, base: segName })
      used.add(cl.clusterId)
      namedBySeg++
    }
  }
  // 同名簇按人均消费降序排序，第一个保持原名，后续加 Ⅱ/Ⅲ/Ⅳ 序号
  const segOrder = pending.sort((a, b) => {
    const ga = Number(list.find(c => c.clusterId === a.id)?.avgAmount || 0)
    const gb = Number(list.find(c => c.clusterId === b.id)?.avgAmount || 0)
    return gb - ga
  })
  const NUM: Record<number, string> = { 2: 'Ⅱ', 3: 'Ⅲ', 4: 'Ⅳ', 5: 'Ⅴ', 6: 'Ⅵ' }
  for (const p of segOrder) {
    const n = (nameCount.get(p.base) || 0) + 1
    nameCount.set(p.base, n)
    m.set(p.id, n === 1 ? p.base : `${p.base}·${NUM[n] || n}`)
  }
  // 若所有簇都被业务分层命名（典型情况），直接返回，不再用相对特征兜底
  if (namedBySeg === list.length) return m

  // ── 兜底路径：四维特征相对水平（仅未命名簇） ──
  const maxOf = (k: string) => Math.max(...list.map(c => Number(c[k] || 0)), 1)
  const scored = list.filter(c => !used.has(c.clusterId)).map(c => ({
    id: c.clusterId,
    amt: Number(c.avgAmount || 0) / maxOf('avgAmount'),
    ord: Number(c.avgOrders || 0) / maxOf('avgOrders'),
    act: (Number(c.avgBrowse || 0) / maxOf('avgBrowse') + Number(c.avgLogin || 0) / maxOf('avgLogin')) / 2
  }))
  const name = (s: any, n: string) => { m.set(s.id, n); used.add(s.id) }

  // 1. 消费最高者：活跃度也高 → 高价值簇，否则高消费簇
  if (scored.length) {
    const topAmt = scored.reduce((p, c) => (c.amt > p.amt ? c : p))
    name(topAmt, topAmt.act >= 0.45 ? '高价值簇' : '高消费簇')
  }

  // 2. 活跃度最高（未命名且显著）→ 活跃簇
  const rest1 = scored.filter(s => !used.has(s.id))
  if (rest1.length) {
    const topAct = rest1.reduce((p, c) => (c.act > p.act ? c : p))
    if (topAct.act >= 0.5) name(topAct, '活跃簇')
  }

  // 3. 消费最低且活跃低 → 沉睡簇
  const rest2 = scored.filter(s => !used.has(s.id))
  if (rest2.length) {
    const low = rest2.reduce((p, c) => (c.amt < p.amt ? c : p))
    if (low.amt < 0.35 && low.act < 0.35) name(low, '沉睡簇')
  }

  // 4. 剩余按消费降序分配：潜力簇 / 一般簇 / 边缘簇
  const rest3 = scored.filter(s => !used.has(s.id)).sort((a, b) => b.amt - a.amt)
  const fallback = ['潜力簇', '一般簇', '边缘簇']
  rest3.forEach((s, i) => name(s, fallback[Math.min(i, fallback.length - 1)]))
  return m
})

function clusterName(cl: any) { return clusterNames.value.get(cl.clusterId) || `簇 ${cl.clusterId}` }

/** 簇代表性特征指纹：相对全量簇均值判定高/中/低，输出语义化标签（消费/活跃/频次） */
function fingerprint(cl: any) {
  const list = clusters.value
  if (!list.length) return []
  const maxOf = (k: string) => Math.max(...list.map(c => Number(c[k] || 0)), 1)
  const amt = Number(cl.avgAmount || 0) / maxOf('avgAmount')
  const act = (Number(cl.avgBrowse || 0) / maxOf('avgBrowse') + Number(cl.avgLogin || 0) / maxOf('avgLogin')) / 2
  const ord = Number(cl.avgOrders || 0) / maxOf('avgOrders')
  const lv = (v: number) => (v >= 0.6 ? '高' : v >= 0.3 ? '中' : '低')
  const chips: { label: string; tone: string }[] = []
  const t = (v: number) => (v >= 0.6 ? 'hi' : v >= 0.3 ? 'mid' : 'lo')
  chips.push({ label: `消费${lv(amt)}`, tone: t(amt) })
  chips.push({ label: `活跃${lv(act)}`, tone: t(act) })
  chips.push({ label: `频次${lv(ord)}`, tone: t(ord) })
  return chips
}

/** 簇画像语义解读：基于四维特征相对水平生成一句话运营画像 */
const clusterProfiles = computed<Map<number, string>>(() => {
  const m = new Map<number, string>()
  const list = clusters.value
  if (!list.length) return m
  const maxOf = (k: string) => Math.max(...list.map(c => Number(c[k] || 0)), 1)
  for (const cl of list) {
    const amt = Number(cl.avgAmount || 0) / maxOf('avgAmount')
    const ord = Number(cl.avgOrders || 0) / maxOf('avgOrders')
    const act = (Number(cl.avgBrowse || 0) / maxOf('avgBrowse') + Number(cl.avgLogin || 0) / maxOf('avgLogin')) / 2
    const name = clusterName(cl)
    let desc = ''
    if (name.startsWith('高价值簇')) desc = '消费与活跃双高，核心高价值客群，建议重点运营与权益倾斜'
    else if (name.startsWith('高消费簇')) desc = '消费力强但活跃偏低，客单价高、触达少，适合定向唤醒与专属活动'
    else if (name.startsWith('活跃簇')) desc = '浏览登录活跃但消费平平，转化潜力大，适合内容种草与促销引导'
    else if (name.startsWith('沉睡簇')) desc = '消费与活跃双低，规模最大的沉默客群，需低成本触达尝试激活'
    else if (name.startsWith('潜力簇')) desc = '消费中高、活跃一般，具备向上跃迁空间，可组合券包与会员权益推动'
    else if (name.startsWith('一般簇')) desc = '各项特征处于中位，可关注消费升级与频次提升机会'
    else if (name.startsWith('流失风险簇')) desc = 'RFM 分层显示存在流失倾向，建议定向召回与关怀触达'
    else desc = '特征边缘化，建议个性化运营策略'
    m.set(cl.clusterId, desc)
  }
  return m
})
function clusterRatio(cl: any) {
  const totalN = clusters.value.reduce((a, b) => a + Number(b.userCount || 0), 0)
  return totalN ? ((Number(cl.userCount) / totalN) * 100).toFixed(1) : '0.0'
}
const activeClusterName = computed(() => {
  const hit = clusters.value.find(x => x.clusterId === activeCluster.value)
  return hit ? clusterName(hit) : ''
})

function selectCluster(id: number) {
  activeCluster.value = id; page.value = 0
  loadUsers()
}
/** 查看簇内用户：先确保选中该簇，再平滑滚动到用户列表 */
function viewClusterUsers(id: number) {
  if (activeCluster.value !== id) selectCluster(id)
  document.querySelector('.section-outer')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

/** 行点击 → 跳转画像详情（/profiles/:userId） */
function rowClick(row: any) {
  if (row?.userId) router.push(`${route.path.startsWith('/user') ? '/user' : ''}/profiles/${row.userId}`)
}

/** 列排序：订单数 / 累计消费（后端 orderBy 白名单防注入） */
let sortOrderBy = ''
let sortOrderDir = ''
async function onSortChange({ prop, order }: any) {
  if (order === null) { sortOrderBy = ''; sortOrderDir = '' }
  else {
    sortOrderBy = prop === 'totalOrderCount' ? 'orderCount' : 'totalPaymentAmount'
    sortOrderDir = order === 'ascending' ? 'asc' : 'desc'
  }
  page.value = 0
  await loadUsers()
}

/** 导出当前簇用户 CSV（UTF-8 BOM，Excel 直开） */
function exportCsv() {
  if (activeCluster.value === null) return
  exportClusterUsersCsv(activeCluster.value)
    .then((blob: any) => {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `簇${activeCluster.value}_${activeClusterName.value}_用户_${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    })
    .catch(() => ElMessage.error('导出失败，请重试'))
}

async function loadUsers() {
  if (activeCluster.value === null) return
  const params: any = { cluster: activeCluster.value, page: page.value, size: pageSize }
  if (sortOrderBy) { params.orderBy = sortOrderBy; params.orderDir = sortOrderDir }
  const res = await fetchClusterUsers(params).catch(() => null)
  list.value = res?.records || []; total.value = res?.total || 0
}

function renderRadar() {
  if (!radarChart.value || !clusters.value.length) return
  // 防重复 init：同一 DOM 节点重算刷新时先销毁旧实例（否则 ECharts 告警 + 实例泄漏）
  const existing = echarts.getInstanceByDom(radarChart.value)
  if (existing) { existing.dispose() }
  const c = echarts.init(radarChart.value); charts.push(c)
  // 每特征按各簇均值归一化到 0-100
  const feats = [
    { key: 'avgAmount', label: '消费力' },
    { key: 'avgOrders', label: '订单量' },
    { key: 'avgBrowse', label: '浏览活跃' },
    { key: 'avgLogin', label: '登录活跃' }
  ]
  const maxOf = (key: string) => Math.max(...clusters.value.map(x => Number(x[key] || 0)), 1)
  const seriesData = clusters.value.map((cl, i) => ({
    name: clusterName(cl),
    value: feats.map(f => Math.round(Number(cl[f.key] || 0) / maxOf(f.key) * 100)),
    raw: cl,
    lineStyle: { color: palette.value[i % palette.value.length], width: 2 },
    itemStyle: { color: palette.value[i % palette.value.length] },
    areaStyle: { opacity: 0.08 }
  }))
  c.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => {
        const r = p.data?.raw || {}
        return `${p.name}<br/>人均消费：¥${fmtMoney(r.avgAmount)}<br/>人均订单：${r.avgOrders} 单<br/>人均浏览：${r.avgBrowse} 次/月<br/>人均登录：${r.avgLogin} 次/月`
      }
    },
    legend: { bottom: 0, itemWidth: 10, itemHeight: 8, textStyle: { fontSize: 11, color: '#64748b' } },
    radar: {
      indicator: feats.map(f => ({ name: f.label, max: 100 })),
      radius: '62%', center: ['50%', '48%'],
      axisName: { fontSize: 12, color: '#475569' },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
      splitArea: { areaStyle: { color: ['#fff', '#f8fafc'] } },
      axisLine: { lineStyle: { color: '#e2e8f0' } }
    },
    series: [{ type: 'radar', symbolSize: 4, data: seriesData }]
  })
}

onMounted(async () => {
  loading.value = true
  await loadAll()
  loading.value = false
  window.addEventListener('resize', handleResize)
})

function handleResize() { charts.forEach(c => c.resize()) }
onUnmounted(() => {
  if (recalcTimer) clearInterval(recalcTimer)
  window.removeEventListener('resize', handleResize); charts.forEach(c => c.dispose())
})
</script>

<style scoped>
/* ═══ 页面头部（企业级统一风格） ═══ */
.page-header {
  display: flex; align-items: flex-end; justify-content: space-between; gap: 20px;
  margin-bottom: 20px; padding-bottom: 18px;
  border-bottom: 1px solid #eef2f6;
}
.ph-left { min-width: 0; }
.ph-title-row { display: flex; align-items: center; gap: 10px; }
.title-accent {
  width: 4px; height: 20px; border-radius: 2px; flex-shrink: 0;
}
.theme-admin .title-accent { background: linear-gradient(180deg, #2563eb 0%, #60a5fa 100%); }
.theme-user  .title-accent { background: linear-gradient(180deg, #0d9488 0%, #5eead4 100%); }
.page-title {
  font-size: 22px; font-weight: 700; color: #0f172a; margin: 0;
  font-family: 'Plus Jakarta Sans', 'Inter', 'PingFang SC', sans-serif;
  letter-spacing: -0.3px; line-height: 1.2;
}
.title-tag {
  font-size: 10px; font-weight: 600; letter-spacing: 1.2px; color: #94a3b8;
  background: #f1f5f9; border-radius: 4px; padding: 2px 6px;
  font-family: 'JetBrains Mono', monospace; text-transform: uppercase;
}
.page-desc {
  font-size: 13px; color: #64748b; margin: 8px 0 0 14px; line-height: 1.6;
  max-width: 600px;
}
.header-meta {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; color: #475569; background: #f8fafc;
  border: 1px solid #e2e8f0; border-radius: 999px;
  padding: 5px 12px; white-space: nowrap; flex-shrink: 0;
}
.meta-dot { width: 6px; height: 6px; border-radius: 50%; background: #10b981; }
.ph-meta { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.header-meta--k { gap: 6px; }
.k-badge {
  display: inline-flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; border-radius: 5px;
  color: #fff;
  font-size: 10px; font-weight: 700; font-family: 'JetBrains Mono', monospace;
}
.theme-admin .k-badge { background: #2563eb; }
.theme-user  .k-badge { background: #0d9488; }
.recalc-select { width: 76px; }
.btn-recalc {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12.5px; font-weight: 500; color: #fff;
  border-radius: 8px;
  padding: 6px 12px; cursor: pointer; transition: all .15s;
  font-family: inherit; white-space: nowrap;
}
.theme-admin .btn-recalc { background: #2563eb; border: 1px solid #2563eb; }
.theme-user  .btn-recalc { background: #0d9488; border: 1px solid #0d9488; }
.theme-admin .btn-recalc:hover:not(:disabled) { background: #1d4ed8; }
.theme-user  .btn-recalc:hover:not(:disabled) { background: #0f766e; }
.theme-admin .btn-recalc:disabled { background: #93c5fd; border-color: #93c5fd; cursor: not-allowed; }
.theme-user  .btn-recalc:disabled { background: #99f6e4; border-color: #99f6e4; cursor: not-allowed; }
.recalc-spinner {
  width: 12px; height: 12px; border-radius: 50%;
  border: 2px solid rgba(255,255,255,.4); border-top-color: #fff;
  animation: recalc-spin .7s linear infinite;
}
@keyframes recalc-spin { to { transform: rotate(360deg); } }

/* ═══ 管理端重算工作台 ═══ */
.recalc-workbench { display: grid; grid-template-columns: 1.3fr 1fr; gap: 18px; max-width: 900px; }
@media (max-width: 900px) { .recalc-workbench { grid-template-columns: 1fr; } }
.wb-card {
  background: var(--default-box-color); border: 1px solid var(--default-border);
  border-radius: 12px; padding: 22px 24px;
}
.wb-card--main { background: linear-gradient(180deg, #fff, #f8fafc); }
.wb-title { font-size: 14px; font-weight: 600; color: #0f172a; margin-bottom: 16px; }
.wb-controls { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.wb-field { display: flex; align-items: center; gap: 8px; }
.wb-label { font-size: 13px; color: #475569; }
.wb-check { display: flex; align-items: flex-start; gap: 8px; margin-top: 14px; }
.wb-check-hint { font-size: 11px; color: #94a3b8; line-height: 1.5; margin-top: 1px; }
.wb-list { margin: 0; padding-left: 18px; display: flex; flex-direction: column; gap: 8px; }
.wb-list li { font-size: 12.5px; color: #64748b; line-height: 1.6; }
.btn-recalc--lg { padding: 8px 16px; font-size: 13px; }
.cluster-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 18px; margin-bottom: 18px; }
.cluster-card { display: flex; flex-direction: column; gap: 10px; background: var(--default-box-color); border: 1px solid var(--default-border); border-radius: 12px; padding: 18px 20px; cursor: pointer; transition: all .2s; min-height: 180px; }
.cluster-card:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(15,23,42,.07); }
.theme-admin .cluster-card--active { border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37,99,235,.12); }
.theme-user  .cluster-card--active { border-color: #0d9488; box-shadow: 0 0 0 2px rgba(13,148,136,.12); }
.cluster-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; display: inline-block; }
.cluster-head { display: flex; align-items: center; gap: 8px; }
.cluster-name { font-size: 13.5px; font-weight: 600; color: #0f172a; }
.cluster-enter { margin-left: auto; color: #94a3b8; opacity: 0; transition: opacity .15s, transform .15s; }
.cluster-card:hover .cluster-enter { opacity: 1; }
.cluster-card:hover .cluster-enter svg { transform: rotate(90deg); }
.cluster-amount { font-size: 18px; font-weight: 700; font-variant-numeric: tabular-nums; letter-spacing: -.3px; }
.theme-admin .cluster-amount { color: #2563eb; }
.theme-user  .cluster-amount { color: #0d9488; }
.cluster-meta { font-size: 11.5px; color: #94a3b8; margin-top: -4px; }
.cluster-fingerprint { display: flex; gap: 4px; flex-wrap: wrap; }
.fp-chip {
  font-size: 10px; font-weight: 600; border-radius: 4px; padding: 1px 6px;
  letter-spacing: .2px;
}
.fp--hi { color: #047857; background: #d1fae5; }
.fp--mid { color: #1d4ed8; background: #dbeafe; }
.theme-user .fp--mid { color: #0f766e; background: #ccfbf1; }
.fp--lo { color: #64748b; background: #f1f5f9; }
.cluster-desc {
  font-size: 11.5px; color: #475569; line-height: 1.55; margin-top: auto;
  padding-top: 6px; border-top: 1px dashed #f1f5f9;
}
.cluster-hint {
  font-size: 10.5px; color: #94a3b8; text-align: right;
  opacity: 0; transition: opacity .15s, color .15s; margin-top: -2px;
  cursor: pointer; user-select: none;
}
.cluster-card:hover .cluster-hint { opacity: 1; }
.cluster-card:hover .cluster-hint:hover { color: #0d9488; font-weight: 600; }
.empty-tip { background: var(--default-box-color); border: 1px dashed var(--default-border); border-radius: 12px; padding: 48px 24px; text-align: center; color: #94a3b8; font-size: 13px; }
.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.chart-card { background: var(--default-box-color); border: 1px solid rgba(15,23,42,0.06); border-radius: 14px; padding: 20px 24px; box-shadow: 0 4px 16px rgba(15,23,42,0.03); transition: box-shadow .3s cubic-bezier(0.32,0.72,0,1); }
.chart-header { display: flex; align-items: baseline; justify-content: space-between; padding-bottom: 12px; margin-bottom: 8px; border-bottom: 1px solid #f1f5f9; }
.chart-title { font-size: 15px; font-weight: 600; color: #0f172a; }
.chart-subtitle { font-size: 12px; color: #94a3b8; }
.list-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.list-title { font-size: 14px; font-weight: 600; color: #0f172a; }
.result-count { font-size: 12.5px; color: #94a3b8; }
.toolbar-spacer { flex: 1; }
.btn-export {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12.5px; font-weight: 500;
  background: #f8fafc; border: 1px solid #dbe4f0; border-radius: 8px;
  padding: 6px 12px; cursor: pointer; transition: all .15s;
  font-family: inherit;
}
.theme-admin .btn-export { color: #2563eb; }
.theme-user  .btn-export { color: #0d9488; }
.theme-admin .btn-export:hover:not(:disabled) { background: #eff6ff; border-color: #93c5fd; }
.theme-user  .btn-export:hover:not(:disabled) { background: #f0fdfa; border-color: #5eead4; }
.btn-export:disabled { color: #94a3b8; cursor: not-allowed; }
/* 用户列表：行可点击跳详情，hover 高亮反馈 */
.data-table :deep(.el-table__row) { cursor: pointer; }
.theme-admin .data-table :deep(.el-table__row:hover > td) { background: #f5f9ff !important; }
.theme-user  .data-table :deep(.el-table__row:hover > td) { background: #f0fdfa !important; }
.pager-row { display: flex; justify-content: flex-end; margin-top: 12px; }
@media (max-width: 1100px) { .chart-grid { grid-template-columns: 1fr; } }
</style>
