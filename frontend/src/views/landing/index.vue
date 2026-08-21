<template>
  <div class="root">
    <!-- ═══ NAVBAR — 悬浮胶囊岛 ═══ -->
    <nav class="nav-island">
      <a href="#/" class="nav-brand">
        <span class="brand-mark">UP</span>
        <span class="brand-name">UserProfile</span>
      </a>
      <div class="nav-links">
        <a href="#features">功能</a>
        <a href="#showcase">数据</a>
        <a href="#tech">技术栈</a>
      </div>
      <a href="#portals" class="nav-login">登录系统</a>
    </nav>

    <!-- ═══ HERO ═══ -->
    <section class="hero">
      <div class="hero-glow g1" aria-hidden="true"></div>
      <div class="hero-glow g2" aria-hidden="true"></div>
      <div class="hero-glow g3" aria-hidden="true"></div>

      <div class="hero-content">
        <span class="eyebrow"
          ><span class="eyebrow-dot" aria-hidden="true"></span>电商用户画像分析平台</span
        >
        <h1 class="hero-title">
          洞察海量用户行为<br />
          <span class="hero-accent">重塑电商增长引擎</span>
        </h1>
        <p class="hero-sub">
          基于 Spark 分布式计算引擎，自动化构建用户标签、RFM
          分层与多维画像，让每一位电商用户可被理解，让每一个运营决策都有数据支撑。
        </p>

        <div class="hero-cta">
          <a href="#demo" class="cta-primary">
            体验画像分析
            <span class="cta-arrow">
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M5 12h14M12 5l7 7-7 7" />
              </svg>
            </span>
          </a>
        </div>

        <!-- Dashboard Mockup（Double-Bezel） -->
        <div class="bezel mockup">
          <div class="card mockup-card">
            <div class="mockup-bar">
              <span class="dot r" aria-hidden="true"></span>
              <span class="dot y" aria-hidden="true"></span>
              <span class="dot g" aria-hidden="true"></span>
              <span class="mockup-bar-tag">dashboard.overview</span>
              <span class="mockup-demo-tag">DEMO DATA</span>
            </div>
            <div class="mockup-body">
              <div class="kpi-grid">
                <div v-for="k in mockKpis" :key="k.label" class="mkpi">
                  <p class="mkpi-label">{{ k.label }}</p>
                  <p class="mkpi-val">{{ k.value }}</p>
                  <p class="mkpi-delta" :style="{ color: k.up ? '#059669' : '#D97706' }">{{
                    k.delta
                  }}</p>
                </div>
              </div>
              <div class="mockup-charts">
                <div class="mockup-line">
                  <svg viewBox="0 0 360 120" class="w-full" aria-hidden="true">
                    <defs>
                      <linearGradient id="mlg" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0" stop-color="#3B82F6" stop-opacity="0.12" />
                        <stop offset="1" stop-color="#3B82F6" stop-opacity="0" />
                      </linearGradient>
                    </defs>
                    <polyline
                      fill="none"
                      stroke="#3B82F6"
                      stroke-width="2.5"
                      points="0,90 30,70 60,80 90,50 120,65 150,35 180,55 210,25 240,45 270,20 300,40 330,15"
                    />
                    <polyline
                      fill="none"
                      stroke="#94A3B8"
                      stroke-width="1.5"
                      points="0,100 30,88 60,78 90,68 120,62 150,52 180,45 210,38 240,32 270,28 300,22 330,18"
                      opacity="0.5"
                    />
                    <path
                      fill="url(#mlg)"
                      d="M0,90 L30,70 L60,80 L90,50 L120,65 L150,35 L180,55 L210,25 L240,45 L270,20 L300,40 L330,15 L330,120 L0,120 Z"
                    />
                  </svg>
                </div>
                <div class="mockup-donut">
                  <svg viewBox="0 0 120 120" class="w-28" aria-hidden="true">
                    <circle cx="60" cy="60" r="46" fill="none" stroke="#E2E8F0" stroke-width="12" />
                    <circle
                      cx="60"
                      cy="60"
                      r="46"
                      fill="none"
                      stroke="#3B82F6"
                      stroke-width="12"
                      stroke-dasharray="140 220"
                      transform="rotate(-90 60 60)"
                    />
                    <circle
                      cx="60"
                      cy="60"
                      r="32"
                      fill="none"
                      stroke="#93C5FD"
                      stroke-width="8"
                      stroke-dasharray="70 260"
                      transform="rotate(-90 60 60)"
                    />
                  </svg>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ FEATURES ═══ -->
    <section id="features" class="section alt">
      <div class="section-head">
        <span class="eyebrow">Core Features</span>
        <h2 class="section-title">全方位的用户画像构建能力</h2>
        <p class="section-sub">覆盖数据处理、AI 分析、画像构建与可视化的完整产品矩阵</p>
      </div>
      <div class="feat-grid">
        <div
          v-for="(f, i) in features"
          :key="i"
          class="bezel feat obs"
          @mousemove="handleCardTilt"
          @mouseleave="resetCardTilt"
        >
          <div class="card feat-card">
            <span
              class="feat-icon"
              :style="{ background: f.bg, color: f.color }"
              v-html="f.icon"
            ></span>
            <h3 class="feat-title">{{ f.title }}</h3>
            <p class="feat-desc">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ DATA SHOWCASE ═══ -->
    <section id="showcase" class="section">
      <div class="section-head">
        <span class="eyebrow">Data Visualization</span>
        <h2 class="section-title">直观的数据洞察，驱动精准决策</h2>
        <p class="section-sub">多维度可视化分析，让用户画像一目了然</p>
      </div>
      <div class="show-grid">
        <div class="bezel show-main obs">
          <div class="card show-card">
            <div class="show-head">
              <h3 class="show-title">用户活跃趋势</h3>
              <span class="show-tag">近 30 日</span>
            </div>
            <svg viewBox="0 0 400 160" class="w-full" aria-hidden="true">
              <line x1="0" y1="140" x2="400" y2="140" stroke="#E2E8F0" stroke-width="1" />
              <line x1="0" y1="100" x2="400" y2="100" stroke="#F1F5F9" stroke-width="1" />
              <line x1="0" y1="60" x2="400" y2="60" stroke="#F1F5F9" stroke-width="1" />
              <defs>
                <linearGradient id="g2" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0" stop-color="#3B82F6" stop-opacity="0.10" />
                  <stop offset="1" stop-color="#3B82F6" stop-opacity="0" />
                </linearGradient>
              </defs>
              <polyline
                fill="none"
                stroke="#3B82F6"
                stroke-width="2.5"
                points="0,130 20,118 40,124 60,100 80,110 100,85 120,95 140,70 160,82 180,56 200,68 220,44 240,58 260,36 280,50 300,28 320,42 340,22 360,34 380,18"
              />
              <path
                fill="url(#g2)"
                d="M0,130 L20,118 L40,124 L60,100 L80,110 L100,85 L120,95 L140,70 L160,82 L180,56 L200,68 L220,44 L240,58 L260,36 L280,50 L300,28 L320,42 L340,22 L360,34 L380,18 L380,140 L0,140 Z"
              />
              <polyline
                fill="none"
                stroke="#93C5FD"
                stroke-width="1.5"
                points="0,138 20,132 40,128 60,122 80,116 100,110 120,104 140,98 160,92 180,86 200,80 220,74 240,68 260,62 280,56 300,50 320,44 340,38 360,32 380,26"
                opacity="0.6"
              />
            </svg>
          </div>
        </div>

        <div class="bezel show-side obs">
          <div class="card show-card">
            <h3 class="show-title">RFM 用户分层</h3>
            <svg viewBox="0 0 200 200" class="w-44 mx-auto" aria-hidden="true">
              <circle cx="100" cy="100" r="80" fill="none" stroke="#E2E8F0" stroke-width="1" />
              <circle cx="100" cy="100" r="55" fill="none" stroke="#F1F5F9" stroke-width="1" />
              <circle cx="100" cy="100" r="30" fill="none" stroke="#F1F5F9" stroke-width="1" />
              <line x1="20" y1="100" x2="180" y2="100" stroke="#F1F5F9" stroke-width="1" />
              <line x1="100" y1="20" x2="100" y2="180" stroke="#F1F5F9" stroke-width="1" />
              <path d="M100 20 A80 80 0 0 1 167 60 L100 100 Z" fill="rgba(37,99,235,0.35)" />
              <path d="M100 20 A80 80 0 0 0 33 60 L100 100 Z" fill="rgba(96,165,250,0.5)" />
              <path d="M33 60 A80 80 0 0 0 33 140 L100 100 Z" fill="rgba(191,219,254,0.6)" />
              <path d="M33 140 A80 80 0 0 0 100 180 L100 100 Z" fill="rgba(219,234,254,0.7)" />
              <path d="M100 180 A80 80 0 0 0 167 140 L100 100 Z" fill="rgba(226,232,240,0.8)" />
              <circle cx="100" cy="100" r="4" fill="#2563EB" />
            </svg>
            <div class="rfm-legend">
              <span class="lg"><span class="lg-dot" style="background: #2563eb"></span>高价值</span>
              <span class="lg"><span class="lg-dot" style="background: #60a5fa"></span>潜力</span>
              <span class="lg"><span class="lg-dot" style="background: #93c5fd"></span>一般</span>
              <span class="lg"
                ><span class="lg-dot" style="background: #bfdbfe"></span>流失风险</span
              >
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ STATS ═══ -->
    <section ref="statsEl" class="stats">
      <div class="stats-grid">
        <div v-for="s in statsData" :key="s.label" class="stat">
          <p class="stat-num">{{ s.current }}{{ s.suffix }}</p>
          <p class="stat-label">{{ s.label }}</p>
        </div>
      </div>
    </section>

    <!-- ═══ LIVE DEMO ═══ -->
    <section id="demo" class="section alt">
      <div class="section-head">
        <span class="eyebrow">Live Demo</span>
        <h2 class="section-title">3 秒生成用户 360° 画像</h2>
        <p class="section-sub">输入演示用户 ID，体验基于 Spark 与 RFM 模型构建的精准画像能力。</p>
      </div>

      <div class="demo-box">
        <div class="demo-input-row">
          <input
            v-model="demoInput"
            @keyup.enter="doDemoSearch"
            class="demo-input"
            placeholder="输入演示 ID（试试 1001 / 8848 / 9527）"
          />
          <button
            type="button"
            class="demo-btn"
            :disabled="!demoInput.trim()"
            @click="doDemoSearch"
          >
            生成画像
            <svg
              class="ml-1.5 w-4 h-4"
              fill="none"
              stroke="currentColor"
              stroke-width="2.5"
              viewBox="0 0 24 24"
            >
              <path d="M5 12h14M12 5l7 7-7 7" />
            </svg>
          </button>
        </div>
        <p class="demo-hint">演示数据 · 登录后可在「用户画像」查看完整画像数据</p>
        <p v-if="showShake" class="demo-shake">请输入用户 ID</p>

        <div v-if="showResult" class="demo-result-wrap">
          <div class="bezel">
            <div class="card demo-result-card">
              <div class="demo-user-head">
                <div>
                  <div class="demo-user-name-row">
                    <span class="demo-user-code">{{ demoUser.userCode }}</span>
                    <span
                      class="demo-seg"
                      :style="{
                        background: segmentBg(demoUser.segment),
                        color: segmentColor(demoUser.segment)
                      }"
                      >{{ demoUser.segment }}</span
                    >
                  </div>
                  <p class="demo-user-meta"
                    >{{ demoUser.gender }} · {{ demoUser.age }} 岁 · {{ demoUser.province }}</p
                  >
                </div>
                <div class="demo-score">
                  <p class="demo-score-label">综合评分</p>
                  <p class="demo-score-val" :style="{ color: scoreColor(demoUser.score) }">{{
                    demoUser.score
                  }}</p>
                </div>
              </div>
              <div class="demo-metrics">
                <div v-for="m in demoUser.metrics" :key="m.l" class="demo-metric">
                  <p class="demo-metric-label">{{ m.l }}</p>
                  <p class="demo-metric-val">{{ m.v }}</p>
                </div>
              </div>
              <div class="demo-tags">
                <span class="demo-tags-label">标签</span>
                <span v-for="t in demoUser.tags" :key="t" class="demo-tag">{{ t }}</span>
              </div>
              <p class="demo-footnote">演示数据 · 登录后查看真实用户画像</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ TECH ═══ -->
    <section id="tech" class="tech">
      <div class="section-head">
        <span class="eyebrow">Tech Stack</span>
        <h2 class="section-title">技术栈</h2>
      </div>
      <div class="marquee-wrap">
        <div class="marquee-row">
          <span v-for="t in doubledTechs" :key="t + 'a'" class="tech-pill">{{ t }}</span>
        </div>
        <div class="marquee-row rev">
          <span v-for="t in doubledTechs" :key="t + 'b'" class="tech-pill">{{ t }}</span>
        </div>
      </div>
    </section>

    <!-- ═══ CTA — 双门户入口 ═══ -->
    <section id="portals" class="section alt">
      <div class="section-head">
        <h2 class="section-title">选择你的入口</h2>
        <p class="section-sub">管理员与运营分析员使用不同门户，登录后系统自动跳转。</p>
      </div>
      <div class="portal-grid">
        <button type="button" class="bezel portal obs" @click="goAdminLogin">
          <div class="card portal-card">
            <span class="portal-icon admin">
              <svg
                width="22"
                height="22"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z" />
              </svg>
            </span>
            <div class="portal-text">
              <h3 class="portal-title">管理员控制台</h3>
              <p class="portal-desc"
                >面向系统管理者：系统用户管理、数据生成、Spark 画像任务调度、AI
                智能分析，全链路掌控数据与计算。</p
              >
            </div>
            <span class="portal-badge">Admin</span>
            <span class="portal-arrow">
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M5 12h14M12 5l7 7-7 7" />
              </svg>
            </span>
          </div>
        </button>

        <button type="button" class="bezel portal obs" @click="goUserLogin">
          <div class="card portal-card">
            <span class="portal-icon user">
              <svg
                width="22"
                height="22"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            </span>
            <div class="portal-text">
              <h3 class="portal-title">运营分析员门户</h3>
              <p class="portal-desc"
                >面向运营分析员：用户画像浏览、标签分析、RFM 分层、AI
                智能洞察，零门槛读懂每一类用户。</p
              >
            </div>
            <span class="portal-badge">User</span>
            <span class="portal-arrow">
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M5 12h14M12 5l7 7-7 7" />
              </svg>
            </span>
          </div>
        </button>
      </div>
      <p class="portal-note">登录后系统会根据你的身份自动跳转到对应门户。</p>
    </section>

    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="footer-mark">UP</span>
          <span class="footer-name">UserProfile</span>
        </div>
        <p class="footer-copy"
          >© 基于Spring Boot+Spark的电商平台用户画像分析系统的设计与实现-欧峰</p
        >
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
  import { useRouter } from 'vue-router'

  defineOptions({ name: 'LandingPage' })
  const r = useRouter()

  const features = [
    {
      title: 'Spark 离线计算',
      desc: '基于 PySpark 本地计算引擎，百级到十万级用户数据分钟级完成画像计算。',
      bg: '#EFF6FF',
      color: '#2563EB',
      icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12" /></svg>'
    },
    {
      title: 'RFM 智能分层',
      desc: 'NTILE(5) 打分算法，综合最近消费、频次、金额三维度，8 分类映射 5 层用户价值，精准识别高价值与流失风险用户。',
      bg: '#ECFDF5',
      color: '#059669',
      icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20V10M18 20V4M6 20v-4" /></svg>'
    },
    {
      title: '全维标签体系',
      desc: '活跃度、消费力、偏好品类、RFM 四维标签全覆盖。基于行为的加权评分，随时掌握用户兴趣变化。',
      bg: '#FFFBEB',
      color: '#D97706',
      icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z" /></svg>'
    },
    {
      title: '智能人群圈选',
      desc: '12 维度条件自由组合，AND/OR 逻辑嵌套。保存人群包，一键多包横向对比，让定向营销直接落地。',
      bg: '#FFF1F2',
      color: '#E11D48',
      icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8" /><path d="M21 21l-4.35-4.35" /></svg>'
    },
    {
      title: 'AI 智能分析师',
      desc: '基于 DeepSeek 大模型，自然语言直接查询画像数据库。SSE 流式实时响应，无需 SQL 即可获得运营洞察。',
      bg: '#F0F9FF',
      color: '#0284C7',
      icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a10 10 0 0110 10c0 5-4 9-10 9S2 17 2 12 6 2 12 2z" /><path d="M8 12h8" /><path d="M12 8v8" /></svg>'
    },
    {
      title: 'ECharts 可视化',
      desc: '基于 ECharts 数据可视化，支持 RFM 玫瑰图、趋势折线、雷达图、省份地图等 10+ 图表，一屏看懂用户全貌。',
      bg: '#FAF5FF',
      color: '#9333EA',
      icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2" /><line x1="8" y1="21" x2="16" y2="21" /><line x1="12" y1="17" x2="12" y2="21" /></svg>'
    }
  ]

  const doubledTechs = computed(() => {
    const t = [
      'Apache Spark',
      'PySpark',
      'DeepSeek',
      'Spring Boot',
      'MyBatis-Plus',
      'Vue',
      'ECharts',
      'MySQL',
      'Python',
      'Tailwind CSS'
    ]
    return [...t, ...t]
  })

  // ═══ Mockup 数据 ═══
  const mockKpis = [
    { label: '总用户', value: '1,284,562', delta: '↑ 12.3%', up: true },
    { label: '已画像', value: '982,340', delta: '↑ 8.7%', up: true },
    { label: '高价值', value: '156,892', delta: '→ 0.4%', up: false },
    { label: '消费总额', value: '¥2.8 亿', delta: '↑ 23.1%', up: true }
  ]

  // ═══ 数字滚动计数 ═══
  const statsEl = ref<HTMLElement>()
  const statsData = ref([
    { target: 22, suffix: '', label: '数据表', current: 0, inView: false },
    { target: 94, suffix: '+', label: 'API 接口', current: 0, inView: false },
    { target: 5, suffix: '', label: '用户分层', current: 0, inView: false },
    { target: 100000, suffix: '+', label: '画像用户', current: 0, inView: false }
  ])
  let countAnimFrame: number | null = null
  function startCountUp() {
    const items = statsData.value.filter((s) => s.inView && s.current < s.target)
    if (items.length === 0) return
    items.forEach((s) => {
      s.current = Math.min(s.current + Math.ceil(s.target / 40), s.target)
    })
    countAnimFrame = requestAnimationFrame(startCountUp)
  }

  // ═══ 卡片 3D 倾角 ═══
  function handleCardTilt(e: MouseEvent) {
    const card = e.currentTarget as HTMLElement
    const rect = card.getBoundingClientRect()
    const x = (e.clientX - rect.left) / rect.width - 0.5
    const y = (e.clientY - rect.top) / rect.height - 0.5
    card.style.transform = `perspective(800px) rotateY(${x * 5}deg) rotateX(${-y * 5}deg) translateY(-4px)`
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
    '1001': {
      userCode: 'UC1001',
      segment: '高价值',
      gender: '男',
      age: 28,
      province: '广东省',
      score: 4.7,
      metrics: [
        { l: '累计消费', v: '¥86,520' },
        { l: '订单数', v: '23' },
        { l: '30日浏览', v: '156 次' },
        { l: '客单价', v: '¥3,762' }
      ],
      tags: ['高消费', '高频次', '数码偏好']
    },
    '8848': {
      userCode: 'UC8848',
      segment: '一般用户',
      gender: '女',
      age: 35,
      province: '浙江省',
      score: 2.3,
      metrics: [
        { l: '累计消费', v: '¥12,380' },
        { l: '订单数', v: '5' },
        { l: '30日浏览', v: '38 次' },
        { l: '客单价', v: '¥2,476' }
      ],
      tags: ['中等消费', '低频次', '家居偏好']
    },
    '9527': {
      userCode: 'UC9527',
      segment: '流失风险',
      gender: '男',
      age: 42,
      province: '上海市',
      score: 1.8,
      metrics: [
        { l: '累计消费', v: '¥5,200' },
        { l: '订单数', v: '2' },
        { l: '30日浏览', v: '12 次' },
        { l: '客单价', v: '¥2,600' }
      ],
      tags: ['低消费', '低频次', '已流失']
    }
  }
  function doDemoSearch() {
    showShake.value = false
    const id = demoInput.value.trim()
    if (!id) {
      showShake.value = true
      return
    }
    const user = demoDB[id] || {
      userCode: `UC${id}`,
      segment: '未分类',
      gender: '未知',
      age: Math.floor(Math.random() * 40 + 20),
      province: '未知',
      score: 0,
      metrics: [
        { l: '累计消费', v: '—' },
        { l: '订单数', v: '—' },
        { l: '30日浏览', v: '—' },
        { l: '客单价', v: '—' }
      ],
      tags: ['待分析']
    }
    demoUser.value = user
    showResult.value = false
    nextTick(() => {
      showResult.value = true
    })
  }
  const demoUser = ref(demoDB['1001'])
  function segmentBg(s: string) {
    return s === '高价值'
      ? '#D1FAE5'
      : s === '流失风险'
        ? '#FEE2E2'
        : s === '一般用户'
          ? '#F1F5F9'
          : '#EFF6FF'
  }
  function segmentColor(s: string) {
    return s === '高价值'
      ? '#059669'
      : s === '流失风险'
        ? '#DC2626'
        : s === '一般用户'
          ? '#64748B'
          : '#2563EB'
  }
  function scoreColor(s: number) {
    return s >= 4 ? '#059669' : s >= 3 ? '#2563EB' : s >= 2 ? '#D97706' : '#DC2626'
  }

  let obsRefs: IntersectionObserver[] = []
  onMounted(() => {
    // 登录须由用户主动点击按钮触发，刷新页面不再自动弹窗
    document.querySelectorAll('a:not([href^="#/"])[href^="#"]').forEach((el) => {
      el.addEventListener('click', (e) => {
        e.preventDefault()
        const id = (el as HTMLAnchorElement).getAttribute('href')?.slice(1)
        if (id) document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
      })
    })
    nextTick(() => {
      document.querySelectorAll('.obs').forEach((el) => {
        const io = new IntersectionObserver(
          (entries) => {
            entries.forEach((e) => {
              if (e.isIntersecting) {
                e.target.classList.add('vis')
                io.unobserve(e.target)
              }
            })
          },
          { threshold: 0.1 }
        )
        io.observe(el)
        obsRefs.push(io)
      })
      if (statsEl.value) {
        const io = new IntersectionObserver(
          ([entry]) => {
            if (entry.isIntersecting) {
              statsData.value.forEach((s) => {
                s.inView = true
              })
              startCountUp()
              io.disconnect()
            }
          },
          { threshold: 0.5 }
        )
        io.observe(statsEl.value)
        obsRefs.push(io)
      }
    })
  })
  onUnmounted(() => {
    obsRefs.forEach((io) => io.disconnect())
    obsRefs = []
    if (countAnimFrame) cancelAnimationFrame(countAnimFrame)
  })
  const goAdminLogin = () => {
    r.push({ path: '/auth/login', query: { role: 'Admin' } })
  }
  const goUserLogin = () => {
    r.push({ path: '/auth/login', query: { role: 'User' } })
  }
