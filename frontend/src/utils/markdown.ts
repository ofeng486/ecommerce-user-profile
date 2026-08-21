/**
 * 轻量 Markdown 渲染（面向 AI 回答文本）。
 * 仅支持加粗与换行，其余一律保持原文：
 * 1. 先做 HTML 转义（AI 内容不可信，防 XSS）；
 * 2. 再把 **加粗** 语法渲染为 <strong>（转义后替换，安全）；
 * 3. 换行由调用方 CSS white-space: pre-wrap 保留，无需转 <br>。
 */
export function renderMarkdown(text: string): string {
  if (!text) return ''
  const escaped = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
  return escaped.replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>')
}
