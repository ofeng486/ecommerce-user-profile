<!-- 画像列表 — frontend-design: 表格数据清晰、信息密度适中、行交互精致 -->
<template>
  <div class="plist">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">画像列表</h1>
          <span class="title-tag">PROFILES</span>
        </div>
        <p class="page-desc">浏览、搜索、筛选和导出所有电商用户的画像数据</p>
      </div>
      <div class="ph-meta">
        <span class="header-meta">共 <strong>{{ total }}</strong> 位用户</span>
      </div>
    </div>

    <!-- 下钻来源提示条（从概览/工作台跳转时显示当前筛选语境） -->
    <div v-if="activeFilterCount > 0" class="filter-bar">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 3H2l8 9.46V19l4 2v-8.54L22 3z"/></svg>
      <span class="filter-label">筛选：</span>
      <span v-if="segmentCode" class="filter-chip" @click="segmentCode=''; search()">分层：{{ segLabel(segmentCode) }} ✕</span>
      <span v-if="province" class="filter-chip" @click="province=''; search()">省份：{{ province }} ✕</span>
      <span v-if="tagCode" class="filter-chip" @click="tagCode=''; tagValue=''; tagName=''; search()">标签：{{ tagLabel() }} ✕</span>
      <span v-if="keyword" class="filter-chip" @click="keyword=''; search()">关键词：{{ keyword }} ✕</span>
      <span v-if="minAmount" class="filter-chip" @click="minAmount=''; search()">消费 ≥ ¥{{ minAmount }} ✕</span>
      <span v-if="maxAmount" class="filter-chip" @click="maxAmount=''; search()">消费 ≤ ¥{{ maxAmount }} ✕</span>
      <button class="filter-clear" @click="clearFilters">清除全部</button>
    </div>

    <!-- 搜索栏 -->
    <div class="plist-toolbar">
      <div class="search-group">
        <input v-model="keyword" placeholder="搜索用户编码（模糊）…" class="search-input" @keyup.enter="search" />
        <el-select
          v-model="segmentCode"
          class="search-select"
          placeholder="全部分层"
          clearable
          @change="search"
        >
          <el-option value="HIGH_VALUE" label="高价值用户" />
          <el-option value="POTENTIAL" label="潜力用户" />
          <el-option value="GENERAL" label="一般用户" />
          <el-option value="AT_RISK" label="流失风险" />
          <el-option value="LOW_VALUE" label="低价值" />
        </el-select>
        <el-select
          v-model="province"
          class="search-select"
          placeholder="全部省份"
          clearable
          @change="search"
        >
          <el-option v-for="p in provinces" :key="p" :value="p" :label="p" />
        </el-select>
        <div class="amount-range">
          <input v-model="minAmount" placeholder="¥最低" type="number" class="search-input search-input--sm" @keyup.enter="search" />
          <span class="amount-sep">—</span>
          <input v-model="maxAmount" placeholder="¥最高" type="number" class="search-input search-input--sm" @keyup.enter="search" />
        </div>
        <button class="search-btn" @click="search">搜索</button>
        <button class="export-btn" @click="exportExcel">导出 Excel</button>
        <button class="export-btn" :disabled="!selectedIds.length" @click="exportSelected">导出选中 ({{ selectedIds.length }})</button>
        <button class="export-btn" :disabled="!selectedIds.length" @click="openCreatePackage">加入人群包 ({{ selectedIds.length }})</button>
      </div>
    </div>

    <!-- 批量建人群包弹窗 -->
    <el-dialog v-model="pkgDialogVisible" title="创建人群包" width="440px">
      <el-form label-width="80px">
        <el-form-item label="人群包名" required>
          <el-input v-model="pkgForm.name" placeholder="如：618 高价值复购人群" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="pkgForm.desc" type="textarea" :rows="2" placeholder="选填" maxlength="200" />
        </el-form-item>
      </el-form>
      <div class="pkg-tip">将 {{ selectedIds.length }} 位选中用户加入新人群包</div>
      <template #footer>
        <el-button @click="pkgDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pkgCreating" :disabled="!pkgForm.name.trim()" @click="createPackage">创建</el-button>
      </template>
    </el-dialog>

    <!-- 表格 -->
    <div class="table-wrap" v-loading="loading">
      <table class="data-table">
        <thead>
          <tr>
            <th class="col-check"><input type="checkbox" :checked="allSelected" @change="toggleAll" /></th>
            <th>用户编码</th><th>性别</th><th>年龄</th><th>省份</th>
            <th class="num sortable" @click="toggleSort('totalOrderCount')">订单数 {{ sortIcon('totalOrderCount') }}</th>
            <th class="num sortable" @click="toggleSort('totalPaymentAmount')">消费金额 {{ sortIcon('totalPaymentAmount') }}</th>
            <th>分层</th><th class="num sortable" @click="toggleSort('segmentScore')">评分 {{ sortIcon('segmentScore') }}</th>
            <th>最近活跃</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.userId" class="data-row" @click="goDetail(row)" @mouseenter="onRowEnter($event, row)" @mouseleave="hoveredRow = null">
            <td class="col-check" @click.stop><input type="checkbox" :checked="selectedIds.includes(row.userId)" @change="toggleSelect(row.userId)" /></td>
            <td class="cell-code">{{ row.userCode }}</td>
            <td><el-tag size="small" :type="genderType(row.gender)" effect="plain">{{ genderLabel(row.gender) }}</el-tag></td>
            <td>{{ row.age ?? '—' }}</td>
            <td>{{ row.province || '—' }}</td>
            <td class="num">{{ row.totalOrderCount || 0 }}</td>
            <td class="num">
              <span class="cell-amount">¥{{ (row.totalPaymentAmount || 0).toLocaleString() }}</span>
              <span class="amount-bar"><span class="amount-bar-fill" :style="{ width: amountRatio(row.totalPaymentAmount) + '%' }"></span></span>
            </td>
            <td><el-tag size="small" :type="segType(row.segmentCode)" effect="light">{{ row.segmentName || '未分层' }}</el-tag></td>
            <td class="num"><span class="score" :style="{color: scoreColor(row.segmentScore)}">{{ row.segmentScore ? Number(row.segmentScore).toFixed(1) : '—' }}</span></td>
            <td class="cell-active">{{ fmtActive(row.lastActiveAt) }}</td>
          </tr>
          <tr v-if="!loading && list.length === 0">
            <td colspan="10" class="empty-row">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 行 hover 预览浮层（teleport 到 body，不被表格容器遮挡） -->
    <Teleport to="body">
      <div v-if="hoveredRow" class="row-preview-float" :style="{ top: hoveredPos.top, left: hoveredPos.left }">
        <div class="preview-title">{{ hoveredRow.userCode }}</div>
        <dl class="preview-grid">
          <dt>性别</dt><dd>{{ genderLabel(hoveredRow.gender) }}</dd>
          <dt>年龄</dt><dd>{{ hoveredRow.age ?? '—' }}</dd>
          <dt>省份</dt><dd>{{ hoveredRow.province || '—' }} {{ hoveredRow.city || '' }}</dd>
          <dt>订单数</dt><dd>{{ hoveredRow.totalOrderCount || 0 }} 单</dd>
          <dt>消费金额</dt><dd>¥{{ (hoveredRow.totalPaymentAmount || 0).toLocaleString() }}</dd>
          <dt>分层</dt><dd>{{ hoveredRow.segmentName || '未分层' }}</dd>
          <dt>评分</dt><dd>{{ hoveredRow.segmentScore ? Number(hoveredRow.segmentScore).toFixed(1) : '—' }}</dd>
        </dl>
        <div class="preview-close-note">点击行查看完整画像 →</div>
      </div>
    </Teleport>

    <!-- 分页（Element Plus，可改每页 + 跳页） -->
    <div v-if="total > 0" class="page-bar">
      <ElPagination
        layout="total, sizes, prev, pager, next, jumper"
        :total="total" :page-size="size"
        :current-page="page"
        :page-sizes="[20, 50, 100]"
        background
        @current-change="p => { page = p; loadData() }"
        @size-change="s => { size = s; page = 1; loadData() }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { fetchProfileList, exportProfilesCsv } from '@/api/profile'
