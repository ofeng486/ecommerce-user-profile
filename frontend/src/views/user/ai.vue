<!-- AI 分析 — 左侧历史会话（删除/清空/相对时间）+ 右侧对话 + 智能可视化（折线/饼图/条形/表格） -->
<template>
  <div class="ai-portal" :class="isAdminSide ? 'theme-admin' : 'theme-user'">
    <!-- 左侧历史 -->
    <aside class="ai-sidebar">
      <div class="ai-sidebar-hd">
        <h3>历史对话</h3>
        <button class="new-chat-btn" @click="newChat">+ 新对话</button>
      </div>
      <div class="ai-history">
        <div
          v-for="(chat, i) in conversations"
          :key="chat.id ?? 'new-' + i"
          class="history-item"
          :class="{ active: i === activeIndex }"
          @click="switchChat(i)"
        >
          <svg class="history-ico" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <div class="history-meta">
            <span class="history-title">{{ chat.title }}</span>
            <span class="history-time">{{ chat.timeText }}</span>
          </div>
          <button class="history-del" title="删除该对话" @click.stop="delHistory(chat, i)">✕</button>
        </div>
        <div v-if="!conversations.length" class="history-empty">暂无对话记录</div>
      </div>
      <button v-if="conversations.length" class="clear-btn" @click="clearHistory">清空全部对话</button>
    </aside>

    <!-- 右侧对话 -->
    <div class="ai-main">
      <div class="ai-chat" ref="chatRef">
        <div v-for="(msg, i) in activeChat" :key="i" class="msg" :class="msg.role">
          <div class="msg-avatar" :class="msg.role">{{ msg.role === 'user' ? 'U' : 'AI' }}</div>
          <div class="msg-body">
            <div class="msg-content">
              <!-- 空内容（loading 初始）→ 打字动画；非空 → 渲染正文 -->
              <div v-if="msg.role === 'assistant' && !msg.content" class="typing"><span class="dot"></span><span class="dot"></span><span class="dot"></span></div>
              <div v-else class="msg-text" v-html="renderMarkdown(stripFollowUps(stripSql(msg.content)))"></div>
              <!-- SQL 结果可视化：时间序列→折线 / 占比→饼图 / 两列数值→条形 / 其余→表格 -->
              <div v-if="msg.data && msg.data.length" class="msg-data">
                <div v-if="chartKind(msg.data, msg.chartType) === 'line'" :ref="setChartEl('line-' + i)" class="chart-box chart-line"></div>
                <div v-else-if="chartKind(msg.data, msg.chartType) === 'pie'" :ref="setChartEl('pie-' + i)" class="chart-box chart-pie"></div>
                <div v-else-if="chartKind(msg.data, msg.chartType) === 'bar'" class="bar-chart">
                  <div v-for="(row, ri) in msg.data" :key="ri" class="bar-row">
                    <span class="bar-label" :title="String(row[columns(msg.data)[0]])">{{ row[columns(msg.data)[0]] }}</span>
                    <div class="bar-track"><div class="bar-fill" :style="{ width: barPct(row, columns(msg.data)[1], msg.data) + '%' }"></div></div>
                    <span class="bar-val">{{ Number(row[columns(msg.data)[1]]).toLocaleString() }}</span>
                  </div>
                </div>
                <div v-else class="table-wrap">
                  <table class="data-table">
                    <thead><tr><th v-for="(c, ci) in columns(msg.data)" :key="ci">{{ c }}</th></tr></thead>
                    <tbody>
                      <tr v-for="(row, ri) in msg.data" :key="ri">
                        <td v-for="(c, ci) in columns(msg.data)" :key="ci">{{ fmtCell(row, c) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
              <!-- 追问建议 chips -->
              <div v-if="followUps(msg.content).length" class="follow-ups">
                <button v-for="q in followUps(msg.content)" :key="q" class="follow-chip" @click="ask(q)">💡 {{ q }}</button>
              </div>
            </div>
            <div class="msg-foot">
              <span class="msg-time">{{ msg.time || '' }}</span>
              <button v-if="msg.role === 'assistant'" class="copy-btn" @click="copyText(msg.content)">复制</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 建议标签 -->
      <div class="suggestions" v-if="!activeChat.length">
        <span class="suggest-label">💡 试试问</span>
        <button v-for="q in suggestions" :key="q" class="suggest-chip" @click="ask(q)">{{ q }}</button>
      </div>

      <!-- 输入 -->
      <div class="ai-input-area">
        <div class="input-wrap">
          <textarea
            v-model="inputText"
            class="input-field"
            rows="1"
            placeholder="输入您的问题，Enter 发送，Shift+Enter 换行…"
            @keydown.enter.exact.prevent="send"
            @keydown.enter.shift.exact="autoGrow"
            @input="autoGrow"
            :disabled="loading"
          ></textarea>
          <button v-if="!loading" class="send-btn" :disabled="!inputText.trim() || loading" @click="send">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
          </button>
          <button v-else class="stop-btn" title="停止生成" @click="stop">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>
          </button>
        </div>
        <div class="input-hint">Enter 发送 · Shift+Enter 换行</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import request from '@/utils/http'
import { renderMarkdown } from '@/utils/markdown'
import { streamChat } from '@/utils/sse'

defineOptions({ name: 'UserAiAnalysis' })

const route = useRoute()
const isAdminSide = computed(() => !route.path.startsWith('/user'))

/** 对话消息：role 角色、content 文本、data 可选 SQL 结果（行对象数组）、time 显示时间、chartType 后端推断图型、sql 数据来源 SQL */
interface AiMessage { role: string; content: string; data?: any[]; time?: string; chartType?: string; sql?: string }
/** 会话：id 后端历史主键（新对话为空）、title 标题、timeText 历史相对时间 */
interface Conversation { id?: number; title: string; timeText: string; messages: AiMessage[] }

const inputText = ref('')
const loading = ref(false)
const activeIndex = ref(0)
const chatRef = ref<HTMLElement>()

const conversations = ref<Conversation[]>([])
const activeChat = computed(() => conversations.value[activeIndex.value]?.messages || [])

/** ECharts 容器收集与实例管理 */
const chartEls: Record<string, HTMLElement> = {}
const chartInsts: echarts.ECharts[] = []
function setChartEl(key: string) {
  return (el: any) => { if (el) chartEls[key] = el }
}

const suggestions = ['高价值用户有哪些特征？', '近30天活跃趋势如何？', '消费金额最高的用户', '各分层用户占比']

/** 从后端加载真实对话历史（含 SQL 结果 JSON） */
async function loadHistory() {
  try {
    const res = await request.get<any>({ url: '/api/v1/ai/history', params: { page: 0, size: 50 }, showErrorMessage: false })
    const records = res?.records || []
    conversations.value = records.map((h: any) => ({
      id: h.id,
      title: h.question.length > 20 ? h.question.slice(0, 20) + '…' : h.question,
      timeText: relTime(h.createdAt),
      messages: [
        { role: 'user', content: h.question, time: fmtTime(h.createdAt) },
        { role: 'assistant', content: h.answer, data: h.dataJson ? safeParse(h.dataJson) : undefined, time: fmtTime(h.createdAt) }
      ]
    }))
  } catch { /* 加载失败时保持空历史 */ }
  if (!conversations.value.length) newChat()
}

function safeParse(json: string): any[] {
  try { const v = JSON.parse(json); return Array.isArray(v) ? v : [] } catch { return [] }
}

/** 相对时间：刚刚 / N分钟前 / N小时前 / N天前 / 日期 */
function relTime(iso?: string): string {
  if (!iso) return ''
  const t = new Date(iso).getTime()
  if (isNaN(t)) return ''
  const diff = Date.now() - t
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚'
  if (m < 60) return `${m} 分钟前`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} 小时前`
  const d = Math.floor(h / 24)
  if (d < 30) return `${d} 天前`
  return new Date(iso).toLocaleDateString('zh-CN')
}

/** 绝对时间短格式：MM-DD HH:mm */
function fmtTime(iso?: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d.getTime())) return ''
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function nowTime(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}`
}

/** 新建对话：若最后一个已是空的新对话则直接切换，避免堆积 */
function newChat() {
  const last = conversations.value[conversations.value.length - 1]
  if (last && last.id === undefined && last.title === '新对话' && last.messages.length <= 1) {
    activeIndex.value = conversations.value.length - 1
    return
  }
  conversations.value.push({ title: '新对话', timeText: '', messages: [{ role: 'assistant', content: '您好！有什么可以帮您的？', time: nowTime() }] })
  activeIndex.value = conversations.value.length - 1
}

function switchChat(i: number) {
  activeIndex.value = i
  renderMsgCharts()
}

/** 删除单条历史（后端仅删本人记录；本地会话直接移除） */
async function delHistory(chat: Conversation, i: number) {
  if (chat.id) {
    try { await request.del<any>({ url: `/api/v1/ai/history/${chat.id}` }) } catch { /* 忽略网络失败，本地照删 */ }
  }
  conversations.value.splice(i, 1)
  if (activeIndex.value >= conversations.value.length) activeIndex.value = Math.max(0, conversations.value.length - 1)
  if (!conversations.value.length) newChat()
  ElMessage.success('已删除该对话')
}

/** 清空全部历史 */
async function clearHistory() {
  try { await request.del<any>({ url: '/api/v1/ai/history' }) } catch { /* 忽略 */ }
  conversations.value = []
  newChat()
  ElMessage.success('已清空全部对话')
}

function ask(q: string) { inputText.value = q; send() }

/** 流式 AbortController（停止按钮） */
let abortCtrl: AbortController | null = null

async function send() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  const chat = conversations.value[activeIndex.value]
  chat.messages.push({ role: 'user', content: text, time: nowTime() })
  if (chat.title === '新对话') chat.title = text.slice(0, 20) + (text.length > 20 ? '…' : '')
  inputText.value = ''; loading.value = true; scroll()
  resetTextarea()

  // 流式生成中的助手消息（打字机逐字追加）
  // 用 reactive 创建：保证 activeAiMsg 指向响应式代理，逐字 content += 才能触发 Vue 渲染
  const aiMsg = reactive<AiMessage>({ role: 'assistant', content: '', time: nowTime() })
  chat.messages.push(aiMsg)
  activeAiMsg = aiMsg
  pendingText = ''
  if (renderTimer) { clearTimeout(renderTimer); renderTimer = null }
  abortCtrl = new AbortController()

  try {
    // 多轮上下文：发最近 6 条消息（去掉 data 与正在生成的 aiMsg），让模型能"接着问"
    const sendMsgs = chat.messages.filter(m => m !== aiMsg).slice(-6).map(m => ({ role: m.role, content: m.content }))
    await streamChat({
      url: '/api/v1/ai/stream',
      body: { messages: sendMsgs, pageContext: '' },
      signal: abortCtrl.signal,
      onText: (t) => { pendingText += t; pumpRender() },
      onData: (d) => {
        aiMsg.data = d.data
        aiMsg.chartType = d.chartType
        aiMsg.sql = d.sql
        renderMsgCharts()
      }
    })
    // 新对话产生真实问答后记录时间
    if (chat.id === undefined && chat.timeText === '') chat.timeText = '刚刚'
  } catch (e: any) {
    if (e.name !== 'AbortError' && !aiMsg.content) aiMsg.content = '⚠️ 分析服务暂时不可用，请稍后再试。'
  } finally {
    loading.value = false; abortCtrl = null
    // 不强制清空残留：让 pumpRender 继续按 CHAR_DELAY 匀速推完（避免"快速过完"）
    if (!pendingText && renderTimer) { clearTimeout(renderTimer); renderTimer = null }
    scroll(); renderMsgCharts()
  }
}

