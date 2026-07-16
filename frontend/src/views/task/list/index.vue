<template>
  <div class="page-body">
    <div class="page-header">
      <div class="page-header-row">
        <div>
          <h1 class="page-title">分析任务</h1>
          <p class="page-desc">先通过数据生成或数据导入将电商数据导入数据库，然后创建 Spark 画像分析任务，系统会自动计算 RFM 分层和用户标签。</p>
        </div>
        <button class="btn-primary-custom" @click="showDialog = true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 5v14M5 12h14"/></svg>
          <span>创建画像任务</span>
        </button>
      </div>
    </div>

    <!-- ═══ 任务列表 ═══ -->
    <div class="section-outer"><div class="section-inner">
      <ElTable :data="tasks" stripe v-loading="loading" size="small" class="data-table">
        <ElTableColumn prop="id" label="ID" width="60" />
        <ElTableColumn prop="taskName" label="任务名称" width="160" />
        <ElTableColumn prop="taskType" label="类型" width="140">
          <template #default="{ row }"><ElTag size="small" :type="row.taskType === 'PROFILE_FULL' ? 'warning' : row.taskType === 'DATA_GENERATE' ? 'success' : 'info'">{{ row.taskType }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }"><ElTag :type="st(row.taskStatus)">{{ row.taskStatus }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn prop="dataVersion" label="数据版本" width="160" show-overflow-tooltip />
        <ElTableColumn prop="errorMessage" label="结果/备注" min-width="200" show-overflow-tooltip />
        <ElTableColumn label="时间" width="160">
          <template #default="{ row }">{{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '-' }}</template>
        </ElTableColumn>
      </ElTable>
    </div></div>

    <!-- 创建任务对话框 -->
    <ElDialog v-model="showDialog" title="创建 Spark 画像分析任务" width="480px">
      <ElForm :model="form" label-width="100px">
        <ElFormItem label="任务名称"><ElInput v-model="form.taskName" placeholder="例如：Spark RFM 画像分析" /></ElFormItem>
        <ElFormItem label="任务类型"><ElSelect v-model="form.taskType" class="w-full"><ElOption label="Spark 画像分析 (PROFILE_FULL)" value="PROFILE_FULL" /></ElSelect></ElFormItem>
        <ElFormItem label="数据版本"><ElInput v-model="form.dataVersion" placeholder="例如：20260714" /></ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="showDialog = false">取消</ElButton>
        <ElButton type="primary" @click="doCreate" :loading="creating">创建并执行</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { fetchAdminTasks, createAdminTask } from '@/api/admin'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'TaskList' })
const tasks = ref<any[]>([]); const loading = ref(false); const showDialog = ref(false); const creating = ref(false)
const form = reactive({ taskName: 'Spark RFM 画像分析', taskType: 'PROFILE_FULL', dataVersion: String(new Date().getFullYear()) + String(Date.now()).slice(-4) })
function st(s: string) { return s === 'Succeeded' ? 'success' : s === 'Running' ? 'warning' : s === 'Failed' ? 'danger' : 'info' }
async function loadTasks() { loading.value = true; try { const res = await fetchAdminTasks({ page: 0, size: 50 }); tasks.value = res?.records || []; checkAndStop() } catch {} finally { loading.value = false } }

let pollTimer: any = null; let pollTimeout: any = null
function startPolling() {
  stopPolling()
  pollTimer = setInterval(loadTasks, 3000)
  pollTimeout = setTimeout(() => stopPolling(), 300000)
}
function stopPolling() { if (pollTimer) { clearInterval(pollTimer); pollTimer = null }; if (pollTimeout) { clearTimeout(pollTimeout); pollTimeout = null } }
function checkAndStop() { const hasActive = tasks.value.some(t => t.taskStatus === 'Pending' || t.taskStatus === 'Running'); if (!hasActive) stopPolling() }

async function doCreate() { if (!form.taskName || !form.dataVersion) { ElMessage.warning('请填写完整信息'); return }; creating.value = true; try { await createAdminTask({ taskName: form.taskName, taskType: form.taskType, dataVersion: form.dataVersion }); ElMessage.success('任务已创建，正在后台执行...'); showDialog.value = false; loadTasks(); startPolling() } catch (e: any) { ElMessage.error('创建失败: ' + (e.message || e)) } finally { creating.value = false } }
onMounted(() => { loadTasks() })
onUnmounted(() => { stopPolling() })
</script>

<style scoped>
.page-body { font-family: 'Geist','Inter','PingFang SC',sans-serif; }
.page-header { margin-bottom: 20px; }
.page-header-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--art-gray-900); margin: 0 0 6px; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; letter-spacing: -.3px; }
.page-desc { font-size: 13px; color: var(--art-gray-500); margin: 0; line-height: 1.6; max-width: 600px; }

.section-outer { padding: 1.5px; border-radius: 14px; background: rgba(0,0,0,.025); }
.section-inner { border-radius: calc(14px - 1.5px); background: var(--default-box-color); padding: 20px 24px 24px; border: 1px solid var(--default-border); }

.btn-primary-custom { display: inline-flex; align-items: center; gap: 8px; padding: 10px 22px; border-radius: 8px; border: none; background: #5D87FF; color: #FFF; font-size: 13px; font-weight: 600; cursor: pointer; font-family: 'Geist','Inter',sans-serif; transition: all .2s cubic-bezier(0.32,0.72,0,1); box-shadow: 0 2px 8px rgba(93,135,255,.2); white-space: nowrap; }
.btn-primary-custom:hover { background: #4A7AFF; transform: translateY(-1px); }

.data-table { width: 100%; }
</style>