import { saveAudiencePackage } from '@/api/admin'
import request from '@/utils/http'

defineOptions({ name: 'UserProfileList' })

const router = useRouter()
const route = useRoute()
// 支持全局搜索跳转（?keyword=xxx）、KPI 钻取（?segment=xxx）、概览页下钻（?province=xxx）与标签页下钻（?tagCode=xxx&tagValue=yyy）初始化
const keyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const segmentCode = ref(typeof route.query.segment === 'string' ? route.query.segment : '')
const province = ref(typeof route.query.province === 'string' ? route.query.province : '')
// 标签下钻：tagCode 标签类型（ACTIVE_LEVEL/CONSUMPTION_LEVEL/FAVORITE_CATEGORY），tagValue 为标签原始值（兼容逗号分隔多值）
const tagCode = ref(typeof route.query.tagCode === 'string' ? route.query.tagCode : '')
const tagValue = ref(typeof route.query.tagValue === 'string' ? route.query.tagValue : '')
const tagName = ref(typeof route.query.tagName === 'string' ? route.query.tagName : '')
const minAmount = ref(''); const maxAmount = ref('')
const list = ref<any[]>([]); const loading = ref(false)
const page = ref(1); const size = ref(20); const total = ref(0)
const provinces = ref<string[]>([])
/** 排序状态：{ field, dir } 或 null */
const sort = ref<{ field: string; dir: 'asc' | 'desc' } | null>(null)

