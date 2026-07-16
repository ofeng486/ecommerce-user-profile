<template>
  <div class="root">

    <!-- ═══ NAVBAR ═══ -->
    <nav class="fixed top-0 inset-x-0 z-50 bg-white/90 backdrop-blur-md border-b border-slate-100" style="font-family:'Inter','Plus Jakarta Sans',sans-serif">
      <div class="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        <a href="#/" class="flex items-center gap-2.5 no-underline">
          <span class="w-8 h-8 rounded-lg flex items-center justify-center text-white text-xs font-bold shadow-sm" style="background:#4F46E5">UP</span>
          <span class="font-bold text-lg text-slate-900 tracking-tight" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">UserProfile</span>
        </a>
        <div class="hidden md:flex items-center gap-8">
          <a href="#features" class="text-sm text-slate-500 no-underline font-medium transition-colors" style="font-family:'Inter',sans-serif">功能</a>
          <a href="#showcase" class="text-sm text-slate-500 no-underline font-medium transition-colors" style="font-family:'Inter',sans-serif">数据</a>
          <a href="#tech" class="text-sm text-slate-500 no-underline font-medium transition-colors" style="font-family:'Inter',sans-serif">技术栈</a>
        </div>
        <button @click="showLogin=true" class="inline-flex items-center px-5 py-2 rounded-full text-white text-sm font-semibold border-none cursor-pointer transition-all shadow-sm hover:shadow-md" style="background:#4F46E5;font-family:'Inter',sans-serif">登录系统</button>
      </div>
    </nav>

    <!-- ═══ HERO ═══ -->
    <section class="relative pt-32 pb-20 md:pt-40 md:pb-28 overflow-hidden">
      <div class="absolute inset-0 pointer-events-none" style="background:radial-gradient(ellipse at 50% -10%,rgba(99,102,241,0.06),transparent 60%)"></div>
      <div class="absolute inset-0 pointer-events-none" style="background:radial-gradient(ellipse at 80% 40%,rgba(99,102,241,0.03),transparent 50%)"></div>
      <div class="max-w-6xl mx-auto px-6 text-center relative z-10">
        <div class="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-full border text-xs font-medium mb-8" style="background:#eef2ff;border-color:#e0e7ff;color:#4338ca;font-family:'Inter',sans-serif">
          <span class="w-1.5 h-1.5 rounded-full" style="background:#6366f1"></span>
          v2.0 — 电商用户画像分析平台
        </div>
        <h1 class="text-5xl md:text-7xl font-bold tracking-tight text-slate-900 leading-[1.08] max-w-4xl mx-auto" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">洞察亿级用户<br/><span style="color:#4F46E5">重塑电商增长引擎</span></h1>
        <p class="mt-6 text-lg md:text-xl text-slate-500 max-w-2xl mx-auto leading-relaxed" style="font-family:'Inter','Plus Jakarta Sans',sans-serif">基于 Spark 分布式计算与 DeepSeek AI，自动化构建用户标签、RFM 分层与画像分析。让每个运营决策都有数据支撑。</p>
        <div class="mt-10 flex items-center justify-center gap-4">
          <button @click="showLogin=true" class="inline-flex items-center px-7 py-3.5 rounded-full text-white text-sm font-semibold border-none cursor-pointer transition-all shadow-sm hover:shadow-md hover:-translate-y-0.5" style="background:#4F46E5;font-family:'Inter',sans-serif">开始体验<svg class="ml-2 w-4 h-4" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="M5 12h14M12 5l7 7-7 7"/></svg></button>
          <router-link to="/public/dashboard" class="inline-flex items-center px-7 py-3.5 rounded-full text-sm font-medium no-underline transition-all" style="border:1px solid #e2e8f0;color:#64748b;font-family:'Inter',sans-serif">查看大屏</router-link>
        </div>
        <!-- Dashboard Mockup -->
        <div class="mt-16 max-w-4xl mx-auto bg-white border border-slate-200 rounded-2xl overflow-hidden" style="box-shadow:0 10px 25px -3px rgba(0,0,0,0.04),0 4px 10px -4px rgba(0,0,0,0.02)">
          <div class="flex items-center gap-2 px-5 py-3.5 border-b border-slate-100" style="background:#f8fafc/80"><span class="w-3 h-3 rounded-full" style="background:#ef4444"></span><span class="w-3 h-3 rounded-full" style="background:#f59e0b"></span><span class="w-3 h-3 rounded-full" style="background:#10b981"></span><span class="ml-3 text-xs text-slate-400 font-mono">dashboard.overview</span></div>
          <div class="p-6">
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-5">
              <div class="p-4 rounded-xl text-left" style="background:#f8fafc;border:1px solid #f1f5f9"><p class="text-xs text-slate-400 font-medium mb-1">总用户</p><p class="text-xl font-bold text-slate-900" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">1,284,562</p><p class="text-xs mt-1" style="color:#10b981">↑ 12.3%</p></div>
              <div class="p-4 rounded-xl text-left" style="background:#f8fafc;border:1px solid #f1f5f9"><p class="text-xs text-slate-400 font-medium mb-1">已画像</p><p class="text-xl font-bold text-slate-900" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">982,340</p><p class="text-xs mt-1" style="color:#10b981">↑ 8.7%</p></div>
              <div class="p-4 rounded-xl text-left" style="background:#f8fafc;border:1px solid #f1f5f9"><p class="text-xs text-slate-400 font-medium mb-1">高价值</p><p class="text-xl font-bold text-slate-900" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">156,892</p><p class="text-xs mt-1" style="color:#f59e0b">→ 0.4%</p></div>
              <div class="p-4 rounded-xl text-left" style="background:#f8fafc;border:1px solid #f1f5f9"><p class="text-xs text-slate-400 font-medium mb-1">消费总额</p><p class="text-xl font-bold text-slate-900" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">¥2.8 亿</p><p class="text-xs mt-1" style="color:#10b981">↑ 23.1%</p></div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div class="md:col-span-2 p-5 rounded-xl" style="background:#f8fafc;border:1px solid #f1f5f9"><svg viewBox="0 0 360 120" class="w-full"><polyline fill="none" stroke="#6366f1" stroke-width="2.5" points="0,90 30,70 60,80 90,50 120,65 150,35 180,55 210,25 240,45 270,20 300,40 330,15" /><polyline fill="none" stroke="#94a3b8" stroke-width="1.5" points="0,100 30,88 60,78 90,68 120,62 150,52 180,45 210,38 240,32 270,28 300,22 330,18" opacity="0.5" /><defs><linearGradient id="lg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#6366f1" stop-opacity="0.12"/><stop offset="1" stop-color="#6366f1" stop-opacity="0"/></linearGradient></defs><path fill="url(#lg)" d="M0,90 L30,70 L60,80 L90,50 L120,65 L150,35 L180,55 L210,25 L240,45 L270,20 L300,40 L330,15 L330,120 L0,120 Z" /></svg></div>
              <div class="p-4 rounded-xl flex items-center justify-center" style="background:#f8fafc;border:1px solid #f1f5f9"><svg viewBox="0 0 120 120" class="w-28"><circle cx="60" cy="60" r="46" fill="none" stroke="#e2e8f0" stroke-width="12" /><circle cx="60" cy="60" r="46" fill="none" stroke="#6366f1" stroke-width="12" stroke-dasharray="140 220" transform="rotate(-90 60 60)" /><circle cx="60" cy="60" r="32" fill="none" stroke="#a5b4fc" stroke-width="8" stroke-dasharray="70 260" transform="rotate(-90 60 60)" /></svg></div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ FEATURES ═══ -->
    <section id="features" class="py-24 md:py-32" style="background:#f8fafc">
      <div class="max-w-6xl mx-auto px-6">
        <div class="text-center max-w-2xl mx-auto mb-16">
          <span class="inline-flex px-4 py-1.5 rounded-full border text-xs font-medium mb-5" style="background:#eef2ff;border-color:#e0e7ff;color:#4338ca;font-family:'Inter',sans-serif">Core Features</span>
          <h2 class="text-3xl md:text-4xl font-bold tracking-tight text-slate-900" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">全方位的用户画像构建能力</h2>
          <p class="mt-4 text-slate-500 leading-relaxed" style="font-family:'Inter',sans-serif">覆盖数据处理、AI 分析、画像构建与可视化的完整产品矩阵</p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div v-for="(f,i) in features" :key="i" class="bg-white border border-slate-200 rounded-2xl p-7 card-hover obs" style="box-shadow:0 1px 3px rgba(0,0,0,0.02);transition:box-shadow 0.3s ease,transform 0.1s ease" @mousemove="handleCardTilt" @mouseleave="resetCardTilt">
            <span class="inline-flex w-11 h-11 rounded-xl items-center justify-center mb-4" :style="{background:f.bg,color:f.color}" v-html="f.icon"></span>
            <h3 class="font-semibold text-lg text-slate-900 mb-2" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">{{ f.title }}</h3>
            <p class="text-sm text-slate-500 leading-relaxed" style="font-family:'Inter',sans-serif">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ DATA SHOWCASE ═══ -->
    <section id="showcase" class="py-24 md:py-32">
      <div class="max-w-6xl mx-auto px-6">
        <div class="text-center max-w-2xl mx-auto mb-16">
          <span class="inline-flex px-4 py-1.5 rounded-full border text-xs font-medium mb-5" style="background:#eef2ff;border-color:#e0e7ff;color:#4338ca;font-family:'Inter',sans-serif">Data Visualization</span>
          <h2 class="text-3xl md:text-4xl font-bold tracking-tight text-slate-900" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">直观的数据洞察，驱动精准决策</h2>
          <p class="mt-4 text-slate-500 leading-relaxed" style="font-family:'Inter',sans-serif">多维度可视化分析，让用户画像一目了然</p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-5 gap-6">
          <div class="md:col-span-3 bg-white border border-slate-200 rounded-2xl p-6" style="box-shadow:0 4px 6px -1px rgba(0,0,0,0.04)">
            <div class="flex items-center justify-between mb-4"><h3 class="font-semibold text-slate-900" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">用户活跃趋势</h3><span class="text-xs text-slate-400 font-medium">近 30 日</span></div>
            <svg viewBox="0 0 400 160" class="w-full"><line x1="0" y1="140" x2="400" y2="140" stroke="#e2e8f0" stroke-width="1" /><line x1="0" y1="100" x2="400" y2="100" stroke="#f1f5f9" stroke-width="1" /><line x1="0" y1="60" x2="400" y2="60" stroke="#f1f5f9" stroke-width="1" /><polyline fill="none" stroke="#6366f1" stroke-width="2.5" points="0,130 20,118 40,124 60,100 80,110 100,85 120,95 140,70 160,82 180,56 200,68 220,44 240,58 260,36 280,50 300,28 320,42 340,22 360,34 380,18" /><defs><linearGradient id="g2" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#6366f1" stop-opacity="0.10"/><stop offset="1" stop-color="#6366f1" stop-opacity="0"/></linearGradient></defs><path fill="url(#g2)" d="M0,130 L20,118 L40,124 L60,100 L80,110 L100,85 L120,95 L140,70 L160,82 L180,56 L200,68 L220,44 L240,58 L260,36 L280,50 L300,28 L320,42 L340,22 L360,34 L380,18 L380,140 L0,140 Z" /><polyline fill="none" stroke="#a5b4fc" stroke-width="1.5" points="0,138 20,132 40,128 60,122 80,116 100,110 120,104 140,98 160,92 180,86 200,80 220,74 240,68 260,62 280,56 300,50 320,44 340,38 360,32 380,26" opacity="0.6" /></svg>
          </div>
          <div class="md:col-span-2 bg-white border border-slate-200 rounded-2xl p-6" style="box-shadow:0 4px 6px -1px rgba(0,0,0,0.04)">
            <h3 class="font-semibold text-slate-900 mb-4" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">RFM 用户分层</h3>
            <svg viewBox="0 0 200 200" class="w-48 mx-auto"><circle cx="100" cy="100" r="80" fill="none" stroke="#e2e8f0" stroke-width="16" /><circle cx="100" cy="100" r="80" fill="none" stroke="#6366f1" stroke-width="16" stroke-dasharray="130 380" transform="rotate(-90 100 100)" /><circle cx="100" cy="100" r="80" fill="none" stroke="#a5b4fc" stroke-width="16" stroke-dasharray="95 380" transform="rotate(44 100 100)" /><circle cx="100" cy="100" r="80" fill="none" stroke="#c7d2fe" stroke-width="16" stroke-dasharray="65 380" transform="rotate(135 100 100)" /><circle cx="100" cy="100" r="80" fill="none" stroke="#e0e7ff" stroke-width="16" stroke-dasharray="40 380" transform="rotate(200 100 100)" /><circle cx="100" cy="100" r="80" fill="none" stroke="#f1f5f9" stroke-width="16" stroke-dasharray="30 380" transform="rotate(240 100 100)" /></svg>
            <div class="grid grid-cols-2 gap-2 mt-3 text-xs"><div class="flex items-center gap-2"><span class="w-2.5 h-2.5 rounded-full" style="background:#4F46E5"></span><span class="text-slate-500">高价值</span></div><div class="flex items-center gap-2"><span class="w-2.5 h-2.5 rounded-full" style="background:#818cf8"></span><span class="text-slate-500">潜力</span></div><div class="flex items-center gap-2"><span class="w-2.5 h-2.5 rounded-full" style="background:#a5b4fc"></span><span class="text-slate-500">一般</span></div><div class="flex items-center gap-2"><span class="w-2.5 h-2.5 rounded-full" style="background:#c7d2fe"></span><span class="text-slate-500">流失风险</span></div></div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ STATS ═══ -->
    <section ref="statsEl" class="py-20 border-y" style="background:#eef2ff/70;border-color:#e0e7ff/50">
      <div class="max-w-6xl mx-auto px-6">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-10 text-center">
          <div v-for="s in statsData" :key="s.label"><p class="text-4xl font-bold text-slate-900 tracking-tight" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">{{ s.current }}{{ s.suffix }}</p><p class="text-sm text-slate-500 mt-1.5" style="font-family:'Inter',sans-serif">{{ s.label }}</p></div>
        </div>
      </div>
    </section>

    <!-- ═══ LIVE DEMO — 交互式画像体验 ═══ -->
    <section id="demo" class="py-24 md:py-32" style="background:#f8fafc">
      <div class="max-w-3xl mx-auto px-6 text-center">
        <span class="inline-flex px-4 py-1.5 rounded-full border text-xs font-medium mb-5" style="background:#eef2ff;border-color:#e0e7ff;color:#4338ca;font-family:'Inter',sans-serif">Live Demo</span>
        <h2 class="text-3xl md:text-4xl font-bold tracking-tight text-slate-900" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">3 秒生成用户 360° 画像</h2>
        <p class="mt-4 text-slate-500 leading-relaxed max-w-xl mx-auto" style="font-family:'Inter',sans-serif">输入任意用户 ID，体验我们基于 Spark 与 RFM 模型构建的精准画像能力。</p>

        <div class="mt-6 flex items-center gap-3 max-w-lg mx-auto">
          <input v-model="demoInput" @keyup.enter="doDemoSearch" class="demo-input flex-1" placeholder="请输入用户 ID（试试输入 1001 或 8848）" />
          <button @click="doDemoSearch" class="demo-btn" :disabled="!demoInput.trim()">生成画像<svg class="ml-1.5 w-4 h-4" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="M5 12h14M12 5l7 7-7 7"/></svg></button>
        </div>

        <p v-if="showShake" class="text-xs mt-2 shake-text">请输入用户 ID</p>

        <div v-if="showResult" class="demo-result-wrap">
          <div class="demo-result-card">
            <div class="flex items-start justify-between mb-4">
              <div>
                <div class="flex items-center gap-2 mb-1">
                  <span class="text-sm font-bold text-slate-900" style="font-family:'Plus Jakarta Sans',sans-serif">{{ demoUser.userCode }}</span>
                  <span class="inline-flex px-2 py-0.5 rounded text-[10px] font-semibold" :style="{background:segmentBg(demoUser.segment),color:segmentColor(demoUser.segment)}">{{ demoUser.segment }}</span>
                </div>
                <p class="text-xs" style="color:#94a3b8">{{ demoUser.gender }} · {{ demoUser.age }} 岁 · {{ demoUser.province }}</p>
              </div>
              <div class="text-right"><p class="text-xs" style="color:#94a3b8">综合评分</p><p class="text-xl font-bold" :style="{color:scoreColor(demoUser.score)}">{{ demoUser.score }}</p></div>
            </div>
            <div class="grid grid-cols-2 gap-3 mb-4">
              <div v-for="m in demoUser.metrics" :key="m.l" class="p-3 rounded-xl text-left" style="background:#f8fafc;border:1px solid #f1f5f9"><p class="text-xs" style="color:#94a3b8">{{ m.l }}</p><p class="text-base font-bold text-slate-900 mt-0.5" style="font-family:'Plus Jakarta Sans',sans-serif">{{ m.v }}</p></div>
            </div>
            <div class="flex items-center gap-2 pt-3 border-t" style="border-color:#f1f5f9"><span class="text-xs" style="color:#94a3b8">标签</span><span v-for="t in demoUser.tags" :key="t" class="inline-flex px-2.5 py-1 rounded-full text-[11px] font-medium" style="background:#eef2ff;color:#4F46E5">{{ t }}</span></div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ TECH ═══ -->
    <section id="tech" class="py-20 overflow-hidden">
      <div class="text-center mb-12 px-6">
        <span class="inline-flex px-4 py-1.5 rounded-full border text-xs font-medium mb-5" style="background:#f1f5f9;border-color:#e2e8f0;color:#64748b;font-family:'Inter',sans-serif">Tech Stack</span>
        <h2 class="text-2xl md:text-3xl font-bold tracking-tight text-slate-900" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">技术栈</h2>
      </div>
      <div class="flex flex-col gap-4">
        <div class="flex gap-4 marquee-anim" style="width:max-content"><span v-for="t in doubledTechs" :key="t+'a'" class="px-6 py-2.5 rounded-full text-sm font-medium" style="background:#f8fafc;border:1px solid #e2e8f0;color:#94a3b8;font-family:'Inter',sans-serif;white-space:nowrap">{{ t }}</span></div>
        <div class="flex gap-4 marquee-anim marquee-rev" style="width:max-content"><span v-for="t in doubledTechs" :key="t+'b'" class="px-6 py-2.5 rounded-full text-sm font-medium" style="background:#f8fafc;border:1px solid #e2e8f0;color:#94a3b8;font-family:'Inter',sans-serif;white-space:nowrap">{{ t }}</span></div>
      </div>
    </section>

    <!-- ═══ CTA ═══ -->
    <section class="py-24 md:py-32" style="background:#f8fafc">
      <div class="max-w-xl mx-auto px-6 text-center">
        <h2 class="text-3xl md:text-4xl font-bold tracking-tight text-slate-900" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">准备好开始了吗？</h2>
        <p class="mt-4 text-slate-500 leading-relaxed" style="font-family:'Inter',sans-serif">体验完整的用户画像分析平台，无需部署，在线使用。</p>
        <button @click="showLogin=true" class="inline-flex items-center px-8 py-4 rounded-full text-white text-sm font-semibold border-none cursor-pointer transition-all shadow-sm hover:shadow-md hover:-translate-y-0.5 mt-10" style="background:#4F46E5;font-family:'Inter',sans-serif">开始使用<svg class="ml-2 w-4 h-4" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="M5 12h14M12 5l7 7-7 7"/></svg></button>
      </div>
    </section>

    <LoginDialog v-model="showLogin" @login-success="handleLoginSuccess" />

    <footer class="py-12 bg-white border-t border-slate-100">
      <div class="max-w-6xl mx-auto px-6 flex flex-col md:flex-row items-center justify-between gap-4">
        <div class="flex items-center gap-2.5"><span class="w-7 h-7 rounded-lg flex items-center justify-center text-white text-[10px] font-bold" style="background:#4F46E5">UP</span><span class="text-sm font-semibold text-slate-700" style="font-family:'Plus Jakarta Sans','Inter',sans-serif">UserProfile</span></div>
        <p class="text-xs text-slate-400" style="font-family:'Inter',sans-serif">© 2026 UserProfile. Ecommerce User Profile Analytics. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import LoginDialog from '@/components/core/views/LoginDialog.vue'

