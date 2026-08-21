<!-- 右下角 AI 快问快答浮窗 — 定位：轻量即问即答；深度分析/历史会话请前往「AI 分析」页。
     流式打字机 + 停止按钮 + 动态推荐问题（按当前页面）+ 数据来源 SQL 折叠 + 追问建议，--acc 主题化 -->
<template>
  <div class="ai-widget" :class="isAdminSide ? 'theme-admin' : 'theme-user'">
    <!-- 收起态：悬浮按钮 -->
    <button v-if="!open" class="fab" @click="open = true">
      <svg class="fab-ico" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
      </svg>
    </button>

    <!-- 展开态：对话窗 -->
    <Transition name="slide-up">
      <div v-if="open" class="panel">
        <div class="panel-hd">
          <div class="panel-title">
            <span class="hd-avatar">AI</span>
            <span class="hd-text">AI 数据分析师</span>
            <span class="hd-tag">快问快答</span>
            <!-- 当前页面实时指示：随路由自动响应，下一次提问会带上此页面上下文 -->
            <span class="hd-page" :title="pageContextText() || '不在已知页面'">
              <svg class="hd-page-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/><path stroke-linecap="round" stroke-linejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
              {{ pageLabel }}
            </span>
          </div>
          <div class="hd-actions">
            <button v-if="messages.length" class="clear-btn" @click="clearChat" title="清空当前对话">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M1 7h22M9 7V5a2 2 0 012-2h2a2 2 0 012 2v2"/></svg>
            </button>
            <button class="close-btn" @click="open = false">
              <svg class="close-ico" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
            </button>
          </div>
        </div>

        <div ref="msgBox" class="msg-box">
          <div v-if="messages.length === 0 && !loading" class="empty">
            <p class="empty-tip">基于你的画像数据库实时分析</p>
            <div class="chips">
              <button v-for="s in quickQuestions" :key="s" @click="send(s)" class="chip">{{ s }}</button>
            </div>
            <p class="empty-nav">深度分析与历史会话请前往「AI 分析」</p>
          </div>

          <div v-for="(msg, i) in messages" :key="i" class="msg" :class="msg.role">
            <span v-if="msg.role === 'assistant'" class="mini-avatar">AI</span>
            <div class="bubble">
              <!-- 空内容（loading 初始）→ 打字动画；非空 → 渲染正文 -->
              <span v-if="msg.role === 'assistant' && !msg.content" class="typing"><span class="dot"></span><span class="dot"></span><span class="dot"></span></span>
              <span v-else-if="msg.role === 'assistant'" v-html="renderMd(stripFollowUps(stripSql(msg.content)))"></span>
              <span v-else>{{ msg.content }}</span>
              <!-- 数据可视化：与 AI 分析页同款判定（折线/饼图/条形/表格） -->
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
          </div>
        </div>

        <div class="input-bar">
          <div class="input-wrap">
            <input v-model="input" @keydown.enter="send()" :disabled="loading" class="input" placeholder="输入问题…" />
            <button v-if="!loading" @click="send()" :disabled="!input.trim()" class="send-btn">
              <svg class="send-ico" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 12h14M12 5l7 7-7 7"/></svg>
            </button>
            <button v-else class="stop-btn" title="停止生成" @click="stop">
              <svg width="11" height="11" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="2"/></svg>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { useUserStore } from '@/store/modules/user'
import mittBus from '@/utils/sys/mittBus'
import { streamChat } from '@/utils/sse'

defineOptions({ name: 'AIChatWidget' })

const route = useRoute()
const isAdminSide = computed(() => !route.path.startsWith('/user'))

interface Message { role: 'user' | 'assistant'; content: string; data?: any[]; chartType?: string; sql?: string }

const userStore = useUserStore()
const open = ref(false), input = ref(''), loading = ref(false)
const messages = ref<Message[]>([]), msgBox = ref<HTMLElement>()
let abortCtrl: AbortController | null = null

