<!-- 登录页面 -->
<template>
  <div class="flex w-full h-screen">
    <LoginLeftView />
    <div class="relative flex-1">
      <AuthTopBar />
      <div class="auth-right-wrap">
        <div class="form">
          <h3 class="title">电商用户画像分析系统</h3>
          <p class="sub-title">登录您的账号</p>
          <ElForm ref="formRef" :model="formData" :rules="rules" @keyup.enter="handleSubmit" style="margin-top: 25px">
            <ElFormItem prop="username">
              <ElInput class="custom-height" placeholder="用户名" v-model.trim="formData.username" />
            </ElFormItem>
            <ElFormItem prop="password">
              <ElInput class="custom-height" placeholder="密码" v-model.trim="formData.password" type="password" autocomplete="off" show-password />
            </ElFormItem>
            <div style="margin-top: 30px">
              <ElButton class="w-full custom-height" type="primary" @click="handleSubmit" :loading="loading" v-ripple>登 录</ElButton>
            </div>
          </ElForm>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/modules/user'
import { fetchLogin } from '@/api/auth'
import { type FormInstance, type FormRules } from 'element-plus'

defineOptions({ name: 'Login' })
const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const loading = ref(false)

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
    const { accessToken } = await fetchLogin({ username: formData.username, password: formData.password })
    if (!accessToken) throw new Error('登录失败')
    userStore.setToken(accessToken)
    userStore.setLoginStatus(true)
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
  } catch (e: any) {
    console.error('登录失败:', e)
  } finally { loading.value = false }
}
</script>

<style scoped>
@import './style.css';
</style>