defineOptions({ name: 'LandingPage' })
const r = useRouter()
const ro = useRoute()
const showLogin = ref(false)

const features = [
  { title:'Spark 离线计算', desc:'基于 PySpark 分布式计算引擎，支持本地与集群双模式，处理百级到十万级用户数据。', bg:'#eef2ff', color:'#4F46E5', icon:'<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>' },
  { title:'RFM 智能分层', desc:'NTILE(5) 打分算法，综合最近消费、频次、金额三维度，8 分类映射 5 层用户价值。', bg:'#ecfdf5', color:'#059669', icon:'<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20V10M18 20V4M6 20v-4"/></svg>' },
  { title:'全维标签体系', desc:'活跃度、消费力、偏好品类、RFM 四维标签全覆盖。基于行为的加权评分，支持扩展。', bg:'#fffbeb', color:'#d97706', icon:'<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z"/></svg>' },
  { title:'智能人群圈选', desc:'12 维度条件自由组合，AND/OR 逻辑嵌套。保存人群包，一键多包横向对比分析。', bg:'#fff1f2', color:'#e11d48', icon:'<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>' },
  { title:'AI 智能分析师', desc:'基于 DeepSeek 大模型，自然语言直接查询画像数据库。SSE 流式实时响应，无需 SQL。', bg:'#f0f9ff', color:'#0284c7', icon:'<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2a10 10 0 0110 10c0 5-4 9-10 9S2 17 2 12 6 2 12 2z"/><path d="M8 12h8"/><path d="M12 8v8"/></svg>' },
  { title:'ECharts 可视化', desc:'基于 ECharts 6 数据大屏，支持 RFM 玫瑰图、趋势折线、雷达图、省份地图等 10+ 图表。', bg:'#faf5ff', color:'#9333ea', icon:'<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>' },
]