/** 停止生成：立即中止 + 清空未渲染的残留字 */
function stop() {
  abortCtrl?.abort()
  pendingText = ''
  if (renderTimer) { clearTimeout(renderTimer); renderTimer = null }
}

/* ─── 追问建议与数据来源 ─── */

/** 字符级慢速渲染队列（让流式输出在浏览器可见，每字 10ms） */
let pendingText = ''
let renderTimer: any = null
const CHAR_DELAY = 20   // ms/字：20ms 流畅且可见（500字 ~10s）
let activeAiMsg: AiMessage | null = null
function pumpRender() {
  if (renderTimer) return
  const tick = () => {
    if (pendingText.length && activeAiMsg) {
      activeAiMsg.content += pendingText[0]
      pendingText = pendingText.slice(1)
      scroll()
      renderTimer = setTimeout(tick, CHAR_DELAY)
    } else {
      renderTimer = null
    }
  }
  renderTimer = setTimeout(tick, 0)
}
/** 流式完成时残留字继续按 CHAR_DELAY 匀速推完（pumpRender 链自行完成，无需强制 flush） */

/** 从回答提取"可追问：a｜b"行 → chips */
function followUps(content: string): string[] {
  const arr: string[] = []
  for (const l of content.split('\n')) {
    if (!l.includes('可追问：')) continue
    const after = l.replace(/^.*?可追问：/, '').trim()
    arr.push(...after.split(/[｜|]/).map(s => s.trim()).filter(Boolean))
  }
  return arr.slice(0, 2)
}

