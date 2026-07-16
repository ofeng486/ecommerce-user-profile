<template>
  <div class="page-body">
    <div class="page-header">
      <h1 class="page-title">个人中心</h1>
    </div>
    <div class="section-outer"><div class="section-inner">
      <ElForm :model="userInfo" label-width="100px" v-loading="loading">
        <ElFormItem label="用户名"><ElInput v-model="userInfo.username" disabled /></ElFormItem>
        <ElFormItem label="显示名称"><ElInput v-model="userInfo.displayName" /></ElFormItem>
        <ElFormItem label="角色"><ElInput v-model="userInfo.role" disabled /></ElFormItem>
        <ElFormItem>
          <ElButton type="primary" @click="doSave" :loading="saving">保存修改</ElButton>
        </ElFormItem>
      </ElForm>
    </div></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import request from '@/utils/http'
defineOptions({ name: 'UserCenter' })
const userStore = useUserStore()
const loading = ref(false); const saving = ref(false)
const userInfo = reactive({ username: '', displayName: '', role: '' })
onMounted(async () => {
  loading.value = true
  try { const res = await request.get<any>({ url: '/api/v1/admin/users/me' }); if (res) { userInfo.username = res.username || ''; userInfo.displayName = res.displayName || ''; userInfo.role = res.role || '' } } catch {}
  finally { loading.value = false }
})
async function doSave() {
  saving.value = true
  try { await request.put({ url: '/api/v1/admin/users/me', data: { displayName: userInfo.displayName } }); ElMessage.success('保存成功') } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}
</script>

<style scoped>
.page-body { font-family: 'Geist','Inter','PingFang SC',sans-serif; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--art-gray-900); margin: 0; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; letter-spacing: -.3px; }
.section-outer { padding: 1.5px; border-radius: 14px; background: rgba(0,0,0,.025); max-width: 600px; }
.section-inner { border-radius: calc(14px - 1.5px); background: var(--default-box-color); padding: 24px; border: 1px solid var(--default-border); }
</style>