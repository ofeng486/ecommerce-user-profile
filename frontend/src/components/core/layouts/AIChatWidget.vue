<template>
  <div v-if="!open" @click="open = true"
    class="fixed bottom-6 right-6 z-50 w-14 h-14 rounded-2xl bg-gradient-to-br from-blue-500 to-violet-600 flex items-center justify-center cursor-pointer shadow-2xl shadow-blue-500/30 hover:scale-110 hover:shadow-blue-500/40 transition-all duration-300 group">
    <svg class="w-6 h-6 text-white group-hover:animate-bounce" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
    </svg>
  </div>

  <Transition name="slide-up">
    <div v-if="open" class="fixed bottom-0 right-0 z-50 w-full sm:w-[420px] h-[600px] max-h-[80vh] bg-white dark:bg-gray-900 rounded-t-2xl sm:rounded-2xl sm:bottom-6 sm:right-6 shadow-2xl border border-gray-200 dark:border-gray-700 flex flex-col overflow-hidden">
      <div class="flex items-center justify-between px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-blue-50 to-violet-50 dark:from-blue-950 dark:to-violet-950 shrink-0">
        <div class="flex items-center gap-2">
          <div class="w-7 h-7 rounded-lg bg-gradient-to-br from-blue-400 to-violet-500 flex items-center justify-center">
            <svg class="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z"/></svg>
          </div>
          <span class="text-sm font-semibold text-gray-800 dark:text-gray-200">AI 数据分析师</span>
        </div>
        <button @click="open = false" class="w-7 h-7 rounded-lg hover:bg-black/10 dark:hover:bg-white/10 flex items-center justify-center transition-colors">
          <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
        </button>
      </div>

      <div ref="msgBox" class="flex-1 overflow-y-auto px-3 py-3 space-y-3">
        <div v-if="messages.length === 0 && !loading" class="text-center py-8">
          <p class="text-xs text-gray-400 mb-3">基于你的画像数据库实时分析</p>
          <div class="flex flex-wrap justify-center gap-1.5">
            <button v-for="s in ['用户分层','高价值用户','广东省分析','流失风险']" :key="s" @click="send(s)"
              class="px-2.5 py-1 text-[11px] border border-gray-200 dark:border-gray-700 rounded-lg text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">{{ s }}</button>
          </div>
        </div>

        <div v-for="(msg, i) in messages" :key="i" :class="msg.role === 'user' ? 'flex justify-end' : 'flex gap-2'">
          <div v-if="msg.role === 'assistant'" class="w-6 h-6 rounded-md bg-gradient-to-br from-blue-400 to-violet-500 flex items-center justify-center text-white text-[10px] font-bold shrink-0 mt-0.5">AI</div>
          <div :class="msg.role === 'user' ? 'max-w-[85%] px-3 py-2 rounded-xl rounded-tr-sm text-xs bg-blue-500 text-white' : 'max-w-[85%] px-3 py-2 rounded-xl rounded-tl-sm text-xs bg-gray-100 dark:bg-gray-800 text-gray-800 dark:text-gray-200'">
            <span v-if="msg.role === 'assistant'" v-html="renderMd(msg.content)"></span>
            <span v-else>{{ msg.content }}</span>
          </div>
        </div>

        <div v-if="loading" class="flex gap-2">
          <div class="w-6 h-6 rounded-md bg-gradient-to-br from-blue-400 to-violet-500 flex items-center justify-center text-white text-[10px] font-bold shrink-0">AI</div>
          <div class="px-3 py-2 rounded-xl rounded-tl-sm bg-gray-100 dark:bg-gray-800 flex items-center gap-1.5">
            <span class="w-1.5 h-1.5 bg-blue-400 rounded-full animate-bounce"></span>
            <span class="w-1.5 h-1.5 bg-blue-400 rounded-full animate-bounce" style="animation-delay:0.1s"></span>
            <span class="w-1.5 h-1.5 bg-blue-400 rounded-full animate-bounce" style="animation-delay:0.2s"></span>
          </div>
        </div>
      </div>

      <div class="p-2 border-t border-gray-200 dark:border-gray-700 shrink-0">
        <div class="flex gap-1.5">
          <input v-model="input" @keydown.enter="send()" :disabled="loading"
            class="flex-1 bg-gray-100 dark:bg-gray-800 rounded-lg px-3 py-2 text-xs text-gray-800 dark:text-gray-200 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="输入问题..." />
          <button @click="send()" :disabled="!input.trim() || loading"
            class="px-3 py-2 bg-blue-500 hover:bg-blue-600 disabled:opacity-30 rounded-lg transition-colors">
            <svg class="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 12h14M12 5l7 7-7 7"/></svg>
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useUserStore } from '@/store/modules/user'
import mittBus from '@/utils/sys/mittBus'

