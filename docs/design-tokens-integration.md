# 项目设计 Token 集成文档

> 基于 design-system skill 的三层 Token 架构（Primitive → Semantic → Component）
> 映射到本项目现有的 `--art-*` CSS 变量体系

---

## 架构总览

```
┌──────────────────────────────────────────────┐
│  Component Tokens（组件层）                    │  每个组件的专属 Token
│  --art-card-border, --el-border-radius-base   │  Element Plus / Tailwind 使用
├──────────────────────────────────────────────┤
│  Semantic Tokens（语义层）                     │  用途别名
│  --art-primary, --art-success                │  Tailwind @theme 引用
├──────────────────────────────────────────────┤
│  Primitive Tokens（原始层）                    │  原始色值
│  oklch(0.7 0.23 260), #f9fafb               │  由设计稿决定
└──────────────────────────────────────────────┘
```

---

## 第一层：Primitive Tokens（原始层）

基础色值，不应在组件中直接引用。

### 品牌主色（OKLCH）

| Token | 亮色模式 | 暗色模式 |
|-------|----------|----------|
| `--art-primary` | `oklch(0.7 0.23 260)` | 同左 |
| `--art-secondary` | `oklch(0.72 0.19 231.6)` | 同左 |
| `--art-error` | `oklch(0.73 0.15 25.3)` | 同左 |
| `--art-info` | `oklch(0.58 0.03 254.1)` | 同左 |
| `--art-success` | `oklch(0.78 0.17 166.1)` | 同左 |
| `--art-warning` | `oklch(0.78 0.14 75.5)` | 同左 |
| `--art-danger` | `oklch(0.68 0.22 25.3)` | 同左 |

### 灰度色阶

| Token | 亮色模式 | 暗色模式 |
|-------|----------|----------|
| `--art-gray-100` | `#f9fafb` | `#110f0f` |
| `--art-gray-200` | `#f2f4f5` | `#17171c` |
| `--art-gray-300` | `#e6eaeb` | `#393946` |
| `--art-gray-400` | `#dbdfe1` | `#505062` |
| `--art-gray-500` | `#949eb7` | `#73738c` |
| `--art-gray-600` | `#7987a1` | `#8f8fa3` |
| `--art-gray-700` | `#4d5875` | `#ababba` |
| `--art-gray-800` | `#383853` | `#c7c7d1` |
| `--art-gray-900` | `#323251` | `#e3e3e8` |

### 圆角

| Token | 来源 | 值 |
|-------|------|----|
| `--custom-radius` | 项目配置（settings panel） | 用户可调（默认 8px 左右） |
| `--el-border-radius-base` | Element Plus | `calc(var(--custom-radius) / 3 + 2px)` |
| `--el-border-radius-small` | Element Plus | `calc(var(--custom-radius) / 3 + 4px)` |

---

## 第二层：Semantic Tokens（语义层）

用途别名，供 Tailwind 和组件引用。

### 语义色

| Tailwind Utility | CSS Variable | 用途 |
|-----------------|--------------|------|
| `bg-primary` / `text-primary` | `--color-primary` = `--art-primary` | 主色调按钮/链接 |
| `bg-secondary` | `--color-secondary` = `--art-secondary` | 次要操作 |
| `text-success` / `bg-success` | `--color-success` = `--art-success` | 成功状态 |
| `text-warning` / `bg-warning` | `--color-warning` = `--art-warning` | 警告状态 |
| `text-error` / `bg-error` | `--color-error` = `--art-error` | 错误状态 |
| `text-danger` / `bg-danger` | `--color-danger` = `--art-danger` | 危险操作 |
| `text-info` / `bg-info` | `--color-info` = `--art-info` | 信息提示 |

### 背景色

| Tailwind Utility | CSS Variable | 用途 |
|-----------------|--------------|------|
| `bg-box` | `--color-box` = `--default-box-color` | 卡片/面板背景（亮：#fff，暗：#161618） |
| `bg-default` | `--default-bg-color` | 页面全局背景（亮：#fafbfc，暗：#070707） |
| `bg-hover-color` | `--color-hover-color` = `--art-hover-color` | 悬浮态 |
| `bg-active-color` | `--color-active-color` = `--art-active-color` | 激活态 |

### 边框

| Tailwind Utility | CSS Variable | 用途 |
|-----------------|--------------|------|
| `border-default` | `--default-border` | 标准边框 |
| `border-default-dashed` | `--default-border-dashed` | 虚框 |
| `border-card-border` | `--art-card-border` | 卡片边框 |

### 灰度文字

| Tailwind Utility | CSS Variable | 用途 |
|-----------------|--------------|------|
| `text-g-500` | `--color-g-500` | 次要文字 |
| `text-g-600` | `--color-g-600` | 辅助文字 |
| `text-g-700` | `--color-g-700` | 正文 |
| `text-g-800` | `--color-g-800` | 标题 |
| `text-g-900` | `--color-g-900` | 强调文字 |

