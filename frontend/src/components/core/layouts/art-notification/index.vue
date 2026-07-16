<!-- 通知组件 - 适配电商用户画像系统 -->
<template>
  <div
    class="art-notification-panel art-card-sm !shadow-xl"
    :style="{
      transform: show ? 'scaleY(1)' : 'scaleY(0.9)',
      opacity: show ? 1 : 0
    }"
    v-show="visible"
    @click.stop
  >
    <div class="flex-cb px-3.5 mt-3.5">
      <span class="text-base font-medium text-g-800">系统通知</span>
      <span class="text-xs text-g-800 px-1.5 py-1 c-p select-none rounded hover:bg-g-200" @click="markAllRead">
        标为已读
      </span>
    </div>

    <ul class="box-border flex items-end w-full h-12.5 px-3.5 border-b-d">
      <li
        v-for="(item, index) in barList"
        :key="index"
        class="h-12 leading-12 mr-5 overflow-hidden text-[13px] text-g-700 c-p select-none"
        :class="{ 'bar-active': barActiveIndex === index }"
        @click="changeBar(index)"
      >
        {{ item.name }} ({{ item.num }})
      </li>
    </ul>

    <div class="w-full h-[calc(100%-95px)]">
      <div class="h-[calc(100%-60px)] overflow-y-scroll scrollbar-thin">
        <!-- 任务动态 -->
        <ul v-show="barActiveIndex === 0">
          <li
            v-for="(item, index) in noticeList"
            :key="index"
            class="box-border flex-c px-3.5 py-3.5 c-p last:border-b-0 hover:bg-g-200/60"
            @click="goToTask(item.taskId)"
          >
            <div
              class="size-9 leading-9 text-center rounded-lg flex-cc"
              :class="[getNoticeStyle(item.type).iconClass]"
            >
              <ArtSvgIcon class="text-lg !bg-transparent" :icon="getNoticeStyle(item.type).icon" />
            </div>
            <div class="w-[calc(100%-45px)] ml-3.5">
              <h4 class="text-sm font-normal leading-5.5 text-g-900">{{ item.title }}</h4>
              <p class="mt-1.5 text-xs text-g-500">{{ item.time }}</p>
            </div>
          </li>
        </ul>

        <!-- 画像动态 -->
        <ul v-show="barActiveIndex === 1">
          <li
            v-for="(item, index) in msgList"
            :key="index"
            class="box-border flex-c px-3.5 py-3.5 c-p last:border-b-0 hover:bg-g-200/60"
            @click="handleMsgClick(item)"
          >
            <div
              class="size-9 leading-9 text-center rounded-lg flex-cc"
              :class="[getNoticeStyle(item.type).iconClass]"
            >
              <ArtSvgIcon class="text-lg !bg-transparent" :icon="getNoticeStyle(item.type).icon" />
            </div>
            <div class="w-[calc(100%-45px)] ml-3.5">
              <h4 class="text-sm font-normal leading-5.5 text-g-900">{{ item.title }}</h4>
              <p class="mt-1.5 text-xs text-g-500">{{ item.time }}</p>
            </div>
          </li>
        </ul>

        <!-- 空状态 -->
        <div
          v-show="currentTabIsEmpty"
          class="relative top-25 h-full text-g-500 text-center !bg-transparent"
        >
          <ArtSvgIcon icon="system-uicons:inbox" class="text-5xl" />
          <p class="mt-3.5 text-xs !bg-transparent">暂无{{ barList[barActiveIndex].name }}</p>
        </div>
      </div>

      <div class="relative box-border w-full px-3.5">
        <ElButton class="w-full mt-3" @click="handleViewAll" v-ripple>
          查看全部
        </ElButton>
      </div>
    </div>

    <div class="h-25"></div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue'
  import { useRouter } from 'vue-router'
