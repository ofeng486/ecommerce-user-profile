/**
 * 系统全局配置
 *
 * 这是系统的核心配置文件，集中管理所有全局配置项。
 * 包含系统信息、主题样式、菜单布局、颜色方案等所有可配置项。
 *
 * ## 主要功能
 *
 * - 系统信息 - 系统名称等基础信息
 * - 主题配置 - 亮色/暗色/自动主题的样式配置
 * - 菜单配置 - 菜单布局、主题、宽度等配置
 * - 颜色方案 - 系统主色和预设颜色列表
 * - 快速入口 - 快速入口应用和链接配置
 * - 顶部栏配置 - 顶部栏功能模块配置
 *
 * ## 配置项说明
 *
 * - systemInfo: 系统基础信息（名称等）
 * - systemThemeStyles: 系统主题样式映射
 * - menuLayoutList: 可选的菜单布局列表
 * - themeList: 菜单主题样式列表
 * - darkMenuStyles: 暗黑模式下的菜单样式
 * - systemMainColor: 预设的系统主色列表
 * - fastEnter: 快速入口配置
 * - headerBar: 顶部栏功能配置
 *
 * @module config
 * @author Art Design Pro Team
 */

import { MenuThemeEnum, MenuTypeEnum, SystemThemeEnum } from '@/enums/appEnum'
import { SystemConfig } from '@/types/config'
import { configImages } from './assets/images'
import fastEnterConfig from './modules/fastEnter'
import { headerBarConfig } from './modules/headerBar'

const appConfig: SystemConfig = {
  // 系统信息
  systemInfo: {
    name: '电商用户画像分析系统' // 系统名称
  },
  // 系统主题
  systemThemeStyles: {
    [SystemThemeEnum.LIGHT]: { className: '' },
    [SystemThemeEnum.DARK]: { className: SystemThemeEnum.DARK }
  },
  // 系统主题列表
  // 菜单布局列表
  menuLayoutList: [
    { name: 'Left', value: MenuTypeEnum.LEFT, img: configImages.menuLayouts.vertical },
    { name: 'Top', value: MenuTypeEnum.TOP, img: configImages.menuLayouts.horizontal },
    { name: 'Mixed', value: MenuTypeEnum.TOP_LEFT, img: configImages.menuLayouts.mixed },
    { name: 'Dual Column', value: MenuTypeEnum.DUAL_MENU, img: configImages.menuLayouts.dualColumn }
  ],
  // 菜单主题列表
  themeList: [
    {
      theme: MenuThemeEnum.DESIGN,
      background: '#FFFFFF',
      systemNameColor: 'var(--art-gray-800)',
      iconColor: '#6B6B6B',
      textColor: '#29343D'
    }
  ],
  // 暗黑模式菜单样式
  darkMenuStyles: [
    {
      theme: MenuThemeEnum.DARK,
      background: 'var(--default-box-color)',
      systemNameColor: '#DDDDDD',
      iconColor: '#BABBBD',
      textColor: 'rgba(#FFFFFF, 0.7)'
    }
  ],
  // 系统主色
  systemMainColor: [
    '#5D87FF',
    '#B48DF3',
    '#1D84FF',
    '#60C041',
    '#38C0FC',
    '#F9901F',
    '#FF80C8'
  ] as const,
  // 快速入口配置
  fastEnter: fastEnterConfig,
  // 顶部栏功能配置
  headerBar: headerBarConfig
}

export default Object.freeze(appConfig)