/** 渲染时剥离"可追问"行（避免正文重复显示） */
function stripFollowUps(content: string): string {
  return content.split('\n').filter(l => !l.includes('可追问：')).join('\n')
}

/** 渲染时剥离 ```sql 块（普通用户不展示 SQL，避免看到技术细节） */
function stripSql(content: string): string {
  return content.replace(/```sql[\s\S]*?```/g, '').trim()
}

/** 复制 AI 回答 */
async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动选择复制')
  }
}

/** textarea 自动增高（上限 120px） */
function autoGrow(e: any) {
  const el = e.target as HTMLTextAreaElement
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}
function resetTextarea() {
  nextTick(() => {
    const el = document.querySelector<HTMLTextAreaElement>('.input-field')
    if (el) { el.style.height = 'auto' }
  })
}

/* ─── SQL 结果可视化 ─── */

/** 结果列名 */
function columns(data: any[]): string[] { return data.length ? Object.keys(data[0]) : [] }

/** 时间列识别：2024-01-01 / 2024/1/1 / 近30天 等 */
function isTimeVal(v: any): boolean {
  if (typeof v !== 'string') return false
  return /^\d{4}[-/]\d{1,2}[-/]\d{1,2}/.test(v) || /^(近|本周|上周|本月|上月|昨天|今天)/.test(v)
}