const doubledTechs = computed(() => {
  const t = ['Apache Spark','PySpark','HDFS','Hive','DeepSeek','Spring Boot 3.3','MyBatis-Plus','Vue 3','ECharts','MySQL 8.0','Python 3.10','TailwindCSS']
  return [...t, ...t]
})

// ═══ 数字滚动计数 ═══
const statsEl = ref<HTMLElement>()
const statsData = ref([
  { target: 19, suffix: '', label: '数据表', current: 0, inView: false },
  { target: 35, suffix: '+', label: 'API 接口', current: 0, inView: false },
  { target: 5, suffix: '', label: '用户分层', current: 0, inView: false },
  { target: 1000, suffix: '+', label: '画像用户', current: 0, inView: false },
])
let countAnimFrame: number | null = null
function startCountUp() {
  const items = statsData.value.filter(s => s.inView && s.current < s.target)
  if (items.length === 0) return
  items.forEach(s => { s.current = Math.min(s.current + Math.ceil(s.target / 40), s.target) })
  countAnimFrame = requestAnimationFrame(startCountUp)
}

// ═══ 卡片 3D 倾角 ═══
function handleCardTilt(e: MouseEvent) {
  const card = e.currentTarget as HTMLElement
  const rect = card.getBoundingClientRect()
  const x = (e.clientX - rect.left) / rect.width - 0.5
  const y = (e.clientY - rect.top) / rect.height - 0.5
  card.style.transform = `perspective(800px) rotateY(${x * 6}deg) rotateX(${-y * 6}deg) translateY(-4px)`
}
function resetCardTilt(e: MouseEvent) {
  const card = e.currentTarget as HTMLElement
  card.style.transform = 'perspective(800px) rotateY(0deg) rotateX(0deg) translateY(0px)'
}

