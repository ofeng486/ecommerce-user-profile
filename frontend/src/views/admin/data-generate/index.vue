<template>
  <div class="page-body">
    <!-- ═══ PAGE HEADER ═══ -->
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">数据生成</h1>
          <span class="title-tag">DATA GENERATION</span>
        </div>
        <p class="page-desc">通过 Python 脚本批量生成合规电商模拟数据，覆盖用户、订单、浏览轨迹等业务数据。生成完成后自动导入数据库。</p>
      </div>
    </div>

    <!-- ═══ 当前参数概览 ═══ -->
    <div class="params-bar-outer"><div class="params-bar-inner">
      <div v-for="p in paramSummary" :key="p.lbl" class="param-pill">
        <span class="param-pill-val">{{ p.val }}</span>
        <span class="param-pill-lbl">{{ p.lbl }}</span>
      </div>
    </div></div>

    <!-- ═══ 预设方案 — Asymmetrical ═══ -->
    <div class="preset-section">
      <h2 class="section-heading">预设方案</h2>
      <div class="preset-grid">
        <div v-for="(p, key) in presets" :key="key"
             class="preset-outer" :class="{ 'preset-active': selectedPreset === key, 'preset-featured': p.featured }"
             @click="applyPreset(key)">
          <div class="preset-inner">
            <div class="preset-top">
              <span class="preset-name">{{ p.label }}</span>
              <span v-if="p.featured" class="preset-badge">推荐</span>
            </div>
            <p class="preset-desc">{{ p.desc }}</p>
            <div class="preset-meta">
              <span>用户 {{ p.users }}</span>
              <span>商品 {{ p.products }}</span>
              <span>行为 {{ p.behaviors }}</span>
              <span>订单 {{ p.orders }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══ 自定义参数 — Double-Bezel ═══ -->
    <div class="form-section-outer"><div class="form-section-inner">
      <h2 class="section-heading">自定义参数</h2>
      <div class="form-grid">
        <div class="field"><label class="field-lbl">用户数量</label><ElInputNumber v-model="form.users" :min="1" :max="50000" :step="100" class="field-ctrl" /></div>
        <div class="field"><label class="field-lbl">商品数量</label><ElInputNumber v-model="form.products" :min="1" :max="10000" :step="50" class="field-ctrl" /></div>
        <div class="field"><label class="field-lbl">浏览行为</label><ElInputNumber v-model="form.behaviors" :min="1" :max="500000" :step="500" class="field-ctrl" /></div>
        <div class="field"><label class="field-lbl">订单数量</label><ElInputNumber v-model="form.orders" :min="1" :max="100000" :step="100" class="field-ctrl" /></div>
        <div class="field"><label class="field-lbl">随机种子</label><ElInputNumber v-model="form.seed" :min="1" :max="99999" class="field-ctrl" /><span class="field-hint">相同种子生成相同数据</span></div>
        <div class="field"><label class="field-lbl">任务名称</label><ElInput v-model="form.taskName" placeholder="数据生成任务" class="field-ctrl" /></div>
      </div>
    </div></div>

    <!-- ═══ 操作按钮 ═══ -->
    <div class="action-bar">
      <button class="btn-primary" :disabled="!form.taskName || generating" @click="doGenerate">
        <span>开始生成</span>
        <span class="btn-icon-wrap"><span class="btn-icon-arr">→</span></span>
      </button>
      <button class="btn-ghost" @click="resetForm">重置</button>
      <button class="btn-danger" @click="doClear" :disabled="clearing" style="margin-left:auto">
        <span>清空全部数据</span>
      </button>
    </div>

    <ElAlert v-if="result" :type="result.type" :title="result.msg" closable class="mb-4" />

    <!-- ═══ 任务历史 ═══ -->
    <div class="table-section-outer"><div class="table-section-inner">
      <h2 class="section-heading">生成任务历史</h2>
      <div class="task-filter">
        <ElInput v-model="keyword" placeholder="搜索任务名称..." clearable class="input-search" @keyup.enter="loadTasks" />
        <ElSelect v-model="status" placeholder="任务状态" clearable class="input-status" @change="loadTasks">
          <ElOption label="待处理" value="Pending" /><ElOption label="运行中" value="Running" />
          <ElOption label="成功" value="Succeeded" /><ElOption label="失败" value="Failed" />
          <ElOption label="已取消" value="Cancelled" />
        </ElSelect>
        <ElButton type="primary" @click="loadTasks">查询</ElButton>
        <span class="filter-sep"></span>
        <ElButton :disabled="!selected.length" type="danger" plain @click="batchDelete"><SvgIcon icon="ri:delete-bin-line" class="mr-1" />批量删除</ElButton>
        <span v-if="selected.length" class="selected-info">已选 {{ selected.length }} 项</span>
      </div>
      <ElTable :data="tasks" stripe v-loading="taskLoading" size="small" class="data-table" @sort-change="onSortChange" @selection-change="sel => selected = sel">
        <ElTableColumn type="selection" width="40" />
        <ElTableColumn prop="taskName" label="任务" width="150" />
        <ElTableColumn prop="taskType" label="类型" width="110">
          <template #default="{ row }"><ElTag size="small" :type="taskTypeTagType(row.taskType)">{{ taskTypeLabel(row.taskType) }}</ElTag></template>
        </ElTableColumn>
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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/http'
import { taskTypeLabel, taskTypeTagType, taskStatusLabel } from '@/utils/taskDict'

defineOptions({ name: 'DataGenerate' })

const presets = ref<Record<string, any>>({})
const selectedPreset = ref('')
const generating = ref(false)
const clearing = ref(false)
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

const form = reactive({
  users: 1000, products: 200, behaviors: 10000, orders: 2000, seed: 2026,
  taskName: '数据生成任务'
})

const paramSummary = computed(() => [
  { val: form.users.toLocaleString(), lbl: '用户' },
  { val: form.products.toLocaleString(), lbl: '商品' },
  { val: form.behaviors.toLocaleString(), lbl: '行为' },
  { val: form.orders.toLocaleString(), lbl: '订单' },
  { val: form.seed.toString(), lbl: '种子' },
])

function st(s: string) {
  return s === 'Succeeded' ? 'success' : s === 'Running' ? 'warning' : s === 'Failed' ? 'danger' : 'info'
}

function applyPreset(key: string) {
  selectedPreset.value = key
  const p = presets.value[key]
  if (!p) return
  form.users = p.users; form.products = p.products; form.behaviors = p.behaviors
  form.orders = p.orders; form.seed = p.seed
  form.taskName = p.label + ' - ' + new Date().toLocaleString()
}

function resetForm() {
  Object.assign(form, { users: 1000, products: 200, behaviors: 10000, orders: 2000, seed: 2026, taskName: '数据生成任务' })
  selectedPreset.value = ''; result.value = null
}

async function doGenerate() {
  generating.value = true; result.value = null
  try {
    // 用 data 发送 JSON body（之前的 params 会被 http 工具转成 data，但 @RequestParam 接不到 JSON body，会用默认值）
    await request.post({ url: '/api/v1/admin/data-generate/generate', data: { ...form }, showErrorMessage: true })
    result.value = { type: 'success', msg: '数据生成任务已创建，正在后台执行（生成CSV → 导入数据库）...' }
    pollTasks()
  } catch (e: any) { result.value = { type: 'error', msg: '创建任务失败: ' + (e.message || e) } }
  finally { generating.value = false }
}

async function doClear() {
  try {
    await ElMessageBox.confirm('此操作将清空所有电商业务数据（用户、商品、订单、行为、画像结果），系统用户和任务历史不受影响。确认继续？', '危险操作', { confirmButtonText: '确认清空', cancelButtonText: '取消', type: 'error' })
    clearing.value = true
    const res = await request.del<any>({ url: '/api/v1/admin/data-generate/clear', showErrorMessage: true })
    const parts = Object.entries(res as Record<string,number>).map(([k,v]) => `${k}: ${v}条`).join(', ')
    result.value = { type: 'success', msg: `数据已全部清空（${parts}）` }; loadTasks()
  } catch (e: any) { if (e !== 'cancel') result.value = { type: 'error', msg: '清空失败: ' + (e.message || e) } }
  finally { clearing.value = false }
}

let pollTimer: any = null; let pollTimeout: any = null
function pollTasks() { loadTasks(); if (pollTimer) clearInterval(pollTimer); if (pollTimeout) clearTimeout(pollTimeout); pollTimer = setInterval(loadTasks, 3000); pollTimeout = setTimeout(() => { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }, 300000) }
function checkAndStopPolling() { const hasActive = tasks.value.some(t => t.taskStatus === 'Pending' || t.taskStatus === 'Running'); if (!hasActive && pollTimer) { clearInterval(pollTimer); pollTimer = null; if (pollTimeout) { clearTimeout(pollTimeout); pollTimeout = null } } }
async function loadTasks() { taskLoading.value = true; try { const res = await request.get<any>({ url: '/api/v1/admin/data-generate/tasks', params: { page: 0, size: 20, taskType: 'DATA_GENERATE', taskStatus: status.value || undefined, keyword: keyword.value || undefined, orderBy: orderBy.value || undefined, orderDir: orderDir.value || undefined }, showErrorMessage: false }); tasks.value = res?.records || []; checkAndStopPolling() } catch {} finally { taskLoading.value = false } }
onMounted(async () => { loadTasks(); try { const res = await request.get<any>({ url: '/api/v1/admin/data-generate/presets', showErrorMessage: false }); presets.value = res || {} } catch {} })
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer); if (pollTimeout) clearTimeout(pollTimeout) })
</script>

