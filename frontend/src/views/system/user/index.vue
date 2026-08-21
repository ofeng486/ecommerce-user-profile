<template>
  <div class="page-body">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">系统用户管理</h1>
          <span class="title-tag">SYSTEM USERS</span>
        </div>
        <p class="page-desc">管理系统用户账号、角色分配和启用状态。</p>
      </div>
    </div>

    <!-- ═══ 搜索 + 筛选栏 ═══ -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="search-box">
          <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          <input v-model="keyword" class="search-input" placeholder="搜索用户名或显示名..." @input="onSearch" />
        </div>
        <el-select
          v-model="roleFilter"
          class="filter-select"
          placeholder="全部角色"
          clearable
          @change="onSearch"
        >
          <el-option value="Admin" label="Admin" />
          <el-option value="User" label="User" />
        </el-select>
        <el-select
          v-model="statusFilter"
          class="filter-select"
          placeholder="全部状态"
          clearable
          @change="onSearch"
        >
          <el-option value="enabled" label="已启用" />
          <el-option value="disabled" label="已禁用" />
        </el-select>
        <span class="filter-sep"></span>
        <button class="btn-plain" :disabled="!selected.length" @click="batchToggle(true)">批量启用</button>
        <button class="btn-plain btn-plain-warn" :disabled="!selected.length" @click="batchToggle(false)">批量禁用</button>
        <span v-if="selected.length" class="selected-info">已选 {{ selected.length }} 项</span>
      </div>
      <button class="btn-primary" @click="showAdd = true">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 5v14M5 12h14"/></svg>
        <span>新增用户</span>
      </button>
    </div>

    <!-- ═══ 用户列表 ═══ -->
    <div class="table-section-outer"><div class="table-section-inner">
      <ElTable :data="displayList" stripe v-loading="loading" class="data-table" @selection-change="sel => selected = sel">
        <ElTableColumn type="selection" width="40" />
        <ElTableColumn prop="id" label="ID" width="60" resizable />
        <ElTableColumn label="用户名" min-width="160" resizable>
          <template #default="{ row }">
            <div class="user-cell"><span class="user-avatar">{{ (row.displayName||row.username||'?')[0] }}</span><div><div class="user-name">{{ row.username }}</div><div class="user-display">{{ row.displayName }}</div></div></div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="角色" width="110" resizable>
          <template #default="{ row }">
            <ElTooltip :content="row.role === 'Admin' ? '管理员：拥有用户管理、数据生成/导入、分析任务、系统设置等全部管理权限' : '普通用户：仅可使用个人中心、画像查询与 AI 分析功能'" placement="top">
              <span class="role-badge" :class="row.role === 'Admin' ? 'role-admin' : 'role-user'">{{ row.role }}</span>
            </ElTooltip>
          </template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="90" resizable>
          <template #default="{ row }"><span class="status-pill" :class="row.enabled ? 'status-on' : 'status-off'"><span class="status-dot"></span>{{ row.enabled ? '启用' : '禁用' }}</span></template>
        </ElTableColumn>
        <ElTableColumn prop="lastLoginAt" label="最后登录" min-width="150" resizable>
          <template #default="{ row }">{{ row.lastLoginAt ? formatTime(row.lastLoginAt) : '-' }}</template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="200" fixed="right" resizable>
          <template #default="{ row }">
            <div class="action-btns">
              <button class="action-btn btn-edit" @click="openEdit(row)" title="编辑">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 3a2.85 2.83 0 114.71 4.05L8.71 20.05 3 21l.95-5.71L17 3z"/></svg>
              </button>
              <button class="action-btn btn-key" @click="openResetPwd(row)" title="重置密码">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
              </button>
              <button class="action-btn action-toggle" :class="row.enabled ? 'btn-warn' : 'btn-success'" @click="toggleStatus(row)" :title="row.enabled ? '禁用' : '启用'">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="5" width="22" height="14" rx="2"/><circle :cx="row.enabled ? '17' : '7'" cy="12" r="3"/></svg>
              </button>
              <button class="action-btn btn-danger" @click="delUser(row)" title="删除">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
              </button>
            </div>
          </template>
        </ElTableColumn>
      </ElTable>
      <div class="pagination-bar" v-if="filteredTotal > size">
        <span class="pagination-info">共 {{ filteredTotal }} 条</span>
        <ElPagination v-model:current-page="page" :page-size="size" :total="filteredTotal" layout="prev,pager,next" small />
      </div>
    </div></div>

    <!-- ═══ 新增弹窗 ═══ -->
    <ElDialog v-model="showAdd" width="440px" class="save-dialog" :close-on-click-modal="false">
      <template #header><span class="dialog-title">新增系统用户</span></template>
      <ElForm :model="form" label-position="top" :rules="rules" ref="formRef">
        <ElFormItem label="用户名" prop="username"><ElInput v-model="form.username" placeholder="字母数字下划线" maxlength="30" /></ElFormItem>
        <ElFormItem label="显示名称" prop="displayName"><ElInput v-model="form.displayName" placeholder="用户显示名称" maxlength="50" /></ElFormItem>
        <ElFormItem label="密码" prop="password"><ElInput v-model="form.password" type="password" placeholder="至少 8 位" maxlength="50" show-password /></ElFormItem>
        <ElFormItem label="角色" prop="role">
          <ElSelect v-model="form.role">
            <ElOption label="普通用户 (User)" value="User" />
            <ElOption label="管理员 (Admin)" value="Admin" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="showAdd = false">取消</ElButton>
        <ElButton type="primary" @click="addUser" :loading="saving">确定创建</ElButton>
      </template>
    </ElDialog>

    <!-- ═══ 编辑弹窗 ═══ -->
    <ElDialog v-model="showEdit" width="440px" class="save-dialog" :close-on-click-modal="false">
      <template #header><span class="dialog-title">编辑用户 · {{ editForm.username }}</span></template>
      <ElForm :model="editForm" label-position="top" :rules="editRules" ref="editFormRef">
        <ElFormItem label="显示名称" prop="displayName"><ElInput v-model="editForm.displayName" placeholder="用户显示名称" maxlength="50" /></ElFormItem>
        <ElFormItem label="角色" prop="role">
          <ElSelect v-model="editForm.role">
            <ElOption label="普通用户 (User)" value="User" />
            <ElOption label="管理员 (Admin)" value="Admin" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="showEdit = false">取消</ElButton>
        <ElButton type="primary" @click="saveEdit" :loading="savingEdit">保存修改</ElButton>
      </template>
    </ElDialog>

    <!-- ═══ 重置密码弹窗 ═══ -->
    <ElDialog v-model="showResetPwd" width="440px" class="save-dialog" :close-on-click-modal="false">
      <template #header><span class="dialog-title">重置密码 · {{ resetPwdForm.username }}</span></template>
      <ElForm :model="resetPwdForm" label-position="top" :rules="pwdRules" ref="pwdFormRef">
        <ElFormItem label="新密码" prop="newPassword">
          <ElInput v-model="resetPwdForm.newPassword" type="password" placeholder="至少 8 位" maxlength="50" show-password />
        </ElFormItem>
        <ElFormItem label="确认密码" prop="confirmPassword">
          <ElInput v-model="resetPwdForm.confirmPassword" type="password" placeholder="再次输入新密码" maxlength="50" show-password />
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="showResetPwd = false">取消</ElButton>
        <ElButton type="primary" @click="saveResetPwd" :loading="savingPwd">确定重置</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAdminUsers, createAdminUser, updateAdminUserStatus, updateAdminUser, resetAdminUserPassword } from '@/api/admin'