/** 活跃筛选条件数（用于显示来源提示条） */
const activeFilterCount = computed(() =>
  (keyword.value ? 1 : 0) + (segmentCode.value ? 1 : 0) + (province.value ? 1 : 0) +
  (minAmount.value ? 1 : 0) + (maxAmount.value ? 1 : 0) + (tagCode.value ? 1 : 0)
)

/** 分层下拉：从 URL 跳转进来时初始化省份选项（TOP 省份） */
onMounted(async () => {
  loadProvinces()
  loadData()
})
async function loadProvinces() {
  const res = await request.get<any[]>({ url: '/api/v1/public/provinces', showErrorMessage: false }).catch(() => [])
  provinces.value = ((res as any[]) || []).map((p: any) => p.province)
}

function segLabel(code: string) {
  const m: Record<string, string> = { HIGH_VALUE: '高价值用户', POTENTIAL: '潜力用户', GENERAL: '一般用户', AT_RISK: '流失风险', LOW_VALUE: '低价值' }
  return m[code] || code
}
function clearFilters() {
  keyword.value = ''; segmentCode.value = ''; province.value = ''
  minAmount.value = ''; maxAmount.value = ''
  tagCode.value = ''; tagValue.value = ''; tagName.value = ''
  sort.value = null
  search()
}

/** 标签筛选显示名：优先用跳转带的 tagName，否则回退 tagValue */
function tagLabel() { return tagName.value || tagValue.value }

function genderType(g: string) {
  const m: Record<string, string> = { '男': 'primary', 'Male': 'primary', '女': 'danger', 'Female': 'danger' }
  return (m[g] || 'info') as any
}
function genderLabel(g: string) {
  if (g === '男' || g === 'Male') return '男'
  if (g === '女' || g === 'Female') return '女'
  return '未知'
}
function segType(c: string) {
  const m: Record<string, string> = { HIGH_VALUE: 'success', POTENTIAL: 'primary', GENERAL: 'info', AT_RISK: 'warning', LOW_VALUE: 'danger' }
  return (m[c] || 'info') as any
}
function scoreColor(s: number) {
  if (s >= 4) return '#059669'; if (s >= 3) return '#0d9488'; if (s >= 2) return '#d97706'
  return '#dc2626'
}

/** 表头排序切换：点击同列升/降/取消循环 */
function toggleSort(field: string) {
  if (!sort.value || sort.value.field !== field) { sort.value = { field, dir: 'desc' } }
  else if (sort.value.dir === 'desc') { sort.value = { field, dir: 'asc' } }
  else { sort.value = null }
  page.value = 1
  loadData()
}
function sortIcon(field: string) {
  if (sort.value?.field !== field) return ''
  return sort.value.dir === 'desc' ? '↓' : '↑'
}

