<template>
  <div class="pkg-page" :class="isAdminSide ? 'theme-admin' : 'theme-user'">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h2 class="page-title">人群包管理</h2>
          <span class="title-tag">AUDIENCE PACKAGES</span>
        </div>
        <p class="page-desc">查看、编辑、删除已保存的人群包，并可查看包内用户。</p>
      </div>
    </div>

    <!-- KPI 概览 -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <span class="kpi-label">人群包总数</span>
        <span class="kpi-value">{{ rows.length }}</span>
        <span class="kpi-sub">已保存的圈选人群</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">覆盖用户总数</span>
        <span class="kpi-value">{{ totalUsers.toLocaleString() }}</span>
        <span class="kpi-sub">所有人群包人数合计</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">平均每包人数</span>
        <span class="kpi-value">{{ avgUsers }}</span>
        <span class="kpi-sub">总人数 ÷ 人群包数</span>
      </div>
    </div>

    <!-- 搜索 + 表格 -->
    <div class="table-card">
      <div class="table-toolbar">
        <el-input v-model="keyword" placeholder="搜索人群包名称 / 描述" clearable class="search-input" :prefix-icon="Search" />
        <span class="toolbar-count" v-if="filtered.length < rows.length">筛选出 {{ filtered.length }} 个</span>
      </div>
      <el-table :data="filtered" stripe v-loading="loading" max-height="480" class="pkg-table">
        <template #empty><el-empty description="暂无人群包，请先在「智能圈选」中创建" /></template>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="packageName" label="名称" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="160">
          <template #default="{ row }">{{ row.description || '—' }}</template>
        </el-table-column>
        <el-table-column label="圈选条件" min-width="220">
          <template #default="{ row }">
            <span v-if="row.rules?.length" class="rule-text" :title="formatRules(row.rules)">{{ formatRules(row.rules) }}</span>
            <span v-else class="rule-none">指定用户</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="人数" width="90" align="center">
          <template #default="{ row }">
            <span class="count-num">{{ (row.totalCount || 0).toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <button class="op-btn op-view" @click="openUsers(row)">查看人群</button>
            <button class="op-btn op-edit" @click="openEdit(row)">编辑</button>
            <el-popconfirm :title="`确定删除人群包「${row.packageName || ''}」？`" @confirm="doDelete(row.id)">
              <template #reference>
                <button class="op-btn op-delete">删除</button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="showEdit" title="编辑人群包" width="440px">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="名称"><el-input v-model="editForm.name" maxlength="50" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.desc" type="textarea" :rows="2" maxlength="200" /></el-form-item>
        <el-form-item label="圈选条件" v-if="editForm.rulesText">
          <div class="rule-preview">{{ editForm.rulesText }}</div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="showEdit = false">取消</el-button><el-button type="primary" @click="doUpdate">保存</el-button></template>
    </el-dialog>

    <!-- 查看人群弹窗 -->
    <el-dialog v-model="showUsers" :title="`查看人群：${usersPkgName}`" width="560px" @closed="usersRows = []">
      <div class="users-summary" v-if="usersTotal > 0">
        <span class="users-total">共 <b>{{ usersTotal.toLocaleString() }}</b> 位用户</span>
        <span v-if="usersRows.length < usersTotal" class="users-truncate">仅展示前 {{ usersRows.length }} 位（避免渲染过多）</span>
        <span class="users-hint">点击用户 ID 可查看画像详情</span>
        <el-button type="primary" size="small" class="users-export" :loading="exporting" @click="doExportUsers">
          <span v-if="!exporting">导出全部 CSV</span><span v-else>导出中…</span>
        </el-button>
      </div>
      <el-table :data="usersRows" v-loading="usersLoading" size="small" max-height="400" class="users-table">
        <template #empty><el-empty description="该人群包暂无用户" :image-size="60" /></template>
        <el-table-column prop="userId" label="用户 ID" min-width="120">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="gotoProfile(row.userId)">{{ row.userId }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="gotoProfile(row.userId)">查看画像详情 →</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchAudiencePackages, fetchAudiencePackageUsers, exportAudiencePackageUsers, updateAudiencePackage, deleteAudiencePackage } from '@/api/admin'

defineOptions({ name: 'AudiencePackages' })

const router = useRouter()
const route = useRoute()
/** 管理端蓝色 / 用户端青色（共享页面按路由自动切换） */
const isAdminSide = computed(() => !route.path.startsWith('/user'))

const rows = ref<any[]>([]); const loading = ref(false)
const keyword = ref('')
const showEdit = ref(false); const editForm = ref({ id: 0, name: '', desc: '', rulesText: '' })
const showUsers = ref(false); const usersPkgName = ref(''); const usersRows = ref<any[]>([])
const usersTotal = ref(0); const usersLoading = ref(false); const exporting = ref(false)
const currentUsersPkgId = ref(0)

/** 字段中文名 + 运算符中文 */
const FIELD_CN: Record<string, string> = {
  gender: '性别', province: '省份', city: '城市',
  segment_code: '用户分层', tag_value: '标签', favorite_category: '偏好品类',
  total_payment_amount: '累计消费', total_order_count: '订单数', average_order_amount: '平均客单价',
  recency_days: '距最近购买', browse_count_30d: '30 天浏览', login_count_30d: '30 天登录',
  age: '年龄'
}
/** 运算符中文（自然语言化） */
const OP_CN: Record<string, string> = {
  eq: '为', neq: '不为', gt: '超过', gte: '达到', lt: '不足', lte: '不超过',
  between: '在', contains: '包含', not_contains: '不包含', in: '属于'
}
/**  值中文化：枚举字段值映射 */
const VALUE_CN: Record<string, Record<string, string>> = {
  gender: { Male: '男', Female: '女', Unknown: '未知' },
  segment_code: {
    HIGH_VALUE: '高价值用户', POTENTIAL: '潜力用户', GENERAL: '一般用户',
    AT_RISK: '流失风险用户', LOW_VALUE: '低价值用户'
  }
}
/** 格式化数值显示（超过 1000 加"千"、带单位） */
function fmtValue(field: string, v: any): string {
  if (v == null || v === '') return '—'
  // 枚举值映射
  if (VALUE_CN[field] && VALUE_CN[field][String(v)]) return VALUE_CN[field][String(v)]
  // 数值：累计消费/订单数加"元"/"单"
  const num = Number(v)
  if (Number.isFinite(num)) {
    if (field === 'total_payment_amount' || field === 'average_order_amount') return `${num.toLocaleString()} 元`
    if (field === 'total_order_count') return `${num} 单`
    if (field === 'browse_count_30d' || field === 'login_count_30d') return `${num} 次`
    if (field === 'recency_days') return `${num} 天`
    if (field === 'age') return `${num} 岁`
  }
  return String(v)
}
/** 规则数组 → 可读文本："性别为女 且 累计消费超过 5000 元" */
function formatRules(rules: any[] | undefined): string {
  if (!rules?.length) return ''
  return rules.map((r: any, i: number) => {
    const field = FIELD_CN[r.field] || r.field
    const op = OP_CN[r.operator] || r.operator
    let valStr: string
    if (r.operator === 'between' && typeof r.value === 'string' && r.value.startsWith('[')) {
      try {
        const arr = JSON.parse(r.value)
        valStr = `${fmtValue(r.field, arr[0])} 到 ${fmtValue(r.field, arr[1])} 之间`
      } catch { valStr = r.value }
    } else {
      valStr = fmtValue(r.field, r.value)
    }
    return `${i > 0 ? (r.logicOp === 'OR' ? ' 或 ' : ' 且 ') : ''}${field}${op}${valStr}`
  }).join('')
}

/** 过滤后的行（搜索名称/描述） */
const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return rows.value
  return rows.value.filter(r =>
    (r.packageName || '').toLowerCase().includes(kw) || (r.description || '').toLowerCase().includes(kw)
  )
})