import request from '@/utils/http'

defineOptions({ name: 'SystemUsers' })
const list = ref<any[]>([]); const loading = ref(false)
const page = ref(1); const size = ref(20)
const keyword = ref(''); const roleFilter = ref(''); const statusFilter = ref('')
const selected = ref<any[]>([])
const showAdd = ref(false); const saving = ref(false)
const formRef = ref<any>(null)
const form = reactive({ username: '', password: '', displayName: '', role: 'User' })
const rules = {
  username: [{ required:true, message:'请输入用户名', trigger:'blur' }, { min:2, max:30, message:'2-30 个字符', trigger:'blur' }],
  displayName: [{ required:true, message:'请输入显示名称', trigger:'blur' }],
  password: [{ required:true, message:'请输入密码', trigger:'blur' }, { min:8, message:'至少 8 位', trigger:'blur' }],
  role: [{ required:true, message:'请选择角色', trigger:'change' }],
}

// ═══ 编辑状态 ═══
const showEdit = ref(false); const savingEdit = ref(false)
const editFormRef = ref<any>(null)
const editForm = reactive({ userId: 0, username: '', displayName: '', role: 'User' })
const editRules = {
  displayName: [{ required:true, message:'请输入显示名称', trigger:'blur' }],
  role: [{ required:true, message:'请选择角色', trigger:'change' }],
}

