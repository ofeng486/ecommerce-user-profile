import { useUserStore } from '@/store/modules/user'

/** SSE 结构化 data 事件负载（后端 executeEmbeddedSql 发送） */
export interface StreamDataEvent {
  data?: any[]
  chartType?: string
  sql?: string
}

export interface StreamChatParams {
  url: string
  body: any
  signal?: AbortSignal
  /** 文本增量回调（打字机） */
  onText: (text: string) => void
  /** 结构化 data 事件回调（图表 + SQL） */
  onData?: (payload: StreamDataEvent) => void
  /** 流结束回调 */
  onDone?: () => void
}

/**
 * OpenAI 兼容 SSE 流式解析（POST + fetch ReadableStream）。
 * 解析两种事件：
 *  - { choices: [{ delta: { content } }] } → 文本增量，回调 onText
 *  - { type: "data", data, chartType, sql } → 结构化结果，回调 onData
 */
export async function streamChat(params: StreamChatParams): Promise<void> {
  const token = useUserStore().accessToken
  const resp = await fetch(params.url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: token ? 'Bearer ' + token : ''
    },
    body: JSON.stringify(params.body),
    signal: params.signal
  })
  if (!resp.ok) throw new Error('HTTP ' + resp.status)
  if (!resp.body) throw new Error('响应无 body')

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''   // 最后一段可能不完整，留到下一轮
    for (const line of lines) {
      const t = line.trim()
      if (!t.startsWith('data:')) continue
      const payload = t.slice(5).trim()
      if (payload === '[DONE]') { params.onDone?.(); return }
      try {
        const json = JSON.parse(payload)
        if (json?.type === 'data') {
          params.onData?.({ data: json.data, chartType: json.chartType, sql: json.sql })
        } else {
          const text = json?.choices?.[0]?.delta?.content
          if (text) params.onText(text)
        }
      } catch { /* 忽略无法解析的行 */ }
    }
  }
  params.onDone?.()
}
