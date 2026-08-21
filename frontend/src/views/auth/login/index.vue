<!-- 登录页面（亮色 Soft Structuralism：角色选择 + 品牌表单体系） -->
<template>
  <AuthLayout>
    <div class="login-box">
      <!-- 双角色选择 -->
      <div class="role-switch" role="group" aria-label="选择登录门户">
        <button
          type="button"
          class="role-card"
          :class="{ active: selectedRole === 'Admin' }"
          :style="selectedRole === 'Admin' ? adminAcc : {}"
          @click="selectedRole = 'Admin'"
        >
          <span class="role-icon admin">
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z" />
            </svg>
          </span>
          <span class="role-text">
            <span class="role-name">管理员</span>
            <span class="role-sub">控制台</span>
          </span>
          <span class="role-check" aria-hidden="true">
            <svg
              width="10"
              height="10"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="3.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </span>
        </button>

        <button
          type="button"
          class="role-card"
          :class="{ active: selectedRole === 'User' }"
          :style="selectedRole === 'User' ? userAcc : {}"
          @click="selectedRole = 'User'"
        >
          <span class="role-icon user">
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </span>
          <span class="role-text">
            <span class="role-name">运营分析员</span>
            <span class="role-sub">门户</span>
          </span>
          <span class="role-check" aria-hidden="true">
            <svg
              width="10"
              height="10"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="3.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </span>
        </button>
      </div>

      <h3 class="title">登录您的账号</h3>
      <p class="sub-title">欢迎回来，请登录以继续</p>

      <ElForm
        ref="formRef"
        :model="formData"
        :rules="rules"
        @keyup.enter="handleSubmit"
        class="login-form"
      >
        <ElFormItem prop="username">
          <ElInput
            class="custom-height"
            placeholder="用户名"
            v-model.trim="formData.username"
            :style="accStyle"
          />
        </ElFormItem>
        <ElFormItem prop="password">
          <ElInput
            class="custom-height"
            placeholder="密码"
            v-model.trim="formData.password"
            type="password"
            autocomplete="off"
            show-password
            :style="accStyle"
          />
        </ElFormItem>
        <div class="submit-wrap">
          <ElButton
            class="w-full custom-height submit-btn"
            type="primary"
            @click="handleSubmit"
            :loading="loading"
            :style="accStyle"
          >
            {{ submitText }}
          </ElButton>
        </div>
        <div class="register-link">
          没有账号？<router-link to="/auth/register" class="reg-link">去注册</router-link>
        </div>
      </ElForm>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
  import { ref, reactive, computed, onMounted } from 'vue'
  import { useUserStore } from '@/store/modules/user'
  import { fetchLogin } from '@/api/auth'
  import { type FormInstance, type FormRules } from 'element-plus'

  defineOptions({ name: 'Login' })
  const userStore = useUserStore()
  const router = useRouter()
  const route = useRoute()
  const formRef = ref<FormInstance>()
  const loading = ref(false)

  /** 登录门户偏好（仅前端跳转偏好；实际鉴权以服务端返回 role 为准） */
  const selectedRole = ref<'Admin' | 'User' | null>(null)

  // 从 landing「选择你的入口」进入时自动预选角色（?role=Admin / ?role=User）
  onMounted(() => {
    const role = route.query.role as string
    if (role === 'Admin' || role === 'User') {
      selectedRole.value = role
    }
  })

  const adminAcc = {
    '--acc': '#2563EB',
    '--acc-dark': '#1E40AF',
    '--acc-soft': 'rgba(37, 99, 235, 0.08)',
    '--acc-line': 'rgba(37, 99, 235, 0.3)'
  }
  const userAcc = {
    '--acc': '#0D9488',
    '--acc-dark': '#0F766E',
    '--acc-soft': 'rgba(13, 148, 136, 0.08)',
    '--acc-line': 'rgba(13, 148, 136, 0.3)'
  }
  /** 当前选中角色的主题变量（未选择时默认品牌蓝） */
  const accStyle = computed(() => (selectedRole.value === 'User' ? userAcc : adminAcc))

  const submitText = computed(() => {
    if (selectedRole.value === 'Admin') return '登录进入管理员控制台'
    if (selectedRole.value === 'User') return '登录进入运营分析门户'
    return '登 录'
  })

  const formData = reactive({ username: '', password: '' })
  const rules: FormRules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
  }

  const handleSubmit = async () => {
    if (!formRef.value) return
    try {
      const valid = await formRef.value.validate()
      if (!valid) return
      loading.value = true
      const { accessToken } = await fetchLogin({
        username: formData.username,
        password: formData.password
      })
      if (!accessToken) throw new Error('登录失败')
      userStore.setToken(accessToken)
      userStore.setLoginStatus(true)
      // 获取用户信息（设置 role 用于路由权限判断）
      const { fetchGetUserInfo } = await import('@/api/auth')
      const userInfo = await fetchGetUserInfo()
      const normalizedRole = userInfo.role
        ? userInfo.role.charAt(0).toUpperCase() + userInfo.role.slice(1).toLowerCase()
        : ''
      userStore.setUserInfo({
        ...userInfo,
        role: normalizedRole,
        roles: normalizedRole ? [normalizedRole] : []
      })
      // 跳转优先级：redirect 参数 > 服务端实际角色 > 用户选择的门户偏好
      const redirect = route.query.redirect as string
      if (redirect) {
        router.push(redirect)
      } else if (
        normalizedRole === 'Admin' ||
        (selectedRole.value === 'Admin' && !normalizedRole)
      ) {
        router.push('/dashboard/overview')
      } else {
        router.push('/user/overview')
      }
    } catch (e: any) {
      console.error('登录失败:', e)
    } finally {
      loading.value = false
    }
  }