/** 图表类型判定：优先用后端 chartType（方案5），缺失时前端启发式判定 */
function chartKind(data: any[], chartType?: string): 'line' | 'pie' | 'bar' | 'table' {
  if (chartType && ['line', 'pie', 'bar', 'table'].includes(chartType)) return chartType as any
  const cols = columns(data)
  if (cols.length !== 2 || typeof data[0][cols[1]] !== 'number') return 'table'
  if (isTimeVal(data[0][cols[0]])) return 'line'
  // 占比判定：数值列名含占比关键词 或 全部数值为 0~1 的小数
  const valKey = cols[1]
  const allSmall = data.every(d => { const n = Number(d[valKey]); return n > 0 && n <= 1 })
  if (allSmall || /rate|ratio|pct|percent|占比|比例/i.test(valKey)) return 'pie'
  return 'bar'
}

/** 条形图宽度百分比（相对当前结果最大值 0-100） */
function barPct(row: any, key: string, data: any[]): number {
  const max = Math.max(...data.map(d => Number(d[key]) || 0), 0)
  const v = Number(row[key]) || 0
  return max > 0 ? Math.round((v / max) * 100) : 0
}

/** 表格单元格格式化（数字千分位） */
function fmtCell(row: any, col: string): string {
  const v = row[col]
  if (typeof v === 'number') return v.toLocaleString()
  return String(v ?? '')
}

/** 读取当前主题主色（--acc CSS 变量） */
function accColor(): string {
  const el = document.querySelector('.ai-portal') as HTMLElement | null
  if (!el) return '#0d9488'
  const v = getComputedStyle(el).getPropertyValue('--acc').trim()
  return v || '#0d9488'
}