---

## 第三层：Component Tokens（组件层）

已通过 Element Plus CSS 变量覆盖实现。

### Element Plus 组件 Token 映射

| Element Plus Token | 绑定的语义变量 | 文件位置 |
|-------------------|---------------|----------|
| `--el-color-primary` | `--art-primary` | Element Plus 默认 |
| `--el-color-success` | `--art-success` | Element Plus 默认 |
| `--el-color-warning` | `--art-warning` | Element Plus 默认 |
| `--el-color-danger` | `--art-danger` | Element Plus 默认 |
| `--el-color-error` | `--art-error` | Element Plus 默认 |
| `--el-color-info` | `--art-info` | Element Plus 默认 |
| `--el-color-white` | `white` | `el-ui.scss` |
| `--el-color-black` | `white` | `el-ui.scss` |
| `--el-border-radius-base` | `calc(var(--custom-radius) / 3 + 2px)` | `el-ui.scss` |
| `--el-border-radius-small` | `calc(var(--custom-radius) / 3 + 4px)` | `el-ui.scss` |
| `--el-component-custom-height` | `36px` | `el-ui.scss` |
| `--el-card-bg-color` | `--default-box-color` | `el-ui.scss` |
| `--el-card-border-color` | `--default-border` | `app.scss` |
| `--el-datepicker-inrange-bg-color` | `--art-gray-200` | `el-ui.scss` |
| `--el-text-color-regular` | `--font-color` | `dark.scss` |

---

## 文件索引

| 文件 | 内容 |
|------|------|
| `src/assets/styles/core/tailwind.css` | Tailwind 4 主题配置 + Primitive Tokens + Dark Mode |
| `src/assets/styles/core/el-ui.scss` | Element Plus 组件 Token 覆盖 |
| `src/assets/styles/core/el-light.scss` | Element Plus 亮色主题变量 |
| `src/assets/styles/core/el-dark.scss` | Element Plus 暗色主题变量 |
| `src/assets/styles/core/dark.scss` | 暗色模式额外覆盖 |
| `src/assets/styles/core/app.scss` | 应用全局样式 |
| `src/assets/styles/index.scss` | 样式入口（汇总 import） |

---

## 使用指南

### 配合 design-system skill 使用

当调用 `design-system` skill 时，引用以下上下文：

1. 本项目使用 **OKLCH** 格式定义颜色，与 design-system 的 HSL 格式不同 — 请保留 OKLCH 原始值
2. 三层的 Primitive Tokens 定义在 `tailwind.css` 的 `:root` 和 `.dark` 中
3. Component Tokens 通过 Element Plus 的 `--el-*` 变量覆盖实现
4. Tailwind 4 的 `@theme` 指令配置在 `tailwind.css` 第 87-122 行
5. 组件高度统一为 `36px`（`--el-component-custom-height`）

### 常见操作

```bash
# 查找项目已有的 Token 定义
rg --art- src/assets/styles/core/tailwind.css

# 查找 Element Plus 覆盖
rg --el- src/assets/styles/core/el-ui.scss

# 新增一个语义色
# 1. 在 tailwind.css :root 中添加 --art-xxx: oklch(...)
# 2. 在 .dark 中添加对应暗色值
# 3. 在 @theme 中添加 --color-xxx: var(--art-xxx)
```

## 设计规范总结

| 维度 | 值 |
|------|-----|
| 主色色相 | 260°（蓝色系） |
| 色彩格式 | OKLCH |
| 组件高度 | 36px（标准）|
| 基础圆角 | `--custom-radius`（用户可配置） |
| 字体权重 | 400（常规）|
| 页面背景（亮） | `#fafbfc` |
| 页面背景（暗） | `#070707` |
| 卡片背景（亮） | `#ffffff` |
| 卡片背景（暗） | `#161618` |
| Tailwind 版本 | 4 |
| UI 组件库 | Element Plus 2.11 |
| 图标库 | Iconify + Element Plus Icons |

## 字体方案

| 用途 | 字体栈 | CSS 变量 |
|------|--------|----------|
| 标题/显示 | `'Plus Jakarta Sans', 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif` | `--font-heading` |
| 正文 | `'Inter', 'PingFang SC', 'Microsoft YaHei', -apple-system, sans-serif` | `--font-body` |
| 等宽/代码 | `'JetBrains Mono', 'Space Grotesk', 'Cascadia Code', monospace` | `--font-mono` |
| 数据数字 | `'Space Grotesk', 'JetBrains Mono', monospace` | `--font-data` |

> 字体文件通过 Google Fonts 在 `index.html` 加载。额外引入了 `Plus Jakarta Sans`（标题）和 `JetBrains Mono`（数据等宽），与原有的 `Inter`（正文）+ `Space Grotesk`（数字）组成完整方案。