/** 用户端页面 → 语义上下文映射：让浮窗 AI 感知用户当前所在页面并针对性回答 */
const PAGE_CONTEXT: Record<string, string> = {
  '/user/dashboard': '用户当前正在【工作台】页面，查看核心数据看板（用户规模、订单、消费趋势）',
  '/user/overview': '用户当前正在【画像概览】页面，查看用户画像核心指标概览',
  '/user/profiles': '用户当前正在【画像列表】页面，浏览全部用户画像（分层、消费力、活跃度等画像维度），可按条件筛选',
  '/user/profiles/': '用户当前正在【画像详情】页面，查看单个用户的完整画像（属性、标签、消费行为）',
  '/user/tags': '用户当前正在【标签分析】页面，查看用户标签分布与统计分析',
  '/user/ai': '用户当前正在【AI 分析】页面，与 AI 数据分析助手进行对话',
  '/user/product-analysis': '用户当前正在【商品分析】页面，查看商品维度的销售与偏好分析',
  '/user/repeat-analysis': '用户当前正在【复购与留存】页面，查看用户复购率与留存分析',
  '/user/churn-analysis': '用户当前正在【流失预警】页面，查看流失风险用户分析',
  '/user/cluster-analysis': '用户当前正在【用户聚类】页面，查看聚类分析结果',
  '/user/audience': '用户当前正在【人群圈选】页面，配置条件圈选目标人群',
  '/user/audience/comparison': '用户当前正在【画像对比】页面，对比两个人群包的画像差异',
  '/user/audience/packages': '用户当前正在【人群包管理】页面，管理已保存的人群包',
  '/user/notifications': '用户当前正在【通知中心】页面，查看系统通知',
  '/user/settings': '用户当前正在【个人中心】页面，管理个人资料与偏好'
}

/** 根据当前路由生成页面上下文描述（精确匹配优先，其次前缀匹配如 /user/profiles/123） */
function pageContextText(): string {
  const p = route.path
  if (PAGE_CONTEXT[p]) return PAGE_CONTEXT[p]
  for (const [k, v] of Object.entries(PAGE_CONTEXT)) {
    if (k.endsWith('/') && p.startsWith(k)) return v
  }
  return ''
}

/** 当前页面名（中文，来自路由 meta.title）——响应式，路由变化自动更新 */
const pageLabel = computed(() => (route.meta?.title as string) || '当前页面')

/** 动态推荐问题：按当前页面给出 3 个相关提问（方案3） */
const PAGE_QUESTIONS: Record<string, string[]> = {
  '/user/dashboard': ['核心指标解读', '最近一周消费趋势', '今日新增用户'],
  '/user/overview': ['各分层用户占比', '高价值用户特征', '近30天活跃趋势'],
  '/user/profiles': ['高价值用户有哪些特征？', '流失风险用户画像', '用户性别与消费的关系'],
  '/user/profiles/': ['这个用户的风险等级', '该用户的消费习惯', '该用户偏好什么品类'],
  '/user/tags': ['热门标签有哪些', '标签分布情况', '偏好品类分布'],
  '/user/product-analysis': ['销量最高的商品', '商品销售趋势', '热销品类排行'],
  '/user/repeat-analysis': ['整体复购率', '高复购用户特征', '留存趋势'],
  '/user/churn-analysis': ['流失风险用户规模', '流失用户特征', '流失预警名单'],
  '/user/cluster-analysis': ['聚类结果分布', '各聚类用户特征', '聚类数量'],
  '/user/audience': ['帮我圈选高价值用户', '流失风险用户怎么圈', '25-30岁用户圈选'],
  '/user/audience/comparison': ['如何选择对比人群', '男女画像差异', '对比维度说明'],
  '/user/audience/packages': ['查看人群包用户', '导出人群名单', '人群包规则说明'],
  '/user/notifications': ['最近的通知', '任务完成情况'],
  '/user/settings': ['修改个人资料', '修改密码'],
  '/user/ai': ['各分层用户占比', '近30天活跃趋势', '高价值用户特征']
}
const DEFAULT_QUESTIONS = ['用户分层', '高价值用户', '广东省分析', '流失风险']
const quickQuestions = computed(() => {
  const p = route.path
  if (PAGE_QUESTIONS[p]) return PAGE_QUESTIONS[p]
  for (const [k, v] of Object.entries(PAGE_QUESTIONS)) {
    if (k.endsWith('/') && p.startsWith(k)) return v
  }
  return DEFAULT_QUESTIONS
})

/** ECharts 容器收集与实例管理 */
const chartEls: Record<string, HTMLElement> = {}
const chartInsts: echarts.ECharts[] = []
function setChartEl(key: string) {
  return (el: any) => { if (el) chartEls[key] = el }
}

