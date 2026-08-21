<!-- 个人中心：基本信息 + 修改密码 + 登录历史 -->
<template>
  <div class="page-body">
    <div class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">个人中心</h1>
          <span class="title-tag">ACCOUNT CENTER</span>
        </div>
        <p class="page-desc">管理你的账号信息、登录密码和登录历史。</p>
      </div>
    </div>

    <div class="layout-grid">

      <!-- ═══ 左列：基本信息 + 修改密码 ═══ -->
      <div class="col-left">
        <!-- 基本信息 -->
        <div class="section-outer"><div class="section-inner">
          <div class="section-head">
            <div class="head-icon" style="background:#eef2ff;color:#2563EB">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </div>
            <div>
              <h2 class="section-title">基本信息</h2>
              <p class="section-desc">展示你的账号资料，显示名称可修改</p>
            </div>
          </div>
          <ElForm :model="userInfo" label-width="100px" label-position="top" v-loading="loading">
            <div class="form-row">
              <ElFormItem label="用户名"><ElInput v-model="userInfo.username" disabled /></ElFormItem>
              <ElFormItem label="角色">
                <div class="role-tag" :class="userInfo.role === 'ADMIN' ? 'role-admin' : 'role-user'">{{ userInfo.role }}</div>
              </ElFormItem>
            </div>
            <ElFormItem label="显示名称"><ElInput v-model="userInfo.displayName" placeholder="用户显示名称" maxlength="50" /></ElFormItem>
            <ElFormItem>
              <ElButton type="primary" @click="doSave" :loading="saving">保存修改</ElButton>
            </ElFormItem>
          </ElForm>
        </div></div>

        <!-- 修改密码 -->
        <div class="section-outer"><div class="section-inner">
          <div class="section-head">
            <div class="head-icon" style="background:#fef3c7;color:#d97706">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
            </div>
            <div>
              <h2 class="section-title">修改密码</h2>
              <p class="section-desc">定期更换密码可提升账号安全性</p>
            </div>
          </div>
          <ElForm :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px" label-position="top">
            <ElFormItem label="原密码" prop="oldPassword">
              <ElInput v-model="pwdForm.oldPassword" type="password" placeholder="请输入当前密码" show-password maxlength="50" />
            </ElFormItem>
            <ElFormItem label="新密码" prop="newPassword">
              <ElInput v-model="pwdForm.newPassword" type="password" placeholder="至少 8 位" show-password maxlength="50" />
            </ElFormItem>
            <ElFormItem label="确认新密码" prop="confirmPassword">
              <ElInput v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password maxlength="50" />
            </ElFormItem>
            <ElFormItem>
              <ElButton type="warning" @click="doChangePwd" :loading="changingPwd">更新密码</ElButton>
            </ElFormItem>
          </ElForm>
        </div></div>
      </div>

      <!-- ═══ 右列：登录历史 ═══ -->
      <div class="col-right">
        <div class="section-outer"><div class="section-inner">
          <div class="section-head">
            <div class="head-icon" style="background:#dcfce7;color:#16a34a">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
            </div>
            <div>
              <h2 class="section-title">最近登录</h2>
              <p class="section-desc">查看最近的登录记录，异常情况请尽快修改密码</p>
            </div>
          </div>
          <div v-loading="logsLoading" class="logs-list">
            <div v-for="log in logs" :key="log.id" class="log-row" :class="{ 'log-row--expanded': log.loginResult !== 1 || expandedId === log.id }" @click="toggleExpand(log)">
              <div class="log-icon" :class="log.loginResult === 1 ? 'log-ok' : 'log-fail'">
                <svg v-if="log.loginResult === 1" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </div>
              <div class="log-content">
                <div class="log-status">
                  <span :class="log.loginResult === 1 ? 'status-ok' : 'status-fail'">{{ log.loginResult === 1 ? '登录成功' : '登录失败' }}</span>
                  <span v-if="log.failureReason" class="log-reason">· {{ log.failureReason }}</span>
                  <span class="log-arrow" :class="{ 'log-arrow--open': log.loginResult !== 1 || expandedId === log.id }">▸</span>
                </div>
                <div class="log-meta"><span class="meta-item">{{ formatTime(log.loginAt) }}</span></div>
                <div v-if="log.loginResult !== 1 || expandedId === log.id" class="log-detail">
                  <span class="meta-item">IP: {{ prettyIp(log.loginIp) }}</span>
                  <span class="meta-sep">·</span>
                  <span v-if="log.userAgent" class="meta-item" :title="log.userAgent">{{ truncateUA(log.userAgent) }}</span>
                </div>
              </div>
            </div>
            <div v-if="logs.length === 0 && !logsLoading" class="empty">暂无登录记录</div>
            <div v-if="logsTotal > logsPageSize" class="pager">
              <ElPagination v-model:current-page="logsPage" :page-size="logsPageSize" :total="logsTotal" layout="prev,pager,next" small />
            </div>
          </div>
        </div></div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { fetchGetUserInfo } from '@/api/auth'
