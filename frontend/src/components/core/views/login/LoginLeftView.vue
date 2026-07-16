<!-- 登录、注册、忘记密码左侧背景 -->
<template>
  <div class="login-left-view">
    <div class="logo">
      <ArtLogo class="icon" size="46" />
      <h1 class="title">{{ AppConfig.systemInfo.name }}</h1>
    </div>

    <!-- 核心可视化（项目专属） -->
    <div class="left-visual">
      <div class="chart-card">
        <div class="chart-row">
          <div class="stat-item stat-blue">
            <div class="stat-icon-small"><ArtSvgIcon icon="ri:user-3-line" /></div>
            <div class="stat-info">
              <div class="stat-label-small">总用户</div>
              <div class="stat-value-small">1,200+</div>
            </div>
          </div>
          <div class="stat-item stat-green">
            <div class="stat-icon-small"><ArtSvgIcon icon="ri:medal-line" /></div>
            <div class="stat-info">
              <div class="stat-label-small">高价值</div>
              <div class="stat-value-small">432</div>
            </div>
          </div>
        </div>
        <div class="chart-row">
          <div class="stat-item stat-orange">
            <div class="stat-icon-small"><ArtSvgIcon icon="ri:money-cny-circle-line" /></div>
            <div class="stat-info">
              <div class="stat-label-small">消费总额</div>
              <div class="stat-value-small">¥1.13亿</div>
            </div>
          </div>
          <div class="stat-item stat-purple">
            <div class="stat-icon-small"><ArtSvgIcon icon="ri:price-tag-3-line" /></div>
            <div class="stat-info">
              <div class="stat-label-small">用户标签</div>
              <div class="stat-value-small">5 类</div>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-mini">
        <div class="chart-mini-title">RFM 用户分层</div>
        <div class="rfm-bar">
          <div class="rfm-row"><span class="rfm-label">高价值</span><div class="rfm-track"><div class="rfm-fill rfm-hv" style="width: 36%"></div></div><span class="rfm-pct">36%</span></div>
          <div class="rfm-row"><span class="rfm-label">潜力</span><div class="rfm-track"><div class="rfm-fill rfm-pt" style="width: 24%"></div></div><span class="rfm-pct">24%</span></div>
          <div class="rfm-row"><span class="rfm-label">一般</span><div class="rfm-track"><div class="rfm-fill rfm-g" style="width: 14%"></div></div><span class="rfm-pct">14%</span></div>
          <div class="rfm-row"><span class="rfm-label">流失风险</span><div class="rfm-track"><div class="rfm-fill rfm-ar" style="width: 16%"></div></div><span class="rfm-pct">16%</span></div>
          <div class="rfm-row"><span class="rfm-label">低价值</span><div class="rfm-track"><div class="rfm-fill rfm-lv" style="width: 10%"></div></div><span class="rfm-pct">10%</span></div>
        </div>
      </div>
    </div>

    <div class="text-wrap">
      <h1>电商用户画像分析系统</h1>
      <p>基于 RFM 模型 + 行为标签的精准用户分层与精细化运营</p>
    </div>

    <!-- 几何装饰元素 -->
    <div class="geometric-decorations">
      <div class="geo-element circle-outline animate-fade-in-up" style="animation-delay: 0s"></div>
      <div class="geo-element square-rotated animate-fade-in-left" style="animation-delay: 0s"></div>
      <div class="geo-element circle-small animate-fade-in-up" style="animation-delay: 0.3s"></div>
      <div class="geo-element square-bottom-right animate-fade-in-right" style="animation-delay: 0s"></div>
      <div class="geo-element bg-bubble animate-scale-in" style="animation-delay: 0.5"></div>
      <div class="geo-element circle-top-right animate-fade-in-down" style="animation-delay: 0.5"></div>
      <div class="geo-element dot dot-top-left animate-bounce-in" style="animation-delay: 0s"></div>
      <div class="geo-element dot dot-top-right animate-bounce-in" style="animation-delay: 0s"></div>
      <div class="geo-element dot dot-center-right animate-bounce-in" style="animation-delay: 0s"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import AppConfig from '@/config'

  defineProps<{
    hideContent?: boolean
  }>()
</script>

