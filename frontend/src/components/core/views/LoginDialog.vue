<template>
  <Teleport to="body">
    <Transition name="overlay-fade">
      <div v-if="modelValue" class="dialog-root" @click.self="close">
        <Transition name="card-rise" appear>
          <div v-if="modelValue" class="dialog-card-outer"><div class="dialog-card-inner">
            <!-- 关闭 -->
            <button @click="close" class="close-btn" aria-label="关闭">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>

            <!-- 品牌 + 标题 -->
            <div class="card-head">
              <div class="brand-mark">UP</div>
              <h2 class="title">{{ isRegister ? '创建账号' : '登录' }}</h2>
              <p class="subtitle">{{ isRegister ? '注册后即可使用全部功能' : '欢迎回到 UserProfile' }}</p>
            </div>

            <!-- 登录表单 -->
            <form v-if="!isRegister" @submit.prevent="handleLogin" class="form-body">
              <div class="field">
                <label class="field-label">用户名</label>
                <input v-model="loginForm.username" type="text" placeholder="输入用户名" class="field-input" autocomplete="username" />
              </div>
              <div class="field">
                <label class="field-label">密码</label>
                <div class="field-input-wrap">
                  <input v-model="loginForm.password" :type="showPwd ? 'text' : 'password'" placeholder="输入密码" class="field-input" autocomplete="current-password" @keydown.enter="handleLogin" />
                  <button type="button" @click="showPwd = !showPwd" class="field-action" tabindex="-1">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <template v-if="showPwd"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></template>
                      <template v-else><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></template>
                    </svg>
                  </button>
                </div>
              </div>
              <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
              <button type="submit" class="submit-btn" :disabled="loading">
                <span v-if="loading" class="spinner"></span>
                <span v-else>登 录</span>
              </button>
            </form>

            <!-- 注册表单 -->
            <form v-else @submit.prevent="handleRegister" class="form-body">
              <div class="field">
                <label class="field-label">显示名称</label>
                <input v-model="registerForm.displayName" type="text" placeholder="输入名称" class="field-input" />
              </div>
              <div class="field">
                <label class="field-label">用户名</label>
                <input v-model="registerForm.username" type="text" placeholder="字母数字下划线" class="field-input" autocomplete="username" />
              </div>
              <div class="field">
                <label class="field-label">密码</label>
                <input v-model="registerForm.password" type="password" placeholder="至少 8 位" class="field-input" autocomplete="new-password" />
              </div>
              <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
              <button type="submit" class="submit-btn" :disabled="loading">
                <span v-if="loading" class="spinner"></span>
                <span v-else>注 册</span>
              </button>
            </form>

            <!-- 底部切换 -->
            <p class="footer-text">
              {{ isRegister ? '已有账号？' : '没有账号？' }}
              <button type="button" @click="isRegister = !isRegister; errorMsg = ''" class="footer-link">{{ isRegister ? '去登录' : '去注册' }}</button>
            </p>
          </div></div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { fetchLogin, fetchRegister } from '@/api/auth'
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'LoginDialog' })
const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [val: boolean]; 'loginSuccess': [] }>()

const userStore = useUserStore()
const loading = ref(false)
const isRegister = ref(false)
const showPwd = ref(false)
const errorMsg = ref('')
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ displayName: '', username: '', password: '' })

function close() { emit('update:modelValue', false) }

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) { errorMsg.value = '请填写用户名和密码'; return }
  errorMsg.value = ''; loading.value = true
  try {
    const data = await fetchLogin({ username: loginForm.username, password: loginForm.password })
    userStore.setToken(data.accessToken); userStore.setLoginStatus(true)
    close(); emit('loginSuccess')
  } catch { errorMsg.value = '用户名或密码错误' }
  finally { loading.value = false }
}

async function handleRegister() {
  if (!registerForm.displayName || !registerForm.username || !registerForm.password) { errorMsg.value = '请完整填写'; return }
  if (registerForm.password.length < 8) { errorMsg.value = '密码至少 8 位'; return }
  errorMsg.value = ''; loading.value = true
  try {
    await fetchRegister({ username: registerForm.username, password: registerForm.password, displayName: registerForm.displayName })
    isRegister.value = false; loginForm.username = registerForm.username; errorMsg.value = ''
  } catch { errorMsg.value = '注册失败' }
  finally { loading.value = false }
}
</script>

<style scoped>
/* ═══ ROOT — Editorial Premium ═══ */
.dialog-root {
  position: fixed; inset: 0; z-index: 9999;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.12);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: 20px;
}

/* ═══ DOUBLE-BEZEL CARD ═══ */
.dialog-card-outer {
  width: 100%; max-width: 400px;
  padding: 1.5px;
  border-radius: 20px;
  background: rgba(0,0,0,.02);
  box-shadow: 0 2px 12px rgba(0,0,0,.02);
}
.dialog-card-inner {
  border-radius: calc(20px - 1.5px);
  background: #FFF;
  padding: 44px 36px 28px;
  position: relative;
}

