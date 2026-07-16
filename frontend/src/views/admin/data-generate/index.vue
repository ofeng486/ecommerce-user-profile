<template>
  <div class="page-body">
    <!-- ═══ PAGE HEADER ═══ -->
    <div class="page-header">
      <div>
        <h1 class="page-title">数据生成</h1>
        <p class="page-desc">通过 Python 脚本批量生成合规电商模拟数据，覆盖用户、订单、浏览轨迹等业务数据。生成完成后自动导入数据库。</p>
      </div>
    </div>

    <!-- ═══ 当前参数概览 ═══ -->
    <div class="params-bar-outer"><div class="params-bar-inner">
      <div v-for="p in paramSummary" :key="p.label" class="param-pill">
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
      <ElTable :data="tasks" stripe v-loading="taskLoading" size="small" class="data-table">
        <ElTableColumn prop="taskName" label="任务" width="160" />
        <ElTableColumn prop="taskType" label="类型" width="120">
          <template #default="{ row }"><ElTag size="small">{{ row.taskType }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }"><ElTag :type="st(row.taskStatus)">{{ row.taskStatus }}</ElTag></template>
        </ElTableColumn>
        <ElTableColumn prop="errorMessage" label="备注/进度" min-width="250" show-overflow-tooltip />
        <ElTableColumn label="时间" width="160">
          <template #default="{ row }">{{ row.createdAt ? new Date(row.createdAt).toLocaleString() : '-' }}</template>
        </ElTableColumn>
      </ElTable>
    </div></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import request from '@/utils/http'

defineOptions({ name: 'DataGenerate' })

const presets = ref<any>({})
const selectedPreset = ref('')
const generating = ref(false)
const clearing = ref(false)
const result = ref<any>(null)
const tasks = ref<any[]>([])
const taskLoading = ref(false)

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
    await request.post({ url: '/api/v1/admin/data-generate/generate', params: { ...form }, showErrorMessage: true })
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
async function loadTasks() { taskLoading.value = true; try { const res = await request.get<any>({ url: '/api/v1/admin/data-generate/tasks', params: { page: 0, size: 20 }, showErrorMessage: false }); tasks.value = res?.records || []; checkAndStopPolling() } catch {} finally { taskLoading.value = false } }
onMounted(async () => { loadTasks(); try { const res = await request.get<any>({ url: '/api/v1/admin/data-generate/presets', showErrorMessage: false }); presets.value = res || {} } catch {} })
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer); if (pollTimeout) clearTimeout(pollTimeout) })
</script>

<style scoped>
.page-body { font-family: 'Geist','Inter','PingFang SC',sans-serif; }

/* ═══ PAGE HEADER ═══ */
.page-header { margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--art-gray-900); margin: 0 0 6px; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; letter-spacing: -.3px; }
.page-desc { font-size: 13px; color: var(--art-gray-500); margin: 0; line-height: 1.6; max-width: 600px; }

/* ═══ PARAMS BAR — Double-Bezel ═══ */
.params-bar-outer { padding: 1.5px; border-radius: 12px; background: rgba(0,0,0,.025); margin-bottom: 20px; }
.params-bar-inner { border-radius: calc(12px - 1.5px); background: var(--default-box-color); padding: 14px 20px; display: flex; gap: 24px; flex-wrap: wrap; border: 1px solid var(--default-border); }
.param-pill { display: flex; align-items: center; gap: 6px; }
.param-pill-val { font-size: 15px; font-weight: 700; color: var(--art-gray-900); font-family: 'JetBrains Mono','Space Grotesk',monospace; }
.param-pill-lbl { font-size: 12px; color: var(--art-gray-500); font-weight: 500; }

/* ═══ SECTION ═══ */
.section-heading { font-size: 15px; font-weight: 700; color: var(--art-gray-900); margin: 0 0 14px; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; letter-spacing: -.2px; }