import request from '@/utils/http'
  import { fetchAdminTasks } from '@/api/admin'
  import mittBus from '@/utils/sys/mittBus'

  defineOptions({ name: 'ArtNotification' })

  interface NoticeItem {
    /** 标题 */
    title: string
    /** 时间 */
    time: string
    /** 类型 */
    type: NoticeType
    /** 关联任务 ID */
    taskId?: number
  }

  interface MessageItem {
    /** 标题 */
    title: string
    /** 时间 */
    time: string
    /** 类型 */
    type: NoticeType
    /** 关联用户 ID */
    userId?: number
  }

  type NoticeType = 'success' | 'failed' | 'running' | 'segment' | 'tag'

  const router = useRouter()

  const props = defineProps<{
    value: boolean
  }>()

  const emit = defineEmits<{
    'update:value': [value: boolean]
  }>()

  const show = ref(false)
  const visible = ref(false)
  const barActiveIndex = ref(0)

  // 任务动态列表
  const noticeList = ref<NoticeItem[]>([])
  // 画像动态列表
  const msgList = ref<MessageItem[]>([])

  // 标签栏数据
  const barList = computed(() => [
    { name: '任务动态', num: noticeList.value.length },
    { name: '画像动态', num: msgList.value.length }
  ])

  const currentTabIsEmpty = computed(() => {
    return barActiveIndex.value === 0
      ? noticeList.value.length === 0
      : msgList.value.length === 0
  })

  // 从后端加载通知数据
  const loadNotifications = async () => {
    try {
      // 任务动态：保持原有分析任务轮询
      const res = await fetchAdminTasks({ page: 0, size: 20 })
      const tasks = res?.records || []
      noticeList.value = tasks
        .filter((t: any) => t.taskStatus !== 'Pending')
        .slice(0, 10)
        .map((t: any) => ({
          title: `【${taskTypeLabel(t.taskType)}】${t.taskName} - ${statusLabel(t.taskStatus)}`,
          time: formatTime(t.finishedAt || t.createdAt),
          type: mapTaskStatusToType(t.taskStatus),
          taskId: t.id
        }))

      // 画像动态：从通知 API 获取真实数据
      try {
        const notifRes = await request.get<any>({
          url: '/api/v1/notifications',
          params: { page: 0, size: 20 },
          showErrorMessage: false
        })
        const records = notifRes?.records || []
        msgList.value = records.map((n: any) => ({
          title: n.title || n.content,
          time: formatTime(n.createdAt),
          type: mapNotifType(n.type),
          userId: n.id
        }))
      } catch {
        // 通知 API 不可用时保留空状态
        msgList.value = []
      }
    } catch (e) {
      console.warn('加载通知失败', e)
    }
  }

  const taskTypeLabel = (type: string) => {
    const map: Record<string, string> = {
      'PROFILE_FULL': 'Spark 画像',
      'DATA_IMPORT': '数据导入',
      'DATA_GENERATE': '数据生成'
    }
    return map[type] || type
  }

  const statusLabel = (status: string) => {
    const map: Record<string, string> = {
      'Succeeded': '执行成功',
      'Failed': '执行失败',
      'Running': '执行中',
      'Cancelled': '已取消'
    }
    return map[status] || status
  }

  const mapTaskStatusToType = (status: string): NoticeType => {
    const map: Record<string, NoticeType> = {
      'Succeeded': 'success',
      'Failed': 'failed',
      'Running': 'running',
      'Cancelled': 'failed'
    }
    return map[status] || 'notice'
  }

  const mapNotifType = (t: string): NoticeType => ({ TASK: "running", DATA: "segment", SYSTEM: "segment", AI: "tag" }[t] || "tag" as NoticeType)

  const formatTime = (iso: string) => {
    if (!iso) return '-'
    const d = new Date(iso)
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }

  // 样式管理
  const noticeStyleMap: Record<NoticeType, { icon: string; iconClass: string }> = {
    success: { icon: 'ri:check-line', iconClass: 'bg-success/12 text-success' },
    failed: { icon: 'ri:close-line', iconClass: 'bg-danger/12 text-danger' },
    running: { icon: 'ri:loader-2-line', iconClass: 'bg-warning/12 text-warning' },
    segment: { icon: 'ri:user-star-line', iconClass: 'bg-theme/12 text-theme' },
    tag: { icon: 'ri:price-tag-3-line', iconClass: 'bg-info/12 text-info' }
  }

  const getNoticeStyle = (type: NoticeType) => {
    return noticeStyleMap[type] || noticeStyleMap.segment
  }

  // 标签页管理
  const changeBar = (index: number) => {
    barActiveIndex.value = index
  }

  const markAllRead = () => {
    noticeList.value = []
    msgList.value = []
  }

  const handleViewAll = () => {
    if (barActiveIndex.value === 0) {
      router.push('/tasks')
    } else {
      router.push('/profiles')
    }
    emit('update:value', false)
  }

  const goToTask = (taskId?: number) => {
    emit('update:value', false)
    router.push('/tasks')
  }

  const handleMsgClick = (item: any) => {
    // AI 通知 → 打开聊天窗
    if (item.type === "tag") { emit("update:value", false); mittBus.emit("openChat"); return }
    goToProfile(item.userId)
  }

  const goToProfile = (userId?: number) => {
    emit('update:value', false)
    router.push(userId ? `/profiles/${userId}` : '/profiles')
  }

  // 动画管理
  const showNotice = (open: boolean) => {
    if (open) {
      visible.value = true
      loadNotifications()
      setTimeout(() => { show.value = true }, 5)
    } else {
      show.value = false
      setTimeout(() => { visible.value = false }, 350)
    }
  }

  // 监听属性变化
  watch(() => props.value, (newValue) => {
    showNotice(newValue)
  })
</script>

<style scoped>
  @reference '@styles/core/tailwind.css';

  .art-notification-panel {
    @apply absolute
    top-14.5
    right-5
    w-90
    h-125
    overflow-hidden
    transition-all
    duration-300
    origin-top
    will-change-[top,left]
    max-[640px]:top-[65px]
    max-[640px]:right-0
    max-[640px]:w-full
    max-[640px]:h-[80vh];
  }

  .bar-active {
    color: var(--theme-color) !important;
    border-bottom: 2px solid var(--theme-color);
  }

  .scrollbar-thin::-webkit-scrollbar {
    width: 5px !important;
  }

  .dark .scrollbar-thin::-webkit-scrollbar-track {
    background-color: var(--default-box-color);
  }

  .dark .scrollbar-thin::-webkit-scrollbar-thumb {
    background-color: #222 !important;
  }
</style>
