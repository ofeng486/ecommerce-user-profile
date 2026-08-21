<!-- 注册页面 -->
<template>
  <AuthLayout>
    <h3 class="title">{{ $t('register.title') }}</h3>
    <p class="sub-title">{{ $t('register.subTitle') }}</p>
    <ElForm
      class="mt-7.5"
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      :key="formKey"
    >
      <ElFormItem prop="username">
        <ElInput
          class="custom-height"
          v-model.trim="formData.username"
          :placeholder="$t('register.placeholder.username')"
        />
      </ElFormItem>

      <ElFormItem prop="password">
        <ElInput
          class="custom-height"
          v-model.trim="formData.password"
          :placeholder="$t('register.placeholder.password')"
          type="password"
          autocomplete="off"
          show-password
        />
      </ElFormItem>

      <ElFormItem prop="confirmPassword">
        <ElInput
          class="custom-height"
          v-model.trim="formData.confirmPassword"
          :placeholder="$t('register.placeholder.confirmPassword')"
          type="password"
          autocomplete="off"
          @keyup.enter="register"
          show-password
        />
      </ElFormItem>

      <ElFormItem prop="agreement">
        <ElCheckbox v-model="formData.agreement">
          {{ $t('register.agreeText') }}
          <RouterLink
            style="color: var(--theme-color); text-decoration: none"
            to="/privacy-policy"
            >{{ $t('register.privacyPolicy') }}</RouterLink
          >
        </ElCheckbox>
      </ElFormItem>

      <div style="margin-top: 15px">
        <ElButton
          class="w-full custom-height"
          type="primary"
          @click="register"
          :loading="loading"
          v-ripple
        >
          {{ $t('register.submitBtnText') }}
        </ElButton>
      </div>

      <div class="mt-5 text-sm text-slate-500">
        <span>{{ $t('register.hasAccount') }}</span>
        <RouterLink class="text-theme" :to="{ name: 'Login' }">{{
          $t('register.toLogin')
        }}</RouterLink>
      </div>
    </ElForm>
  </AuthLayout>
</template>

<script setup lang="ts">
  import { useI18n } from 'vue-i18n'
  import type { FormInstance, FormRules } from 'element-plus'
  import { fetchRegister } from '@/api/auth'

  defineOptions({ name: 'Register' })

  interface RegisterForm {
    username: string
    password: string
    confirmPassword: string
    agreement: boolean
  }

  const USERNAME_MIN_LENGTH = 3
  const USERNAME_MAX_LENGTH = 20
  const PASSWORD_MIN_LENGTH = 6
  const REDIRECT_DELAY = 1000

  const { t, locale } = useI18n()
  const router = useRouter()
  const formRef = ref<FormInstance>()

  const loading = ref(false)
  const formKey = ref(0)

  // 监听语言切换，重置表单
  watch(locale, () => {
    formKey.value++
  })

  const formData = reactive<RegisterForm>({
    username: '',
    password: '',
    confirmPassword: '',
    agreement: false
  })

  /**
   * 验证密码
   * 当密码输入后，如果确认密码已填写，则触发确认密码的验证
   */
  const validatePassword = (_rule: any, value: string, callback: (error?: Error) => void) => {
    if (!value) {
      callback(new Error(t('register.placeholder.password')))
      return
    }

    if (formData.confirmPassword) {
      formRef.value?.validateField('confirmPassword')
    }

    callback()
  }

  /**
   * 验证确认密码
   * 检查确认密码是否与密码一致
   */
  const validateConfirmPassword = (
    _rule: any,
    value: string,
    callback: (error?: Error) => void
  ) => {
    if (!value) {
      callback(new Error(t('register.rule.confirmPasswordRequired')))
      return
    }

    if (value !== formData.password) {
      callback(new Error(t('register.rule.passwordMismatch')))
      return
    }

    callback()
  }

  /**
   * 验证用户协议
   * 确保用户已勾选同意协议
   */
  const validateAgreement = (_rule: any, value: boolean, callback: (error?: Error) => void) => {
    if (!value) {
      callback(new Error(t('register.rule.agreementRequired')))
      return
    }
    callback()
  }

  const rules = computed<FormRules<RegisterForm>>(() => ({
    username: [
      { required: true, message: t('register.placeholder.username'), trigger: 'blur' },
      {
        min: USERNAME_MIN_LENGTH,
        max: USERNAME_MAX_LENGTH,
        message: t('register.rule.usernameLength'),
        trigger: 'blur'
      }
    ],
    password: [
      { required: true, validator: validatePassword, trigger: 'blur' },
      { min: PASSWORD_MIN_LENGTH, message: t('register.rule.passwordLength'), trigger: 'blur' }
    ],
    confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }],
    agreement: [{ validator: validateAgreement, trigger: 'change' }]
  }))

  /**
   * 注册用户
   * 验证表单后提交注册请求
   */
  const register = async () => {
    if (!formRef.value) return

    try {
      await formRef.value.validate()
      loading.value = true

      // 调用真实注册 API
      await fetchRegister({
        username: formData.username,
        password: formData.password,
        displayName: formData.username
      })
      ElMessage.success('注册成功')
      toLogin()
    } catch (error: any) {
      console.error('注册失败:', error)
      loading.value = false
      if (error?.message && error?.message !== 'validate error!') {
        ElMessage.error(error.message)
      }
    }
  }

  /**
   * 跳转到登录页面
   */
  const toLogin = () => {
    setTimeout(() => {
      router.push({ name: 'Login' })
    }, REDIRECT_DELAY)
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
