<template>
  <div class="page-body">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">数据导入</h1>
          <span class="title-tag">DATA IMPORT</span>
        </div>
        <p class="page-desc">下载数据模板填写后上传。主模板：用户数据必填，交易/互动/商品数据按需；填得越多画像维度越全。商品、订单等关联数据会自动生成。</p>
      </div>
    </div>

    <!-- ═══ 模板下载区 — 主模板 4 张（合并模板）+ 高级模板折叠 ═══ -->
    <div class="section-outer"><div class="section-inner">
      <h2 class="section-heading">下载数据模板</h2>
      <p class="section-hint">只需下载主模板填写：<b>用户数据</b>必填，其余按需。填得越多，画像维度越全。上传时可选多个文件一并导入。</p>
      <div class="template-grid">
        <div v-for="t in mainTemplateCards" :key="t.table" class="template-card" :class="'tc--' + t.table" :style="{ '--tc': t.color }" @click="downloadTemplate(t.table)">
          <div class="tc-top">
            <span class="tc-table">{{ t.table }}</span>
            <span class="tc-name">{{ t.name }}</span>
          </div>
          <p class="tc-desc">{{ t.desc }}</p>
          <div class="tc-cols" :title="t.columns.map(toCn).join(', ')">{{ t.columns.slice(0, 5).map(toCn).join(', ') }}{{ t.columns.length > 5 ? ' …' : '' }}</div>
          <div class="tc-foot">
            <span class="tc-need" :class="'tc-need--' + t.needLabel">{{ t.needLabel }}</span>
            <span class="tc-dl">下载 CSV</span>
            <span class="tc-dl tc-dl--excel" @click.stop="downloadTemplateXlsx(t.table)">Excel</span>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>
          </div>
        </div>
      </div>
      <details class="advanced-templates">
        <summary class="advanced-summary">高级模板（按单表拆分 / 数据总表，老流程）</summary>
        <div class="template-grid template-grid--sm">
          <div v-for="t in advancedTemplateCards" :key="t.table" class="template-card" :style="{ '--tc': t.color }" @click="downloadTemplate(t.table)">
            <div class="tc-top">
              <span class="tc-table">{{ t.table }}</span>
              <span class="tc-name">{{ t.name }}</span>
            </div>
            <p class="tc-desc">{{ t.desc }}</p>
            <div class="tc-cols" :title="t.columns.map(toCn).join(', ')">{{ t.columns.slice(0, 5).map(toCn).join(', ') }}{{ t.columns.length > 5 ? ' …' : '' }}</div>
            <div class="tc-foot">
              <span class="tc-dl">下载 CSV</span>
              <span class="tc-dl tc-dl--excel" @click.stop="downloadTemplateXlsx(t.table)">Excel</span>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>
            </div>
          </div>
        </div>
        <button class="btn-outline btn-sm" title="已精简模板列，仅保留必填/常用列；留空列按默认处理" @click="downloadOverview">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>
          下载数据总表模板（一个文件导入全部表）<span class="tc-tip">· 仅保留必填/常用列，留空列按默认处理</span>
        </button>
        <button class="btn-outline btn-sm" @click="downloadOverviewXlsx">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>
          下载 Excel 版总表
        </button>
      </details>
    </div></div>

    <!-- ═══ 天池数据集一键导入 ═══ -->
    <div class="section-outer"><div class="section-inner">
      <h2 class="section-heading">天池数据集一键导入</h2>
      <p class="section-hint">适配天池常用数据集，自动转换并导入，无需手动填模板。支持两种格式（自动识别）：<br/>① <b>淘宝用户行为数据集</b>（user_id,item_id,item_category,behavior_type,timestamp）→ 生成用户/商品/互动数据<br/>② <b>电商订单/发票数据集</b>（invoice_no,customer_id,gender,age,category,quantity,price,...）→ 生成用户/商品/交易数据（订单自动汇总金额）。支持带/不带表头、UTF-8/GBK 编码。</p>
      <div class="upload-bar">
        <ElInput v-model="tianchiName" placeholder="任务名称（必填）" class="input-task" size="large" clearable />
        <ElInput v-model.number="tianchiLimit" placeholder="抽样行数（默认 20000）" class="input-task input-task--sm" size="large" clearable />
        <ElUpload :auto-upload="false" :on-change="onTianchiFile" accept=".csv" :show-file-list="false">
          <button class="btn-outline">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>
            选择天池 CSV
          </button>
        </ElUpload>
        <span v-if="tianchiRaw" class="tianchi-file">{{ tianchiRaw.name }}（{{ fmtSize(tianchiRaw.size) }}）</span>
        <button class="btn-primary-custom" :disabled="!tianchiRaw || !tianchiName || tianchiUploading" @click="doTianchi">
          <span>{{ tianchiUploading ? '转换导入中…' : '转换并导入' }}</span>
          <span class="btn-icon-wrap"><span class="btn-icon-arr">→</span></span>
        </button>
      </div>
      <div v-if="tianchiResult" class="tianchi-result" :class="tianchiResult.type === 'error' ? 'tianchi-result--error' : ''">{{ tianchiResult.msg }}</div>
    </div></div>

    <!-- ═══ 上传区 ═══ -->
    <div class="section-outer"><div class="section-inner">
      <h2 class="section-heading">上传数据文件</h2>
      <div class="upload-bar">
        <ElInput v-model="taskName" placeholder="任务名称（必填）" class="input-task" size="large" clearable />
        <ElUpload :auto-upload="false" :on-change="handleFiles" multiple accept=".csv,.xlsx,.xls" :show-file-list="false">
          <button class="btn-outline">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><path d="M14 2v6h6M12 18v-6M9 15l3 3 3-3"/></svg>
            选择 CSV 文件
          </button>
        </ElUpload>
        <button class="btn-primary-custom" :disabled="!files.length || !taskName || uploading" @click="doUpload">
          <span>开始导入</span>
          <span class="btn-icon-wrap"><span class="btn-icon-arr">→</span></span>
        </button>
      </div>

      <!-- 文件预览（已选择、未导入） -->
      <div v-if="files.length" class="file-preview">
        <div class="fp-head">
          <span class="fp-title">已选文件 <b class="fp-count">{{ files.length }}</b> 个</span>
          <button class="fp-clear" @click="clearFiles">清空重新选择</button>
        </div>
        <div class="fp-grid">
          <div v-for="f in fileList" :key="f.name" class="file-card" :class="{ 'file-card--unknown': f.guessedTable === 'unknown' }">
            <div class="fc-top">
              <span class="fc-icon">CSV</span>
              <div class="fc-meta">
                <div class="fc-name" :title="f.name">{{ f.name }}</div>
                <div class="fc-sub">
                  <span class="fc-badge" :class="f.guessedTable === 'unknown' ? 'fc-badge--warn' : 'fc-badge--ok'">
                    {{ f.guessedTable === 'unknown' ? '⚠ 无法识别' : '✓ ' + tableLabel(f.guessedTable) }}
                  </span>
                  <span class="fc-size">{{ f.sizeText }}</span>
                </div>
              </div>
            </div>
            <div class="fc-header" :title="f.header">{{ f.header }}</div>
            <div v-if="f.sampleRows && f.sampleRows.length" class="fc-samples">
              <div v-for="(row, i) in f.sampleRows" :key="i" class="fc-sample">
                <span class="fc-lineno">{{ i + 2 }}</span>
                <span class="fc-rowtext">{{ row }}</span>
              </div>
              <span v-if="f.hasMore" class="fc-more">… 仅预览前几行</span>
            </div>
          </div>
        </div>
      </div>
    </div></div>

    <ElAlert v-if="result" :type="result.type" :title="result.msg" closable class="mb-4" />

    <!-- ═══ 导入历史 ═══ -->
    <div class="section-outer"><div class="section-inner">
      <h2 class="section-heading">导入历史</h2>
      <div class="task-filter">
        <ElInput v-model="keyword" placeholder="搜索任务名称..." clearable class="input-search" @keyup.enter="loadTasks" />
        <ElSelect v-model="status" placeholder="任务状态" clearable class="input-status" @change="loadTasks">
          <ElOption label="待处理" value="Pending" /><ElOption label="运行中" value="Running" />
          <ElOption label="成功" value="Succeeded" /><ElOption label="失败" value="Failed" />
          <ElOption label="已取消" value="Cancelled" />
        </ElSelect>
        <ElButton type="primary" @click="loadTasks">查询</ElButton>
        <span class="filter-sep"></span>
        <ElTooltip content="暂不支持：系统未保存导入参数，如需重试请重新上传文件" placement="top">
          <span><ElButton disabled><SvgIcon icon="ri:restart-line" class="mr-1" />批量重试</ElButton></span>
        </ElTooltip>
        <ElButton :disabled="!selected.length" type="danger" plain @click="batchDelete"><SvgIcon icon="ri:delete-bin-line" class="mr-1" />批量删除</ElButton>
        <span v-if="selected.length" class="selected-info">已选 {{ selected.length }} 项</span>
      </div>
      <ElTable :data="tasks" stripe v-loading="taskLoading" size="small" class="data-table" @sort-change="onSortChange" @selection-change="sel => selected = sel">
        <ElTableColumn type="selection" width="40" />
        <ElTableColumn prop="taskName" label="任务" width="140" />
        <ElTableColumn label="状态" width="90">
          <template #default="{ row }"><ElTag :type="st(row.taskStatus)">{{ taskStatusLabel(row.taskStatus) }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn label="数据版本" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span :title="row.dataVersion ? '数据版本：标记本次任务的数据批次（格式 yyyyMMddHHmmss）' : ''">{{ row.dataVersion || '-' }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="结果/备注" min-width="220">
          <template #default="{ row }">
            <span v-if="row.errorMessage" class="remark-cell" @click="showRemark(row)">
              {{ row.errorMessage.length > 60 ? row.errorMessage.slice(0, 60) + '…' : row.errorMessage }}
              <span v-if="row.errorMessage.length > 60" class="remark-more">查看详情 →</span>
            </span>
            <span v-else class="remark-empty">-</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="createdAt" label="创建时间" width="150" sortable="custom">
          <template #default="{ row }">{{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '-' }}</template>
        </ElTableColumn>
        <ElTableColumn label="耗时" width="90">
          <template #default="{ row }">{{ durationText(row.createdAt, row.finishedAt) }}</template>
        </ElTableColumn>
      </ElTable>
    </div></div>

    <!-- 任务结果详情弹窗 -->
    <ElDialog v-model="remarkVisible" :title="'任务结果详情' + (remarkItem ? ' — ' + remarkItem.taskName : '')" width="640px" class="remark-dialog">
      <div v-if="remarkItem" class="remark-body">
        <div class="remark-meta">
          <ElTag size="small" :type="st(remarkItem.taskStatus)">{{ taskStatusLabel(remarkItem.taskStatus) }}</ElTag>
          <span class="remark-time">创建：{{ remarkItem.createdAt ? new Date(remarkItem.createdAt).toLocaleString() : '-' }}</span>
          <span class="remark-time">耗时：{{ durationText(remarkItem.createdAt, remarkItem.finishedAt) }}</span>
        </div>
        <pre class="remark-content">{{ remarkItem.errorMessage }}</pre>
      </div>
      <template #footer>
        <ElButton @click="copyRemark" :disabled="!remarkItem?.errorMessage">复制内容</ElButton>
        <ElButton type="primary" @click="remarkVisible = false">关闭</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '@/utils/http'
