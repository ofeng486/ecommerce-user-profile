<template>
  <AuthLayout>
    <h3 class="title">{{ $t('forgetPassword.title') }}</h3>
    <p class="sub-title">{{ $t('forgetPassword.subTitle') }}</p>
    <div class="mt-5">
      <span class="input-label" v-if="showInputLabel">账号</span>
      <ElInput
        class="custom-height"
        :placeholder="$t('forgetPassword.placeholder')"
        v-model.trim="username"
      />
    </div>

    <div style="margin-top: 15px">
      <ElButton
        class="w-full custom-height"
        type="primary"
        @click="register"
        :loading="loading"
        v-ripple
      >
        {{ $t('forgetPassword.submitBtnText') }}
      </ElButton>
    </div>

    <div style="margin-top: 15px">
      <ElButton class="w-full custom-height" plain @click="toLogin">
        {{ $t('forgetPassword.backBtnText') }}
      </ElButton>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
  import { ElMessage } from 'element-plus'

  defineOptions({ name: 'ForgetPassword' })

  const router = useRouter()
  const showInputLabel = ref(false)

  const username = ref('')
  const loading = ref(false)

  const register = async () => {
    // 验证账号不能为空
    const trimmed = username.value.trim()
    if (!trimmed) {
      ElMessage.warning('请输入您的账号')
      return
    }

    loading.value = true
    try {
      // 尝试使用错误密码登录来验证账号是否存在
      const { fetchLogin } = await import('@/api/auth')
      await fetchLogin({ username: trimmed, password: '__reset_check__' })
      // 如果登录成功（不应该发生），提示联系管理员
      ElMessage.info('请联系管理员重置密码')
    } catch (e: any) {
      // 如果返回的是"用户不存在"，提示账号不存在
      const msg = e?.message || ''
      if (msg.includes('不存在') || msg.includes('not found') || msg.includes('未注册')) {
        ElMessage.error('该账号不存在，请检查后重试')
      } else {
        // 账号存在（密码错误），显示重置指引
        ElMessage.success('账号验证通过！请联系管理员进行密码重置。')
        setTimeout(() => {
          router.push({ name: 'Login' })
        }, 2000)
      }
    } finally {
      loading.value = false
    }
  }

  const toLogin = () => {
    router.push({ name: 'Login' })
  }
</script>

<style scoped>
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
  .custom-height {
    height: 44px !important;
  }
  /* 输入框 focus 光环（品牌蓝） */
  .custom-height :deep(.el-input__wrapper) {
    border-radius: 10px;
    box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.08) inset;
    transition: box-shadow 0.25s cubic-bezier(0.32, 0.72, 0, 1);
    background: #fafbfc;
  }
  .custom-height :deep(.el-input__wrapper.is-focus) {
    box-shadow:
      0 0 0 1.5px #2563eb inset,
      0 0 0 4px rgba(37, 99, 235, 0.08) !important;
    background: #fff;
  }
  .custom-height :deep(.el-input__inner) {
    font-family: 'Plus Jakarta Sans', 'Inter', 'PingFang SC', sans-serif;
  }
  /* 主按钮渐变胶囊 */
  .w-full.custom-height.el-button--primary {
    border-radius: 999px !important;
    border: none !important;
    background: linear-gradient(135deg, #2563eb, #1e40af) !important;
    font-weight: 600 !important;
    letter-spacing: 0.06em !important;
    box-shadow: 0 6px 18px rgba(37, 99, 235, 0.18) !important;
    transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1) !important;
  }
  .w-full.custom-height.el-button--primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 10px 26px rgba(37, 99, 235, 0.26) !important;
  }
  .w-full.custom-height.el-button--primary.is-plain {
    background: #fff !important;
    color: #2563eb !important;
    border: 1.5px solid rgba(37, 99, 235, 0.25) !important;
    box-shadow: none !important;
  }
  .w-full.custom-height.el-button--primary.is-plain:hover {
    background: #eff6ff !important;
  }
</style>
