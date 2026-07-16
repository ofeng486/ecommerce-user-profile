<template>
  <div class="page-body">
    <div class="page-header">
      <h1 class="page-title">系统用户管理</h1>
      <p class="page-desc">管理系统用户账号、角色分配和启用状态</p>
    </div>

    <!-- ═══ 搜索 + 筛选栏 ═══ -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="search-box">
          <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          <input v-model="keyword" class="search-input" placeholder="搜索用户名或显示名..." @input="onSearch" />
        </div>
        <select v-model="roleFilter" class="filter-select" @change="onSearch">
          <option value="">全部角色</option>
          <option value="Admin">Admin</option>
          <option value="User">User</option>
        </select>
        <select v-model="statusFilter" class="filter-select" @change="onSearch">
          <option value="">全部状态</option>
          <option value="enabled">已启用</option>
          <option value="disabled">已禁用</option>
        </select>
      </div>
      <button class="btn-primary" @click="showAdd = true">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 5v14M5 12h14"/></svg>
        <span>新增用户</span>
      </button>
    </div>

    <!-- ═══ 用户列表 ═══ -->
    <div class="table-section-outer"><div class="table-section-inner">
      <ElTable :data="displayList" stripe v-loading="loading" class="data-table">
        <ElTableColumn prop="id" label="ID" width="60" resizable />
        <ElTableColumn label="用户名" min-width="160" resizable>
          <template #default="{ row }">
            <div class="user-cell"><span class="user-avatar">{{ (row.displayName||row.username||'?')[0] }}</span><div><div class="user-name">{{ row.username }}</div><div class="user-display">{{ row.displayName }}</div></div></div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="角色" width="100" resizable>
          <template #default="{ row }"><span class="role-badge" :class="row.role === 'Admin' ? 'role-admin' : 'role-user'">{{ row.role }}</span></template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="90" resizable>
          <template #default="{ row }"><span class="status-pill" :class="row.enabled ? 'status-on' : 'status-off'"><span class="status-dot"></span>{{ row.enabled ? '启用' : '禁用' }}</span></template>
        </ElTableColumn>
        <ElTableColumn prop="lastLoginAt" label="最后登录" min-width="150" resizable>
          <template #default="{ row }">{{ row.lastLoginAt ? formatTime(row.lastLoginAt) : '-' }}</template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="120" fixed="right" resizable>
          <template #default="{ row }">
            <div class="action-btns">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAdminUsers, createAdminUser, updateAdminUserStatus } from '@/api/admin'
import request from '@/utils/http'

defineOptions({ name: 'SystemUsers' })
const list = ref<any[]>([]); const loading = ref(false)
const page = ref(1); const size = ref(20)
const keyword = ref(''); const roleFilter = ref(''); const statusFilter = ref('')
const showAdd = ref(false); const saving = ref(false)
const formRef = ref<any>(null)
const form = reactive({ username: '', password: '', displayName: '', role: 'User' })
const rules = {
  username: [{ required:true, message:'请输入用户名', trigger:'blur' }, { min:2, max:30, message:'2-30 个字符', trigger:'blur' }],
  displayName: [{ required:true, message:'请输入显示名称', trigger:'blur' }],
  password: [{ required:true, message:'请输入密码', trigger:'blur' }, { min:8, message:'至少 8 位', trigger:'blur' }],
  role: [{ required:true, message:'请选择角色', trigger:'change' }],
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
    await createAdminUser({ username: form.value.username, password: form.value.password, displayName: form.value.displayName, role: form.value.role })
    ElMessage.success('用户创建成功')
    showAdd.value = false; form.value = { username:'', password:'', displayName:'', role:'User' }; load()
  } catch (e: any) { ElMessage.error('创建失败: ' + (e?.message || '')) }
  finally { saving.value = false }
}