import { ElMessage } from 'element-plus'
import { taskStatusLabel } from '@/utils/taskDict'

defineOptions({ name: 'DataImport' })
const taskName = ref('')
const files = ref<any[]>([])
const fileList = ref<any[]>([])
const uploading = ref(false)
/* ─── 天池数据集导入 ─── */
const tianchiRaw = ref<File | null>(null)
const tianchiName = ref('')
const tianchiLimit = ref<number>(20000)
const tianchiUploading = ref(false)
const tianchiResult = ref<any>(null)
function onTianchiFile(f: any) { tianchiRaw.value = f?.raw || null; tianchiResult.value = null }
async function doTianchi() {
  if (!tianchiRaw.value || !tianchiName.value) return
  tianchiUploading.value = true; tianchiResult.value = null
  try {
    const fd = new FormData()
    fd.append('file', tianchiRaw.value)
    fd.append('taskName', tianchiName.value)
    fd.append('limit', String(tianchiLimit.value || 20000))
    await request.post({ url: '/api/v1/admin/import/tianchi', data: fd, headers: { 'Content-Type': 'multipart/form-data' }, showErrorMessage: false })
    tianchiResult.value = { type: 'success', msg: '转换完成，导入任务已创建，正在后台执行（可查看下方导入历史）...' }
    tianchiRaw.value = null; tianchiName.value = ''
    pollTasks()
  } catch (e: any) { tianchiResult.value = { type: 'error', msg: '转换/导入失败: ' + (e.message || e) } }
  finally { tianchiUploading.value = false }
}
const result = ref<any>(null)
const tasks = ref<any[]>([])
const taskLoading = ref(false)
const status = ref(''); const keyword = ref('')
const orderBy = ref(''); const orderDir = ref('')
const remarkVisible = ref(false); const remarkItem = ref<any>(null)
const selected = ref<any[]>([])
/** 批量删除任务记录 */
async function batchDelete() {
  if (!selected.value.length) return
  const ok: number[] = []; const failed: number[] = []
  for (const t of selected.value) {
    try { await request.del<any>({ url: `/api/v1/admin/analysis-tasks/${t.id}`, showErrorMessage: false }); ok.push(t.id) } catch { failed.push(t.id) }
  }
  ElMessage.success(`已删除 ${ok.length} 个任务记录${failed.length ? `，失败 ${failed.length} 个` : ''}`)
  selected.value = []
  loadTasks()
}
/** 打开结果/备注详情 */
function showRemark(row: any) { remarkItem.value = row; remarkVisible.value = true }
/** 复制完整内容（排障时方便贴出） */
async function copyRemark() {
  if (!remarkItem.value?.errorMessage) return
  try { await navigator.clipboard.writeText(remarkItem.value.errorMessage); ElMessage.success('已复制完整内容') }
  catch { ElMessage.warning('复制失败，请手动选择复制') }
}
/** 耗时文本 */
function durationText(createdAt: string, finishedAt: string) {
  if (!createdAt || !finishedAt) return '—'
  const sec = Math.max(0, Math.floor((new Date(finishedAt).getTime() - new Date(createdAt).getTime()) / 1000))
  if (sec < 60) return sec + '秒'
  if (sec < 3600) return Math.floor(sec / 60) + '分' + (sec % 60 ? sec % 60 + '秒' : '')
  return Math.floor(sec / 3600) + '时' + Math.floor((sec % 3600) / 60) + '分'
}
/** 列排序（服务端） */
function onSortChange({ prop, order }: any) {
  orderBy.value = order ? (prop === 'createdAt' ? 'createdAt' : '') : ''
  orderDir.value = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : ''
  loadTasks()
}
const templates = ref<any[]>([])