// ═══ 交互式 Demo ═══
const demoInput = ref('')
const showResult = ref(false)
const showShake = ref(false)
const demoDB: Record<string, any> = {
  '1001': { userCode:'UC1001', segment:'高价值', gender:'男', age:28, province:'广东省', score:4.7, metrics:[{l:'累计消费',v:'¥86,520'},{l:'订单数',v:'23'},{l:'30日浏览',v:'156 次'},{l:'客单价',v:'¥3,762'}], tags:['高消费','高频次','数码偏好'] },
  '8848': { userCode:'UC8848', segment:'一般用户', gender:'女', age:35, province:'浙江省', score:2.3, metrics:[{l:'累计消费',v:'¥12,380'},{l:'订单数',v:'5'},{l:'30日浏览',v:'38 次'},{l:'客单价',v:'¥2,476'}], tags:['中等消费','低频次','家居偏好'] },
  '9527': { userCode:'UC9527', segment:'流失风险', gender:'男', age:42, province:'上海市', score:1.8, metrics:[{l:'累计消费',v:'¥5,200'},{l:'订单数',v:'2'},{l:'30日浏览',v:'12 次'},{l:'客单价',v:'¥2,600'}], tags:['低消费','低频次','已流失'] },
}
function doDemoSearch() {
  showShake.value = false
  const id = demoInput.value.trim()
  if (!id) { showShake.value = true; return }
  const user = demoDB[id] || { userCode:`UC${id}`, segment:'未分类', gender:'未知', age:Math.floor(Math.random()*40+20), province:'未知', score:0, metrics:[{l:'累计消费',v:'—'},{l:'订单数',v:'—'},{l:'30日浏览',v:'—'},{l:'客单价',v:'—'}], tags:['待分析'] }
  demoUser.value = user
  showResult.value = false
  nextTick(() => { showResult.value = true })
}
const demoUser = ref(demoDB['1001'])
function segmentBg(s: string) { return s === '高价值' ? '#d1fae5' : s === '流失风险' ? '#fee2e2' : s === '一般用户' ? '#f1f5f9' : '#eef2ff' }
function segmentColor(s: string) { return s === '高价值' ? '#059669' : s === '流失风险' ? '#dc2626' : s === '一般用户' ? '#64748b' : '#4F46E5' }
function scoreColor(s: number) { return s >= 4 ? '#059669' : s >= 3 ? '#4F46E5' : s >= 2 ? '#d97706' : '#dc2626' }