/* ═══ CLOSE BUTTON ═══ */
.close-btn {
  position: absolute; top: 16px; right: 16px;
  width: 32px; height: 32px; border-radius: 8px; border: none; cursor: pointer;
  background: transparent; color: #bbb; display: flex; align-items: center; justify-content: center;
  transition: all .2s cubic-bezier(0.32,0.72,0,1);
}
.close-btn:hover { background: #f5f5f5; color: #1a1a2e; }
.close-btn svg { width: 18px; height: 18px; }

/* ═══ BRAND + TITLE ═══ */
.card-head { text-align: center; margin-bottom: 32px; }
.brand-mark {
  width: 44px; height: 44px; margin: 0 auto 16px;
  border-radius: 12px;
  background: linear-gradient(135deg,#5D87FF,#3B6CE0);
  color: #FFF; font-size: 14px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(93,135,255,.25);
  font-family: 'Plus Jakarta Sans',sans-serif;
}
.title { font-size: 24px; font-weight: 800; color: #111; margin: 0; letter-spacing: -.5px; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; }
.subtitle { font-size: 14px; color: #888; margin-top: 8px; font-family: 'Geist','Inter','PingFang SC',sans-serif; }

/* ═══ FORM ═══ */
.form-body { display: flex; flex-direction: column; gap: 18px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field-label { font-size: 13px; font-weight: 500; color: #444; font-family: 'Geist','Inter','PingFang SC',sans-serif; }
.field-input-wrap { position: relative; display: flex; align-items: center; }
.field-input {
  width: 100%; padding: 12px 14px;
  border: 1.5px solid #e8e8e8; border-radius: 10px;
  font-size: 14px; color: #111; background: #FFF;
  outline: none; transition: all .2s cubic-bezier(0.32,0.72,0,1);
  font-family: 'Geist','Inter','PingFang SC',sans-serif; box-sizing: border-box;
}
.field-input::placeholder { color: #bbb; }
.field-input:focus { border-color: #5D87FF; box-shadow: 0 0 0 3px rgba(93,135,255,.10); background: #fafcff; }
.field-input-wrap .field-input { padding-right: 40px; }
.field-action {
  position: absolute; right: 10px; border: none; background: none; cursor: pointer;
  color: #bbb; padding: 5px; display: flex; border-radius: 6px;
  transition: all .15s;
}
.field-action:hover { color: #555; background: #f5f5f5; }

/* ═══ ERROR ═══ */
.error-msg { font-size: 12px; color: #FF4D4F; padding-left: 2px; animation: shake .4s ease; font-family: 'Geist',sans-serif; }
@keyframes shake { 0%,100%{transform:translateX(0)} 25%{transform:translateX(-4px)} 75%{transform:translateX(4px)} }

/* ═══ SUBMIT BUTTON ═══ */
.submit-btn {
  width: 100%; padding: 13px; margin-top: 6px;
  border: none; border-radius: 10px;
  background: #111;
  color: #FFF;
  font-size: 15px; font-weight: 600; cursor: pointer;
  transition: all .25s cubic-bezier(0.32,0.72,0,1);
  display: flex; align-items: center; justify-content: center;
  min-height: 48px;
  font-family: 'Geist','Inter','PingFang SC',sans-serif;
  letter-spacing: .04em;
}
.submit-btn:hover:not(:disabled) { background: #222; transform: translateY(-1px); }
.submit-btn:active:not(:disabled) { transform: scale(.98); background: #333; }
.submit-btn:disabled { opacity: .4; cursor: not-allowed; }

/* ═══ SPINNER ═══ */
.spinner {
  width: 18px; height: 18px;
  border: 2px solid rgba(255,255,255,.2); border-top-color: #FFF;
  border-radius: 50%; animation: spin .6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ═══ FOOTER ═══ */
.footer-text { text-align: center; font-size: 13px; color: #999; margin-top: 24px; font-family: 'Geist','Inter','PingFang SC',sans-serif; }
.footer-link { background: none; border: none; color: #111; font-weight: 600; cursor: pointer; font-size: 13px; padding: 0; font-family: inherit; transition: all .15s; }
.footer-link:hover { color: #5D87FF; }

/* ═══ TRANSITIONS ═══ */
.overlay-fade-enter-active { transition: opacity .35s ease; }
.overlay-fade-leave-active { transition: opacity .2s ease; }
.overlay-fade-enter-from, .overlay-fade-leave-to { opacity: 0; }

.card-rise-enter-active { transition: all .45s cubic-bezier(0.32,0.72,0,1); }
.card-rise-leave-active { transition: all .2s ease; }
.card-rise-enter-from { opacity: 0; transform: translateY(24px) scale(.95); }
.card-rise-leave-to { opacity: 0; transform: translateY(12px) scale(.97); }
</style>