/** 模板元数据：中文名 / 用途说明 / 主题色（前端维护，卡片展示用） */
const TEMPLATE_META: Record<string, { name: string; desc: string; color: string }> = {
  ecommerce_user: { name: '用户基本信息表', desc: '用户基础属性与注册信息，画像分析的主体数据', color: '#2563EB' },
  product_category: { name: '商品分类表', desc: '商品类目层级，品类偏好分析的基础', color: '#13DEB9' },
  product: { name: '商品表', desc: '商品档案与价格，消费偏好统计的对象', color: '#FFAE1F' },
  sales_order: { name: '订单主表', desc: '消费订单与支付金额，RFM 模型核心输入', color: '#FF4D4F' },
  sales_order_item: { name: '订单明细表', desc: '订单商品明细，消费结构与品类偏好来源', color: '#8b5cf6' },
  user_browse_behavior: { name: '浏览互动行为表', desc: '浏览/点击/收藏/加购行为，活跃度与偏好计算来源', color: '#2563eb' },
  user_login_behavior: { name: '登录行为表', desc: '登录会话与在线时长，活跃趋势统计来源', color: '#f59e0b' },
  transaction_data: { name: '交易数据', desc: '订单+明细一个文件：一行一条商品明细，同一订单自动汇总；商品不存在时自动建档（填分类名）', color: '#FF4D4F' },
  interaction_data: { name: '互动数据', desc: '浏览+登录一个文件：按行为类型自动分拣，活跃度分析来源', color: '#2563eb' },
  product_data: { name: '商品数据', desc: '分类+商品一个文件：分类行/商品行自动识别，支持两级分类', color: '#FFAE1F' }
}

