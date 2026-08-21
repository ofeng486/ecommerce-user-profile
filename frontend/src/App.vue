<template>
  <ElConfigProvider
    size="default"
    :locale="zh"
    :z-index="3000"
    :card="{
      shadow: 'never'
    }"
  >
    <RouterView></RouterView>
  </ElConfigProvider>
</template>

<script setup lang="ts">
  import zh from 'element-plus/es/locale/lang/zh-cn'
</script>
