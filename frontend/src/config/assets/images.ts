/**
 * 配置图片资源
 *
 * 统一管理设置中心使用的预览图片资源。
 *
 * @module config/assets/images
 * @author Art Design Pro Team
 */

import verticalLayout from '@imgs/settings/menu_layouts/vertical.png'
import horizontalLayout from '@imgs/settings/menu_layouts/horizontal.png'
import mixedLayout from '@imgs/settings/menu_layouts/mixed.png'
import dualColumnLayout from '@imgs/settings/menu_layouts/dual_column.png'

/**
 * 配置中心图片资源对象
 */
export const configImages = {
  /** 菜单布局预览图 */
  menuLayouts: {
    /** 左侧菜单 */
    vertical: verticalLayout,
    /** 顶部菜单 */
    horizontal: horizontalLayout,
    /** 混合菜单 */
    mixed: mixedLayout,
    /** 双栏菜单 */
    dualColumn: dualColumnLayout
  }
}
