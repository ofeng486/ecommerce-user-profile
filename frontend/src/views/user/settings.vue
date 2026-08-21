<!-- 个人中心 — 头像大卡 + 三列布局 + 右侧栏（账号安全/快捷入口） -->
<template>
  <div class="settings-view" v-loading="loading">
    <header class="page-header">
      <div class="ph-left">
        <div class="ph-title-row">
          <span class="title-accent"></span>
          <h1 class="page-title">个人中心</h1>
          <span class="title-tag">PROFILE</span>
        </div>
        <p class="page-desc">管理账号信息、修改密码、查看最近登录活动。</p>
      </div>
    </header>

    <div class="sk-body">
      <div class="sk-main">
        <!-- 头像大卡（横跨整宽） -->
        <div class="profile-hero">
          <div class="ph-avatar"><span>{{ avatarLetter }}</span></div>
          <div class="ph-info">
            <div class="ph-name">{{ userStore.info?.displayName || userStore.info?.username || '—' }}</div>
            <div class="ph-username">@{{ userStore.info?.username || '—' }}</div>
            <div class="ph-tags">
              <span class="ph-role" :class="isAdmin ? 'ph-role--admin' : 'ph-role--user'">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M9 12l2 2 4-4M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                {{ isAdmin ? '系统管理员' : '运营分析员' }}
              </span>
              <span class="ph-meta">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                最近登录 {{ lastLoginTime }}
              </span>
            </div>
          </div>
        </div>

        <!-- 账号信息 + 修改密码 两列 -->
        <div class="sk-row">
          <section class="card-outer"><div class="card-inner">
            <div class="card-head">
              <div class="card-icon" style="background:#f0f9ff;color:#0d9488">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </div>
              <div>
                <h2 class="card-title">账号信息</h2>
                <p class="card-desc">账号基本信息，登录后无法修改</p>
              </div>
            </div>
            <dl class="info-list">
              <div class="info-row"><dt>账号ID</dt><dd class="info-mono">#{{ userStore.info?.userId || 1 }}</dd></div>
              <div class="info-row">
                <dt>显示名称</dt>
                <dd class="info-editable">
                  <el-input v-if="editingName" v-model="newName" size="small" maxlength="50" @keydown.enter="saveName" @keydown.esc="cancelEdit" />
                  <span v-else>{{ userStore.info?.displayName || '—' }}</span>
                  <button v-if="!editingName" class="info-edit-btn" @click="startEditName" title="修改显示名称">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                  </button>
                  <span v-if="editingName" class="info-edit-actions">
                    <button class="info-edit-btn info-edit-btn--ok" @click="saveName" title="保存">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><polyline points="20 6 9 17 4 12"/></svg>
                    </button>
                    <button class="info-edit-btn info-edit-btn--cancel" @click="cancelEdit" title="取消">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    </button>
                  </span>
                </dd>
              </div>
              <div class="info-row"><dt>用户名</dt><dd>{{ userStore.info?.username || '—' }}</dd></div>
              <div class="info-row"><dt>角色</dt><dd><span class="role-tag" :class="isAdmin ? 'role-admin' : 'role-user'">{{ isAdmin ? '系统管理员' : '运营分析员' }}</span></dd></div>
              <div class="info-row"><dt>账号状态</dt><dd><span class="status-tag"><span class="status-dot"></span>正常</span></dd></div>
              <div class="info-row"><dt>注册时间</dt><dd>{{ registerTime }}</dd></div>
            </dl>
          </div></section>

          <section class="card-outer"><div class="card-inner">
            <div class="card-head">
              <div class="card-icon" style="background:#fef3c7;color:#d97706">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
              </div>
              <div>
                <h2 class="card-title">修改密码</h2>
                <p class="card-desc">每 3 个月换一次更安全</p>
              </div>
            </div>
            <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="80px" class="pwd-form">
              <el-form-item label="当前密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位" />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="changingPwd" @click="changePassword" round>修改密码</el-button>
              </el-form-item>
            </el-form>
          </div></section>
        </div>

        <!-- 活动记录（横跨） -->
        <section class="card-outer"><div class="card-inner card-inner--full">
          <div class="card-head">
            <div class="card-icon" style="background:#ecfdf5;color:#059669">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            </div>
            <div>
              <h2 class="card-title">最近活动</h2>
              <p class="card-desc">查看最近的登录记录，异常情况请尽快修改密码</p>
            </div>
          </div>
          <div v-if="activities.length === 0" class="act-empty">暂无活动记录</div>
          <div v-else class="act-timeline">
            <div v-for="(act, i) in activities" :key="i" class="act-item">
              <div class="act-dot"></div>
              <div class="act-body">
                <span class="act-desc">{{ act.desc }}</span>
                <span class="act-time">{{ act.time }}</span>
              </div>
            </div>
          </div>
        </div></section>
      </div>

      <!-- 右侧栏：账号安全 + 快捷入口 -->
      <aside class="sk-aside">
        <section class="card-outer"><div class="card-inner">
          <div class="card-head">
            <div class="card-icon" style="background:#fef2f2;color:#dc2626">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            </div>
            <div>
              <h2 class="card-title">账号安全</h2>
              <p class="card-desc">基本防护一览</p>
            </div>
          </div>
          <ul class="safety-list">
            <li class="safety-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><polyline points="20 6 9 17 4 12"/></svg>
              <span>密码已设置</span>
            </li>
            <li class="safety-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><polyline points="20 6 9 17 4 12"/></svg>
              <span>近 7 天登录活跃</span>
            </li>
            <li class="safety-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><polyline points="20 6 9 17 4 12"/></svg>
              <span>实名运营账号</span>
            </li>
          </ul>
        </div></section>

        <section class="card-outer"><div class="card-inner">
          <div class="card-head">
            <div class="card-icon" style="background:#eff6ff;color:#0D9488">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            </div>
            <div>
              <h2 class="card-title">快捷入口</h2>
              <p class="card-desc">常用页面直达</p>
            </div>
          </div>
          <ul class="quick-list">
            <li class="quick-item" @click="goTo('/user/cluster-analysis')">
              <svg class="quick-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
              <div class="quick-meta">
                <div class="quick-title">用户聚类</div>
                <div class="quick-desc">查看最新分组结果</div>
              </div>
            </li>
            <li class="quick-item" @click="goTo('/user/overview')">
              <svg class="quick-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/></svg>
              <div class="quick-meta">
                <div class="quick-title">画像概览</div>
                <div class="quick-desc">核心指标一览</div>
              </div>
            </li>
            <li class="quick-item" @click="goTo('/user/ai')">
              <svg class="quick-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z"/></svg>
              <div class="quick-meta">
                <div class="quick-title">AI 数据分析</div>
                <div class="quick-desc">自然语言查询数据</div>
              </div>
            </li>
            <li class="quick-item" @click="goTo('/user/notifications')">
              <svg class="quick-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
              <div class="quick-meta">
                <div class="quick-title">通知中心</div>
                <div class="quick-desc">查看任务与系统消息</div>
              </div>
            </li>
          </ul>
        </div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import request from '@/utils/http'