/** 渲染所有 ECharts 图（折线/饼图），先 dispose 旧实例再重建 */
function renderMsgCharts() {
  chartInsts.forEach(c => c.dispose())
  chartInsts.length = 0
  nextTick(() => {
    const msgs = activeChat.value
    msgs.forEach((msg, i) => {
      if (!msg.data?.length) return
      const kind = chartKind(msg.data, msg.chartType)
      if (kind !== 'line' && kind !== 'pie') return
      const el = chartEls[kind + '-' + i]
      if (!el || !el.isConnected) return
      const c = echarts.init(el)
      chartInsts.push(c)
      const cols = columns(msg.data)
      const labels = msg.data.map(r => String(r[cols[0]]))
      const values = msg.data.map(r => Number(r[cols[1]]))
      const acc = accColor()
      const accSoft = acc + '22'
      if (kind === 'line') {
        c.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: 42, right: 16, top: 24, bottom: 30 },
          xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 10, color: '#94a3b8' }, axisLine: { show: false }, axisTick: { show: false } },
          yAxis: { type: 'value', axisLabel: { fontSize: 10, color: '#94a3b8' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
          series: [{
            type: 'line', data: values, smooth: true, symbolSize: 5,
            lineStyle: { color: acc, width: 2 },
            itemStyle: { color: acc },
            areaStyle: { color: accSoft }
          }]
        })
      } else {
        c.setOption({
          tooltip: { trigger: 'item', formatter: '{b}<br/>占比：{d}%' },
          legend: { orient: 'horizontal', bottom: 0, textStyle: { fontSize: 10, color: '#64748b' }, itemWidth: 10, itemHeight: 10, itemGap: 12 },
          color: ['#0d9488', '#6366f1', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#14b8a6', '#f97316', '#06b6d4', '#84cc16'],
          series: [{
            type: 'pie', radius: ['42%', '66%'], center: ['50%', '44%'],
            itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
            label: { fontSize: 10, color: '#64748b', formatter: '{b} {d}%' },
            data: msg.data.map((r, ri) => ({ name: String(r[cols[0]]), value: Number(r[cols[1]]) }))
          }]
        })
      }
    })
  })
}

function scroll() { nextTick(() => chatRef.value?.scrollTo({ top: chatRef.value.scrollHeight, behavior: 'smooth' })) }
onMounted(async () => {
  await loadHistory().then(scroll)
  renderMsgCharts()
  // 工作台 AI 输入框跳转携带的问题（?q=xxx）自动发起提问
  const q = route.query.q
  if (typeof q === 'string' && q.trim()) ask(q.trim())
})
onUnmounted(() => chartInsts.forEach(c => c.dispose()))
</script>