</script>

<style scoped>
  /* ═══════════════════════════════════════════════════════════════
   Design System — Soft Structuralism（与数据大屏同一设计语言）
   依据 ui-ux-pro-max + high-end-visual-design
   ═══════════════════════════════════════════════════════════════ */
  .root {
    min-height: 100vh;
    background: #f8fafc;
    color: #0f172a;
    font-family: 'Plus Jakarta Sans', 'Inter', 'PingFang SC', system-ui, sans-serif;
    -webkit-font-smoothing: antialiased;
    overflow-x: hidden;
  }

  /* ─────────── Double-Bezel 通用卡片 ─────────── */
  .bezel {
    position: relative;
    padding: 6px;
    background: linear-gradient(180deg, #ffffff, #f1f5f9);
    border: 1px solid rgba(15, 23, 42, 0.06);
    border-radius: 22px;
    box-shadow: 0 8px 30px rgba(15, 23, 42, 0.05);
    transition:
      transform 0.4s cubic-bezier(0.32, 0.72, 0, 1),
      box-shadow 0.4s cubic-bezier(0.32, 0.72, 0, 1);
  }
  .bezel:hover {
    transform: translateY(-3px);
    box-shadow: 0 18px 44px rgba(15, 23, 42, 0.09);
  }
  .card {
    background: #fff;
    border-radius: 16px;
    box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.6);
  }

  /* ─────────── NAVBAR 悬浮胶囊岛 ─────────── */
  .nav-island {
    position: fixed;
    top: 16px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 50;
    display: flex;
    align-items: center;
    gap: 28px;
    padding: 10px 12px 10px 18px;
    background: rgba(255, 255, 255, 0.82);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border: 1px solid rgba(15, 23, 42, 0.07);
    border-radius: 999px;
    box-shadow: 0 10px 34px rgba(15, 23, 42, 0.07);
    animation: fadeDown 0.6s cubic-bezier(0.32, 0.72, 0, 1) both;
  }
  .nav-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    text-decoration: none;
  }
  .brand-mark {
    width: 32px;
    height: 32px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 11px;
    font-weight: 800;
    background: linear-gradient(135deg, #1e40af, #3b82f6);
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
  }
  .brand-name {
    font-weight: 700;
    font-size: 16px;
    letter-spacing: -0.02em;
    color: #0f172a;
  }
  .nav-links {
    display: flex;
    align-items: center;
    gap: 24px;
  }
  .nav-links a {
    font-size: 13px;
    font-weight: 500;
    color: #64748b;
    text-decoration: none;
    transition: color 0.25s cubic-bezier(0.32, 0.72, 0, 1);
  }
  .nav-links a:hover {
    color: #1d4ed8;
  }
  .nav-login {
    padding: 8px 20px;
    border: none;
    border-radius: 999px;
    background: #1e40af;
    color: #fff;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    font-family: inherit;
    text-decoration: none;
    transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1);
    box-shadow: 0 2px 10px rgba(30, 64, 175, 0.25);
  }
  .nav-login:hover {
    background: #1d4ed8;
    transform: translateY(-1px);
    box-shadow: 0 6px 18px rgba(30, 64, 175, 0.3);
  }
  .nav-login:active {
    transform: translateY(0) scale(0.98);
  }

  /* ─────────── HERO ─────────── */
  .hero {
    position: relative;
    padding: 150px 0 96px;
    overflow: hidden;
  }
  .hero-glow {
    position: absolute;
    inset: 0;
    pointer-events: none;
  }
  .g1 {
    background: radial-gradient(
      ellipse 60% 38% at 50% -8%,
      rgba(37, 99, 235, 0.07),
      transparent 65%
    );
  }
  .g2 {
    background: radial-gradient(
      ellipse 34% 26% at 82% 32%,
      rgba(37, 99, 235, 0.04),
      transparent 60%
    );
  }
  .g3 {
    background: radial-gradient(
      ellipse 30% 22% at 14% 46%,
      rgba(217, 119, 6, 0.03),
      transparent 55%
    );
  }
  .hero-content {
    position: relative;
    z-index: 1;
    max-width: 72rem;
    margin: 0 auto;
    padding: 0 24px;
    text-align: center;
  }

  /* eyebrow 徽章 */
  .eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 7px 16px;
    border-radius: 999px;
    border: 1px solid rgba(37, 99, 235, 0.14);
    background: rgba(239, 246, 255, 0.7);
    color: #1d4ed8;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.02em;
    margin-bottom: 28px;
    animation: fadeUp 0.6s cubic-bezier(0.32, 0.72, 0, 1) 0.08s both;
  }
  .eyebrow-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #3b82f6;
    animation: pulse 2.2s ease-out infinite;
  }
  .hero-title {
    font-size: clamp(2.6rem, 6vw, 4.5rem);
    font-weight: 700;
    letter-spacing: -0.035em;
    line-height: 1.08;
    color: #0f172a;
    font-family: 'Space Grotesk', 'Plus Jakarta Sans', 'PingFang SC', sans-serif;
    margin: 0 auto;
    max-width: 64rem;
    animation: fadeUp 0.7s cubic-bezier(0.32, 0.72, 0, 1) 0.16s both;
  }
  .hero-accent {
    background: linear-gradient(100deg, #1e40af 10%, #2563eb 55%, #60a5fa 95%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    color: transparent;
  }
  .hero-sub {
    margin: 26px auto 0;
    max-width: 40rem;
    font-size: 17px;
    line-height: 1.75;
    color: #64748b;
    animation: fadeUp 0.7s cubic-bezier(0.32, 0.72, 0, 1) 0.24s both;
  }
  .hero-cta {
    margin-top: 38px;
    animation: fadeUp 0.7s cubic-bezier(0.32, 0.72, 0, 1) 0.32s both;
  }

  /* 磁吸主按钮（button-in-button） */
  .cta-primary {
    display: inline-flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px 14px 30px;
    border-radius: 999px;
    background: linear-gradient(135deg, #1e40af, #2563eb);
    color: #fff;
    font-size: 15px;
    font-weight: 600;
    text-decoration: none;
    box-shadow: 0 8px 24px rgba(37, 99, 235, 0.3);
    transition: all 0.35s cubic-bezier(0.32, 0.72, 0, 1);
  }
  .cta-primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 14px 34px rgba(37, 99, 235, 0.36);
  }
  .cta-primary:active {
    transform: translateY(0) scale(0.98);
  }
  .cta-arrow {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.18);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.35s cubic-bezier(0.32, 0.72, 0, 1);
  }
  .cta-primary:hover .cta-arrow {
    transform: translateX(3px);
  }

  /* Mockup */
  .mockup {
    margin: 68px auto 0;
    max-width: 56rem;
    text-align: left;
    animation: fadeUp 0.8s cubic-bezier(0.32, 0.72, 0, 1) 0.44s both;
  }
  .mockup-card {
    overflow: hidden;
  }
  .mockup-bar {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 13px 18px;
    border-bottom: 1px solid rgba(15, 23, 42, 0.05);
    background: #fafbfc;
  }
  .dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
  }
  .dot.r {
    background: #ef4444;
  }
  .dot.y {
    background: #f59e0b;
  }
  .dot.g {
    background: #10b981;
  }
  .mockup-bar-tag {
    margin-left: 12px;
    font-size: 11px;
    color: #94a3b8;
    font-family: 'JetBrains Mono', monospace;
  }
  .mockup-demo-tag {
    margin-left: auto;
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 0.14em;
    color: #94a3b8;
    padding: 3px 10px;
    border-radius: 999px;
    border: 1px solid rgba(15, 23, 42, 0.07);
    background: #f8fafc;
    font-family: 'JetBrains Mono', monospace;
  }
  .mockup-body {
    padding: 20px;
  }
  .mockup-body .kpi-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    margin-bottom: 18px;
  }
  .mkpi {
    padding: 14px 16px;
    border-radius: 14px;
    background: #f8fafc;
    border: 1px solid rgba(15, 23, 42, 0.04);
  }
  .mkpi-label {
    font-size: 11px;
    color: #94a3b8;
    font-weight: 500;
    margin: 0 0 4px;
  }
  .mkpi-val {
    font-size: 19px;
    font-weight: 700;
    color: #0f172a;
    font-family: 'JetBrains Mono', monospace;
    margin: 0 0 3px;
    letter-spacing: -0.02em;
  }
  .mkpi-delta {
    font-size: 11px;
    margin: 0;
    font-weight: 600;
  }
  .mockup-charts {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 14px;
    align-items: center;
  }
  .mockup-line,
  .mockup-donut {
    padding: 14px;
    border-radius: 14px;
    background: #f8fafc;
    border: 1px solid rgba(15, 23, 42, 0.04);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  /* ─────────── SECTION 通用 ─────────── */
  .section {
    padding: 110px 0;
  }
  .section.alt {
    background: #f8fafc;
  }
  .section-head {
    text-align: center;
    max-width: 42rem;
    margin: 0 auto 64px;
    padding: 0 24px;
  }
  .section-head .eyebrow {
    margin-bottom: 20px;
  }
  .section-title {
    font-size: clamp(1.8rem, 3.6vw, 2.5rem);
    font-weight: 700;
    letter-spacing: -0.03em;
    line-height: 1.15;
    color: #0f172a;
    font-family: 'Space Grotesk', 'Plus Jakarta Sans', 'PingFang SC', sans-serif;
    margin: 0;
  }
  .section-sub {
    margin-top: 16px;
    font-size: 15px;
    color: #64748b;
    line-height: 1.7;
  }

  /* ─────────── FEATURES ─────────── */
  .feat-grid {
    max-width: 72rem;
    margin: 0 auto;
    padding: 0 24px;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 22px;
  }
  .feat-card {
    padding: 30px;
    height: 100%;
  }
  .feat-icon {
    width: 46px;
    height: 46px;
    border-radius: 14px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 20px;
  }
  .feat-title {
    font-size: 17px;
    font-weight: 700;
    color: #0f172a;
    margin: 0 0 10px;
    letter-spacing: -0.01em;
  }
  .feat-desc {
    font-size: 13.5px;
    line-height: 1.7;
    color: #64748b;
    margin: 0;
  }

  /* ─────────── SHOWCASE ─────────── */
  .show-grid {
    max-width: 72rem;
    margin: 0 auto;
    padding: 0 24px;
    display: grid;
    grid-template-columns: 3fr 2fr;
    gap: 22px;
  }
  .show-card {
    padding: 26px;
  }
  .show-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 18px;
  }
  .show-title {
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
    margin: 0;
  }
  .show-tag {
    font-size: 11px;
    color: #94a3b8;
    font-family: 'JetBrains Mono', monospace;
  }
  .rfm-legend {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px 12px;
    margin-top: 16px;
  }
  .lg {
    display: flex;
    align-items: center;
    gap: 7px;
    font-size: 11.5px;
    color: #64748b;
  }
  .lg-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  /* ─────────── STATS ─────────── */
  .stats {
    padding: 72px 0;
    border-top: 1px solid rgba(15, 23, 42, 0.05);
    border-bottom: 1px solid rgba(15, 23, 42, 0.05);
    background: linear-gradient(180deg, rgba(239, 246, 255, 0.55), rgba(248, 250, 252, 0.4));
  }
  .stats-grid {
    max-width: 72rem;
    margin: 0 auto;
    padding: 0 24px;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 40px;
    text-align: center;
  }
  .stat-num {
    font-size: 42px;
    font-weight: 700;
    letter-spacing: -0.03em;
    color: #0f172a;
    font-family: 'JetBrains Mono', monospace;
    margin: 0 0 8px;
  }
  .stat-label {
    font-size: 13px;
    color: #64748b;
    margin: 0;
  }

  /* ─────────── LIVE DEMO ─────────── */
  .demo-box {
    max-width: 40rem;
    margin: 0 auto;
    padding: 0 24px;
    text-align: center;
  }
  .demo-input-row {
    display: flex;
    gap: 12px;
    max-width: 30rem;
    margin: 0 auto;
  }
  .demo-input {
    flex: 1;
    padding: 13px 18px;
    border-radius: 999px;
    border: 1.5px solid rgba(15, 23, 42, 0.08);
    font-size: 14px;
    color: #0f172a;
    background: #fff;
    outline: none;
    font-family: inherit;
    transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1);
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
  }
  .demo-input:focus {
    border-color: #2563eb;
    box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.1);
  }
  .demo-input::placeholder {
    color: #94a3b8;
  }
  .demo-btn {
    display: inline-flex;
    align-items: center;
    padding: 13px 26px;
    border-radius: 999px;
    border: none;
    background: linear-gradient(135deg, #1e40af, #2563eb);
    color: #fff;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    font-family: inherit;
    transition: all 0.3s cubic-bezier(0.32, 0.72, 0, 1);
    white-space: nowrap;
    box-shadow: 0 4px 14px rgba(37, 99, 235, 0.24);
  }
  .demo-btn:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 8px 22px rgba(37, 99, 235, 0.3);
  }
  .demo-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
    box-shadow: none;
  }
  .demo-hint {
    margin-top: 14px;
    font-size: 12px;
    color: #94a3b8;
  }
  .demo-shake {
    margin-top: 10px;
    font-size: 12px;
    color: #dc2626;
    animation: shake 0.4s ease;
  }
  @keyframes shake {
    0%,
    100% {
      transform: translateX(0);
    }
    25% {
      transform: translateX(-4px);
    }
    75% {
      transform: translateX(4px);
    }
  }
  .demo-result-wrap {
    overflow: hidden;
    max-height: 600px;
    animation: slideDown 0.5s cubic-bezier(0.16, 1, 0.3, 1);
    text-align: left;
    margin-top: 30px;
  }
  .demo-result-card {
    padding: 28px;
  }
  .demo-user-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 20px;
  }
  .demo-user-name-row {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 6px;
  }
  .demo-user-code {
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
    font-family: 'Space Grotesk', 'Plus Jakarta Sans', sans-serif;
  }
  .demo-seg {
    padding: 3px 10px;
    border-radius: 999px;
    font-size: 10px;
    font-weight: 700;
  }
  .demo-user-meta {
    font-size: 12px;
    color: #94a3b8;
    margin: 0;
  }
  .demo-score {
    text-align: right;
  }
  .demo-score-label {
    font-size: 11px;
    color: #94a3b8;
    margin: 0 0 2px;
  }
  .demo-score-val {
    font-size: 24px;
    font-weight: 700;
    font-family: 'JetBrains Mono', monospace;
    margin: 0;
  }
  .demo-metrics {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    margin-bottom: 18px;
  }
  .demo-metric {
    padding: 14px;
    border-radius: 14px;
    background: #f8fafc;
    border: 1px solid rgba(15, 23, 42, 0.04);
  }
  .demo-metric-label {
    font-size: 11px;
    color: #94a3b8;
    margin: 0 0 4px;
  }
  .demo-metric-val {
    font-size: 15px;
    font-weight: 700;
    color: #0f172a;
    margin: 0;
    font-family: 'JetBrains Mono', monospace;
  }
  .demo-tags {
    display: flex;
    align-items: center;
    gap: 8px;
    padding-top: 16px;
    border-top: 1px solid rgba(15, 23, 42, 0.05);
  }
  .demo-tags-label {
    font-size: 12px;
    color: #94a3b8;
  }
  .demo-tag {
    padding: 4px 12px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 600;
    background: #eff6ff;
    color: #2563eb;
  }
  .demo-footnote {
    margin: 16px 0 0;
    font-size: 10px;
    text-align: center;
    color: #cbd5e1;
  }
  @keyframes slideDown {
    from {
      max-height: 0;
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      max-height: 600px;
      opacity: 1;
      transform: translateY(0);
    }
  }

  /* ─────────── TECH ─────────── */
  .tech {
    padding: 100px 0 80px;
    overflow: hidden;
  }
  .marquee-wrap {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .marquee-row {
    display: flex;
    gap: 14px;
    width: max-content;
    animation: marquee 36s linear infinite;
  }
  .marquee-row.rev {
    animation-direction: reverse;
    animation-duration: 42s;
  }
  .tech-pill {
    padding: 10px 22px;
    border-radius: 999px;
    font-size: 13px;
    font-weight: 500;
    color: #64748b;
    background: #fff;
    border: 1px solid rgba(15, 23, 42, 0.07);
    white-space: nowrap;
    font-family: 'JetBrains Mono', monospace;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
  }
  @keyframes marquee {
    0% {
      transform: translateX(0);
    }
    100% {
      transform: translateX(-50%);
    }
  }

  /* ─────────── CTA 双门户 ─────────── */
  .portal-grid {
    max-width: 52rem;
    margin: 0 auto;
    padding: 0 24px;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 22px;
  }
  .portal {
    cursor: pointer;
    text-align: left;
    border: 1px solid rgba(15, 23, 42, 0.06);
  }
  .portal-card {
    display: flex;
    align-items: flex-start;
    gap: 18px;
    padding: 30px;
    position: relative;
  }
  .portal-icon {
    width: 52px;
    height: 52px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: #fff;
  }
  .portal-icon.admin {
    background: linear-gradient(135deg, #2563eb, #1e40af);
    box-shadow: 0 4px 14px rgba(37, 99, 235, 0.28);
  }
  .portal-icon.user {
    background: linear-gradient(135deg, #0d9488, #0f766e);
    box-shadow: 0 4px 14px rgba(13, 148, 136, 0.28);
  }
  .portal-text {
    flex: 1;
  }
  .portal-title {
    font-size: 17px;
    font-weight: 700;
    color: #0f172a;
    margin: 0 0 8px;
    letter-spacing: -0.01em;
  }
  .portal-desc {
    font-size: 13px;
    line-height: 1.7;
    color: #64748b;
    margin: 0;
  }
  .portal-badge {
    position: absolute;
    top: 26px;
    right: 54px;
    font-size: 10px;
    font-weight: 700;
    padding: 4px 12px;
    border-radius: 999px;
  }
  .portal:nth-child(1) .portal-badge {
    background: #eff6ff;
    color: #1d4ed8;
  }
  .portal:nth-child(2) .portal-badge {
    background: #ecfdf5;
    color: #0f766e;
  }
  .portal-arrow {
    position: absolute;
    top: 26px;
    right: 22px;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #cbd5e1;
    transition: all 0.35s cubic-bezier(0.32, 0.72, 0, 1);
  }
  .portal:hover .portal-arrow {
    color: #fff;
    background: #2563eb;
    transform: translateX(2px);
  }
  .portal:nth-child(2):hover .portal-arrow {
    background: #0d9488;
  }
  .portal-note {
    margin-top: 28px;
    text-align: center;
    font-size: 12px;
    color: #94a3b8;
  }

  /* ─────────── FOOTER ─────────── */
  .footer {
    padding: 40px 0;
    background: #fff;
    border-top: 1px solid rgba(15, 23, 42, 0.05);
  }
  .footer-inner {
    max-width: 72rem;
    margin: 0 auto;
    padding: 0 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
  }
  .footer-brand {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .footer-mark {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 9px;
    font-weight: 800;
    background: linear-gradient(135deg, #1e40af, #3b82f6);
  }
  .footer-name {
    font-size: 14px;
    font-weight: 700;
    color: #334155;
  }
  .footer-copy {
    font-size: 12px;
    color: #94a3b8;
    margin: 0;
  }

  /* ─────────── 入场动效 ─────────── */
  @keyframes fadeUp {
    from {
      opacity: 0;
      transform: translateY(16px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  @keyframes fadeDown {
    from {
      opacity: 0;
      transform: translateY(-14px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  @keyframes pulse {
    0%,
    100% {
      box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4);
    }
    50% {
      box-shadow: 0 0 0 7px rgba(59, 130, 246, 0);
    }
  }

  /* IntersectionObserver 入场（只动 transform/opacity） */
  .obs {
    transform: translateY(24px);
    filter: blur(4px);
    transition: all 0.7s cubic-bezier(0.16, 1, 0.3, 1);
  }
  .vis {
    transform: translateY(0);
    filter: blur(0);
  }

  /* ─────────── 响应式 ─────────── */
  @media (max-width: 1024px) {
    .feat-grid {
      grid-template-columns: repeat(2, 1fr);
    }
    .show-grid {
      grid-template-columns: 1fr;
    }
  }
  @media (max-width: 768px) {
    .nav-island {
      top: 10px;
      gap: 12px;
      padding: 8px 10px 8px 14px;
    }
    .nav-links {
      display: none;
    }
    .hero {
      padding: 120px 0 72px;
    }
    .feat-grid {
      grid-template-columns: 1fr;
    }
    .stats-grid {
      grid-template-columns: repeat(2, 1fr);
      gap: 28px;
    }
    .demo-metrics {
      grid-template-columns: repeat(2, 1fr);
    }
    .portal-grid {
      grid-template-columns: 1fr;
    }
    .mockup-body .kpi-grid {
      grid-template-columns: repeat(2, 1fr);
    }
    .mockup-charts {
      grid-template-columns: 1fr;
    }
    .section {
      padding: 80px 0;
    }
  }

  /* ─────────── 无障碍：减弱动效 ─────────── */
  @media (prefers-reduced-motion: reduce) {
    .root *,
    .nav-island,
    .hero-title,
    .hero-sub,
    .hero-cta,
    .eyebrow,
    .mockup,
    .bezel {
      animation: none !important;
    }
    .marquee-row {
      animation-duration: 120s;
    }
    .obs {
      transform: none;
      filter: none;
      transition: none;
    }
    .bezel,
    .cta-primary,
    .nav-login,
    .demo-btn,
    .demo-input,
    .portal-arrow {
      transition: none;
    }
  }
</style>