<style scoped>
.page-body { font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; }

/* ═══ PAGE HEADER ═══ */
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

/* ═══ PARAMS BAR — Double-Bezel ═══ */
.params-bar-outer { padding: 6px; border-radius: 18px; background: linear-gradient(180deg, #ffffff, #eef2f7); border: 1px solid rgba(15,23,42,0.06); margin-bottom: 20px; box-shadow: 0 6px 22px rgba(15,23,42,0.04); }
.params-bar-inner { border-radius: 12px; background: var(--default-box-color); padding: 14px 20px; display: flex; gap: 24px; flex-wrap: wrap; box-shadow: inset 0 1px 1px rgba(255,255,255,0.6); }
.param-pill { display: flex; align-items: center; gap: 6px; }
.param-pill-val { font-size: 15px; font-weight: 700; color: #323251; font-family: 'JetBrains Mono','Space Grotesk',monospace; }
.param-pill-lbl { font-size: 12px; color: #949eb7; font-weight: 500; }

/* ═══ SECTION ═══ */
.section-heading { font-size: 15px; font-weight: 700; color: #323251; margin: 0 0 14px; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; letter-spacing: -.2px; }

/* ═══ PRESETS — Asymmetrical ═══ */
.preset-section { margin-bottom: 20px; }
.preset-grid { display: grid; grid-template-columns: 1.3fr 1fr; gap: 12px; }
@media(max-width:640px){.preset-grid{grid-template-columns:1fr}}
.preset-outer { padding: 6px; border-radius: 18px; background: linear-gradient(180deg, #ffffff, #eef2f7); border: 1px solid rgba(15,23,42,0.06); cursor: pointer; transition: all .3s cubic-bezier(0.32,0.72,0,1); box-shadow: 0 4px 16px rgba(15,23,42,0.03); }
.preset-outer:hover { background: rgba(37,99,235,.06); }
.preset-active { background: rgba(37,99,235,.12) !important; }
.preset-featured { grid-row: span 2; }
.preset-inner { padding: 18px 20px; border-radius: 12px; background: var(--default-box-color); height: 100%; box-shadow: inset 0 1px 1px rgba(255,255,255,0.6); }
.preset-active .preset-inner { border-color: #2563EB; }
.preset-top { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.preset-name { font-size: 14px; font-weight: 700; color: #323251; font-family: 'Plus Jakarta Sans',sans-serif; }
.preset-badge { font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 4px; background: rgba(37,99,235,.1); color: #2563EB; }
.preset-desc { font-size: 12px; color: #949eb7; margin: 0 0 10px; line-height: 1.5; }
.preset-meta { display: flex; gap: 12px; flex-wrap: wrap; }
.preset-meta span { font-size: 11px; color: #dbdfe1; }

/* ═══ FORM — Double-Bezel ═══ */
.form-section-outer { padding: 1.5px; border-radius: 14px; background: rgba(0,0,0,.025); margin-bottom: 16px; }
.form-section-inner { border-radius: calc(14px - 1.5px); background: var(--default-box-color); padding: 20px 24px 24px; border: 1px solid var(--default-border); }
.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
@media(max-width:768px){.form-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:480px){.form-grid{grid-template-columns:1fr}}
.field { display: flex; flex-direction: column; gap: 4px; }
.field-lbl { font-size: 12px; font-weight: 600; color: #4d5875; }
.field-ctrl { width: 100%; }
.field-hint { font-size: 11px; color: #dbdfe1; margin-top: 2px; }

/* ═══ ACTIONS ═══ */
.action-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.btn-primary { display: inline-flex; align-items: center; gap: 8px; padding: 10px 10px 10px 22px; border-radius: 8px; border: none; background: #2563EB; color: #FFF; font-size: 13px; font-weight: 600; cursor: pointer; font-family: 'Plus Jakarta Sans','Inter',sans-serif; transition: all .2s cubic-bezier(0.32,0.72,0,1); box-shadow: 0 2px 8px rgba(37,99,235,.2); }
.btn-primary:hover:not(:disabled) { background: #4A7AFF; transform: translateY(-1px); box-shadow: 0 4px 14px rgba(37,99,235,.3); }
.btn-primary:disabled { opacity: .5; cursor: not-allowed; }
.btn-icon-wrap { width: 26px; height: 26px; border-radius: 6px; background: rgba(255,255,255,.15); display: flex; align-items: center; justify-content: center; }
.btn-icon-arr { font-size: 12px; font-family: 'JetBrains Mono',monospace; transition: transform .2s; }
.btn-primary:hover:not(:disabled) .btn-icon-arr { transform: translateX(3px); }
.btn-ghost { padding: 10px 22px; border-radius: 8px; border: 1.5px solid var(--default-border); background: transparent; color: #4d5875; font-size: 13px; font-weight: 500; cursor: pointer; font-family: 'Plus Jakarta Sans','Inter',sans-serif; transition: all .2s; }
.btn-ghost:hover { border-color: #2563EB; color: #2563EB; }
.btn-danger { padding: 10px 22px; border-radius: 8px; border: none; background: #FF4D4F; color: #FFF; font-size: 13px; font-weight: 500; cursor: pointer; font-family: 'Plus Jakarta Sans','Inter',sans-serif; transition: all .2s; }
.btn-danger:hover:not(:disabled) { background: #E04345; }
.btn-danger:disabled { opacity: .5; cursor: not-allowed; }

/* ═══ TABLE — Double-Bezel ═══ */
.table-section-outer { padding: 1.5px; border-radius: 14px; background: rgba(0,0,0,.025); }
.table-section-inner { border-radius: calc(14px - 1.5px); background: var(--default-box-color); padding: 20px 24px 24px; border: 1px solid var(--default-border); }
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