let obsRefs: IntersectionObserver[] = []
onMounted(() => {
  if (ro.query.login === 'true') showLogin.value = true
  document.querySelectorAll('a:not([href^="#/"])[href^="#"]').forEach(el => {
    el.addEventListener('click', (e) => { e.preventDefault(); const id = (el as HTMLAnchorElement).getAttribute('href')?.slice(1); if (id) document.getElementById(id)?.scrollIntoView({ behavior:'smooth' }) })
  })
  nextTick(() => {
    document.querySelectorAll('.obs').forEach(el => { const io = new IntersectionObserver(entries => { entries.forEach(e => { if (e.isIntersecting) { e.target.classList.add('vis'); io.unobserve(e.target) } }) }, { threshold:0.1 }); io.observe(el); obsRefs.push(io) })
    if (statsEl.value) { const io = new IntersectionObserver(([entry]) => { if (entry.isIntersecting) { statsData.value.forEach(s => { s.inView = true }); startCountUp(); io.disconnect() } }, { threshold:0.5 }); io.observe(statsEl.value); obsRefs.push(io) }
  })
})
onUnmounted(() => { obsRefs.forEach(io => io.disconnect()); obsRefs = []; if (countAnimFrame) cancelAnimationFrame(countAnimFrame) })
const handleLoginSuccess = () => { r.push((ro.query.redirect as string) || '/dashboard/overview') }
</script>

