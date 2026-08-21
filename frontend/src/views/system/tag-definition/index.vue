<template>
  <div class="page-body">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">标签体系管理</h1>
          <span class="title-tag">TAG DEFINITION</span>
        </div>
        <p class="page-desc">维护用户标签的定义与分类，支持标签的新增、编辑和停用，为画像计算与标签分析提供标准化依据。</p>
      </div>
    </div>

    <!-- ═══ 工具栏 ═══ -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="search-box">
          <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          <input v-model="keyword" class="search-input" placeholder="搜索标签名称或编码..." @input="onSearch" />
        </div>
        <el-select v-model="categoryFilter" class="filter-select" placeholder="全部分类" clearable @change="onSearch">
          <el-option v-for="c in categories" :key="c" :value="c" :label="c" />
        </el-select>
        <el-select v-model="statusFilter" class="filter-select" placeholder="全部状态" clearable @change="onSearch">
          <el-option value="enabled" label="已启用" />
          <el-option value="disabled" label="已停用" />
        </el-select>
        <span class="count-info">共 {{ filteredList.length }} 个标签定义</span>
      </div>
      <div class="toolbar-right">
        <button class="btn-plain" :disabled="recalculating" @click="handleRecalculate">
          <svg v-if="!recalculating" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
          <span v-else class="spin-dot"></span>
          <span>{{ recalculating ? '重算中…' : '重算全部标签' }}</span>
        </button>
        <button class="btn-primary" @click="openAdd">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 5v14M5 12h14"/></svg>
          <span>新增标签</span>
        </button>
      </div>
    </div>

    <!-- ═══ 标签列表 ═══ -->
    <div class="table-section-outer"><div class="table-section-inner">
      <ElTable :data="filteredList" stripe v-loading="loading" class="data-table">
        <ElTableColumn prop="id" label="ID" width="60" resizable />
        <ElTableColumn label="标签" min-width="180" resizable>
          <template #default="{ row }">
            <div class="tag-cell">
              <span class="tag-avatar">{{ (row.tagName || '?')[0] }}</span>
              <div>
                <div class="tag-name">{{ row.tagName }}</div>
                <div class="tag-code">{{ row.tagCode }}</div>
              </div>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="tagCategory" label="分类" width="110" resizable>
          <template #default="{ row }"><span class="cat-badge">{{ row.tagCategory }}</span></template>
        </ElTableColumn>
        <ElTableColumn prop="valueType" label="值类型" width="100" resizable>
          <template #default="{ row }"><span class="vt-pill">{{ valueTypeLabel(row.valueType) }}</span></template>
        </ElTableColumn>
        <ElTableColumn label="计算依据" width="130" resizable>
          <template #default="{ row }">
            <span class="vt-pill">{{ metricLabel(row.sourceTable, row.ruleExpression) }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="计算规则" min-width="180" resizable show-overflow-tooltip>
          <template #default="{ row }">
            <span class="rule-text">{{ row.calculationRule || '—' }}</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="90" resizable>
          <template #default="{ row }">
            <span class="status-pill" :class="row.enabled ? 'status-on' : 'status-off'">
              <span class="status-dot"></span>{{ row.enabled ? '已启用' : '已停用' }}
            </span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <button class="action-btn" title="编辑" @click="openEdit(row)">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
              </button>
              <button class="action-btn" :class="row.enabled ? 'btn-off' : 'btn-on'" :title="row.enabled ? '停用' : '启用'" @click="toggleStatus(row)">
                <svg v-if="row.enabled" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M18.36 6.64a9 9 0 1 1-12.73 0"/><line x1="12" y1="2" x2="12" y2="12"/></svg>
                <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
              </button>
              <button v-if="!isPreset(row)" class="action-btn btn-del" title="删除" @click="handleDelete(row)">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
              </button>
            </div>
          </template>
        </ElTableColumn>
      </ElTable>
    </div></div>

    <!-- ═══ 新增/编辑弹窗 ═══ -->
    <ElDialog v-model="dialogVisible" :title="dialogTitle" width="520px" class="save-dialog" :close-on-click-modal="false">
      <el-form :model="form" label-width="88px" label-position="left">
        <el-form-item label="标签名称" :required="true">
          <ElInput v-model="form.tagName" placeholder="如 活跃度 / 消费能力" :maxlength="100" />
        </el-form-item>
        <div class="auto-info">
          <span class="auto-chip">分类：{{ autoCategory }}</span>
          <span class="auto-chip">值类型：{{ autoValueType }}</span>
          <span class="auto-chip">档位：{{ autoTierSummary }}</span>
          <span class="auto-tip">以上由系统根据你的选择自动生成，无需手动填写</span>
        </div>
        <el-form-item label="看哪个数据" v-if="!isEdit" :required="true">
          <el-select v-model="form.metric" class="full-select" placeholder="选择用哪个经营数据来判断">
            <el-option v-for="m in metrics" :key="m.field" :label="m.label" :value="m.field" />
          </el-select>
        </el-form-item>
        <el-form-item label="分成几档" v-if="!isEdit" :required="true">
          <el-radio-group v-model="form.tierCount" class="tier-radio">
            <el-radio-button :value="2">2 档</el-radio-button>
            <el-radio-button :value="3">3 档</el-radio-button>
          </el-radio-group>
          <div class="form-tip">数字越大代表要求越高，系统自动按「高 / 中 / 低」命名</div>
        </el-form-item>
        <el-form-item label="分档标准" v-if="!isEdit" :required="true">
          <div class="tier-editor">
            <div v-for="(th, i) in form.thresholds" :key="i" class="tier-row">
              <span class="tier-name">{{ tierName(i) }}</span>
              <span class="tier-when">大于等于</span>
              <ElInput v-model="form.thresholds[i]" class="tier-input" placeholder="数字，如 5000" />
            </div>
            <div class="tier-row">
              <span class="tier-name">{{ elseName }}</span>
              <span class="tier-when">其余用户</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="标签说明">
          <ElInput v-model="form.calculationRule" type="textarea" :rows="2" placeholder="选填：给运营看的说明，如 根据累计消费金额分为高中低三档" :maxlength="2000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="handleSave">保存</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAdminTags, createAdminTag, updateAdminTag, updateAdminTagStatus, deleteAdminTag, recalculateTags } from '@/api/admin'

defineOptions({ name: 'AdminTagDefinition' })

const loading = ref(false)
const saving = ref(false)
const recalculating = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const categoryFilter = ref('')
const statusFilter = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const dialogTitle = ref('新增标签定义')

function valueTypeLabel(t: string): string {
  return ({ STRING: '字符串', NUMBER: '数值', BOOLEAN: '布尔', ENUM: '枚举', DATE: '日期' } as Record<string, string>)[t] || t || '—'
}

const metrics = [
  { label: '累计消费金额', table: 'user_profile_summary', field: 'total_payment_amount' },
  { label: '订单总数', table: 'user_profile_summary', field: 'total_order_count' },
  { label: '平均客单价', table: 'user_profile_summary', field: 'average_order_amount' },
  { label: '近30天浏览数', table: 'user_profile_summary', field: 'browse_count_30d' },
  { label: '近30天登录数', table: 'user_profile_summary', field: 'login_count_30d' },
  { label: 'RFM综合评分', table: 'user_segment', field: 'segment_score' }
]
/** 计算依据友好名称：优先按规则表达式内容精确识别，其次表名兜底 */
function metricLabel(table?: string, rule?: string): string {
  if (rule) {
    const r = rule
    if (r.includes('login_count_30d + browse_count_30d')) return '登录+浏览活跃度'
    if (r.includes('total_payment_amount') && r.includes('average_order_amount')) return '消费金额/客单价'
    if (r.includes('total_payment_amount')) return '累计消费金额'
    if (r.includes('average_order_amount')) return '平均客单价'
    if (r.includes('total_order_count')) return '订单总数'
    if (r.includes('browse_count_30d')) return '近30天浏览数'
    if (r.includes('login_count_30d')) return '近30天登录数'
    if (r.includes('favorite_category_id')) return '偏好品类'
    if (r.includes('segment_score')) return '用户分层评分'
    if (r.includes('r_score') || r.includes('f_score') || r.includes('m_score')) return 'RFM评分'
  }
  const fallback: Record<string, string> = {
    user_profile_summary: '用户画像汇总',
    ads_user_rfm: 'RFM评分',
    user_segment: '用户分层'
  }
  return fallback[table || ''] || table || '未配置'
}


const form = ref({ tagName: '', metric: '', tierCount: 3, thresholds: ['', ''], calculationRule: '' })

/** 计算依据 → 标签分类 自动映射 */
const CATEGORY_MAP: Record<string, string> = {
  total_payment_amount: '消费特征',
  total_order_count: '行为特征',
  average_order_amount: '消费特征',
  browse_count_30d: '行为特征',
  login_count_30d: '行为特征',
  segment_score: '用户价值'
}
/** 档位命名（2档：高/低；3档：高/中/低） */
const tierNames = (n: number) => (n === 3 ? ['高', '中', '低'] : ['高', '低'])
const tierName = (i: number) => tierNames(form.value.tierCount)[i] || ''
const elseName = computed(() => tierNames(form.value.tierCount)[form.value.tierCount - 1] || '低')
/** 编辑时保留原分类（新增时随计算依据自动推断） */
const rowCategory = ref('')
const autoCategory = computed(() => {
  const m = metrics.find(x => x.field === form.value.metric)
  return (m && CATEGORY_MAP[m.field]) || '自定义标签'
})
const autoValueType = computed(() => '字符串')
const autoTierSummary = computed(() => tierNames(form.value.tierCount).join(' / '))

/** 档数变化时同步阈值数组长度 */
watch(() => form.value.tierCount, (n) => {
  const len = n - 1
  const arr = (form.value.thresholds || []).slice(0, len)
  while (arr.length < len) arr.push('')
  form.value.thresholds = arr
})
const categories = computed(() => [...new Set(list.value.map((t: any) => t.tagCategory).filter(Boolean))])
const filteredList = computed(() => {
  let arr = list.value
  const kw = keyword.value.trim().toLowerCase()
  if (kw) arr = arr.filter((t: any) => (t.tagName || '').toLowerCase().includes(kw) || (t.tagCode || '').toLowerCase().includes(kw))
  if (categoryFilter.value) arr = arr.filter((t: any) => t.tagCategory === categoryFilter.value)
  if (statusFilter.value === 'enabled') arr = arr.filter((t: any) => t.enabled)
  if (statusFilter.value === 'disabled') arr = arr.filter((t: any) => !t.enabled)
  return arr
})

function onSearch() { /* computed 自动响应 */ }

async function load() {
  loading.value = true
  try {
    const res = await fetchAdminTags({ page: 0, size: 100 })
    list.value = res?.records || []
  } catch { list.value = [] }
  loading.value = false
}

function openAdd() {
  isEdit.value = false; editId.value = null
  Object.assign(form.value, { tagName: '', metric: '', tierCount: 3, thresholds: ['', ''], calculationRule: '' })
  rowCategory.value = ''
  dialogTitle.value = '新增标签定义'
  dialogVisible.value = true
}
function openEdit(row: any) {
  isEdit.value = true; editId.value = row.id
  Object.assign(form.value, {
    tagName: row.tagName,
    metric: '', tierCount: 3, thresholds: ['', ''],
    calculationRule: row.calculationRule || ''
  })
  rowCategory.value = row.tagCategory || '自定义标签' 
  dialogTitle.value = '编辑标签定义'
  dialogVisible.value = true
}

function buildExpression(): { sourceTable: string; ruleExpression: string } | null {
  const m = metrics.find(x => x.field === form.value.metric)
  if (!m) return null
  const names = tierNames(form.value.tierCount)
  const vals = form.value.thresholds.map((x: any) => String(x).trim())
  if (vals.some((x: any) => x === '')) return null
  const whens = vals.map((v, i) => 'WHEN ' + m.field + ' >= ' + Number(v) + " THEN '" + names[i] + "'")
  return { sourceTable: m.table, ruleExpression: 'CASE ' + whens.join(' ') + " ELSE '" + names[names.length - 1] + "' END" }
}

async function handleRecalculate() {
  try {
    await ElMessageBox.confirm('将按当前启用的标签定义，重新为所有用户计算标签结果。确定继续？', '重算标签', { confirmButtonText: '开始重算', cancelButtonText: '取消', type: 'warning' })
  } catch { return }
  recalculating.value = true
  try {
    const res = await recalculateTags()
    const data = res?.data || {}
    const failed = data.failed || []
    let msg = '重算完成：成功 ' + data.success + ' 个，跳过 ' + data.skipped + ' 个'
    if (failed.length) msg += '，失败 ' + failed.length + ' 个（' + failed.join('；') + '）'
    ElMessage.success(msg)
    await load()
  } catch { /* 错误由全局拦截提示 */ }
  recalculating.value = false
}

async function handleSave() {
  if (!form.value.tagName.trim()) { ElMessage.warning('请填写标签名称'); return }
  if (!isEdit.value && !form.value.metric.trim()) { ElMessage.warning('请选择看哪个数据'); return }
  if (!isEdit.value && form.value.thresholds.some((x: any) => String(x).trim() === '')) {
    ElMessage.warning('请填写分档标准'); return
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await updateAdminTag(editId.value!, {
        tagName: form.value.tagName.trim(),
        tagCategory: rowCategory.value,
        valueType: 'STRING',
        calculationRule: form.value.calculationRule.trim() || undefined
      })
      ElMessage.success('标签定义已更新')
    } else {
      const rule = buildExpression()
      await createAdminTag({
        tagName: form.value.tagName.trim(),
        tagCategory: autoCategory.value,
        valueType: 'STRING',
        calculationRule: form.value.calculationRule.trim() || undefined,
        sourceTable: rule?.sourceTable || undefined,
        ruleExpression: rule?.ruleExpression || undefined
      })
      ElMessage.success('标签定义已创建')
    }
    dialogVisible.value = false
    await load()
  } catch { /* 错误由全局拦截提示 */ }
  saving.value = false
}