defineOptions({ name: 'UserSettings' })

const router = useRouter()
const userStore = useUserStore()
const isAdmin = computed(() => (userStore.info?.role || '').toUpperCase() === 'ADMIN')
const avatarLetter = computed(() => (userStore.info?.displayName || userStore.info?.username || 'U').charAt(0).toUpperCase())
const loading = ref(false)
const registerTime = ref('—')
const lastLoginTime = ref('—')
const activities = ref<any[]>([])

const pwdFormRef = ref<FormInstance>()
const changingPwd = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (_: any, v: string, cb: Function) => v !== pwdForm.value.newPassword ? cb(new Error('两次密码不一致')) : cb(), trigger: 'blur' }
  ]
}

// 显示名称内联编辑
const editingName = ref(false)
const newName = ref('')
function startEditName() {
  newName.value = userStore.info?.displayName || ''
  editingName.value = true
}
function cancelEdit() { editingName.value = false }
async function saveName() {
  const name = newName.value.trim()
  if (!name) { ElMessage.warning('显示名称不能为空'); return }
  if (name === userStore.info?.displayName) { editingName.value = false; return }
  try {
    await request.patch({ url: '/api/v1/auth/me/display-name', data: { displayName: name } })
    if (userStore.info) userStore.info.displayName = name
    ElMessage.success('显示名称已更新')
    editingName.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '更新失败')
  }
}