onMounted(() => { mittBus.on('openChat', () => { open.value = true }) })

function renderMd(text: string) {
  let h = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  h = h.replace(/```(\w+)?\n?([\s\S]*?)```/g, '<pre class="md-pre"><code>$2</code></pre>')
  h = h.replace(/`([^`]+)`/g, '<code class="md-code">$1</code>')
  h = h.replace(/\*\*(.+?)\*\*/g, '<strong class="md-strong">$1</strong>')
  h = h.replace(/^(📊\s*.+)$/gm, '<p class="md-head">$1</p>')
  h = h.replace(/^- (.+)$/gm, '<div class="md-li"><span class="md-dot">•</span><span>$1</span></div>')
  h = h.replace(/\|(.+)\|\n\|[-: |]+\|\n((?:\|.+\|\n?)+)/g, (_, header, body) => {
    const hcells = header.split('|').filter((c: string) => c.trim()).map((c: string) => `<th class="md-th">${c.trim()}</th>`).join('')
    const rows = body.trim().split('\n').map((r: string) => {
      const cells = r.split('|').filter((c: string) => c.trim()).map((c: string) => `<td class="md-td">${c.trim()}</td>`).join('')
      return `<tr>${cells}</tr>`
    }).join('')
    return `<table class="md-table"><thead><tr>${hcells}</tr></thead><tbody>${rows}</tbody></table>`
  })
  h = h.replace(/\n/g, '<br>')
  return h
}

function ask(q: string) { input.value = q; send() }

async function send(preset?: string) {
  const q = (preset || input.value).trim()
  if (!q || loading.value) return
  if (!preset) input.value = ''
  open.value = true

  messages.value.push({ role: 'user', content: q })
  // 用 reactive 创建：保证 activeAiMsg 指向响应式代理，逐字 content += 才能触发 Vue 渲染
  const aiMsg = reactive<Message>({ role: 'assistant', content: '' })
  messages.value.push(aiMsg)
  activeAiMsg = aiMsg
  pendingText = ''
  if (renderTimer) { clearTimeout(renderTimer); renderTimer = null }
  loading.value = true
  abortCtrl = new AbortController()
  await nextTick(); scrollBottom()

  try {
    const sendMsgs = messages.value.filter(m => m !== aiMsg).map(m => ({ role: m.role, content: m.content }))
    await streamChat({
      url: '/api/v1/ai/stream',
      body: { messages: sendMsgs, pageContext: pageContextText() },
      signal: abortCtrl.signal,
      onText: (t) => { pendingText += t; pumpRender() },
      onData: (d) => {
        aiMsg.data = d.data
        aiMsg.chartType = d.chartType
        aiMsg.sql = d.sql
        renderMsgCharts()
      }
    })
  } catch (e: any) {
    if (e.name !== 'AbortError' && !aiMsg.content) aiMsg.content = '抱歉，服务异常'
  } finally {
    loading.value = false; abortCtrl = null
    // 不强制清空残留：让 pumpRender 继续按 CHAR_DELAY 匀速推完（避免"快速过完"）
    if (!pendingText && renderTimer) { clearTimeout(renderTimer); renderTimer = null }
  }
}

/** 停止生成：立即中止 + 清空未渲染的残留字 */
function stop() {
  abortCtrl?.abort()
  pendingText = ''
  if (renderTimer) { clearTimeout(renderTimer); renderTimer = null }
}

/** 字符级慢速渲染队列（让流式输出在浏览器可见，每字 10ms） */
let pendingText = ''
let renderTimer: any = null
const CHAR_DELAY = 20   // ms/字：20ms 流畅且可见（500字 ~10s）
let activeAiMsg: Message | null = null
function pumpRender() {
  if (renderTimer) return
  const tick = () => {
    if (pendingText.length && activeAiMsg) {
      activeAiMsg.content += pendingText[0]
      pendingText = pendingText.slice(1)
      scrollBottom()
      renderTimer = setTimeout(tick, CHAR_DELAY)
    } else {
      renderTimer = null
    }
  }
  renderTimer = setTimeout(tick, 0)
}

/** 清空当前对话（含 ECharts 实例释放，避免内存泄漏） */
function clearChat() {
  chartInsts.forEach(c => c.dispose())
  chartInsts.length = 0
  for (const k in chartEls) delete chartEls[k]
  messages.value = []
}

/* ─── 追问建议与数据来源 ─── */

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

/* ─── 数据可视化（与 AI 分析页同款判定） ─── */

function columns(data: any[]): string[] { return data.length ? Object.keys(data[0]) : [] }

function isTimeVal(v: any): boolean {
  if (typeof v !== 'string') return false
  return /^\d{4}[-/]\d{1,2}[-/]\d{1,2}/.test(v) || /^(近|本周|上周|本月|上月|昨天|今天)/.test(v)
}

function chartKind(data: any[], chartType?: string): 'line' | 'pie' | 'bar' | 'table' {
  if (chartType && ['line', 'pie', 'bar', 'table'].includes(chartType)) return chartType as any
  const cols = columns(data)
  if (cols.length !== 2 || typeof data[0][cols[1]] !== 'number') return 'table'
  if (isTimeVal(data[0][cols[0]])) return 'line'
  const valKey = cols[1]
  const allSmall = data.every(d => { const n = Number(d[valKey]); return n > 0 && n <= 1 })
  if (allSmall || /rate|ratio|pct|percent|占比|比例/i.test(valKey)) return 'pie'
  return 'bar'
}

function barPct(row: any, key: string, data: any[]): number {
  const max = Math.max(...data.map(d => Number(d[key]) || 0), 0)
  const v = Number(row[key]) || 0
  return max > 0 ? Math.round((v / max) * 100) : 0
}

function fmtCell(row: any, col: string): string {
  const v = row[col]
  if (typeof v === 'number') return v.toLocaleString()
  return String(v ?? '')
}

function accColor(): string {
  const el = document.querySelector('.ai-widget') as HTMLElement | null
  if (!el) return '#0d9488'
  const v = getComputedStyle(el).getPropertyValue('--acc').trim()
  return v || '#0d9488'
}

function renderMsgCharts() {
  chartInsts.forEach(c => c.dispose())
  chartInsts.length = 0
  nextTick(() => {
    messages.value.forEach((msg, i) => {
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
          grid: { left: 40, right: 14, top: 20, bottom: 28 },
          xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 10, color: '#94a3b8' }, axisLine: { show: false }, axisTick: { show: false } },
          yAxis: { type: 'value', axisLabel: { fontSize: 10, color: '#94a3b8' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
          series: [{ type: 'line', data: values, smooth: true, symbolSize: 5, lineStyle: { color: acc, width: 2 }, itemStyle: { color: acc }, areaStyle: { color: accSoft } }]
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
            data: msg.data.map(r => ({ name: String(r[cols[0]]), value: Number(r[cols[1]]) }))
          }]
        })
      }
    })
  })
}

function scrollBottom() { nextTick(() => { const el = msgBox.value; if (el) el.scrollTop = el.scrollHeight }) }
onUnmounted(() => chartInsts.forEach(c => c.dispose()))
</script>

<style scoped>
/* 主题变量：与全局页面一致的 --acc 体系（用户端青色 / 管理端蓝色） */
.theme-admin.ai-widget { --acc: #2563eb; --acc-dark: #1d4ed8; --acc-soft: rgba(37,99,235,.08); --acc-line: #93c5fd; }
.theme-user.ai-widget { --acc: #0d9488; --acc-dark: #0f766e; --acc-soft: rgba(13,148,136,.08); --acc-line: #5eead4; }

.ai-widget { font-family:'Inter','PingFang SC',system-ui,sans-serif; }

/* ─── 悬浮按钮 ─── */
.fab {
  position: fixed; right: 24px; bottom: 24px; z-index: 50;
  width: 48px; height: 48px; border: none; border-radius: 14px;
  background: var(--acc); color: #fff; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 6px 20px rgba(15,23,42,.18);
  transition: all .2s;
}
.fab:hover { background: var(--acc-dark); transform: translateY(-2px); box-shadow: 0 10px 26px rgba(15,23,42,.22); }
.fab-ico { width: 20px; height: 20px; }

/* ─── 展开面板 ─── */
.panel {
  position: fixed; right: 24px; bottom: 24px; z-index: 50;
  width: 400px; max-width: calc(100vw - 32px); height: 560px; max-height: 80vh;
  background: var(--default-box-color, #fff); border: 1px solid rgba(15,23,42,0.07);
  border-radius: 20px; box-shadow: 0 24px 64px rgba(15,23,42,0.18), 0 4px 16px rgba(15,23,42,0.06);
  display: flex; flex-direction: column; overflow: hidden;
}

/* 标题栏：浅色主题染底，非渐变 */
.panel-hd {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid var(--default-border, #eef2f6);
  background: var(--acc-soft); flex-shrink: 0;
}
.panel-title { display: flex; align-items: center; gap: 8px; min-width: 0; }
.hd-avatar {
  width: 26px; height: 26px; border-radius: 8px;
  background: var(--acc); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 600; letter-spacing: .3px;
}
.hd-text { font-size: 14px; font-weight: 600; color: var(--text-1, #18181b); flex-shrink: 0; }
.hd-tag { font-size: 10px; padding: 1px 7px; border-radius: 8px; background: rgba(0,0,0,.05); color: #64748b; flex-shrink: 0; }
/* 当前页面实时指示（响应式） */
.hd-page {
  display: inline-flex; align-items: center; gap: 3px;
  font-size: 11px; padding: 2px 8px; border-radius: 8px;
  background: var(--acc); color: #fff; font-weight: 500;
  max-width: 130px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex-shrink: 1;
}
.hd-page-ico { width: 11px; height: 11px; flex-shrink: 0; }
.hd-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.clear-btn {
  width: 26px; height: 26px; border: none; background: transparent; border-radius: 8px; cursor: pointer;
  display: flex; align-items: center; justify-content: center; color: #64748b; transition: all .15s;
}
.clear-btn:hover { background: rgba(220,38,38,.1); color: #dc2626; }
.clear-btn svg { width: 14px; height: 14px; }
.close-btn { width: 26px; height: 26px; border: none; background: transparent; border-radius: 8px; cursor: pointer; display: flex; align-items: center; justify-content: center; color: #64748b; transition: all .15s; }
.close-btn:hover { background: rgba(0,0,0,.06); color: #334155; }
.close-ico { width: 15px; height: 15px; }

/* ─── 消息区 ─── */
.msg-box { flex: 1; overflow-y: auto; padding: 14px; display: flex; flex-direction: column; gap: 10px; }

.empty { text-align: center; padding: 32px 0 24px; }
.empty-tip { font-size: 12px; color: #94a3b8; margin-bottom: 12px; }
.chips { display: flex; flex-wrap: wrap; justify-content: center; gap: 6px; }
.chip {
  padding: 4px 12px; font-size: 12px; border: 1px solid var(--default-border, #e4e4e7);
  background: transparent; border-radius: 14px; color: #64748b; cursor: pointer; transition: all .15s; font-family: inherit;
}
.chip:hover { border-color: var(--acc); color: var(--acc); background: var(--acc-soft); }
.empty-nav { margin-top: 14px; font-size: 11px; color: #cbd5e1; }

.msg { display: flex; gap: 8px; }
.msg.user { justify-content: flex-end; }
.mini-avatar {
  width: 22px; height: 22px; border-radius: 6px; flex-shrink: 0; margin-top: 2px;
  background: var(--acc); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 9px; font-weight: 600; letter-spacing: .3px;
}
.bubble {
  max-width: 92%; padding: 8px 12px; border-radius: 10px;
  font-size: 13px; line-height: 1.6; word-break: break-word; color: var(--text-1, #18181b);
  min-width: 0;
}
.msg.assistant .bubble { background: #f4f4f5; border-top-left-radius: 3px; }
.msg.user .bubble { background: var(--acc); color: #fff; border-top-right-radius: 3px; }

/* Markdown 细节样式 */
.md-pre { background: #1e293b; color: #e2e8f0; border-radius: 8px; padding: 8px 10px; margin: 6px 0; font-size: 11px; overflow-x: auto; }
.md-code { background: rgba(0,0,0,.06); padding: 0 4px; border-radius: 4px; font-size: 12px; }
.md-strong { font-weight: 600; }
.md-head { font-weight: 600; color: #334155; margin: 8px 0 4px; }
.md-li { display: flex; gap: 6px; margin: 3px 0; }
.md-dot { color: var(--acc); flex-shrink: 0; }
.md-table { margin: 6px 0; border-collapse: collapse; border: 1px solid #e2e8f0; border-radius: 6px; overflow: hidden; width: 100%; }
.md-th { padding: 4px 8px; font-size: 11px; font-weight: 600; border: 1px solid #e2e8f0; background: #f8fafc; text-align: left; }
.md-td { padding: 4px 8px; font-size: 11px; border: 1px solid #e2e8f0; }

/* ─── 数据可视化 ─── */
.msg-data { margin-top: 8px; border-top: 1px dashed var(--acc-line); padding-top: 6px; }
.chart-box { width: 100%; background: #fff; border-radius: 8px; }
.chart-line { height: 200px; }
.chart-pie { height: 220px; }
.table-wrap { max-height: 220px; overflow-y: auto; border: 1px solid #f0f0f3; border-radius: 8px; }
.data-table { width: 100%; border-collapse: collapse; font-size: 12px; background: #fff; }
.data-table th, .data-table td { padding: 5px 10px; border-bottom: 1px solid #f0f0f3; text-align: left; white-space: nowrap; }
.data-table th { background: #fafafa; color: #71717a; font-weight: 600; position: sticky; top: 0; }
.data-table td { color: #334155; font-variant-numeric: tabular-nums; }
.bar-chart { display: flex; flex-direction: column; gap: 4px; background: #fff; border-radius: 8px; padding: 6px 8px; }
.bar-row { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.bar-label { width: 90px; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #52525b; }
.bar-track { flex: 1; height: 13px; background: #f4f4f5; border-radius: 7px; overflow: hidden; }
.bar-fill { height: 100%; background: linear-gradient(90deg,var(--acc),var(--acc-dark)); border-radius: 7px; transition: width .3s; }
.bar-val { width: 56px; text-align: right; color: var(--acc); font-weight: 600; font-variant-numeric: tabular-nums; }

/* 追问建议 */
.follow-ups { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.follow-chip {
  font-size: 11.5px; padding: 3px 10px; border: 1px solid var(--acc-line);
  background: var(--acc-soft); border-radius: 12px; color: var(--acc); cursor: pointer;
  transition: all .15s; font-family: inherit;
}
.follow-chip:hover { background: var(--acc); color: #fff; border-color: var(--acc); }

/* ─── 输入区 ─── */
.input-bar { padding: 10px 12px; border-top: 1px solid var(--default-border, #eef2f6); flex-shrink: 0; }
.input-wrap { display: flex; gap: 6px; }
.input {
  flex: 1; min-width: 0; border: 1px solid var(--default-border, #e4e4e7); border-radius: 9px;
  padding: 8px 12px; font-size: 13px; font-family: inherit; outline: none; background: var(--default-box-color, #fff);
  color: var(--text-1, #18181b); transition: border-color .2s, box-shadow .2s;
}
.input::placeholder { color: #cbd5e1; }
.input:focus { border-color: var(--acc); box-shadow: 0 0 0 3px var(--acc-soft); }
.send-btn {
  width: 36px; height: 36px; flex-shrink: 0; border: none; border-radius: 9px;
  background: var(--acc); color: #fff; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: background .15s;
}
.send-btn:hover:not(:disabled) { background: var(--acc-dark); }
.send-btn:disabled { background: #e4e4e7; color: #a1a1aa; cursor: not-allowed; }
.send-ico { width: 15px; height: 15px; }
.stop-btn {
  width: 36px; height: 36px; flex-shrink: 0; border: none; border-radius: 9px;
  background: #ef4444; color: #fff; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: background .15s;
}
.stop-btn:hover { background: #dc2626; }

/* 打字指示 */
.typing { display: flex; gap: 4px; align-items: center; }
.dot { width: 5px; height: 5px; background: #94a3b8; border-radius: 50%; animation: bounce 1.2s infinite; }
.dot:nth-child(2) { animation-delay: .2s; }
.dot:nth-child(3) { animation-delay: .4s; }
@keyframes bounce { 0%,60%,100%{transform:translateY(0)} 30%{transform:translateY(-3px)} }

/* 开合动画 */
.slide-up-enter-active, .slide-up-leave-active { transition: all .25s ease; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(24px) scale(.96); }
</style>