/** 系统预设标签（与 Spark 作业联动，不允许删除） */
const PRESET_PREFIXES = ['ACTIVE_LEVEL', 'CONSUMPTION_LEVEL', 'FAVORITE_CATEGORY', 'RFM_SEGMENT']
function isPreset(row: any): boolean {
  const code = (row.tagCode || '').toUpperCase()
  return PRESET_PREFIXES.some(p => code.startsWith(p))
}

/** 删除标签（连带清除该标签画像数据） */
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      '删除标签「' + row.tagName + '」后，该标签下所有用户的画像结果也将一并清除，且不可恢复。确定删除？',
      '删除标签', { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' })
  } catch { return }
  try {
    await deleteAdminTag(row.id)
    ElMessage.success('标签已删除')
    await load()
  } catch { /* 错误由全局拦截提示 */ }
}

async function toggleStatus(row: any) {
  const next = !row.enabled
  const action = next ? '启用' : '停用'
  try {
    await ElMessageBox.confirm('确定' + action + '标签「' + row.tagName + '」吗？', '提示', { confirmButtonText: action, cancelButtonText: '取消', type: 'warning' })
  } catch { return }
  try {
    await updateAdminTagStatus(row.id, next)
    ElMessage.success('已' + action)
    await load()
  } catch { /* 错误由全局拦截提示 */ }
}

