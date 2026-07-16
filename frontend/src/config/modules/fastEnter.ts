/**
 * 快速入口配置 —— 本项目不使用，预留接口
 * 包含：应用列表、快速链接等配置
 */
import type { FastEnterConfig } from '@/types/config'

const fastEnterConfig: FastEnterConfig = {
  // 显示条件（屏幕宽度）
  minWidth: 1200,
  // 应用列表（已清空，本项目不需要快速入口）
  applications: [],
  // 快速链接（已清空）
  quickLinks: []
}

export default Object.freeze(fastEnterConfig)