<style scoped>
.theme-admin.ai-portal { --acc: #2563eb; --acc-dark: #1d4ed8; --acc-soft: rgba(37,99,235,.08); --acc-line: #93c5fd; }
.theme-user.ai-portal { --acc: #0d9488; --acc-dark: #0f766e; --acc-soft: rgba(13,148,136,.08); --acc-line: #5eead4; }

.ai-portal { display:flex; height:calc(100vh - 100px); gap:14px; font-family:'Inter','PingFang SC',system-ui,sans-serif; }

/* ─── Sidebar ─── */
.ai-sidebar { width:240px; flex-shrink:0; display:flex; flex-direction:column; background:var(--default-box-color,#fff); border:1px solid var(--default-border,#e4e4e7); border-radius:12px; box-shadow:0 1px 2px rgba(15,23,42,.04); padding:16px; }
.ai-sidebar-hd { display:flex; justify-content:space-between; align-items:center; margin-bottom:14px; }
.ai-sidebar-hd h3 { font-size:14px; font-weight:600; color:var(--text-1,#18181b); margin:0; }
.new-chat-btn { font-size:11px; padding:4px 10px; border:1px solid var(--default-border,#e4e4e7); background:transparent; border-radius:8px; cursor:pointer; color:var(--text-2,#71717a); transition:all .15s; font-family:inherit; }
.new-chat-btn:hover { border-color:var(--acc); color:var(--acc); }
.ai-history { flex:1; display:flex; flex-direction:column; gap:2px; overflow-y:auto; }
.history-item { display:flex; align-items:center; gap:8px; width:100%; padding:8px 10px; border:none; background:transparent; border-radius:8px; cursor:pointer; font-size:12.5px; color:var(--text-2,#71717a); text-align:left; transition:all .12s; font-family:inherit; }
.history-item:hover { background:#f4f4f5; }
.history-item.active { background:var(--acc-soft); color:var(--acc); font-weight:500; }
.history-ico { flex-shrink:0; }
.history-meta { flex:1; min-width:0; display:flex; flex-direction:column; gap:1px; }
.history-title { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.history-time { font-size:11px; color:#a1a1aa; font-weight:400; }
.history-del { width:18px; height:18px; display:none; align-items:center; justify-content:center; border:none; background:transparent; border-radius:4px; color:#a1a1aa; cursor:pointer; font-size:11px; flex-shrink:0; transition:all .12s; }
.history-item:hover .history-del { display:inline-flex; }
.history-del:hover { background:#fee2e2; color:#dc2626; }
.history-empty { text-align:center; padding:24px 0; color:#a1a1aa; font-size:13px; }
.clear-btn { margin-top:10px; padding:6px 0; border:none; background:transparent; border-top:1px solid var(--default-border,#f0f0f3); color:#a1a1aa; font-size:12px; cursor:pointer; transition:color .15s; font-family:inherit; }
.clear-btn:hover { color:#dc2626; }

/* ─── Main ─── */
.ai-main { flex:1; display:flex; flex-direction:column; background:var(--default-box-color,#fff); border:1px solid var(--default-border,#e4e4e7); border-radius:12px; box-shadow:0 1px 2px rgba(15,23,42,.04); padding:20px; min-width:0; }

.ai-chat { flex:1; overflow-y:auto; display:flex; flex-direction:column; gap:16px; margin-bottom:14px; }

.msg { display:flex; gap:10px; max-width:88%; }
.msg.user { align-self:flex-end; flex-direction:row-reverse; }

.msg-avatar { width:30px; height:30px; display:flex; align-items:center; justify-content:center; background:#f1f5f9; color:#64748b; border-radius:8px; font-size:12px; font-weight:600; letter-spacing:.3px; flex-shrink:0; }
.msg-avatar.assistant { background:var(--acc); color:#fff; }
.msg-avatar.user { background:#e2e8f0; color:#475569; }

.msg-body { min-width:0; display:flex; flex-direction:column; }
.msg-content { padding:10px 14px; border-radius:12px; font-size:14px; line-height:1.65; word-break:break-word; color:var(--text-1,#18181b); }
.msg.user .msg-content { background:var(--acc); color:#fff; border-bottom-right-radius:4px; }
.msg.assistant .msg-content { background:#f4f4f5; border-bottom-left-radius:4px; }

.msg-foot { display:flex; align-items:center; gap:8px; margin-top:4px; padding:0 4px; opacity:0; transition:opacity .15s; }
.msg:hover .msg-foot { opacity:1; }
.msg-time { font-size:11px; color:#a1a1aa; }
.copy-btn { font-size:11px; padding:1px 8px; border:1px solid var(--default-border,#e4e4e7); background:transparent; border-radius:6px; color:var(--text-2,#71717a); cursor:pointer; transition:all .15s; font-family:inherit; }
.copy-btn:hover { border-color:var(--acc); color:var(--acc); }

/* ─── SQL 结果可视化 ─── */
.msg-data { margin-top:10px; border-top:1px dashed var(--acc-line); padding-top:8px; }
.chart-box { width:100%; background:#fff; border-radius:8px; }
.chart-line { height:220px; }
.chart-pie { height:240px; }
.table-wrap { max-height:260px; overflow-y:auto; border:1px solid #f0f0f3; border-radius:8px; }
.data-table { width:100%; border-collapse:collapse; font-size:12px; background:#fff; }
.data-table th, .data-table td { padding:6px 10px; border-bottom:1px solid #f0f0f3; text-align:left; white-space:nowrap; }
.data-table th { background:#fafafa; color:#71717a; font-weight:600; position:sticky; top:0; }
.data-table td { color:#334155; font-variant-numeric:tabular-nums; }
.bar-chart { display:flex; flex-direction:column; gap:5px; background:#fff; border-radius:8px; padding:8px 10px; }
.bar-row { display:flex; align-items:center; gap:8px; font-size:12px; }
.bar-label { width:110px; flex-shrink:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:#52525b; }
.bar-track { flex:1; height:14px; background:#f4f4f5; border-radius:7px; overflow:hidden; }
.bar-fill { height:100%; background:linear-gradient(90deg,var(--acc),var(--acc-dark)); border-radius:7px; transition:width .3s; }
.bar-val { width:60px; text-align:right; color:var(--acc); font-weight:600; font-variant-numeric:tabular-nums; }

/* ─── Typing ─── */
.typing { display:flex; gap:4px; padding:10px 14px; }
.dot { width:6px; height:6px; background:#a1a1aa; border-radius:50%; animation:bounce 1.2s infinite; }
.dot:nth-child(2) { animation-delay:.2s; }
.dot:nth-child(3) { animation-delay:.4s; }
@keyframes bounce { 0%,60%,100%{transform:translateY(0)} 30%{transform:translateY(-4px)} }

/* ─── Suggestions ─── */
.suggestions { display:flex; align-items:center; gap:6px; flex-wrap:wrap; margin-bottom:12px; }
.suggest-label { font-size:12px; color:#a1a1aa; }
.suggest-chip { padding:4px 12px; border:1px solid var(--default-border,#e4e4e7); background:transparent; border-radius:16px; font-size:12px; color:var(--text-2,#71717a); cursor:pointer; transition:all .15s; font-family:inherit; }
.suggest-chip:hover { border-color:var(--acc); color:var(--acc); }

/* ─── Input ─── */
.ai-input-area { flex-shrink:0; }
.input-wrap { display:flex; gap:8px; align-items:flex-end; background:var(--default-box-color,#fff); border:1px solid var(--default-border,#e4e4e7); border-radius:16px; padding:8px 8px 8px 16px; transition:border-color .2s; }
.input-wrap:focus-within { border-color:var(--acc); box-shadow:0 0 0 3px var(--acc-soft); }
.input-field { flex:1; border:none; outline:none; font-size:14px; padding:6px 0; font-family:inherit; background:transparent; color:var(--text-1,#18181b); resize:none; line-height:1.5; max-height:120px; }
.input-field::placeholder { color:#cbd5e1; }
.send-btn { width:38px; height:38px; display:flex; align-items:center; justify-content:center; border:none; background:var(--acc); color:#fff; border-radius:12px; cursor:pointer; transition:background .15s; flex-shrink:0; }
.send-btn:hover:not(:disabled) { background:var(--acc-dark); }
.send-btn:disabled { background:#e4e4e7; color:#a1a1aa; cursor:not-allowed; }
.stop-btn { width:38px; height:38px; display:flex; align-items:center; justify-content:center; border:none; background:#ef4444; color:#fff; border-radius:12px; cursor:pointer; flex-shrink:0; transition:background .15s; }
.stop-btn:hover { background:#dc2626; }
.input-hint { margin-top:6px; font-size:11px; color:#cbd5e1; text-align:right; }
/* 追问建议 */
.follow-ups { display:flex; flex-wrap:wrap; gap:6px; margin-top:8px; }
.follow-chip { font-size:11.5px; padding:3px 10px; border:1px solid var(--acc-line); background:var(--acc-soft); border-radius:12px; color:var(--acc); cursor:pointer; transition:all .15s; font-family:inherit; }
.follow-chip:hover { background:var(--acc); color:#fff; border-color:var(--acc); }

@media (max-width: 900px) {
  .ai-portal { flex-direction:column; height:auto; }
  .ai-sidebar { width:100%; max-height:200px; }
}
</style>