/** 主模板（合并模板）与高级模板（单表）分组 */
const MAIN_TABLES = ['ecommerce_user', 'transaction_data', 'interaction_data', 'product_data']
const NEED_META: Record<string, { label: string }> = {
  ecommerce_user: { label: '必填' },
  transaction_data: { label: '推荐' },
  interaction_data: { label: '可选' },
  product_data: { label: '可选' }
}

/** 列名中英映射（展示用；后端模板已输出中文，此处用于卡片/总览展示） */
const COLUMN_CN: Record<string, string> = {
  id: '主键', parent_id: '父分类ID', category_name: '分类名称', category_level: '分类层级', status: '状态',
  product_code: '商品编码', category_id: '分类ID', product_name: '商品名称', brand_name: '品牌', unit_price: '单价',
  user_code: '用户编码', gender: '性别', age: '年龄', province: '省份', city: '城市',
  register_channel: '注册渠道', membership_level: '会员等级', registered_at: '注册时间',
  user_id: '用户ID', session_id: '会话ID', device_type: '设备类型', login_channel: '登录渠道',
  login_at: '登录时间', logout_at: '登出时间', duration_seconds: '登录时长(秒)',
  product_id: '商品ID', behavior_type: '行为类型', channel: '访问渠道', behavior_at: '行为时间',
  order_no: '订单号', order_status: '订单状态', total_amount: '订单金额', discount_amount: '优惠金额',
  payment_amount: '实付金额', payment_method: '支付方式', ordered_at: '下单时间',
  paid_at: '支付时间', completed_at: '完成时间', order_id: '订单ID',
  product_name_snapshot: '商品快照', quantity: '数量', item_amount: '明细金额', parent_category_name: '父分类名称'
}
const toCn = (c: string) => COLUMN_CN[c] || c

import * as XLSX from 'xlsx'

