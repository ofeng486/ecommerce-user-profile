<!-- 403 无权限页面 -->
<template>
  <div class="exception-page">
    <img :src="imgUrl" alt="403" class="exception-img" />
    <h1 class="exception-title">403</h1>
    <p class="exception-desc">抱歉，您没有权限访问该页面</p>
    <ElButton type="primary" @click="$router.push('/')">返回首页</ElButton>
  </div>
</template>

<script setup lang="ts">
  import imgUrl from '@imgs/svg/403.svg'
  defineOptions({ name: 'Exception403' })
</script>

<style scoped>
.exception-page {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-height: 100vh; gap: 8px; background: #f7f9fc;
}
.exception-img { width: 320px; max-width: 70vw; margin-bottom: 8px; }
.exception-title { font-size: 48px; font-weight: 700; color: #1f2d3d; margin: 0; }
.exception-desc { font-size: 14px; color: #909399; margin-bottom: 16px; }
</style>