<style lang="scss" scoped>
  $primary-light-7: var(--el-color-primary-light-7);
  $primary-light-8: var(--el-color-primary-light-8);
  $primary-light-9: var(--el-color-primary-light-9);
  $primary-base: var(--el-color-primary);
  $main-bg: var(--default-box-color);

  $bg-mix-light-9: color-mix(in srgb, $primary-light-9 100%, $main-bg);
  $bg-mix-light-8: color-mix(in srgb, $primary-light-8 80%, $main-bg);
  $bg-mix-light-7: color-mix(in srgb, $primary-light-7 80%, $main-bg);

  .login-left-view {
    position: relative;
    box-sizing: border-box;
    width: 65vw;
    height: 100%;
    padding: 15px;
    overflow: hidden;
    background-color: $bg-mix-light-9;

    .logo {
      position: relative;
      z-index: 100;
      display: flex;
      align-items: center;

      .title {
        margin-left: 10px;
        font-size: 18px;
        font-weight: 500;
        color: var(--art-gray-900);
      }
    }

    /* 项目专属可视化区域 */
    .left-visual {
      position: absolute;
      top: 50%; left: 50%;
      transform: translate(-50%, -55%);
      z-index: 10;
      width: 56%;
      max-width: 480px;
      animation: slideInLeft 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
    }

    .chart-card {
      background: rgba(255, 255, 255, 0.85);
      backdrop-filter: blur(8px);
      border-radius: 16px;
      padding: 16px;
      box-shadow: 0 8px 32px rgba(93, 135, 255, 0.12);
      border: 1px solid rgba(255, 255, 255, 0.6);
    }

    .chart-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      margin-bottom: 10px;
      &:last-child { margin-bottom: 0; }
    }

    .stat-item {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 10px;
      background: #f9fafb;
      border-radius: 10px;
    }
    .stat-icon-small {
      width: 28px; height: 28px;
      border-radius: 6px;
      display: flex; align-items: center; justify-content: center;
      color: #fff; font-size: 14px;
    }
    .stat-info { flex: 1; min-width: 0; }
    .stat-label-small { font-size: 10px; color: #9ca3af; line-height: 1.2; }
    .stat-value-small { font-size: 14px; font-weight: 700; color: #1f2937; line-height: 1.3; }

    .stat-blue .stat-icon-small { background: linear-gradient(135deg, #5D87FF, #3B6CE0); }
    .stat-green .stat-icon-small { background: linear-gradient(135deg, #36CBCB, #1DA8A8); }
    .stat-orange .stat-icon-small { background: linear-gradient(135deg, #F9901F, #E07A0A); }
    .stat-purple .stat-icon-small { background: linear-gradient(135deg, #B48DF3, #9B6FD9); }

    .chart-mini {
      margin-top: 12px;
      background: rgba(255, 255, 255, 0.85);
      backdrop-filter: blur(8px);
      border-radius: 16px;
      padding: 14px 16px;
      box-shadow: 0 8px 32px rgba(93, 135, 255, 0.12);
      border: 1px solid rgba(255, 255, 255, 0.6);
    }
    .chart-mini-title {
      font-size: 12px; font-weight: 600; color: #4b5563;
      margin-bottom: 10px; display: flex; align-items: center; gap: 4px;
      &::before { content: '▎'; color: #5D87FF; font-size: 14px; }
    }
    .rfm-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; font-size: 11px; }
    .rfm-row:last-child { margin-bottom: 0; }
    .rfm-label { width: 50px; color: #6b7280; }
    .rfm-track { flex: 1; height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
    .rfm-fill { height: 100%; border-radius: 4px; transition: width 1s ease; }
    .rfm-hv { background: linear-gradient(90deg, #52c41a, #95de64); }
    .rfm-pt { background: linear-gradient(90deg, #1890ff, #69c0ff); }
    .rfm-g { background: linear-gradient(90deg, #8c8c8c, #bfbfbf); }
    .rfm-ar { background: linear-gradient(90deg, #faad14, #ffd666); }
    .rfm-lv { background: linear-gradient(90deg, #ff4d4f, #ff7875); }
    .rfm-pct { width: 36px; text-align: right; color: #4b5563; font-weight: 600; }

    .text-wrap {
      position: absolute;
      bottom: 80px;
      width: 100%;
      text-align: center;
      animation: slideInLeft 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;

      h1 {
        font-size: 24px;
        font-weight: 500;
        color: var(--art-gray-900) !important;
      }
      p {
        margin-top: 10px;
        font-size: 14px;
        color: var(--art-gray-600) !important;
      }
    }

    .geometric-decorations {
      pointer-events: none;
      .geo-element {
        position: absolute;
        opacity: 0;
        animation-fill-mode: forwards;
        animation-duration: 0.8s;
        animation-timing-function: cubic-bezier(0.25, 0.46, 0.45, 0.94);
      }

      @mixin fadeAnimation($direction: '', $rotation: 0deg) {
        from { opacity: 0;
          @if $direction == 'up' { transform: translateY(30px) rotate($rotation); }
          @else if $direction == 'down' { transform: translateY(-30px) rotate($rotation); }
          @else if $direction == 'left' { transform: translateX(-30px) rotate($rotation); }
          @else if $direction == 'right' { transform: translateX(30px) rotate($rotation); }
        }
        to { opacity: 1;
          @if $direction == 'up' or $direction == 'down' { transform: translateY(0) rotate($rotation); }
          @else { transform: translateX(0) rotate($rotation); }
        }
      }

      @keyframes fadeInUp { @include fadeAnimation('up'); }
      @keyframes fadeInDown { @include fadeAnimation('down'); }
      @keyframes fadeInLeft { @include fadeAnimation('left'); }
      @keyframes fadeInLeftRotated { @include fadeAnimation('left', -25deg); }
      @keyframes fadeInRight { @include fadeAnimation('right'); }
      @keyframes fadeInRightRotated { @include fadeAnimation('right', 45deg); }
      @keyframes scaleIn { from { opacity: 0; transform: scale(0.8); } to { opacity: 1; transform: scale(1); } }
      @keyframes bounceIn {
        0% { opacity: 0; transform: scale(0.3); }
        50% { opacity: 1; transform: scale(1.05); }
        70% { transform: scale(0.9); }
        100% { opacity: 1; transform: scale(1); }
      }
      @keyframes slideInLeft { from { opacity: 0; transform: translateX(-30px); } to { opacity: 1; transform: translateX(0); } }

      .animate-fade-in-up { animation-name: fadeInUp; }
      .animate-fade-in-down { animation-name: fadeInDown; }
      .animate-fade-in-left { animation-name: fadeInLeft; }
      .animate-fade-in-right { animation-name: fadeInRight; }
      .animate-scale-in { animation-name: scaleIn; animation-duration: 1.2s; }
      .animate-bounce-in { animation-name: bounceIn; animation-duration: 0.6s; }

      .circle-outline { top: 10%; left: 25%; width: 42px; height: 42px; border: 2px solid $primary-light-8; border-radius: 50%; }
      .square-rotated { top: 18%; left: 14%; width: 50px; height: 50px; background-color: $bg-mix-light-8; transform: rotate(15deg); }
      .circle-small { bottom: 26%; left: 30%; width: 18px; height: 18px; background-color: $primary-light-8; border-radius: 50%; }
      .circle-top-right { top: 3%; right: 3%; z-index: 100; width: 50px; height: 50px; background: $bg-mix-light-7; border-radius: 50%; }
      .square-bottom-right { right: 10%; bottom: 22%; width: 50px; height: 50px; background-color: $primary-light-8; transform: rotate(30deg); }
      .bg-bubble { top: -120px; right: -120px; width: 360px; height: 360px; background-color: $bg-mix-light-8; border-radius: 50%; }
      .dot { width: 12px; height: 12px; background-color: $primary-light-7; border-radius: 50%; }
      .dot-top-left { top: 140px; left: 100px; }
      .dot-top-right { top: 140px; right: 120px; }
      .dot-center-right { top: 46%; right: 22%; background-color: $primary-light-8; }
    }

    @media only screen and (width <= 1600px) {
      width: 60vw;
      .text-wrap { bottom: 40px; }
      .left-visual { width: 65%; }
    }

    @media only screen and (width <= 1180px) {
      width: auto;
      height: auto;
      padding: 0;
      background: transparent;
      .left-img, .text-wrap, .geometric-decorations, .left-visual { display: none; }
      .logo { display: none; }
    }
  }

  /* 暗色主题适配 */
  .dark .login-left-view {
    background-color: color-mix(in srgb, $primary-light-9 60%, #070707);
    @media only screen and (width <= 1180px) { background: transparent; }
    .chart-card, .chart-mini {
      background: rgba(40, 40, 50, 0.85);
      border-color: rgba(255, 255, 255, 0.1);
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
    }
    .stat-item { background: rgba(255, 255, 255, 0.05); }
    .stat-label-small { color: #9ca3af; }
    .stat-value-small { color: #f3f4f6; }
    .chart-mini-title { color: #d1d5db; }
    .rfm-label, .rfm-pct { color: #d1d5db; }
    .rfm-track { background: rgba(255, 255, 255, 0.08); }
    .geometric-decorations {
      .circle-top-right { background-color: $bg-mix-light-8; rotate: -48deg; }
      .square-rotated, .square-bottom-right { background-color: $bg-mix-light-9; }
      .circle-small, .dot { background-color: $primary-light-8; }
      .bg-bubble { background-color: $bg-mix-light-9; }
    }
  }
</style>