/** 下载数据总表模板（可导入：第一列填表名，只填该表对应列，其余留空；列名中文，精简黑名单列） */
function downloadOverview() {
  // 全部模板列并集（按表顺序去重，同名列只保留一列；剔除精简黑名单列）——与后端 /template/total 规则一致
  const OMIT = new Set(['id', 'brand_name', 'logout_at', 'paid_at', 'completed_at', 'duration_seconds',
    'product_name_snapshot', 'parent_category_name', 'discount_amount', 'item_amount'])
  const seen = new Set<string>()
  const cols: string[] = []
  for (const t of templateCards.value) {
    for (const c of t.columns) {
      if (!OMIT.has(c) && !seen.has(c)) { seen.add(c); cols.push(c) }
    }
  }
  const header = '表名,' + cols.map(toCn).join(',')
  const blob = new Blob(['\uFEFF' + header], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '数据导入总表.csv'
  a.click()
  URL.revokeObjectURL(a.href)
}

/** 模板卡片数据：接口列名 + 前端元数据合并 */
const templateCards = computed(() =>
  templates.value.map((t: any) => ({
    table: t.table,
    columns: t.columns || [],
    needLabel: NEED_META[t.table]?.label || '',
    ...(TEMPLATE_META[t.table] || { name: t.table, desc: '数据表模板', color: '#94a3b8' })
  }))
)
/** 主模板（必填/推荐/可选 4 张） */
const mainTemplateCards = computed(() => templateCards.value.filter(t => MAIN_TABLES.includes(t.table)))
/** 高级模板（原单表拆分） */
const advancedTemplateCards = computed(() => templateCards.value.filter(t => !MAIN_TABLES.includes(t.table)))

function st(s: string) { return s === 'Succeeded' ? 'success' : s === 'Running' ? 'warning' : s === 'Failed' ? 'danger' : 'info' }

/** Excel 文件 → CSV 文本（SheetJS 解析首个工作表，复用现有 CSV 导入链路） */
async function csvTextFromXlsx(file: File): Promise<string> {
  const buf = await file.arrayBuffer()
  const wb = XLSX.read(buf, { type: 'array' })
  const ws = wb.Sheets[wb.SheetNames[0]]
  return XLSX.utils.sheet_to_csv(ws)
}

/** Excel 模板下载：复用后端 CSV 模板内容，转成 .xlsx 下载 */
function downloadTemplateXlsx(table: string) {
  request.get<Blob>({ url: `/api/v1/admin/import/template/${table}`, responseType: 'blob', showErrorMessage: true })
    .then(async (blob) => {
      const csv = await blob.text()
      const rows = csv.split(/\r?\n/).filter(r => r.trim() !== '').map(r => r.replace(/^\uFEFF/, '').split(','))
      const wb = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet(rows), '模板')
      XLSX.writeFile(wb, `${table}_模板.xlsx`)
    })
    .catch((e: any) => { ElMessage.error('Excel 模板下载失败: ' + (e?.message || e)) })
}

/** 数据总表模板 Excel 版 */
function downloadOverviewXlsx() {
  const OMIT = new Set(['id', 'brand_name', 'logout_at', 'paid_at', 'completed_at', 'duration_seconds',
    'product_name_snapshot', 'parent_category_name', 'discount_amount', 'item_amount'])
  const seen = new Set<string>()
  const cols: string[] = []
  for (const t of templateCards.value) {
    for (const c of t.columns) {
      if (!OMIT.has(c) && !seen.has(c)) { seen.add(c); cols.push(c) }
    }
  }
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet([['表名', ...cols.map(toCn)]]), '数据总表')
  XLSX.writeFile(wb, '数据导入总表.xlsx')
}