async function toggleStatus(row: any) {
  try {
    await updateAdminUserStatus(row.id, !row.enabled)
    ElMessage.success(row.enabled ? '已禁用' : '已启用')
    load()
  } catch { ElMessage.error('操作失败') }
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
.page-body{font-family:'Geist','Inter','PingFang SC',sans-serif}
.page-header{margin-bottom:16px}
.page-title{font-size:22px;font-weight:700;color:var(--art-gray-900);margin:0 0 4px;font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif;letter-spacing:-.3px}
.page-desc{font-size:13px;color:var(--art-gray-500);margin:0}

/* ═══ TOOLBAR ═══ */
.toolbar{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:16px;flex-wrap:wrap}
.toolbar-left{display:flex;align-items:center;gap:8px;flex-wrap:wrap;flex:1}
.search-box{position:relative;flex:1;max-width:300px;min-width:180px}
.search-icon{position:absolute;left:12px;top:50%;transform:translateY(-50%);color:var(--art-gray-400);pointer-events:none}
.search-input{width:100%;padding:8px 12px 8px 36px;border-radius:8px;border:1.5px solid var(--default-border);font-size:13px;color:var(--art-gray-800);background:var(--default-box-color);outline:none;font-family:'Geist','Inter',sans-serif;transition:all .2s;box-sizing:border-box}
.search-input:focus{border-color:#5D87FF;box-shadow:0 0 0 3px rgba(93,135,255,.1)}
.search-input::placeholder{color:var(--art-gray-400)}
.filter-select{padding:8px 12px;border-radius:8px;border:1.5px solid var(--default-border);font-size:13px;color:var(--art-gray-700);background:var(--default-box-color);outline:none;font-family:'Geist','Inter',sans-serif;cursor:pointer;transition:all .2s;min-width:110px}
.filter-select:focus{border-color:#5D87FF}
.btn-primary{display:inline-flex;align-items:center;gap:6px;padding:9px 20px;border-radius:8px;border:none;background:#5D87FF;color:#FFF;font-size:13px;font-weight:600;cursor:pointer;font-family:'Geist',sans-serif;transition:all .2s cubic-bezier(0.32,0.72,0,1);box-shadow:0 2px 8px rgba(93,135,255,.2);white-space:nowrap}
.btn-primary:hover{background:#4A7AFF;transform:translateY(-1px)}

/* ═══ TABLE ═══ */
.table-section-outer{padding:1.5px;border-radius:14px;background:rgba(0,0,0,.025)}
.table-section-inner{border-radius:calc(14px-1.5px);background:var(--default-box-color);padding:20px 24px 24px;border:1px solid var(--default-border)}
.data-table{width:100%}
.user-cell{display:flex;align-items:center;gap:10px}
.user-avatar{width:30px;height:30px;border-radius:8px;background:linear-gradient(135deg,#5D87FF,#A0C0FF);color:#FFF;display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:700;flex-shrink:0;font-family:'Plus Jakarta Sans',sans-serif}
.user-name{font-size:13px;font-weight:600;color:var(--art-gray-800)}
.user-display{font-size:11px;color:var(--art-gray-400)}

/* ═══ BADGES ═══ */
.role-badge{display:inline-flex;padding:3px 10px;border-radius:6px;font-size:11px;font-weight:600}
.role-admin{background:rgba(93,135,255,.1);color:#5D87FF}
.role-user{background:rgba(19,222,185,.1);color:#13DEB9}
.status-pill{display:inline-flex;align-items:center;gap:5px;font-size:12px;font-weight:500}
.status-dot{width:7px;height:7px;border-radius:50%}
.status-on .status-dot{background:#13DEB9}
.status-off .status-dot{background:var(--art-gray-300)}
.status-on{color:#13DEB9}
.status-off{color:var(--art-gray-500)}

/* ═══ ACTIONS ═══ */
.action-btns{display:flex;gap:6px}
.action-btn{width:30px;height:30px;border-radius:8px;border:none;display:flex;align-items:center;justify-content:center;cursor:pointer;transition:all .15s}
.btn-warn{background:rgba(255,174,31,.1);color:#FFAE1F}
.btn-warn:hover{background:rgba(255,174,31,.2)}
.btn-success{background:rgba(19,222,185,.1);color:#13DEB9}
.btn-success:hover{background:rgba(19,222,185,.2)}
.btn-danger{background:rgba(255,77,79,.08);color:#FF4D4F}
.btn-danger:hover{background:rgba(255,77,79,.15)}

/* ═══ PAGINATION ═══ */
.pagination-bar{display:flex;align-items:center;justify-content:flex-end;gap:16px;margin-top:16px}
.pagination-info{font-size:12px;color:var(--art-gray-400)}

/* ═══ DIALOG ═══ */
.dialog-title{font-size:16px;font-weight:700;color:var(--art-gray-900);font-family:'Plus Jakarta Sans','Inter','PingFang SC',sans-serif}
</style>