<template>
  <div class="page-body">
    <div class="page-header">
      <h1 class="page-title">数据导入</h1>
      <p class="page-desc">下载 CSV 模板填写数据，或使用测试数据目录中的文件。选择 7 个 CSV 文件一起上传，系统会自动识别对应的数据表。</p>
    </div>

    <!-- ═══ 模板下载区 — Double-Bezel ═══ -->
    <div class="section-outer"><div class="section-inner">
      <h2 class="section-heading">下载 CSV 模板</h2>
      <p class="section-hint">按模板填写后导入更准确，也可直接使用 test-output 目录中的测试数据</p>
      <div class="template-grid">
        <button v-for="t in templates" :key="t.table" class="template-btn" @click="downloadTemplate(t.table)">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>
          <span>{{ t.table }}</span>
        </button>
      </div>
    </div></div>

    <!-- ═══ 上传区 ═══ -->
    <div class="section-outer"><div class="section-inner">
      <h2 class="section-heading">上传数据文件</h2>
      <div class="upload-bar">
        <ElInput v-model="taskName" placeholder="任务名称" class="input-task" />
        <ElUpload :auto-upload="false" :on-change="handleFiles" multiple accept=".csv">
          <button class="btn-outline">选择 CSV 文件</button>
        </ElUpload>
        <button class="btn-primary-custom" :disabled="!files.length || !taskName || uploading" @click="doUpload">
          <span>开始导入</span>
          <span class="btn-icon-wrap"><span class="btn-icon-arr">→</span></span>
        </button>
      </div>

      <!-- 文件预览 -->
      <div v-if="files.length" class="file-preview">
        <div v-for="f in fileList" :key="f.name" class="file-row">
          <div class="file-row-top">
            <span class="file-name">{{ f.name }}</span>
            <ElTag size="small" :type="f.guessedTable === 'unknown' ? 'warning' : 'success'">
              {{ f.guessedTable === 'unknown' ? '无法识别' : f.guessedTable }}
            </ElTag>
            <span class="file-header">{{ f.header }}</span>
          </div>
          <div v-if="f.sampleRows && f.sampleRows.length" class="file-samples">
            <div v-for="(row, i) in f.sampleRows" :key="i" class="file-sample">{{ row }}</div>
            <span v-if="f.hasMore" class="file-more">... 更多行未显示</span>
          </div>
        </div>
      </div>
    </div></div>

    <ElAlert v-if="result" :type="result.type" :title="result.msg" closable class="mb-4" />

    <!-- ═══ 导入历史 ═══ -->
    <div class="section-outer"><div class="section-inner">
      <h2 class="section-heading">导入历史</h2>
      <ElTable :data="tasks" stripe v-loading="taskLoading" size="small" class="data-table">
        <ElTableColumn prop="taskName" label="任务" width="140" />
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }"><ElTag :type="st(row.taskStatus)">{{ row.taskStatus }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn prop="errorMessage" label="备注" min-width="200" show-overflow-tooltip />
        <ElTableColumn label="时间" width="160">
          <template #default="{ row }">{{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '-' }}</template>
        </ElTableColumn>
      </ElTable>
    </div></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import request from '@/utils/http'

defineOptions({ name: 'DataImport' })
const taskName = ref('')
const files = ref<any[]>([])
const fileList = ref<any[]>([])
const uploading = ref(false)
const result = ref<any>(null)
const tasks = ref<any[]>([])
const taskLoading = ref(false)
const templates = ref<any[]>([])

function st(s: string) { return s === 'Succeeded' ? 'success' : s === 'Running' ? 'warning' : s === 'Failed' ? 'danger' : 'info' }

async function handleFiles(file: any, fileListRaw: any) {
  files.value = fileListRaw.map((f: any) => f.raw)
  const previews = []
  for (const f of files.value) {
    try { const fd = new FormData(); fd.append('file', f); const res = await request.post<any>({ url: '/api/v1/admin/import/preview', data: fd, headers: { 'Content-Type': 'multipart/form-data' }, showErrorMessage: false }); previews.push(res || { fileName: f.name, guessedTable: 'unknown', header: '' }) }
    catch { previews.push({ fileName: f.name, guessedTable: 'unknown', header: '' }) }
  }
  fileList.value = previews
}
async function doUpload() {
  uploading.value = true; result.value = null
  try { const fd = new FormData(); files.value.forEach(f => fd.append('files', f)); fd.append('taskName', taskName.value); await request.post({ url: '/api/v1/admin/import/upload', data: fd, headers: { 'Content-Type': 'multipart/form-data' }, showErrorMessage: false }); result.value = { type: 'success', msg: '导入任务已创建，正在后台执行...' }; files.value = []; fileList.value = []; taskName.value = ''; pollTasks() }
  catch (e: any) { result.value = { type: 'error', msg: '导入失败: ' + (e.message || e) } } finally { uploading.value = false }
}
function downloadTemplate(table: string) { window.open('/api/v1/admin/import/template/' + table) }