async function handleFiles(file: any, fileListRaw: any) {
  const converted = []
  for (const f of fileListRaw.map((x: any) => x.raw)) {
    // Excel 文件先转为 CSV 文本文件，后续预览/导入完全复用 CSV 链路
    if (/\.(xlsx|xls)$/i.test(f.name)) {
      try {
        const csv = await csvTextFromXlsx(f)
        converted.push(new File([csv], f.name.replace(/\.(xlsx|xls)$/i, '.csv'), { type: 'text/csv' }))
      } catch { ElMessage.warning(`文件 ${f.name} 解析失败，请确认是有效的 Excel 文件`); continue }
    } else converted.push(f)
  }
  files.value = converted
  const previews = []
  for (const f of converted) {
    try {
      const fd = new FormData(); fd.append('file', f)
      const res = await request.post<any>({ url: '/api/v1/admin/import/preview', data: fd, headers: { 'Content-Type': 'multipart/form-data' }, showErrorMessage: false })
      previews.push({ ...(res || {}), name: res?.fileName || f.name, sizeText: fmtSize(f.size) })
    }
    catch { previews.push({ name: f.name, guessedTable: 'unknown', header: '', sampleRows: [], sizeText: fmtSize(f.size) }) }
  }
  fileList.value = previews
}
/** 清空已选文件 */
function clearFiles() { files.value = []; fileList.value = [] }
/** 文件大小格式化 */
function fmtSize(n: number) {
  if (!n) return ''
  if (n < 1024) return n + ' B'
  if (n < 1048576) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1048576).toFixed(1) + ' MB'
}
/** 识别表名 → 中文展示 */
function tableLabel(t: string) {
  if (t === '数据总表') return '数据总表（全部 7 表）'
  return TEMPLATE_META[t]?.name || t
}
async function doUpload() {
  uploading.value = true; result.value = null
  try { const fd = new FormData(); files.value.forEach(f => fd.append('files', f)); fd.append('taskName', taskName.value); await request.post({ url: '/api/v1/admin/import/upload', data: fd, headers: { 'Content-Type': 'multipart/form-data' }, showErrorMessage: false }); result.value = { type: 'success', msg: '导入任务已创建，正在后台执行...' }; files.value = []; fileList.value = []; taskName.value = ''; pollTasks() }
  catch (e: any) { result.value = { type: 'error', msg: '导入失败: ' + (e.message || e) } } finally { uploading.value = false }
}
function downloadTemplate(table: string) {
  // 用带 JWT 的 fetch 下载（window.open 不带 Authorization 头会被 401 拦截）
  request.get<Blob>({ url: `/api/v1/admin/import/template/${table}`, responseType: 'blob', showErrorMessage: true })
    .then((blob) => {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${table}_模板.csv`
      a.click()
      URL.revokeObjectURL(url)
    })
    .catch((e: any) => { ElMessage.error('模板下载失败: ' + (e?.message || e)) })
}

let pollTimer: any = null; let pollTimeout: any = null
function pollTasks() { loadTasks(); if (pollTimer) clearInterval(pollTimer); if (pollTimeout) clearTimeout(pollTimeout); pollTimer = setInterval(loadTasks, 3000); pollTimeout = setTimeout(() => { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }, 300000) }
function checkAndStopPolling() { const hasActive = tasks.value.some(t => t.taskStatus === 'Pending' || t.taskStatus === 'Running'); if (!hasActive && pollTimer) { clearInterval(pollTimer); pollTimer = null; if (pollTimeout) { clearTimeout(pollTimeout); pollTimeout = null } } }
async function loadTasks() { taskLoading.value = true; try { const res = await request.get<any>({ url: '/api/v1/admin/import/tasks', params: { page: 0, size: 20, taskType: 'DATA_IMPORT', taskStatus: status.value || undefined, keyword: keyword.value || undefined, orderBy: orderBy.value || undefined, orderDir: orderDir.value || undefined }, showErrorMessage: false }); tasks.value = res?.records || []; checkAndStopPolling() } catch {} finally { taskLoading.value = false } }
onMounted(async () => { loadTasks(); try { const res = await request.get<any>({ url: '/api/v1/admin/import/templates', showErrorMessage: false }); templates.value = res || [] } catch {} })
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer); if (pollTimeout) clearTimeout(pollTimeout) })
</script>

<style scoped>
.page-body { font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; }
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
  background: linear-gradient(180deg, #2563eb 0%, #60a5fa 100%);
}
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

.section-outer { padding: 6px; border-radius: 20px; background: linear-gradient(180deg, #ffffff, #eef2f7); border: 1px solid rgba(15,23,42,0.06); margin-bottom: 16px; box-shadow: 0 8px 28px rgba(15,23,42,0.05); transition: box-shadow .35s cubic-bezier(0.32,0.72,0,1); }
.section-inner { border-radius: 14px; background: var(--default-box-color); padding: 20px 24px 24px; box-shadow: inset 0 1px 1px rgba(255,255,255,0.6); }
.section-heading { font-size: 15px; font-weight: 700; color: #323251; margin: 0 0 6px; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; }
.section-hint { font-size: 12px; color: #949eb7; margin: 0 0 14px; }

.template-actions { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.btn-sm { display: inline-flex; align-items: center; gap: 6px; height: 32px; padding: 0 12px; font-size: 12.5px; border-radius: 8px; }
.template-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(245px, 1fr)); gap: 12px; }
.template-card {
  --tc: #94a3b8;
  display: flex; flex-direction: column; gap: 8px;
  padding: 14px 16px 12px; border-radius: 12px; cursor: pointer;
  border: 1.5px solid var(--default-border); border-top: 3px solid var(--tc);
  background: var(--default-box-color, #fff); transition: all .2s;
}
.template-card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(15,23,42,.08); border-color: var(--tc); }
.tc-top { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; }
.tc-table { font-family: 'JetBrains Mono', monospace; font-size: 13px; font-weight: 700; color: var(--tc); }
.tc-name { font-size: 11px; color: #64748b; background: #f1f5f9; padding: 2px 8px; border-radius: 10px; white-space: nowrap; }
.tc-desc { font-size: 12px; color: #64748b; line-height: 1.5; margin: 0; min-height: 36px; }
.tc-cols { font-size: 10.5px; color: #94a3b8; font-family: 'JetBrains Mono', monospace; background: #f8fafc; border-radius: 6px; padding: 5px 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tc-foot { display: flex; align-items: center; justify-content: flex-end; gap: 4px; font-size: 11.5px; color: var(--tc); font-weight: 600; margin-top: auto; }
.tc-need { font-size: 10.5px; font-weight: 700; padding: 1px 8px; border-radius: 10px; margin-right: auto; }
.tc-need--必填 { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }
.tc-need--推荐 { background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe; }
.tc-need--可选 { background: #f5f3ff; color: #7c3aed; border: 1px solid #ddd6fe; }
.tc-dl { display: inline-flex; align-items: center; gap: 4px; }
.tc-dl--excel { cursor: pointer; font-size: 11px; color: #16a34a; background: #f0fdf4; padding: 2px 8px; border-radius: 8px; transition: all .15s; }
.tc-dl--excel:hover { background: #dcfce7; }
/* 主模板卡片强调：必填卡片左侧色条加粗 */
.tc--ecommerce_user { box-shadow: inset 0 0 0 1.5px var(--tc); }
.template-grid--sm { grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); }
.advanced-templates { margin-top: 14px; border-top: 1px dashed var(--default-border); padding-top: 12px; }
.advanced-summary { font-size: 12.5px; color: #94a3b8; cursor: pointer; user-select: none; padding: 4px 0; }
.advanced-summary:hover { color: #2563EB; }
.advanced-templates .template-grid { margin: 10px 0 12px; }
.advanced-templates .btn-outline { margin-top: 2px; }

.upload-bar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-bottom: 18px; }
.input-task { width: 220px; }
.input-task--sm { width: 200px; }
.tianchi-file { font-size: 12px; color: #2563EB; }
.tianchi-result { margin-top: 10px; font-size: 12.5px; color: #059669; background: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 8px; padding: 8px 12px; }
.tianchi-result--error { color: #dc2626; background: #fef2f2; border-color: #fecaca; }
.btn-outline { display: inline-flex; align-items: center; gap: 7px; height: 40px; padding: 0 18px; border-radius: 10px; border: 1.5px solid var(--default-border); background: transparent; color: #4d5875; font-size: 13px; font-weight: 500; cursor: pointer; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; transition: all .3s cubic-bezier(0.32,0.72,0,1); }
.btn-outline:hover { border-color: #2563EB; color: #2563EB; background: rgba(37,99,235,.05); }
.btn-primary-custom { display: inline-flex; align-items: center; gap: 8px; height: 40px; padding: 0 8px 0 18px; border-radius: 8px; border: none; background: #2563EB; color: #FFF; font-size: 13px; font-weight: 600; cursor: pointer; font-family: 'Plus Jakarta Sans','Inter',sans-serif; transition: all .2s cubic-bezier(0.32,0.72,0,1); box-shadow: 0 2px 8px rgba(37,99,235,.2); }
.btn-primary-custom:hover:not(:disabled) { background: #4A7AFF; transform: translateY(-1px); }
.btn-primary-custom:disabled { opacity: .5; cursor: not-allowed; }
.btn-icon-wrap { width: 26px; height: 26px; border-radius: 6px; background: rgba(255,255,255,.15); display: flex; align-items: center; justify-content: center; }
.btn-icon-arr { font-size: 12px; font-family: 'JetBrains Mono',monospace; transition: transform .2s; }
.btn-primary-custom:hover:not(:disabled) .btn-icon-arr { transform: translateX(3px); }

.file-preview { margin-top: 2px; }
.fp-head { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.fp-title { font-size: 13px; color: #7987a1; }
.fp-count { color: #2563EB; font-size: 14px; }
.fp-clear { margin-left: auto; font-size: 12px; color: #dbdfe1; background: none; border: none; cursor: pointer; font-family: inherit; transition: color .15s; }
.fp-clear:hover { color: #ef4444; }
.fp-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(310px, 1fr)); gap: 12px; }
.file-card { border: 1px solid #f2f4f5; border-radius: 10px; background: #fff; padding: 12px 14px; transition: all .18s; }
.file-card:hover { border-color: #c7d6ff; box-shadow: 0 4px 14px rgba(15,23,42,.06); transform: translateY(-1px); }
.file-card--unknown { border-color: #fde2c8; background: #fffaf5; }
.fc-top { display: flex; align-items: center; gap: 10px; }
.fc-icon { flex-shrink: 0; width: 34px; height: 34px; border-radius: 8px; background: linear-gradient(135deg, #22c55e, #16a34a); color: #fff; font-size: 9px; font-weight: 700; letter-spacing: .5px; display: flex; align-items: center; justify-content: center; }
.file-card--unknown .fc-icon { background: linear-gradient(135deg, #f59e0b, #d97706); }
.fc-meta { min-width: 0; flex: 1; }
.fc-name { font-size: 12.5px; font-weight: 600; color: #383853; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fc-sub { display: flex; align-items: center; gap: 8px; margin-top: 3px; }
.fc-badge { font-size: 11px; padding: 1px 8px; border-radius: 10px; font-weight: 500; }
.fc-badge--ok { color: #15803d; background: #dcfce7; }
.fc-badge--warn { color: #b45309; background: #fef3c7; }
.fc-size { font-size: 11px; color: #dbdfe1; }
.fc-header { margin-top: 10px; font-size: 11px; font-family: 'JetBrains Mono', monospace; color: #dbdfe1; background: #f9fafb; border-radius: 6px; padding: 6px 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fc-samples { margin-top: 8px; max-height: 108px; overflow-y: auto; border-top: 1px dashed #f2f4f5; padding-top: 6px; }
.fc-sample { display: flex; gap: 8px; font-size: 11px; font-family: 'JetBrains Mono', monospace; color: #949eb7; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding: 1px 0; }
.fc-lineno { flex-shrink: 0; color: #e6eaeb; }
.fc-rowtext { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fc-more { font-size: 11px; color: #e6eaeb; }

.data-table { width: 100%; }

/* ─── system/users 风格工具栏视觉（组件不变，CSS 覆写） ─── */
.task-filter :deep(.el-input__wrapper),
.task-filter :deep(.el-select__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1.5px var(--default-border) inset;
  transition: box-shadow .2s;
}
.task-filter :deep(.el-input__wrapper:hover),
.task-filter :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1.5px #c7d7fe inset;
}
.task-filter :deep(.el-input__wrapper.is-focus),
.task-filter :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1.5px #2563EB inset, 0 0 0 3px rgba(37,99,235,.1);
}
.task-filter :deep(.el-button) { border-radius: 8px; }
.task-filter :deep(.el-button--primary) { box-shadow: 0 2px 8px rgba(37,99,235,.2); }
</style>

<style scoped>

.task-filter { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.task-filter .input-search { width: 220px; }
.task-filter .input-status { width: 140px; }
.filter-sep { width: 1px; height: 20px; background: #e2e8f0; margin: 0 4px; }
.selected-info { font-size: 12px; color: #2563EB; }

.remark-cell { cursor: pointer; color: #334155; transition: color .15s; }
.remark-cell:hover { color: #2563EB; }
.remark-more { color: #2563EB; font-size: 12px; margin-left: 4px; }
.remark-empty { color: #cbd5e1; }
.remark-body { height: 60vh; display: flex; flex-direction: column; }
.remark-meta { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; flex-shrink: 0; }
.remark-time { font-size: 12px; color: #94a3b8; }
.remark-content { flex: 1; min-height: 0; overflow: auto; margin: 0; padding: 12px 14px; background: #0f172a; color: #e2e8f0; border-radius: 8px; font-size: 12px; line-height: 1.7; font-family: 'JetBrains Mono', Consolas, monospace; white-space: pre-wrap; word-break: break-all; }

.task-filter { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.task-filter .input-search { width: 220px; }
.task-filter .input-status { width: 140px; }
.filter-sep { width: 1px; height: 20px; background: #e2e8f0; margin: 0 4px; }
.selected-info { font-size: 12px; color: #2563EB; }

.remark-cell { cursor: pointer; color: #334155; transition: color .15s; }
.remark-cell:hover { color: #2563EB; }
.remark-more { color: #2563EB; font-size: 12px; margin-left: 4px; }
.remark-empty { color: #cbd5e1; }
.remark-body { height: 60vh; display: flex; flex-direction: column; }
.remark-meta { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; flex-shrink: 0; }
.remark-time { font-size: 12px; color: #94a3b8; }
.remark-content { flex: 1; min-height: 0; overflow: auto; margin: 0; padding: 12px 14px; background: #0f172a; color: #e2e8f0; border-radius: 8px; font-size: 12px; line-height: 1.7; font-family: 'JetBrains Mono', Consolas, monospace; white-space: pre-wrap; word-break: break-all; }

/* ─── system/users 风格工具栏视觉（组件不变，CSS 覆写） ─── */
.task-filter :deep(.el-input__wrapper),
.task-filter :deep(.el-select__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1.5px var(--default-border) inset;
  transition: box-shadow .2s;
}
.task-filter :deep(.el-input__wrapper:hover),
.task-filter :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1.5px #c7d7fe inset;
}
.task-filter :deep(.el-input__wrapper.is-focus),
.task-filter :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1.5px #2563EB inset, 0 0 0 3px rgba(37,99,235,.1);
}
.task-filter :deep(.el-button) { border-radius: 8px; }
.task-filter :deep(.el-button--primary) { box-shadow: 0 2px 8px rgba(37,99,235,.2); }
</style>
