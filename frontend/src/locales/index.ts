/**
 * 国际化配置 — 仅中文
 *
 * @module locales
 */

import { createI18n } from 'vue-i18n'
import type { I18n, I18nOptions } from 'vue-i18n'

import zhMessages from './langs/zh.json'

const i18nOptions: I18nOptions = {
  locale: 'zh',
  legacy: false,
  globalInjection: true,
  fallbackLocale: 'zh',
  messages: { zh: zhMessages }
}

const i18n: I18n = createI18n(i18nOptions)

export const $t = i18n.global.t as (key: string) => string

export const languageOptions = [
  { value: 'zh', label: '简体中文' }
]

export default i18n