<style scoped>
.root { -webkit-font-smoothing: antialiased; }
.obs { transform: translateY(24px); filter: blur(4px); transition: all 0.7s cubic-bezier(0.16, 1, 0.3, 1); }
.vis { transform: translateY(0); filter: blur(0); }

@keyframes marquee { 0% { transform: translateX(0); } 100% { transform: translateX(-50%); } }
.marquee-anim { animation: marquee 36s linear infinite; }
.marquee-rev { animation-direction: reverse; animation-duration: 42s; }

.card-hover { transition: box-shadow 0.3s ease; }

.demo-input { display: block; width: 100%; padding: 12px 16px; border-radius: 10px; border: 1.5px solid #e2e8f0; font-size: 14px; color: #1e293b; background: #FFF; outline: none; font-family: 'Inter',sans-serif; transition: all 0.2s; box-shadow: 0 2px 8px rgba(0,0,0,0.02); }
.demo-input:focus { border-color: #4F46E5; box-shadow: 0 0 0 3px rgba(79,70,229,0.08); }
.demo-input::placeholder { color: #94a3b8; }
.demo-btn { display: inline-flex; align-items: center; padding: 12px 24px; border-radius: 10px; border: none; background: #4F46E5; color: #FFF; font-size: 14px; font-weight: 600; cursor: pointer; font-family: 'Inter',sans-serif; transition: all 0.2s; white-space: nowrap; flex-shrink: 0; }
.demo-btn:hover:not(:disabled) { background: #4338ca; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(79,70,229,0.2); }
.demo-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.shake-text { color:#ef4444; animation: shake 0.4s ease; }
@keyframes shake { 0%,100% { transform: translateX(0); } 25% { transform: translateX(-4px); } 75% { transform: translateX(4px); } }

.demo-result-wrap { overflow: hidden; max-height: 600px; animation: slideDown 0.5s cubic-bezier(0.16, 1, 0.3, 1); }
@keyframes slideDown { from { max-height: 0; opacity: 0; transform: translateY(-10px); } to { max-height: 600px; opacity: 1; transform: translateY(0); } }
.demo-result-card { margin-top: 24px; padding: 24px; border-radius: 16px; background: rgba(255,255,255,0.85); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); border: 1px solid rgba(226,232,240,0.5); text-align: left; box-shadow: 0 8px 32px rgba(0,0,0,0.04); }
</style>