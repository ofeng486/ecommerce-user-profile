<template>
  <div class="page-body">
    <div class="page-header">
      <div class="page-header-row">
        <div class="ph-left">
          <div class="ph-title-row">
            <span class="title-accent"></span>
            <h1 class="page-title">任务管理</h1>
            <span class="title-tag">TASK CENTER</span>
          </div>
          <p class="page-desc">统一管理画像分析、数据生成与数据导入任务，支持按类型/状态筛选、批量重试与删除。</p>
        </div>
        <button class="btn-primary-custom" @click="showDialog = true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 5v14M5 12h14"/></svg>
          <span>创建画像任务</span>
        </button>
      </div>
    </div>

    <!-- ═══ 任务列表 ═══ -->
    <div class="section-outer"><div class="section-inner">
      <div class="task-stat-bar">
        <span class="task-stat-item"><span class="task-stat-lbl">当前列表</span><b>{{ tasks.length }}</b> 条任务</span>
        <span class="task-stat-item"><span class="task-stat-lbl">平均耗时</span><b>{{ avgDurationText }}</b></span>
        <span class="task-stat-item"><span class="task-stat-lbl">成功</span><b class="stat-ok">{{ tasks.filter(t => t.taskStatus === 'Succeeded').length }}</b></span>
        <span class="task-stat-item"><span class="task-stat-lbl">失败</span><b class="stat-fail">{{ tasks.filter(t => t.taskStatus === 'Failed').length }}</b></span>
      </div>
      <div class="task-filter">
        <ElInput v-model="keyword" placeholder="搜索任务名称..." clearable class="input-search" @keyup.enter="loadTasks" />
        <ElSelect v-model="taskType" placeholder="任务类型" clearable class="input-type" @change="loadTasks">
          <ElOption label="画像分析" value="PROFILE_FULL" /><ElOption label="数据生成" value="DATA_GENERATE" /><ElOption label="数据导入" value="DATA_IMPORT" />
        </ElSelect>
        <ElSelect v-model="status" placeholder="任务状态" clearable class="input-status" @change="loadTasks">
          <ElOption label="待处理" value="Pending" /><ElOption label="运行中" value="Running" />
          <ElOption label="成功" value="Succeeded" /><ElOption label="失败" value="Failed" />
          <ElOption label="已取消" value="Cancelled" />
        </ElSelect>
        <ElButton type="primary" @click="loadTasks">查询</ElButton>
        <span class="filter-sep"></span>
        <ElButton :disabled="!selected.length" @click="batchRetry"><SvgIcon icon="ri:restart-line" class="mr-1" />批量重试</ElButton>
        <ElButton :disabled="!selected.length" type="danger" plain @click="batchDelete"><SvgIcon icon="ri:delete-bin-line" class="mr-1" />批量删除</ElButton>
        <span v-if="selected.length" class="selected-info">已选 {{ selected.length }} 项</span>
      </div>
      <ElTable :data="tasks" stripe v-loading="loading" size="small" class="data-table" @selection-change="sel => selected = sel" @sort-change="onSortChange">
        <ElTableColumn type="selection" width="40" />
        <ElTableColumn prop="id" label="ID" width="60" />
        <ElTableColumn prop="taskName" label="任务名称" width="160" />
        <ElTableColumn prop="taskType" label="类型" width="130">
          <template #default="{ row }">
            <ElTag size="small" :type="taskTypeTagType(row.taskType)">
              <SvgIcon :icon="taskTypeIcon(row.taskType)" class="mr-1" />{{ taskTypeLabel(row.taskType) }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }"><ElTag :type="st(row.taskStatus)">{{ taskStatusLabel(row.taskStatus) }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn prop="dataVersion" label="数据版本" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span :title="row.dataVersion ? '数据版本：标记本次任务的数据批次（格式 yyyyMMddHHmmss）' : ''">{{ row.dataVersion || '-' }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="errorMessage" label="结果/备注" min-width="200">
          <template #default="{ row }">
            <span v-if="row.errorMessage" class="remark-cell" @click="showRemark(row)">
              {{ row.errorMessage.length > 80 ? row.errorMessage.slice(0, 80) + '…' : row.errorMessage }}
              <span v-if="row.errorMessage.length > 80" class="remark-more">查看详情 →</span>
            </span>
            <span v-else class="remark-empty">-</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="createdAt" label="创建时间" width="150" sortable="custom">
          <template #default="{ row }">{{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '-' }}</template>
        </ElTableColumn>
        <ElTableColumn prop="duration" label="耗时" width="90" sortable="custom">
          <template #default="{ row }">{{ durationText(row.createdAt, row.finishedAt) }}</template>
        </ElTableColumn>
      </ElTable>
    </div></div>

    <!-- 创建任务对话框 -->
    <ElDialog v-model="showDialog" title="创建 Spark 画像分析任务" width="480px">
      <ElForm :model="form" label-width="100px">
        <ElFormItem label="任务名称"><ElInput v-model="form.taskName" placeholder="例如：Spark RFM 画像分析" /></ElFormItem>
        <ElFormItem label="任务类型"><ElSelect v-model="form.taskType" class="w-full"><ElOption label="画像分析 (PROFILE_FULL)" value="PROFILE_FULL" /></ElSelect></ElFormItem>
        <ElFormItem label="数据版本"><ElInput v-model="form.dataVersion" placeholder="例如：20260714" /></ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="showDialog = false">取消</ElButton>
        <ElButton type="primary" @click="doCreate" :loading="creating">创建并执行</ElButton>
      </template>
    </ElDialog>
    <!-- 任务结果详情弹窗 -->
    <ElDialog v-model="remarkVisible" :title="'任务结果详情' + (remarkItem ? ' — ' + remarkItem.taskName : '')" width="640px" class="remark-dialog">
      <div v-if="remarkItem" class="remark-body">
        <div class="remark-meta">
          <ElTag size="small" :type="st(remarkItem.taskStatus)">{{ taskStatusLabel(remarkItem.taskStatus) }}</ElTag>
          <ElTag size="small" type="info">{{ taskTypeLabel(remarkItem.taskType) }}</ElTag>
          <span v-if="remarkItem.dataVersion" class="remark-time">数据版本：{{ remarkItem.dataVersion }}</span>
          <span class="remark-time">创建：{{ remarkItem.createdAt ? new Date(remarkItem.createdAt).toLocaleString() : '-' }}</span>
          <span class="remark-time">耗时：{{ durationText(remarkItem.createdAt, remarkItem.finishedAt) }}</span>
        </div>
        <pre class="remark-content" :class="remarkItem.taskStatus === 'Succeeded' ? 'remark-content--ok' : ''">{{ remarkItem.errorMessage || '（无内容）' }}</pre>
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
import { useRoute } from 'vue-router'
import { fetchAdminTasks, createAdminTask } from '@/api/admin'
import { ElMessage } from 'element-plus'
import request from '@/utils/http'
import { taskTypeLabel, taskTypeTagType, taskStatusLabel } from '@/utils/taskDict'