/** 消费金额占比条（相对当前页最高值） */
function amountRatio(v: any) {
  const max = Math.max(...list.value.map(x => Number(x.totalPaymentAmount || 0)), 1)
  return Math.round((Number(v || 0) / max) * 100)
}
/** 最近活跃格式化 */
function fmtActive(t: string) {
  if (!t) return '—'
  const d = new Date(t)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function search() { page.value = 1; loadData() }
function goDetail(row: any) { router.push(`/user/profiles/${row.userId}`) }

// ─── 批量选择 → 创建人群包 ───
const selectedIds = ref<number[]>([])
const allSelected = computed(() => list.value.length > 0 && list.value.every(r => selectedIds.value.includes(r.userId)))
function toggleSelect(id: number) {
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter(x => x !== id)
    : [...selectedIds.value, id]
}
function toggleAll() {
  selectedIds.value = allSelected.value ? [] : list.value.map(r => r.userId)
}
const pkgDialogVisible = ref(false)
const pkgCreating = ref(false)
const pkgForm = ref({ name: '', desc: '' })
function openCreatePackage() {
  if (!selectedIds.value.length) return
  pkgForm.value = { name: '', desc: '' }
  pkgDialogVisible.value = true
}
async function createPackage() {
  if (!pkgForm.value.name.trim() || !selectedIds.value.length) return
  pkgCreating.value = true
  try {
    await saveAudiencePackage({
      packageName: pkgForm.value.name.trim(),
      description: pkgForm.value.desc.trim() || undefined,
      userIds: selectedIds.value
    })
    ElMessage.success(`已创建人群包「${pkgForm.value.name.trim()}」（${selectedIds.value.length} 人）`)
    pkgDialogVisible.value = false
    selectedIds.value = []
  } catch { ElMessage.error('创建失败，请重试') } finally { pkgCreating.value = false }
}

/** 行 hover 预览（teleport 到 body，避免被表格上方工具栏遮挡） */
const hoveredRow = ref<any>(null)
const hoveredPos = ref({ top: '0px', left: '0px' })
function onRowEnter(e: MouseEvent, row: any) {
  hoveredRow.value = row
  // 预览卡跟随鼠标位置（clientX/clientY），与行无关——任意列 hover 都能就近显示
  const PREVIEW_W = 252, PREVIEW_H = 200, GAP = 12, VIEW_PAD = 8
  const mx = e.clientX, my = e.clientY
  const vw = window.innerWidth, vh = window.innerHeight
  // 默认显示在鼠标右下方；右侧越界翻到左下方；下方越界翻到鼠标上方
  const showRight = mx + GAP + PREVIEW_W < vw - VIEW_PAD
  const showBelow = my + GAP + PREVIEW_H < vh - VIEW_PAD
  const top = showBelow ? `${my + GAP}px` : `${Math.max(VIEW_PAD, my - PREVIEW_H - GAP)}px`
  const left = showRight ? `${mx + GAP}px` : `${Math.max(VIEW_PAD, mx - PREVIEW_W - GAP)}px`
  hoveredPos.value = { top, left }
}
/** 导出：走后端全量接口（按当前筛选条件导出全部，而非仅当前页） */
async function exportExcel() {
  try {
    const params: any = {
      keyword: keyword.value || undefined,
      segmentCode: segmentCode.value || undefined,
      province: province.value || undefined,
      minAmount: minAmount.value ? Number(minAmount.value) : undefined,
      maxAmount: maxAmount.value ? Number(maxAmount.value) : undefined,
      tagCode: tagCode.value || undefined,
      tagValue: tagValue.value || undefined,
      orderBy: sort.value?.field || undefined,
      orderDir: sort.value?.dir || undefined
    }
    const blob = await exportProfilesCsv(params)
    downloadCsv(blob, `用户画像_${new Date().toISOString().slice(0, 10)}.csv`)
    ElMessage.success('导出成功（当前筛选条件下的全部数据）')
  } catch {
    ElMessage.error('导出失败，请重试')
  }
}

/** 导出选中的用户（当前页勾选的行，前端直接生成 CSV） */
function exportSelected() {
  if (!selectedIds.value.length) { ElMessage.warning('请先勾选要导出的用户'); return }
  const rows = list.value.filter(r => selectedIds.value.includes(r.userId))
  const esc = (v: any) => {
    const s = String(v ?? '')
    return /[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s
  }
  const headers = ['用户编码', '性别', '年龄', '省份', '订单数', '消费金额', '分层', '评分']
  const csv = '\uFEFF' + [headers.join(','), ...rows.map(row => [
    row.userCode, genderLabel(row.gender), row.age ?? '', row.province || '',
    row.totalOrderCount || 0, row.totalPaymentAmount || 0, row.segmentName || '', row.segmentScore ?? ''
  ].map(esc).join(','))].join('\n')
  downloadCsv(new Blob([csv], { type: 'text/csv;charset=utf-8;' }), `选中用户_${new Date().toISOString().slice(0, 10)}.csv`)
  ElMessage.success(`已导出选中 ${rows.length} 条数据`)
}

/** 通用 CSV 下载 */
function downloadCsv(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
async function loadData() {
  loading.value = true
  try {
    const res = await fetchProfileList({
      keyword: keyword.value || undefined,
      segmentCode: segmentCode.value || undefined,
      province: province.value || undefined,
      minAmount: minAmount.value ? Number(minAmount.value) : undefined,
      maxAmount: maxAmount.value ? Number(maxAmount.value) : undefined,
      tagCode: tagCode.value || undefined,
      tagValue: tagValue.value || undefined,
      orderBy: sort.value?.field || undefined,
      orderDir: sort.value?.dir || undefined,
      page: page.value - 1, size: size.value
    })
    if (res) { list.value = res.records || []; total.value = res.total || 0 }
  } catch {} finally { loading.value = false }
}
</script>

<style scoped>
.plist {
  font-family: var(--font-body, 'Inter', system-ui);
  max-width: 1200px; margin: 0 auto;
}

.plist-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; gap: 12px; flex-wrap: wrap;
}

.search-group { display: flex; gap: 8px; flex-wrap: wrap; }
.search-input {
  width: 200px; padding: 8px 12px; border: 1px solid #e2e8f0; border-radius: 6px;
  font-size: 13px; outline: none; transition: border-color 0.2s; font-family: inherit;
}
.search-input:focus { border-color: #0d9488; box-shadow: 0 0 0 3px rgba(13,148,136,0.12); }
.search-input--sm { width: 84px; }

.search-select {
  width: 120px;
}
.search-select :deep(.el-select__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1.5px rgba(15, 23, 42, 0.08) inset;
  min-height: 36px;
  padding: 0 12px;
  transition: box-shadow 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}
.search-select :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1.5px rgba(15, 23, 42, 0.14) inset;
}
.search-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1.5px #0d9488 inset, 0 0 0 3px rgba(13, 148, 136, 0.12);
}
.search-select :deep(.el-select__placeholder) {
  color: #94a3b8;
  font-size: 13px;
}
.search-select :deep(.el-select__selected-item) {
  font-size: 13px;
  color: #1e293b;
}

.amount-range { display: flex; align-items: center; gap: 4px; }
.amount-sep { color: #94a3b8; font-size: 12px; }

.search-btn {
  padding: 8px 16px; background: #0d9488; color: #fff; border: none;
  border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer;
  transition: background 0.15s; font-family: inherit;
}
.search-btn:hover { background: #0f766e; }

.export-btn {
  padding: 8px 16px; background: #fff; color: #0d9488; border: 1px solid #99f6e4;
  border-radius: 6px; font-size: 13px; font-weight: 500; cursor: pointer;
  transition: all 0.15s; font-family: inherit;
}
.export-btn:hover { background: #f0fdfa; }
.export-btn:disabled { color: #cbd5e1; border-color: #e2e8f0; background: #f8fafc; cursor: not-allowed; }

/* ─── 下钻来源筛选条 ─── */
.filter-bar {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px;
  padding: 8px 14px; margin-bottom: 14px; font-size: 12.5px;
}
.filter-bar svg { color: #0d9488; flex-shrink: 0; }
.filter-label { color: #64748b; font-weight: 500; }
.filter-chip {
  display: inline-flex; align-items: center; gap: 4px;
  background: #f0fdfa; border: 1px solid #99f6e4; color: #0f766e;
  border-radius: 999px; padding: 2px 10px; cursor: pointer;
  font-size: 12px; transition: all 0.15s;
}
.filter-chip:hover { background: #ccfbf1; }
.filter-clear {
  margin-left: auto; background: none; border: none; color: #dc2626;
  font-size: 12px; cursor: pointer; font-family: inherit; padding: 2px 4px;
}
.filter-clear:hover { text-decoration: underline; }


/* ─── Table ─── */
.table-wrap {
  background: #fff; border: 1px solid rgba(15,23,42,0.06); border-radius: 14px; overflow: hidden;
  box-shadow: 0 6px 22px rgba(13,148,136,0.04), 0 1px 3px rgba(15,23,42,0.02);
}

.data-table { width: 100%; border-collapse: collapse; }
.data-table th {
  padding: 12px 16px; font-size: 11px; font-weight: 600; color: #94a3b8;
  text-transform: uppercase; letter-spacing: 0.5px; text-align: left;
  border-bottom: 1px solid #f1f5f9; background: #fafbfc;
}
.data-table th.num { text-align: right; }
.data-table th.sortable { cursor: pointer; user-select: none; transition: color 0.15s; white-space: nowrap; }
.data-table th.sortable:hover { color: #0d9488; }
.data-table .col-check { width: 36px; text-align: center; }
.data-table .col-check input[type="checkbox"] { width: 14px; height: 14px; cursor: pointer; accent-color: #0d9488; }
.pkg-tip {
  margin: 4px 0 0; font-size: 12px; color: #64748b;
  background: #f8fafc; border: 1px dashed #e2e8f0; border-radius: 6px; padding: 8px 12px;
}

.data-row { cursor: pointer; transition: background 0.12s; }
/* 行 hover 浮层预览（teleport 到 body，绝对定位） */
.row-preview-float {
  position: fixed; z-index: 9999;
  min-width: 240px; background: #fff; border: 1px solid #e2e8f0; border-radius: 10px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.15); padding: 12px 14px;
  font-size: 12px; color: #475569; line-height: 1.9;
  pointer-events: none;
}
.preview-title { font-size: 12.5px; font-weight: 600; color: #0f172a; margin-bottom: 4px; font-family: var(--font-mono, monospace); }
.preview-grid { display: grid; grid-template-columns: auto 1fr; gap: 2px 12px; }
.preview-grid dt { color: #94a3b8; }
.preview-grid dd { margin: 0; text-align: right; font-weight: 500; color: #334155; }
.preview-close-note { margin-top: 6px; padding-top: 6px; border-top: 1px dashed #f1f5f9; font-size: 11px; color: #94a3b8; }
.data-row td {
  padding: 12px 16px; font-size: 13px; color: #1e293b;
  border-bottom: 1px solid #f8fafc;
}
.data-row:last-child td { border-bottom: none; }

.cell-code { font-family: var(--font-mono, monospace); color: #0d9488; font-size: 12px; }
.num { text-align: right; }
.cell-amount { font-family: var(--font-mono, monospace); color: #d97706; font-weight: 500; display: inline-block; margin-right: 6px; }
.amount-bar { display: inline-block; width: 44px; height: 4px; background: #f1f5f9; border-radius: 2px; vertical-align: 2px; overflow: hidden; }
.amount-bar-fill { display: block; height: 100%; background: linear-gradient(90deg, #fbbf24, #f59e0b); border-radius: 2px; }
.score { font-weight: 600; font-family: var(--font-mono, monospace); }
.cell-active { font-size: 12px; color: #94a3b8; white-space: nowrap; }

.empty-row { text-align: center; padding: 40px 16px !important; color: #94a3b8; }

/* ─── Pagination ─── */
.page-bar {
  display: flex; justify-content: flex-end; align-items: center;
  margin-top: 16px;
}
/* Element Plus 分页微调（青色主题） */
.page-bar :deep(.el-pagination) { --el-pagination-hover-color: #0d9488; }
.page-bar :deep(.el-pagination.is-background .el-pager li.is-active) { background: #0d9488; }
</style>