import request from '@/utils/http'

defineOptions({ name: 'UserCenter' })

const userStore = useUserStore()

// ═══ 基本信息 ═══
const loading = ref(false); const saving = ref(false)
const userInfo = reactive({ userId: 0, username: '', displayName: '', role: '' })

// ═══ 修改密码 ═══
const changingPwd = ref(false)
const pwdFormRef = ref<any>(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 8, message: '至少 8 位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: (_r: any, v: string, cb: (e?: Error) => void) => v === pwdForm.newPassword ? cb() : cb(new Error('两次输入的密码不一致')), trigger: 'blur' }
  ],
}

// ═══ 登录历史 ═══
const logsLoading = ref(false)
const logs = ref<any[]>([])
const expandedId = ref<number | null>(null)
/** 展开/收起：失败记录默认展开无需点击，成功记录点击切换 */
function toggleExpand(log: any) {
  if (log.loginResult !== 1) return
  expandedId.value = expandedId.value === log.id ? null : log.id
}
/** IPv6 回环统一显示为 127.0.0.1，其余原样 */
function prettyIp(ip: string) {
  if (!ip) return '-'
  if (ip === '::1' || ip === '0:0:0:0:0:0:0:1' || ip === '0:0:0:0:0:0:0:0') return '127.0.0.1'
  return ip
}
const logsPage = ref(1); const logsPageSize = ref(10); const logsTotal = ref(0)

function formatTime(t: string) {
  if (!t) return '-'
  const d = new Date(t)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function truncateUA(ua: string) {
  if (!ua) return ''
  // 截短显示：浏览器优先识别 Edg/OPR，其次 Chrome/Safari/Firefox
  const browser = ua.match(/(Edg|OPR|Chrome|Safari|Firefox)\/[\d.]+/)
  // 操作系统：Win11 的 UA 仍标记为 Windows NT 10.0，统一友好显示
  let os = ua.match(/\((Windows NT|Macintosh|Linux|X11)[^)]*\)/)
  let osLabel = ''
  if (os) {
    const raw = os[0]
    if (raw.startsWith('(Windows NT 10.0')) osLabel = 'Windows 10/11'
    else if (raw.startsWith('(Windows')) osLabel = 'Windows'
    else if (raw.startsWith('(Macintosh')) osLabel = 'macOS'
    else if (raw.startsWith('(Linux')) osLabel = 'Linux'
    else osLabel = raw.slice(1, -1)
  }
  const b = browser?.[0] || ''
  return [osLabel, b].filter(Boolean).join(' · ') || ua.slice(0, 60)
}

async function loadUser() {
  loading.value = true
  try {
    const res = await fetchGetUserInfo()
    if (res) {
      userInfo.userId = res.userId
      userInfo.username = res.username || ''
      userInfo.displayName = res.displayName || ''
      userInfo.role = res.role || ''
    }
  } finally { loading.value = false }
}

async function doSave() {
  saving.value = true
  try {
    await request.put({ url: `/api/v1/admin/users/${userInfo.userId}`, data: { displayName: userInfo.displayName } })
    userStore.setUserInfo({ ...userStore.info, displayName: userInfo.displayName } as any)
    ElMessage.success('保存成功')
  } catch (e: any) { ElMessage.error('保存失败: ' + (e?.message || '')) }
  finally { saving.value = false }
}