defineOptions({ name: 'TaskList' })
const tasks = ref<any[]>([]); const loading = ref(false); const showDialog = ref(false); const creating = ref(false)
const route = useRoute()
const status = ref(''); const keyword = ref('')
const taskType = ref('') // 空 = 全部类型（概览「最近任务」跳转后能看到生成/导入任务）
const orderBy = ref(''); const orderDir = ref('')
const selected = ref<any[]>([])
const remarkVisible = ref(false); const remarkItem = ref<any>(null)
/** 打开结果/备注详情 */
function showRemark(row: any) { remarkItem.value = row; remarkVisible.value = true }
/** 复制完整内容（排障时方便贴出） */
async function copyRemark() {
  if (!remarkItem.value?.errorMessage) return
  try { await navigator.clipboard.writeText(remarkItem.value.errorMessage); ElMessage.success('已复制完整内容') }
  catch { ElMessage.warning('复制失败，请手动选择复制') }
}
/** 任务类型图标（SvgIcon remix 图标） */
function taskTypeIcon(t: string) {
  return t === 'PROFILE_FULL' ? 'ri:bar-chart-box-line' : t === 'DATA_GENERATE' ? 'ri:database-2-line' : 'ri:upload-2-line'
}
/** 平均耗时文本（当前列表已完成任务，未完成不计入） */
const avgDurationText = computed(() => {
  const durs = tasks.value
    .filter((t: any) => t.createdAt && t.finishedAt)
    .map((t: any) => Math.max(0, (new Date(t.finishedAt).getTime() - new Date(t.createdAt).getTime()) / 1000))
  if (!durs.length) return '—'
  const avg = durs.reduce((a, b) => a + b, 0) / durs.length
  if (avg < 60) return Math.round(avg) + ' 秒'
  if (avg < 3600) return (avg / 60).toFixed(1) + ' 分钟'
  return (avg / 3600).toFixed(1) + ' 小时'
})
/** 耗时文本：完成时间 - 创建时间（未完成显示 —） */
function durationText(createdAt: string, finishedAt: string) {
  if (!createdAt || !finishedAt) return '—'
  const sec = Math.max(0, Math.floor((new Date(finishedAt).getTime() - new Date(createdAt).getTime()) / 1000))
  if (sec < 60) return sec + '秒'
  if (sec < 3600) return Math.floor(sec / 60) + '分' + (sec % 60 ? sec % 60 + '秒' : '')
  return Math.floor(sec / 3600) + '时' + Math.floor((sec % 3600) / 60) + '分'
}
/** 批量重试：按原任务参数重新创建并执行 */
async function batchRetry() {
  if (!selected.value.length) return
  const ok: string[] = []; const failed: string[] = []
  for (const t of selected.value) {
    try { await createAdminTask({ taskName: t.taskName, taskType: t.taskType, dataVersion: t.dataVersion }); ok.push(t.taskName) } catch { failed.push(t.taskName) }
  }
  ElMessage.success(`成功创建 ${ok.length} 个任务${failed.length ? `，失败 ${failed.length} 个` : ''}`)
  selected.value = []
  loadTasks()
}
/** 批量删除任务记录 */
async function batchDelete() {
  if (!selected.value.length) return
  const ids = selected.value.map((t: any) => t.id)
  const ok: number[] = []; const failed: number[] = []
  for (const id of ids) {
    try { await request.del<any>({ url: `/api/v1/admin/analysis-tasks/${id}`, showErrorMessage: false }); ok.push(id) } catch { failed.push(id) }
  }
  ElMessage.success(`已删除 ${ok.length} 个任务记录${failed.length ? `，失败 ${failed.length} 个` : ''}`)
  selected.value = []
  loadTasks()
}
const form = reactive({ taskName: 'Spark RFM 画像分析', taskType: 'PROFILE_FULL', dataVersion: String(new Date().getFullYear()) + String(Date.now()).slice(-4) })
function st(s: string) { return s === 'Succeeded' ? 'success' : s === 'Running' ? 'warning' : s === 'Failed' ? 'danger' : 'info' }
async function loadTasks() { loading.value = true; try { const res = await fetchAdminTasks({ page: 0, size: 50, taskType: taskType.value || undefined, taskStatus: status.value || undefined, keyword: keyword.value || undefined, orderBy: orderBy.value || undefined, orderDir: orderDir.value || undefined }); tasks.value = res?.records || []; checkAndStop(); openFromQuery() } catch {} finally { loading.value = false } }

