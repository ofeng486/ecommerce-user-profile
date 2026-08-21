import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'

/**
 * Vitest 配置（单元测试）。
 * 仅覆盖纯逻辑工具函数，无需浏览器环境；
 * __APP_VERSION__ 与 vite build 的 define 保持一致，alias 与 vite.config.ts 同步。
 */
export default defineConfig({
  define: {
    __APP_VERSION__: '"1.0.0"'
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  test: {
    environment: 'node',
    include: ['src/**/__tests__/**/*.test.ts']
  }
})