/** KPI：覆盖用户总数 / 平均每包人数 */
const totalUsers = computed(() => rows.value.reduce((s, r) => s + (r.totalCount || 0), 0))
const avgUsers = computed(() => {
  const n = rows.value.length
  return n ? Math.round(totalUsers.value / n).toLocaleString() : '—'
})

function formatDate(v: string) { if (!v) return '—'; return v.replace('T', ' ').substring(0, 19) }

async function load() {
  loading.value = true
  try {
    const res = await fetchAudiencePackages({ page: 0, size: 500 }) as any
    const list = Array.isArray(res) ? res : (res?.records ?? res?.list ?? res?.data ?? [])
    rows.value = list
      .filter((p: any) => p.status !== 0)
      .sort((a: any, b: any) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime())  // 创建时间倒序（最新在前）
  } catch { rows.value = [] }
  finally { loading.value = false }
}

function openEdit(row: any) {
  editForm.value = { id: row.id, name: row.packageName || '', desc: row.description || '', rulesText: formatRules(row.rules) }
  showEdit.value = true
}

async function doUpdate() {
  if (!editForm.value.name.trim()) { ElMessage.warning('请输入人群包名称'); return }
  try {
    await updateAudiencePackage(editForm.value.id, { packageName: editForm.value.name, description: editForm.value.desc })
    ElMessage.success('更新成功'); showEdit.value = false; load()
  } catch { ElMessage.error('更新失败') }
}