/** 从通知跳转而来：?taskId=xx 自动打开对应任务详情（若在当页列表中） */
function openFromQuery() {
  const id = Number(route.query.taskId)
  if (!id) return
  const row = tasks.value.find(t => t.id === id)
  if (row) { showRemark(row); return }
  // 不在当前列表（可能被筛选/分页排除），拉取全量再找一次
  fetchAdminTasks({ page: 0, size: 500 }).then(res => {
    const hit = (res?.records || []).find(t => t.id === id)
    if (hit) showRemark(hit)
    else ElMessage.warning('未找到该任务（可能已被删除）')
  }).catch(() => {})
}
/** 列排序（服务端）：createdAt 创建时间 / duration 耗时 */
function onSortChange({ prop, order }: any) {
  orderBy.value = order ? (prop === 'duration' ? 'duration' : 'createdAt') : ''
  orderDir.value = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : ''
  loadTasks()
}

let pollTimer: any = null; let pollTimeout: any = null
function startPolling() {
  stopPolling()
  pollTimer = setInterval(loadTasks, 3000)
  pollTimeout = setTimeout(() => stopPolling(), 300000)
}
function stopPolling() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null }; if (pollTimeout) { clearTimeout(pollTimeout); pollTimeout = null } }
function checkAndStop() { const hasActive = tasks.value.some(t => t.taskStatus === 'Pending' || t.taskStatus === 'Running'); if (!hasActive) stopPolling() }

