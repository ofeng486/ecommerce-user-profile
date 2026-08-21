<!-- 认证页面共享布局（亮色 Soft Structuralism：品牌区 + 表单区分栏） -->
<template>
  <div class="auth-layout">
    <!-- 左侧品牌区 -->
    <div class="auth-brand">
      <div class="brand-glow g1" aria-hidden="true"></div>
      <div class="brand-glow g2" aria-hidden="true"></div>
      <div class="brand-box">
        <div class="brand-logo">
          <svg
            width="26"
            height="26"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path
              d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"
            />
            <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
            <line x1="12" y1="22.08" x2="12" y2="12" />
          </svg>
        </div>
        <h1 class="brand-title">电商用户画像分析系统</h1>
        <p class="brand-sub">基于 Spark 分布式计算引擎，让每一位电商用户可被理解</p>
        <ul class="brand-points">
          <li><span class="bp-dot"></span>多维用户标签画像</li>
          <li><span class="bp-dot"></span>RFM 价值分层体系</li>
          <li><span class="bp-dot"></span>AI 智能分析助手</li>
        </ul>
      </div>
    </div>

    <!-- 右侧内容区 -->
    <div class="auth-content">
      <div class="auth-content-inner">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  defineOptions({ name: 'AuthLayout' })
</script>

<style scoped>
  .auth-layout {
    display: flex;
    width: 100%;
    min-height: 100vh;
    background: #f4f6fa;
    font-family: 'Plus Jakarta Sans', 'Inter', 'PingFang SC', system-ui, sans-serif;
  }

  /* ─── 左侧品牌区（亮色 Soft Structuralism） ─── */
  .auth-brand {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 1.15;
    padding: 48px;
    position: relative;
    overflow: hidden;
    background:
      linear-gradient(rgba(15, 23, 42, 0.02) 1px, transparent 1px),
      linear-gradient(90deg, rgba(15, 23, 42, 0.02) 1px, transparent 1px),
      linear-gradient(180deg, #f4f6fa 0%, #eef2f8 100%);
    background-size:
      28px 28px,
      28px 28px,
      100% 100%;
  }
  .brand-glow {
    position: absolute;
    inset: 0;
    pointer-events: none;
  }
  .g1 {
    background: radial-gradient(
      ellipse 55% 40% at 18% 12%,
      rgba(37, 99, 235, 0.08),
      transparent 65%
    );
  }
  .g2 {
    background: radial-gradient(
      ellipse 45% 35% at 85% 85%,
      rgba(13, 148, 136, 0.05),
      transparent 60%
    );
  }

  .brand-box {
    position: relative;
    z-index: 1;
    max-width: 420px;
    animation: brandRise 0.7s cubic-bezier(0.32, 0.72, 0, 1) both;
  }
  .brand-logo {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #1e40af, #2563eb);
    color: #fff;
    border-radius: 16px;
    margin-bottom: 26px;
    box-shadow: 0 10px 30px rgba(37, 99, 235, 0.28);
  }
  .brand-title {
    font-size: 30px;
    font-weight: 700;
    margin: 0 0 12px;
    letter-spacing: -0.5px;
    color: #0f172a;
    font-family: 'Space Grotesk', 'Plus Jakarta Sans', 'PingFang SC', sans-serif;
    line-height: 1.2;
  }
  .brand-sub {
    font-size: 14.5px;
    color: #64748b;
    margin: 0 0 34px;
    line-height: 1.7;
  }
  .brand-points {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .brand-points li {
    display: inline-flex;
    align-items: center;
    gap: 12px;
    font-size: 13.5px;
    color: #475569;
    padding: 11px 18px;
    background: rgba(255, 255, 255, 0.75);
    border: 1px solid rgba(15, 23, 42, 0.06);
    border-radius: 12px;
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
  }
  .bp-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: linear-gradient(135deg, #2563eb, #60a5fa);
    flex-shrink: 0;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
  }

  /* ─── 右侧内容区 ─── */
  .auth-content {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff;
    padding: 40px;
    border-left: 1px solid rgba(15, 23, 42, 0.05);
    box-shadow: -12px 0 40px rgba(15, 23, 42, 0.05);
  }
  .auth-content-inner {
    width: 100%;
    max-width: 340px;
  }

  /* ─── 入场动效 ─── */
  @keyframes brandRise {
    from {
      opacity: 0;
      transform: translateY(18px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  /* ─── 响应式 ─── */
  @media (max-width: 900px) {
    .auth-layout {
      flex-direction: column;
    }
    .auth-brand {
      display: none;
    }
    .auth-content {
      width: 100%;
      min-height: 100vh;
      border-left: none;
      box-shadow: none;
    }
  }

  /* ─── 无障碍：减弱动效 ─── */
  @media (prefers-reduced-motion: reduce) {
    .brand-box {
      animation: none;
    }
  }
</style>