async function doDelete(id: number) {
  try { await deleteAudiencePackage(id); ElMessage.success('已删除'); load() }
  catch { ElMessage.error('删除失败') }
}

/** 查看人群包内用户（规则包按规则重算 / 指定用户包读关联表）——大包只渲染前 100 个防卡 */
async function openUsers(row: any) {
  showUsers.value = true
  usersPkgName.value = row.packageName || ''
  usersTotal.value = row.totalCount || 0
  currentUsersPkgId.value = row.id
  usersLoading.value = true
  try {
    const res = await fetchAudiencePackageUsers(row.id) as any
    const ids: number[] = Array.isArray(res) ? res : (res?.data ?? [])
    if (ids.length > 100) {
      usersRows.value = ids.slice(0, 100).map(userId => ({ userId }))
      usersTotal.value = ids.length  // 实际数量（可能超过包的 totalCount 快照）
    } else {
      usersRows.value = ids.map(userId => ({ userId }))
    }
  } catch (e: any) {
    usersRows.value = []
    ElMessage.error('加载用户失败：' + (e?.message || '未知错误'))
  } finally { usersLoading.value = false }
}

/** 导出人群包全部用户 CSV */
async function doExportUsers() {
  if (!currentUsersPkgId.value || exporting.value) return
  exporting.value = true
  try {
    const blob: any = await exportAudiencePackageUsers(currentUsersPkgId.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `人群包_${usersPkgName.value}_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success(`已导出 ${usersTotal.value.toLocaleString()} 位用户`)
  } catch (e: any) {
    ElMessage.error('导出失败：' + (e?.message || '未知错误'))
  } finally { exporting.value = false }
}

/** 跳画像详情 */
function gotoProfile(userId: number) {
  if (!userId) return
  showUsers.value = false
  const base = isAdminSide.value ? '' : '/user'
  router.push(`${base}/profiles/${userId}`)
}

onMounted(load)
</script>

<style scoped>
.pkg-page { padding: 28px 32px; width: 100%; font-family: 'Inter', 'PingFang SC', sans-serif; }
/* 主题变量族：管理端蓝 / 用户端青 */
.theme-admin.pkg-page { --acc: #2563eb; --acc-dark: #1d4ed8; --acc-soft: rgba(37,99,235,.08); --acc-line: #93c5fd; }
.theme-user.pkg-page { --acc: #0d9488; --acc-dark: #0f766e; --acc-soft: rgba(13,148,136,.08); --acc-line: #5eead4; }
.theme-user.pkg-page { --el-color-primary: #0d9488; --el-color-primary-dark-2: #0f766e; }

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
.theme-user .title-accent { background: linear-gradient(180deg, #0d9488 0%, #5eead4 100%); }
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
.page-desc { font-size: 13px; color: #64748b; margin: 8px 0 0 14px; line-height: 1.6; max-width: 600px; }

/* KPI 概览 */
.kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 16px; }
.kpi-card {
  background: var(--default-box-color); border: 1px solid var(--default-border);
  border-radius: 12px; padding: 18px 22px; display: flex; flex-direction: column; gap: 4px;
  box-shadow: 0 1px 2px rgba(0,0,0,.03);
}
.kpi-label { font-size: 12.5px; color: #64748b; }
.kpi-value { font-size: 26px; font-weight: 700; color: var(--acc); font-variant-numeric: tabular-nums; letter-spacing: -.5px; }
.kpi-sub { font-size: 11.5px; color: #94a3b8; }

/* 表格卡片 */
.table-card {
  background: var(--default-box-color); border: 1px solid var(--default-border);
  border-radius: 12px; padding: 16px 20px 20px; box-shadow: 0 1px 2px rgba(0,0,0,.03);
}
.table-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.search-input { width: 260px; }
.toolbar-count { font-size: 12px; color: #94a3b8; }
.pkg-table { width: 100%; }
.pkg-table :deep(.el-table__inner-wrapper) { border-radius: 12px; }
.pkg-table :deep(th.el-table__cell) {
  background: #f8fafc !important;
  color: #475569 !important;
  font-weight: 600 !important;
  font-size: 12.5px !important;
  font-family: 'Plus Jakarta Sans', 'Inter', 'PingFang SC', sans-serif !important;
}
.pkg-table :deep(.el-table__row:hover > td.el-table__cell) { background: var(--acc-faint, rgba(37,99,235,0.03)) !important; }
.pkg-table :deep(.el-table__row) { transition: background 0.2s cubic-bezier(0.32, 0.72, 0, 1); }

/* 弹窗定制 */
.pkg-page :deep(.el-dialog) {
  border-radius: 18px;
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.16);
  border: 1px solid rgba(15, 23, 42, 0.06);
}
.pkg-page :deep(.el-dialog__header) { padding: 20px 24px 14px; border-bottom: 1px solid rgba(15, 23, 42, 0.05); }
.pkg-page :deep(.el-dialog__title) { font-size: 16px; font-weight: 700; color: #0f172a; font-family: 'Plus Jakarta Sans', 'Inter', 'PingFang SC', sans-serif; }
.pkg-page :deep(.el-dialog__body) { padding: 20px 24px; }
.pkg-page :deep(.el-dialog__footer) { padding: 14px 24px 20px; border-top: 1px solid rgba(15, 23, 42, 0.05); }
.pkg-page :deep(.el-overlay) { background: rgba(15, 23, 42, 0.28); backdrop-filter: blur(6px); }
.count-num { font-weight: 600; color: #334155; font-variant-numeric: tabular-nums; }
.rule-text {
  display: inline-block; font-size: 12px; color: #475569; line-height: 1.5;
  max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.rule-none { font-size: 12px; color: #94a3b8; }
.rule-preview {
  width: 100%; background: #f8fafc; border: 1px solid #eef2f6; border-radius: 8px;
  padding: 8px 12px; font-size: 12.5px; color: #475569; line-height: 1.7;
}
.users-truncate { font-size: 12px; color: #f59e0b; }
.users-export { margin-left: auto; }

/* 操作按钮：纯文字风格 + hover 柔和背景（与 el-link 浅蓝 hover 背景对比，更专业） */
.op-btn {
  background: transparent; border: none; padding: 4px 10px; margin: 0 2px;
  font-size: 13px; font-family: inherit; cursor: pointer; border-radius: 6px;
  transition: all .15s; line-height: 1.6; font-variant-numeric: tabular-nums;
}
.op-view  { color: #475569; }
.op-edit  { color: var(--acc); }
.op-delete { color: #dc2626; }
.op-view:hover  { background: #f1f5f9; color: #0f172a; }
.op-edit:hover  { background: var(--acc-soft); }
.op-delete:hover { background: #fef2f2; color: #b91c1c; }
.op-btn:active { transform: scale(0.96); }

/* 查看人群弹窗 */
.users-summary { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; font-size: 13px; color: #475569; }
.users-total b { color: var(--acc); }
.users-hint { font-size: 12px; color: #94a3b8; }
</style>