async function doCreate() { if (!form.taskName || !form.dataVersion) { ElMessage.warning('请填写完整信息'); return }; creating.value = true; try { await createAdminTask({ taskName: form.taskName, taskType: form.taskType, dataVersion: form.dataVersion }); ElMessage.success('任务已创建，正在后台执行...'); showDialog.value = false; loadTasks(); startPolling() } catch (e: any) { ElMessage.error('创建失败: ' + (e.message || e)) } finally { creating.value = false } }
onMounted(() => {
  // 从概览状态分布图跳转而来：?taskStatus=xxx 预选对应状态
  const qs = String(route.query.taskStatus || '')
  if (['Succeeded', 'Failed', 'Running', 'Pending', 'Cancelled'].includes(qs)) status.value = qs
  loadTasks()
})
onUnmounted(() => { stopPolling() })
</script>

<style scoped>
.page-body { font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; }
/* ═══ 页面头部（企业级统一风格） ═══ */
.page-header {
  display: flex; margin-bottom: 20px; padding-bottom: 18px;
  border-bottom: 1px solid #eef2f6;
}
.page-header-row { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex: 1; }
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

.section-outer { padding: 1.5px; border-radius: 14px; background: rgba(0,0,0,.025); }
.section-inner { border-radius: calc(14px - 1.5px); background: var(--default-box-color); padding: 20px 24px 24px; border: 1px solid var(--default-border); }
.task-filter { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; position: sticky; top: 0; z-index: 5; background: var(--default-box-color); padding: 8px 2px; }
.task-stat-bar { display: flex; flex-wrap: wrap; gap: 8px 24px; padding: 10px 4px 12px; font-size: 12.5px; color: #64748b; }
.task-stat-item { display: inline-flex; align-items: center; gap: 6px; }
.task-stat-lbl { color: #94a3b8; }
.task-stat-item b { font-size: 14px; color: #0f172a; }
.task-stat-item b.stat-ok { color: #10b981; }
.task-stat-item b.stat-fail { color: #ef4444; }
.task-filter .input-search { width: 220px; }
.task-filter .input-type { width: 130px; }
.task-filter .input-status { width: 140px; }
.filter-sep { width: 1px; height: 20px; background: #e2e8f0; margin: 0 4px; }
.selected-info { font-size: 12px; color: #2563EB; }

/* ─── 结果/备注详情 ─── */
.remark-cell { cursor: pointer; color: #334155; display: inline; transition: color .15s; }
.remark-cell:hover { color: #2563EB; }
.remark-more { color: #2563EB; font-size: 12px; margin-left: 4px; flex-shrink: 0; }
.remark-empty { color: #cbd5e1; }
.remark-body { height: 60vh; display: flex; flex-direction: column; }
.remark-meta { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; flex-shrink: 0; }
.remark-time { font-size: 12px; color: #94a3b8; }
.remark-content { flex: 1; min-height: 0; overflow: auto; margin: 0; padding: 12px 14px; background: #0f172a; color: #e2e8f0; border-radius: 8px; font-size: 12px; line-height: 1.7; font-family: 'JetBrains Mono', Consolas, monospace; white-space: pre-wrap; word-break: break-all; }
/* 成功态：浅色结果卡片样式（区别于失败态的深色错误面板） */
.remark-content--ok { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; }

.btn-primary-custom { display: inline-flex; align-items: center; gap: 8px; padding: 10px 22px; border-radius: 8px; border: none; background: #2563EB; color: #FFF; font-size: 13px; font-weight: 600; cursor: pointer; font-family: 'Plus Jakarta Sans','Inter',sans-serif; transition: all .2s cubic-bezier(0.32,0.72,0,1); box-shadow: 0 2px 8px rgba(37,99,235,.2); white-space: nowrap; }
.btn-primary-custom:hover { background: #4A7AFF; transform: translateY(-1px); }

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