onMounted(load)
</script>

<style scoped>
.page-body{font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif}
.page-header{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin-bottom:16px;padding-bottom:18px;border-bottom:1px solid #eef2f6}
.ph-left{min-width:0}
.ph-title-row{display:flex;align-items:center;gap:10px}
.title-accent{width:4px;height:20px;border-radius:2px;flex-shrink:0;background:linear-gradient(180deg,#2563eb 0%,#60a5fa 100%)}
.page-title{font-size:22px;font-weight:700;color:#0f172a;margin:0;font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif;letter-spacing:-0.3px;line-height:1.2}
.title-tag{font-size:10px;font-weight:600;letter-spacing:1.2px;color:#94a3b8;background:#f1f5f9;border-radius:4px;padding:2px 6px;font-family:'JetBrains Mono',monospace;text-transform:uppercase}
.page-desc{font-size:13px;color:#64748b;margin:8px 0 0 14px;line-height:1.6;max-width:640px}

.toolbar{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:16px;flex-wrap:wrap}
.toolbar-right{display:flex;align-items:center;gap:8px}
.toolbar-left{display:flex;align-items:center;gap:8px;flex-wrap:wrap;flex:1}
.search-box{position:relative;flex:1;max-width:300px;min-width:180px}
.search-icon{position:absolute;left:12px;top:50%;transform:translateY(-50%);color:#dbdfe1;pointer-events:none}
.search-input{width:100%;padding:8px 12px 8px 36px;border-radius:8px;border:1.5px solid var(--default-border);font-size:13px;color:#383853;background:var(--default-box-color);outline:none;transition:all .2s;box-sizing:border-box}
.search-input:focus{border-color:#2563EB;box-shadow:0 0 0 3px rgba(37,99,235,.1)}
.search-input::placeholder{color:#dbdfe1}
.filter-select{width:120px}
.filter-select :deep(.el-select__wrapper){border-radius:8px;box-shadow:0 0 0 1.5px var(--default-border) inset;min-height:36px;padding:0 12px;transition:all .25s}
.filter-select :deep(.el-select__wrapper:hover){box-shadow:0 0 0 1.5px #cbd5e1 inset}
.filter-select :deep(.el-select__wrapper.is-focused){box-shadow:0 0 0 1.5px #2563eb inset,0 0 0 3px rgba(37,99,235,.12)}
.filter-select :deep(.el-select__placeholder){color:#94a3b8;font-size:13px}
.filter-select :deep(.el-select__selected-item){font-size:13px;color:#1e293b}
.count-info{font-size:12px;color:#94a3b8;margin-left:4px}
.btn-plain{display:inline-flex;align-items:center;gap:6px;padding:8px 14px;border-radius:8px;border:1.5px solid var(--default-border);background:var(--default-box-color);color:#4d5875;font-size:13px;font-weight:600;cursor:pointer;transition:all .2s;white-space:nowrap}
.btn-plain:hover:not(:disabled){border-color:#2563EB;color:#2563EB}
.btn-plain:disabled{opacity:.5;cursor:not-allowed}
.btn-primary{display:inline-flex;align-items:center;gap:6px;padding:9px 20px;border-radius:8px;border:none;background:#2563EB;color:#FFF;font-size:13px;font-weight:600;cursor:pointer;font-family:'Plus Jakarta Sans',sans-serif;transition:all .2s cubic-bezier(0.32,0.72,0,1);box-shadow:0 2px 8px rgba(37,99,235,.2);white-space:nowrap}
.btn-primary:hover{background:#4A7AFF;transform:translateY(-1px)}
.spin-dot{width:12px;height:12px;border:2px solid #2563EB;border-top-color:transparent;border-radius:50%;display:inline-block;animation:spin 0.8s linear infinite}
@keyframes spin{to{transform:rotate(360deg)}}
.form-tip{font-size:12px;color:#94a3b8;margin-top:4px;line-height:1.5}
.tier-editor{width:100%;display:flex;flex-direction:column;gap:8px}
.tier-row{display:flex;align-items:center;gap:8px;width:100%}
.tier-when{font-size:13px;color:#64748b;white-space:nowrap}
.tier-input{width:120px}
.tier-input :deep(.el-input__wrapper){border-radius:8px;box-shadow:0 0 0 1.5px var(--default-border) inset}
.tier-name{display:inline-flex;align-items:center;justify-content:center;min-width:36px;padding:2px 10px;border-radius:6px;background:rgba(37,99,235,.1);color:#2563EB;font-size:13px;font-weight:600;white-space:nowrap}
.tier-radio :deep(.el-radio-button__inner){border-radius:6px;padding:6px 16px;font-size:13px}
.auto-info{display:flex;align-items:center;gap:8px;flex-wrap:wrap;padding:0 0 2px 88px;margin-bottom:14px}
.auto-chip{display:inline-flex;padding:3px 10px;border-radius:6px;background:#f1f5f9;color:#64748b;font-size:12px;font-weight:500}
.auto-tip{font-size:12px;color:#b0b8c4;margin-left:4px}

.table-section-outer{padding:1.5px;border-radius:14px;background:rgba(0,0,0,.025)}
.table-section-inner{border-radius:calc(14px-1.5px);background:var(--default-box-color);padding:20px 24px 24px;border:1px solid var(--default-border)}
.data-table{width:100%}
.tag-cell{display:flex;align-items:center;gap:10px}
.tag-avatar{width:30px;height:30px;border-radius:8px;background:linear-gradient(135deg,#2563EB,#A0C0FF);color:#FFF;display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:700;flex-shrink:0}
.tag-name{font-size:13px;font-weight:600;color:#383853}
.tag-code{font-size:11px;color:#dbdfe1;font-family:'JetBrains Mono',monospace}
.cat-badge{display:inline-flex;padding:3px 10px;border-radius:6px;font-size:11px;font-weight:600;background:rgba(37,99,235,.1);color:#2563EB}
.vt-pill{display:inline-flex;padding:2px 8px;border-radius:5px;font-size:11px;font-weight:600;background:#f1f5f9;color:#64748b;font-family:'JetBrains Mono',monospace}
.rule-text{font-size:12px;color:#64748b}
.status-pill{display:inline-flex;align-items:center;gap:5px;font-size:12px;font-weight:500}
.status-dot{width:7px;height:7px;border-radius:50%}
.status-on .status-dot{background:#13DEB9}
.status-off .status-dot{background:#e6eaeb}
.status-on{color:#13DEB9}
.status-off{color:#949eb7}
.action-btns{display:flex;gap:6px}
.action-btn{width:30px;height:30px;border-radius:8px;border:none;display:flex;align-items:center;justify-content:center;cursor:pointer;transition:all .15s;background:rgba(37,99,235,.1);color:#2563EB}
.action-btn:hover{background:rgba(37,99,235,.2)}
.btn-off{background:rgba(148,163,184,.12);color:#94a3b8}
.btn-off:hover{background:rgba(239,68,68,.12);color:#ef4444}
.btn-on{background:rgba(19,222,185,.12);color:#13DEB9}
.btn-on:hover{background:rgba(19,222,185,.22)}
.btn-del{background:rgba(239,68,68,.08);color:#ef4444}
.btn-del:hover{background:rgba(239,68,68,.18)}
.full-select{width:100%}
.full-select :deep(.el-select__wrapper){box-shadow:0 0 0 1.5px var(--default-border) inset;border-radius:8px}
.save-dialog :deep(.el-dialog__title){font-weight:600;color:#0f172a}
</style>