</script>

<style scoped>
  .login-box {
    animation: formRise 0.6s cubic-bezier(0.32, 0.72, 0, 1) 0.1s both;
  }

  /* ─── 双角色选择 ─── */
  .role-switch {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
    margin-bottom: 26px;
  }
  .role-card {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 14px;
    border-radius: 14px;
    border: 1.5px solid rgba(15, 23, 42, 0.07);
    background: #fafbfc;
    cursor: pointer;
    font-family: inherit;
    transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1);
    position: relative;
    text-align: left;
  }
  .role-card:hover {
    border-color: rgba(15, 23, 42, 0.14);
    transform: translateY(-1px);
  }
  .role-card.active {
    background: var(--acc-soft);
    border-color: var(--acc-line);
    box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
  }
  .role-icon {
    width: 32px;
    height: 32px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
  .role-icon.admin {
    background: linear-gradient(135deg, #2563eb, #1e40af);
    color: #fff;
  }
  .role-icon.user {
    background: linear-gradient(135deg, #0d9488, #0f766e);
    color: #fff;
  }
  .role-text {
    display: flex;
    flex-direction: column;
    gap: 1px;
    min-width: 0;
  }
  .role-name {
    font-size: 13px;
    font-weight: 600;
    color: #0f172a;
    white-space: nowrap;
  }
  .role-sub {
    font-size: 10.5px;
    color: #94a3b8;
  }
  .role-check {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    background: #e2e8f0;
    transition: all 0.25s cubic-bezier(0.32, 0.72, 0, 1);
    opacity: 0;
  }
  .role-card.active .role-check {
    opacity: 1;
    background: var(--acc);
  }

  /* ─── 标题 ─── */
  .title {
    font-size: 24px;
    font-weight: 700;
    color: #0f172a;
    margin: 0;
    font-family: 'Space Grotesk', 'Plus Jakarta Sans', 'PingFang SC', sans-serif;
    letter-spacing: -0.3px;
  }
  .sub-title {
    font-size: 13.5px;
    color: #94a3b8;
    margin-top: 6px;
  }

  /* ─── 表单 ─── */
  .login-form {
    margin-top: 24px;
  }
  .custom-height {
    height: 44px !important;
  }
  /* 输入框 focus 光环跟随角色主题色 */
  .custom-height :deep(.el-input__wrapper) {
    border-radius: 10px;
    box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.08) inset;
    transition: box-shadow 0.25s cubic-bezier(0.32, 0.72, 0, 1);
    background: #fafbfc;
  }
  .custom-height :deep(.el-input__wrapper.is-focus) {
    box-shadow:
      0 0 0 1.5px var(--acc) inset,
      0 0 0 4px var(--acc-soft) !important;
    background: #fff;
  }
  .custom-height :deep(.el-input__inner) {
    font-family: 'Plus Jakarta Sans', 'Inter', 'PingFang SC', sans-serif;
  }
  .submit-wrap {
    margin-top: 26px;
  }
  .submit-btn {
    border-radius: 999px !important;
    border: none !important;
    background: linear-gradient(135deg, var(--acc), var(--acc-dark)) !important;
    font-weight: 600 !important;
    letter-spacing: 0.06em !important;
    box-shadow: 0 6px 18px var(--acc-soft) !important;
    transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1) !important;
  }
  .submit-btn:hover {
    transform: translateY(-1px);
    box-shadow: 0 10px 26px var(--acc-soft) !important;
  }
  .submit-btn.is-loading {
    opacity: 0.7;
  }
  .register-link {
    margin-top: 18px;
    font-size: 13px;
    color: #94a3b8;
    text-align: center;
  }
  .reg-link {
    color: var(--acc);
    text-decoration: none;
    font-weight: 600;
    transition: color 0.2s cubic-bezier(0.32, 0.72, 0, 1);
  }
  .reg-link:hover {
    color: var(--acc-dark);
  }

  /* ─── 入场动效 ─── */
  @keyframes formRise {
    from {
      opacity: 0;
      transform: translateY(16px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  /* ─── 无障碍：减弱动效 ─── */
  @media (prefers-reduced-motion: reduce) {
    .login-box {
      animation: none;
    }
    .role-card,
    .custom-height :deep(.el-input__wrapper),
    .submit-btn,
    .reg-link {
      transition: none;
    }
  }
</style>