/* ═══ PRESETS — Asymmetrical ═══ */
.preset-section { margin-bottom: 20px; }
.preset-grid { display: grid; grid-template-columns: 1.3fr 1fr; gap: 12px; }
@media(max-width:640px){.preset-grid{grid-template-columns:1fr}}
.preset-outer { padding: 1.5px; border-radius: 12px; background: rgba(0,0,0,.025); cursor: pointer; transition: all .25s cubic-bezier(0.32,0.72,0,1); }
.preset-outer:hover { background: rgba(93,135,255,.06); }
.preset-active { background: rgba(93,135,255,.12) !important; }
.preset-featured { grid-row: span 2; }
.preset-inner { padding: 18px 20px; border-radius: calc(12px - 1.5px); background: var(--default-box-color); border: 1px solid var(--default-border); height: 100%; }
.preset-active .preset-inner { border-color: #5D87FF; }
.preset-top { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.preset-name { font-size: 14px; font-weight: 700; color: var(--art-gray-900); font-family: 'Plus Jakarta Sans',sans-serif; }
.preset-badge { font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 4px; background: rgba(93,135,255,.1); color: #5D87FF; }
.preset-desc { font-size: 12px; color: var(--art-gray-500); margin: 0 0 10px; line-height: 1.5; }
.preset-meta { display: flex; gap: 12px; flex-wrap: wrap; }
.preset-meta span { font-size: 11px; color: var(--art-gray-400); }

/* ═══ FORM — Double-Bezel ═══ */
.form-section-outer { padding: 1.5px; border-radius: 14px; background: rgba(0,0,0,.025); margin-bottom: 16px; }
.form-section-inner { border-radius: calc(14px - 1.5px); background: var(--default-box-color); padding: 20px 24px 24px; border: 1px solid var(--default-border); }
.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
@media(max-width:768px){.form-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:480px){.form-grid{grid-template-columns:1fr}}
.field { display: flex; flex-direction: column; gap: 4px; }
.field-lbl { font-size: 12px; font-weight: 600; color: var(--art-gray-700); }
.field-ctrl { width: 100%; }
.field-hint { font-size: 11px; color: var(--art-gray-400); margin-top: 2px; }

/* ═══ ACTIONS ═══ */
.action-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.btn-primary { display: inline-flex; align-items: center; gap: 8px; padding: 10px 10px 10px 22px; border-radius: 8px; border: none; background: #5D87FF; color: #FFF; font-size: 13px; font-weight: 600; cursor: pointer; font-family: 'Geist','Inter',sans-serif; transition: all .2s cubic-bezier(0.32,0.72,0,1); box-shadow: 0 2px 8px rgba(93,135,255,.2); }
.btn-primary:hover:not(:disabled) { background: #4A7AFF; transform: translateY(-1px); box-shadow: 0 4px 14px rgba(93,135,255,.3); }
.btn-primary:disabled { opacity: .5; cursor: not-allowed; }
.btn-icon-wrap { width: 26px; height: 26px; border-radius: 6px; background: rgba(255,255,255,.15); display: flex; align-items: center; justify-content: center; }
.btn-icon-arr { font-size: 12px; font-family: 'JetBrains Mono',monospace; transition: transform .2s; }
.btn-primary:hover:not(:disabled) .btn-icon-arr { transform: translateX(3px); }
.btn-ghost { padding: 10px 22px; border-radius: 8px; border: 1.5px solid var(--default-border); background: transparent; color: var(--art-gray-700); font-size: 13px; font-weight: 500; cursor: pointer; font-family: 'Geist','Inter',sans-serif; transition: all .2s; }
.btn-ghost:hover { border-color: #5D87FF; color: #5D87FF; }
.btn-danger { padding: 10px 22px; border-radius: 8px; border: none; background: #FF4D4F; color: #FFF; font-size: 13px; font-weight: 500; cursor: pointer; font-family: 'Geist','Inter',sans-serif; transition: all .2s; }
.btn-danger:hover:not(:disabled) { background: #E04345; }
.btn-danger:disabled { opacity: .5; cursor: not-allowed; }

/* ═══ TABLE — Double-Bezel ═══ */
.table-section-outer { padding: 1.5px; border-radius: 14px; background: rgba(0,0,0,.025); }
.table-section-inner { border-radius: calc(14px - 1.5px); background: var(--default-box-color); padding: 20px 24px 24px; border: 1px solid var(--default-border); }
.data-table { width: 100%; }
</style>