defineOptions({ name: 'AIChatWidget' })
interface Message { role: 'user' | 'assistant'; content: string }

const userStore = useUserStore()
const open = ref(false), input = ref(''), loading = ref(false)
const messages = ref<Message[]>([]), msgBox = ref<HTMLElement>()
let abortCtrl: AbortController | null = null

onMounted(() => { mittBus.on('openChat', () => { open.value = true }) })

function renderMd(text: string) {
  let h = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  h = h.replace(/```(\w+)?\n?([\s\S]*?)```/g, '<pre class="bg-gray-800 text-gray-100 rounded-lg p-2 my-1 text-[10px] overflow-x-auto"><code>$2</code></pre>')
  h = h.replace(/`([^`]+)`/g, '<code class="bg-black/5 dark:bg-white/10 px-1 rounded text-[10px]">$1</code>')
  h = h.replace(/\*\*(.+?)\*\*/g, '<strong class="font-semibold">$1</strong>')
  h = h.replace(/^(📊\s*.+)$/gm, '<p class="font-semibold text-gray-700 dark:text-gray-300 mt-3 mb-1">$1</p>')
  h = h.replace(/^- (.+)$/gm, '<div class="flex gap-1.5 ml-0.5 my-0.5"><span class="text-blue-400">•</span><span>$1</span></div>')
  // Markdown 表格 → 简易 HTML 表格
  h = h.replace(/\|(.+)\|\n\|[-: |]+\|\n((?:\|.+\|\n?)+)/g, (_, header, body) => {
    const hcells = header.split('|').filter((c: string) => c.trim()).map((c: string) => `<th class="px-2 py-1 text-[10px] font-semibold border border-gray-300 dark:border-gray-600">${c.trim()}</th>`).join('')
    const rows = body.trim().split('\n').map((r: string) => {
      const cells = r.split('|').filter((c: string) => c.trim()).map((c: string) => `<td class="px-2 py-1 text-[10px] border border-gray-300 dark:border-gray-600">${c.trim()}</td>`).join('')
      return `<tr>${cells}</tr>`
    }).join('')
    return `<table class="my-2 border-collapse border border-gray-300 dark:border-gray-600 rounded-lg overflow-hidden"><thead><tr>${hcells}</tr></thead><tbody>${rows}</tbody></table>`
  })
  h = h.replace(/\n/g, '<br>')
  return h
}

async function send(preset?: string) {
  const q = (preset || input.value).trim()
  if (!q || loading.value) return
  if (!preset) input.value = ''
  open.value = true

  messages.value.push({ role: 'user', content: q })
  const aiMsg: Message = { role: 'assistant', content: '' }
  messages.value.push(aiMsg)
  loading.value = true
  abortCtrl = new AbortController()
  const timeoutId = setTimeout(() => abortCtrl?.abort(), 45000)
  await nextTick(); scrollBottom()

  try {
    const resp = await fetch('/api/v1/ai/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + userStore.accessToken },
      body: JSON.stringify({ messages: messages.value.filter(m => m !== aiMsg).map(m => ({ role: m.role, content: m.content })) }),
      signal: abortCtrl.signal
    })
    clearTimeout(timeoutId)
    if (!resp.ok) { aiMsg.content = resp.status === 401 ? '请先登录' : 'AI 异常 ' + resp.status; return }
    const data = await resp.json()
    aiMsg.content = data.answer || '暂无回复'
    await nextTick(); scrollBottom()
  } catch (e: any) {
    clearTimeout(timeoutId)
    if (e.name !== 'AbortError') aiMsg.content = '抱歉，服务异常'
  } finally { clearTimeout(timeoutId); loading.value = false; abortCtrl = null }
}

function scrollBottom() { nextTick(() => { const el = msgBox.value; if (el) el.scrollTop = el.scrollHeight }) }
</script>

<style scoped>
.slide-up-enter-active, .slide-up-leave-active { transition: all 0.3s ease; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(30px) scale(0.95); }
</style>