let pollTimer: any = null; let pollTimeout: any = null
function pollTasks() { loadTasks(); if (pollTimer) clearInterval(pollTimer); if (pollTimeout) clearTimeout(pollTimeout); pollTimer = setInterval(loadTasks, 3000); pollTimeout = setTimeout(() => { if (pollTimer) { clearInterval(pollTimer); pollTimer = null } }, 300000) }
function checkAndStopPolling() { const hasActive = tasks.value.some(t => t.taskStatus === 'Pending' || t.taskStatus === 'Running'); if (!hasActive && pollTimer) { clearInterval(pollTimer); pollTimer = null; if (pollTimeout) { clearTimeout(pollTimeout); pollTimeout = null } } }
async function loadTasks() { taskLoading.value = true; try { const res = await request.get<any>({ url: '/api/v1/admin/import/tasks', params: { page: 0, size: 20 }, showErrorMessage: false }); tasks.value = res?.records || []; checkAndStopPolling() } catch {} finally { taskLoading.value = false } }
onMounted(async () => { loadTasks(); try { const res = await request.get<any>({ url: '/api/v1/admin/import/templates', showErrorMessage: false }); templates.value = res || [] } catch {} })
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer); if (pollTimeout) clearTimeout(pollTimeout) })
</script>

<style scoped>
.page-body { font-family: 'Geist','Inter','PingFang SC',sans-serif; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--art-gray-900); margin: 0 0 6px; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; letter-spacing: -.3px; }
.page-desc { font-size: 13px; color: var(--art-gray-500); margin: 0; line-height: 1.6; max-width: 600px; }

.section-outer { padding: 1.5px; border-radius: 14px; background: rgba(0,0,0,.025); margin-bottom: 16px; }
.section-inner { border-radius: calc(14px - 1.5px); background: var(--default-box-color); padding: 20px 24px 24px; border: 1px solid var(--default-border); }
.section-heading { font-size: 15px; font-weight: 700; color: var(--art-gray-900); margin: 0 0 6px; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; }
.section-hint { font-size: 12px; color: var(--art-gray-500); margin: 0 0 14px; }

.template-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.template-btn { display: inline-flex; align-items: center; gap: 6px; padding: 7px 16px; border-radius: 8px; border: 1.5px solid var(--default-border); background: transparent; color: var(--art-gray-700); font-size: 13px; font-weight: 500; cursor: pointer; font-family: 'Geist','Inter',sans-serif; transition: all .2s; }
.template-btn:hover { border-color: #5D87FF; color: #5D87FF; background: rgba(93,135,255,.04); }

.upload-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
.input-task { width: 200px; }
.btn-outline { display: inline-flex; align-items: center; padding: 9px 20px; border-radius: 8px; border: 1.5px solid var(--default-border); background: transparent; color: var(--art-gray-700); font-size: 13px; font-weight: 500; cursor: pointer; font-family: 'Geist','Inter',sans-serif; transition: all .2s; }
.btn-outline:hover { border-color: #5D87FF; color: #5D87FF; }
.btn-primary-custom { display: inline-flex; align-items: center; gap: 8px; padding: 9px 9px 9px 20px; border-radius: 8px; border: none; background: #5D87FF; color: #FFF; font-size: 13px; font-weight: 600; cursor: pointer; font-family: 'Geist','Inter',sans-serif; transition: all .2s cubic-bezier(0.32,0.72,0,1); box-shadow: 0 2px 8px rgba(93,135,255,.2); }
.btn-primary-custom:hover:not(:disabled) { background: #4A7AFF; transform: translateY(-1px); }
.btn-primary-custom:disabled { opacity: .5; cursor: not-allowed; }
.btn-icon-wrap { width: 26px; height: 26px; border-radius: 6px; background: rgba(255,255,255,.15); display: flex; align-items: center; justify-content: center; }
.btn-icon-arr { font-size: 12px; font-family: 'JetBrains Mono',monospace; transition: transform .2s; }
.btn-primary-custom:hover:not(:disabled) .btn-icon-arr { transform: translateX(3px); }

.file-preview { display: flex; flex-direction: column; gap: 8px; }
.file-row { padding: 12px; border-radius: 8px; background: var(--art-gray-100); border: 1px solid var(--art-gray-200); }
.file-row-top { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.file-name { font-size: 12px; font-family: 'JetBrains Mono',monospace; color: var(--art-gray-700); font-weight: 500; }
.file-header { font-size: 11px; color: var(--art-gray-400); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 300px; }
.file-samples { margin-top: 6px; padding-left: 4px; }
.file-sample { font-size: 11px; color: var(--art-gray-400); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-more { font-size: 11px; color: var(--art-gray-300); }

.data-table { width: 100%; }
</style>