// ═══ 重置密码状态 ═══
const showResetPwd = ref(false); const savingPwd = ref(false)
const pwdFormRef = ref<any>(null)
const resetPwdForm = reactive({ userId: 0, username: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  newPassword: [{ required:true, message:'请输入新密码', trigger:'blur' }, { min:8, message:'至少 8 位', trigger:'blur' }],
  confirmPassword: [
    { required:true, message:'请再次输入新密码', trigger:'blur' },
    { validator: (_rule:any, value:string, cb:(e?:Error)=>void) => value === resetPwdForm.newPassword ? cb() : cb(new Error('两次输入的密码不一致')), trigger:'blur' }
  ],
}

function openEdit(row: any) {
  editForm.userId = row.id
  editForm.username = row.username
  editForm.displayName = row.displayName || ''
  editForm.role = row.role === 'Admin' ? 'Admin' : 'User'
  showEdit.value = true
}

async function saveEdit() {
  if (!editFormRef.value) return
  try { await editFormRef.value.validate() } catch { return }
  savingEdit.value = true
  try {
    await updateAdminUser(editForm.userId, { displayName: editForm.displayName, role: editForm.role })
    ElMessage.success('保存成功')
    showEdit.value = false; load()
  } catch (e: any) { ElMessage.error('保存失败: ' + (e?.message || '')) }
  finally { savingEdit.value = false }
}

function openResetPwd(row: any) {
  resetPwdForm.userId = row.id
  resetPwdForm.username = row.username
  resetPwdForm.newPassword = ''
  resetPwdForm.confirmPassword = ''
  showResetPwd.value = true
}

async function saveResetPwd() {
  if (!pwdFormRef.value) return
  try { await pwdFormRef.value.validate() } catch { return }
  savingPwd.value = true
  try {
    await resetAdminUserPassword(resetPwdForm.userId, resetPwdForm.newPassword)
    ElMessage.success(`密码已重置，请通知「${resetPwdForm.username}」使用新密码登录`)
    showResetPwd.value = false
  } catch (e: any) { ElMessage.error('重置失败: ' + (e?.message || '')) }
  finally { savingPwd.value = false }
}

let debounce: any = null
function onSearch() { clearTimeout(debounce); debounce = setTimeout(() => { page.value = 1 }, 300) }

function formatTime(t: string) {
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

// 前端过滤（后端不支持 keyword/role/enabled 参数）
const filteredList = computed(() => {
  let data = list.value
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    data = data.filter(r => (r.username||'').toLowerCase().includes(kw) || (r.displayName||'').toLowerCase().includes(kw))
  }
  if (roleFilter.value) data = data.filter(r => r.role === roleFilter.value)
  if (statusFilter.value === 'enabled') data = data.filter(r => r.enabled)
  else if (statusFilter.value === 'disabled') data = data.filter(r => !r.enabled)
  return data
})
const filteredTotal = computed(() => filteredList.value.length)
const displayList = computed(() => {
  const start = (page.value - 1) * size.value
  return filteredList.value.slice(start, start + size.value)
})

async function load() {
  loading.value = true
  try {
    const res = await fetchAdminUsers({ page: 0, size: 100 })
    list.value = (res as any)?.records || []
  } catch {} finally { loading.value = false }
}

async function addUser() {
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  saving.value = true
  try {
    await createAdminUser({ username: form.username, password: form.password, displayName: form.displayName, role: form.role })
    ElMessage.success('用户创建成功')
    showAdd.value = false; Object.assign(form, { username:'', password:'', displayName:'', role:'User' }); load()
  } catch (e: any) { ElMessage.error('创建失败: ' + (e?.message || '')) }
  finally { saving.value = false }
}

async function toggleStatus(row: any) {
  // 禁用需二次确认（影响登录），启用直接执行
  if (row.enabled) {
    try {
      await ElMessageBox.confirm(`确定要禁用用户「${row.username}」吗？禁用后该用户将无法登录。`, '禁用确认', { confirmButtonText:'确认禁用', cancelButtonText:'取消', type:'warning' })
    } catch { return }
  }
  try {
    await updateAdminUserStatus(row.id, !row.enabled)
    ElMessage.success(row.enabled ? '已禁用' : '已启用')
    load()
  } catch { ElMessage.error('操作失败') }
}

/** 批量启用/禁用：循环调用单用户接口，统计成功失败数 */
async function batchToggle(enabled: boolean) {
  if (!selected.value.length) return
  const action = enabled ? '启用' : '禁用'
  const ok: number[] = []; const failed: number[] = []
  for (const row of selected.value) {
    if (row.enabled === enabled) { ok.push(row.id); continue } // 状态已一致，跳过
    try { await updateAdminUserStatus(row.id, enabled); ok.push(row.id) } catch { failed.push(row.id) }
  }
  ElMessage.success(`已${action} ${ok.length} 个用户${failed.length ? `，失败 ${failed.length} 个` : ''}`)
  selected.value = []
  load()
}

async function delUser(row: any) {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？此操作不可恢复。`, '删除确认', { confirmButtonText:'确认删除', cancelButtonText:'取消', type:'error' })
    await request.del({ url: `/api/v1/admin/users/${row.id}` })
    ElMessage.success('用户已删除')
    load()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error('删除失败: ' + (e?.message || '')) }
}

onMounted(load)
</script>

<style scoped>
.page-body{font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif}
/* ═══ 页面头部（企业级统一风格） ═══ */
.page-header{
  display:flex;align-items:flex-end;justify-content:space-between;gap:20px;
  margin-bottom:16px;padding-bottom:18px;
  border-bottom:1px solid #eef2f6;
}
.ph-left{min-width:0}
.ph-title-row{display:flex;align-items:center;gap:10px}
.title-accent{
  width:4px;height:20px;border-radius:2px;flex-shrink:0;
  background:linear-gradient(180deg,#2563eb 0%,#60a5fa 100%);
}
.page-title{
  font-size:22px;font-weight:700;color:#0f172a;margin:0;
  font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif;
  letter-spacing:-0.3px;line-height:1.2;
}
.title-tag{
  font-size:10px;font-weight:600;letter-spacing:1.2px;color:#94a3b8;
  background:#f1f5f9;border-radius:4px;padding:2px 6px;
  font-family:'JetBrains Mono',monospace;text-transform:uppercase;
}
.page-desc{font-size:13px;color:#64748b;margin:8px 0 0 14px;line-height:1.6;max-width:600px}

/* ═══ TOOLBAR ═══ */
.toolbar{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:16px;flex-wrap:wrap}
.toolbar-left{display:flex;align-items:center;gap:8px;flex-wrap:wrap;flex:1}
.search-box{position:relative;flex:1;max-width:300px;min-width:180px}
.search-icon{position:absolute;left:12px;top:50%;transform:translateY(-50%);color:#dbdfe1;pointer-events:none}
.search-input{width:100%;padding:8px 12px 8px 36px;border-radius:8px;border:1.5px solid var(--default-border);font-size:13px;color:#383853;background:var(--default-box-color);outline:none;font-family:'Plus Jakarta Sans','Inter',sans-serif;transition:all .2s;box-sizing:border-box}
.search-input:focus{border-color:#2563EB;box-shadow:0 0 0 3px rgba(37,99,235,.1)}
.search-input::placeholder{color:#dbdfe1}
.filter-select {
  width: 110px;
}
.filter-select :deep(.el-select__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1.5px var(--default-border) inset;
  min-height: 36px;
  padding: 0 12px;
  transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
}
.filter-select :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1.5px #cbd5e1 inset;
}
.filter-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1.5px #2563eb inset, 0 0 0 3px rgba(37, 99, 235, 0.12);
}
.filter-select :deep(.el-select__placeholder) {
  color: #94a3b8;
  font-size: 13px;
}
.filter-select :deep(.el-select__selected-item) {
  font-size: 13px;
  color: #1e293b;
}
.btn-primary{display:inline-flex;align-items:center;gap:6px;padding:9px 20px;border-radius:8px;border:none;background:#2563EB;color:#FFF;font-size:13px;font-weight:600;cursor:pointer;font-family:'Plus Jakarta Sans',sans-serif;transition:all .2s cubic-bezier(0.32,0.72,0,1);box-shadow:0 2px 8px rgba(37,99,235,.2);white-space:nowrap}
.btn-primary:hover{background:#4A7AFF;transform:translateY(-1px)}
.filter-sep{width:1px;height:20px;background:var(--default-border);margin:0 4px}
.btn-plain{display:inline-flex;align-items:center;gap:6px;padding:8px 14px;border-radius:8px;border:1.5px solid var(--default-border);background:var(--default-box-color);color:#4d5875;font-size:13px;font-weight:600;cursor:pointer;font-family:'Plus Jakarta Sans',sans-serif;transition:all .2s;white-space:nowrap}
.btn-plain:hover:not(:disabled){border-color:#2563EB;color:#2563EB}
.btn-plain-warn:hover:not(:disabled){border-color:#FFAE1F;color:#FFAE1F}
.btn-plain:disabled{opacity:.5;cursor:not-allowed}
.selected-info{font-size:12px;color:#2563EB}

/* ═══ TABLE ═══ */
.table-section-outer{padding:1.5px;border-radius:14px;background:rgba(0,0,0,.025)}
.table-section-inner{border-radius:calc(14px-1.5px);background:var(--default-box-color);padding:20px 24px 24px;border:1px solid var(--default-border)}
.data-table{width:100%}
.user-cell{display:flex;align-items:center;gap:10px}
.user-avatar{width:30px;height:30px;border-radius:8px;background:linear-gradient(135deg,#2563EB,#A0C0FF);color:#FFF;display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:700;flex-shrink:0;font-family:'Plus Jakarta Sans',sans-serif}
.user-name{font-size:13px;font-weight:600;color:#383853}
.user-display{font-size:11px;color:#dbdfe1}

/* ═══ BADGES ═══ */
.role-badge{display:inline-flex;padding:3px 10px;border-radius:6px;font-size:11px;font-weight:600}
.role-admin{background:rgba(37,99,235,.1);color:#2563EB}
.role-user{background:rgba(19,222,185,.1);color:#13DEB9}
.status-pill{display:inline-flex;align-items:center;gap:5px;font-size:12px;font-weight:500}
.status-dot{width:7px;height:7px;border-radius:50%}
.status-on .status-dot{background:#13DEB9}
.status-off .status-dot{background:#e6eaeb}
.status-on{color:#13DEB9}
.status-off{color:#949eb7}

/* ═══ ACTIONS ═══ */
.action-btns{display:flex;gap:6px}
.action-btn{width:30px;height:30px;border-radius:8px;border:none;display:flex;align-items:center;justify-content:center;cursor:pointer;transition:all .15s}
.btn-warn{background:rgba(255,174,31,.1);color:#FFAE1F}
.btn-warn:hover{background:rgba(255,174,31,.2)}
.btn-success{background:rgba(19,222,185,.1);color:#13DEB9}
.btn-success:hover{background:rgba(19,222,185,.2)}
.btn-edit{background:rgba(37,99,235,.1);color:#2563EB}
.btn-edit:hover{background:rgba(37,99,235,.2)}
.btn-key{background:rgba(166,77,255,.1);color:#A64DFF}
.btn-key:hover{background:rgba(166,77,255,.2)}
.btn-danger{background:rgba(255,77,79,.08);color:#FF4D4F}
.btn-danger:hover{background:rgba(255,77,79,.15)}

/* ═══ PAGINATION ═══ */
.pagination-bar{display:flex;align-items:center;justify-content:flex-end;gap:16px;margin-top:16px}
.pagination-info{font-size:12px;color:#dbdfe1}

/* ═══ DIALOG ═══ */
.dialog-title{font-size:16px;font-weight:700;color:#323251;font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif}
</style>