async function changePassword() {
  if (!pwdFormRef.value) return
  try {
    const valid = await pwdFormRef.value.validate()
    if (!valid) return
    changingPwd.value = true
    await request.patch({
      url: '/api/v1/auth/me/password',
      data: { oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword }
    })
    ElMessage.success('密码修改成功')
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e: any) {
    if (e?.message !== 'validate error!') ElMessage.error(e?.message || '修改失败')
  } finally { changingPwd.value = false }
}

function fmt(t: string) {
  if (!t) return '—'
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

function goTo(path: string) { router.push(path) }

onMounted(async () => {
  loading.value = true
  try {
    const logRes = await request.get<any>({
      url: '/api/v1/auth/me/login-logs',
      params: { page: 0, size: 5 }
    })
    if (logRes) {
      const logs = (logRes as any).records || []
      if (logs.length > 0) lastLoginTime.value = fmt(logs[0].loginAt)
      activities.value = logs.map((l: any) => ({
        desc: `登录系统（${Number(l.loginResult) === 1 ? '成功' : '失败'}）`,
        time: fmt(l.loginAt)
      }))
    }
  } catch {} finally { loading.value = false }
})
</script>

<style scoped>
.settings-view {
  font-family: var(--font-body, 'Inter', 'PingFang SC', system-ui, sans-serif);
}

/* ─── Page Header ─── */
.page-header {
  display: flex; align-items: flex-end; justify-content: space-between; gap: 20px;
  margin-bottom: 20px; padding-bottom: 18px;
  border-bottom: 1px solid #eef2f6;
}
.ph-left { min-width: 0; }
.ph-title-row { display: flex; align-items: center; gap: 10px; }
.title-accent {
  width: 4px; height: 20px; border-radius: 2px; flex-shrink: 0;
  background: linear-gradient(180deg, #0d9488 0%, #2dd4bf 100%);
}
.page-title {
  font-size: 22px; font-weight: 700; color: #0f172a; margin: 0;
  font-family: var(--font-display, 'Plus Jakarta Sans', 'Inter', 'PingFang SC', sans-serif);
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

/* ─── 三列布局：主区 + 右侧栏 ─── */
.sk-body {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 16px;
  align-items: start;
}
@media (max-width: 1100px) {
  .sk-body { grid-template-columns: 1fr; }
  .sk-aside { display: none; }
}
.sk-main { min-width: 0; display: flex; flex-direction: column; gap: 16px; }
.sk-aside { display: flex; flex-direction: column; gap: 16px; position: sticky; top: 16px; }

/* ─── 头像大卡（横跨整宽） ─── */
.profile-hero {
  display: flex; align-items: center; gap: 20px;
  padding: 22px 28px;
  background: linear-gradient(135deg, #f0fdfa 0%, #ffffff 60%, #ecfeff 100%);
  border: 1px solid #99f6e4;
  border-radius: 16px;
  position: relative; overflow: hidden;
}
.profile-hero::before {
  content: ''; position: absolute; right: -40px; top: -40px;
  width: 180px; height: 180px; border-radius: 50%;
  background: radial-gradient(circle, rgba(45, 212, 191, 0.12) 0%, transparent 70%);
  pointer-events: none;
}
.ph-avatar {
  width: 64px; height: 64px; border-radius: 50%;
  background: linear-gradient(135deg, #0d9488 0%, #14b8a6 100%);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 26px; font-weight: 700; flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(13, 148, 136, 0.25);
}
.ph-info { flex: 1; min-width: 0; }
.ph-name { font-size: 18px; font-weight: 600; color: #0f172a; line-height: 1.3; }
.ph-username { font-size: 13px; color: #64748b; margin-top: 4px; font-family: 'JetBrains Mono', monospace; }
.ph-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.ph-role {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11px; font-weight: 500; padding: 3px 10px;
  border-radius: 12px; letter-spacing: 0.2px;
}
.ph-role--admin { background: #ecfeff; color: #0d9488; border: 1px solid #99f6e4; }
.ph-role--user  { background: #ecfdf5; color: #059669; border: 1px solid #a7f3d0; }
.ph-meta {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11px; color: #64748b; padding: 3px 10px;
  border-radius: 12px; background: rgba(241, 245, 249, 0.7);
}

/* ─── 账号信息 + 修改密码 两列 ─── */
.sk-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; align-items: start; }
@media (max-width: 768px) { .sk-row { grid-template-columns: 1fr; } }

/* ─── 光晕卡片 ─── */
.card-outer { padding: 6px; border-radius: 20px; background: linear-gradient(180deg, #ffffff, #f0f5f3); border: 1px solid rgba(15, 23, 42, 0.06); box-shadow: 0 8px 28px rgba(13, 148, 136, 0.05), 0 2px 8px rgba(15, 23, 42, 0.03); }
.card-inner {
  border-radius: calc(16px - 1.5px);
  background: #ffffff;
  padding: 22px 24px;
  border: 1px solid #eef2f6;
  transition: all 0.2s;
}
.card-inner--full { padding: 22px 28px; }
.card-outer:hover .card-inner { border-color: #d4d4d8; box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04); }

.card-head { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 18px; }
.card-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.card-title { font-size: 15px; font-weight: 600; color: #0f172a; margin: 0; line-height: 1.3; }
.card-desc { font-size: 12px; color: #94a3b8; margin: 2px 0 0; line-height: 1.5; }

/* ─── Info ─── */
.info-list { display: flex; flex-direction: column; gap: 12px; }
.info-row { display: flex; align-items: center; gap: 16px; }
.info-row dt { font-size: 13px; color: #94a3b8; min-width: 72px; }
.info-row dd { font-size: 14px; color: #1e293b; margin: 0; }
.info-mono { font-family: 'JetBrains Mono', monospace; color: #64748b; font-size: 13px; }

/* 可编辑显示名称 */
.info-editable { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; }
.info-editable :deep(.el-input) { width: 180px; }
.info-edit-actions { display: inline-flex; gap: 4px; }
.info-edit-btn {
  width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center;
  border: none; background: transparent; border-radius: 5px;
  color: #94a3b8; cursor: pointer; transition: all 0.12s;
}
.info-edit-btn:hover { background: #f1f5f9; color: #0d9488; }
.info-edit-btn--ok:hover { background: #ecfdf5; color: #059669; }
.info-edit-btn--cancel:hover { background: #fef2f2; color: #dc2626; }

/* 角色 + 状态小徽章 */
.role-tag {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 12px; font-weight: 500; padding: 2px 10px; border-radius: 10px;
}
.role-admin { background: #ecfeff; color: #0d9488; border: 1px solid #99f6e4; }
.role-user  { background: #ecfdf5; color: #059669; border: 1px solid #a7f3d0; }
.status-tag {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; color: #059669; font-weight: 500;
}
.status-dot {
  width: 6px; height: 6px; background: #10b981; border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.15);
}

/* ─── Password ─── */
.pwd-form { max-width: 360px; }
.pwd-form :deep(.el-form-item) { margin-bottom: 16px; }
.pwd-form :deep(.el-input__wrapper) { border-radius: 10px; }

/* ─── Activities ─── */
.act-empty { text-align: center; padding: 24px 0; color: #94a3b8; font-size: 14px; }
.act-timeline { display: flex; flex-direction: column; gap: 0; }
.act-item {
  display: flex; gap: 14px; padding: 12px 0;
  border-left: 2px solid #e2e8f0; margin-left: 4px; padding-left: 20px;
  position: relative;
}
.act-item:last-child { border-left-color: transparent; }
.act-dot {
  position: absolute; left: -5px; top: 16px;
  width: 8px; height: 8px; background: #0d9488; border-radius: 50%;
  border: 2px solid #fff;
}
.act-body { display: flex; flex-direction: column; gap: 2px; }
.act-desc { font-size: 14px; color: #1e293b; }
.act-time { font-size: 12px; color: #94a3b8; }

/* ─── 账号安全列表 ─── */
.safety-list { display: flex; flex-direction: column; gap: 10px; }
.safety-item {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #475569;
  padding: 6px 0;
}
.safety-item svg { color: #10b981; flex-shrink: 0; }

/* ─── 快捷入口 ─── */
.quick-list { display: flex; flex-direction: column; gap: 2px; }
.quick-item {
  display: flex; align-items: center; gap: 10px; padding: 8px 6px;
  border-radius: 8px; cursor: pointer; transition: background 0.12s;
}
.quick-item:hover { background: #f8fafc; }
.quick-ico { width: 18px; height: 18px; color: #0d9488; flex-shrink: 0; }
.quick-meta { flex: 1; min-width: 0; }
.quick-title { font-size: 13px; font-weight: 500; color: #334155; line-height: 1.3; }
.quick-desc { font-size: 11px; color: #94a3b8; margin-top: 2px; }
</style>