async function doChangePwd() {
  if (!pwdFormRef.value) return
  try { await pwdFormRef.value.validate() } catch { return }
  changingPwd.value = true
  try {
    await request.patch({ url: '/api/v1/auth/me/password', data: { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword } })
    ElMessage.success('密码已更新，请使用新密码重新登录')
    pwdForm.oldPassword = ''; pwdForm.newPassword = ''; pwdForm.confirmPassword = ''
    // 修改密码后 token 可能失效，跳转登录
    setTimeout(() => userStore.logOut(), 1500)
  } catch (e: any) {
    const msg = e?.message?.includes('原密码') ? e.message : '密码修改失败: ' + (e?.message || '')
    ElMessage.error(msg)
  }
  finally { changingPwd.value = false }
}

async function loadLogs() {
  logsLoading.value = true
  try {
    const res: any = await request.get({ url: '/api/v1/auth/me/login-logs', params: { page: logsPage.value - 1, size: logsPageSize.value } })
    logs.value = res?.records || []
    logsTotal.value = res?.total || 0
  } catch {} finally { logsLoading.value = false }
}

onMounted(() => { loadUser(); loadLogs() })
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

/* ═══ 布局 ═══ */
.layout-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; align-items: start; }
.col-left { display: flex; flex-direction: column; gap: 20px; }
@media (max-width: 960px) { .layout-grid { grid-template-columns: 1fr; } }

/* ═══ 卡片外框 ═══ */
.section-outer { padding: 1.5px; border-radius: 14px; background: rgba(0,0,0,.025); }
.section-inner { border-radius: calc(14px - 1.5px); background: var(--default-box-color); padding: 24px; border: 1px solid var(--default-border); }

/* ═══ Section 头部 ═══ */
.section-head { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid var(--default-border); }
.head-icon { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.section-title { font-size: 15px; font-weight: 700; color: #323251; margin: 0; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; }
.section-desc { font-size: 12px; color: #949eb7; margin: 2px 0 0; }

/* ═══ 表单 ═══ */
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

/* ═══ Role tag ═══ */
.role-tag { display: inline-flex; padding: 6px 12px; border-radius: 8px; font-size: 12px; font-weight: 600; width: fit-content; height: fit-content; }
.role-admin { background: rgba(37,99,235,.1); color: #2563EB; }
.role-user { background: rgba(19,222,185,.1); color: #13DEB9; }

/* ═══ 登录历史 ═══ */
.logs-list { display: flex; flex-direction: column; gap: 10px; min-height: 200px; }
.log-row { display: flex; gap: 12px; padding: 11px 12px; border-radius: 10px; background: var(--default-bg-color, #fafbfc); transition: all .15s; cursor: pointer; }
.log-row:hover { background: var(--default-border); }
.log-row--expanded { background: var(--default-bg-color, #fafbfc); box-shadow: inset 3px 0 0 rgba(37,99,235,.45); }
.log-arrow { margin-left: auto; font-size: 10px; color: #dbdfe1; transition: transform .18s; transform: rotate(90deg); }
.log-arrow--open { transform: rotate(90deg); }
.log-arrow:not(.log-arrow--open) { transform: rotate(0deg); }
.log-icon { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.log-ok { background: rgba(19,222,185,.12); color: #13DEB9; }
.log-fail { background: rgba(255,77,79,.12); color: #FF4D4F; }
.log-content { flex: 1; min-width: 0; }
.log-status { display: flex; align-items: center; font-size: 13px; font-weight: 600; }
.status-ok { color: #13DEB9; }
.status-fail { color: #FF4D4F; }
.log-reason { color: #949eb7; font-weight: 400; margin-left: 4px; }
.log-meta { display: flex; gap: 6px; font-size: 11px; color: #949eb7; margin-top: 4px; }
.meta-sep { color: #e6eaeb; }
.log-detail { display: flex; gap: 6px; flex-wrap: wrap; font-size: 11px; color: #949eb7; margin-top: 6px; padding-top: 6px; border-top: 1px dashed var(--default-border); font-family: 'JetBrains Mono', monospace; }
.empty { padding: 40px 0; text-align: center; color: #dbdfe1; font-size: 13px; }
.pager { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>