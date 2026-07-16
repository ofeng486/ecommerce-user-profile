<template>
  <div class="pkg-page">
    <div class="page-header">
      <h2>人群包管理</h2>
      <p class="page-desc">查看、编辑、删除已保存的人群包</p>
    </div>

    <el-table :data="rows" stripe v-loading="loading" max-height="520" class="pkg-table">
      <template #empty><el-empty description="暂无人群包，请先在「智能圈选」中创建" /></template>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="packageName" label="名称" min-width="150" />
      <el-table-column prop="description" label="描述" min-width="200">
        <template #default="{ row }">{{ row.description || '—' }}</template>
      </el-table-column>
      <el-table-column prop="totalCount" label="人数" width="80" align="center" />
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right" align="center">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该人群包？" @confirm="doDelete(row.id)">
            <template #reference><el-button type="danger" link>删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showEdit" title="编辑人群包" width="420px">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="名称"><el-input v-model="editForm.name" maxlength="50" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.desc" type="textarea" :rows="2" maxlength="200" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showEdit = false">取消</el-button><el-button type="primary" @click="doUpdate">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchAudiencePackages, updateAudiencePackage, deleteAudiencePackage } from '@/api/admin'

defineOptions({ name: 'AudiencePackages' })

const rows = ref<any[]>([]); const loading = ref(false)
const showEdit = ref(false); const editForm = ref({ id: 0, name: '', desc: '' })

function formatDate(v: string) { if (!v) return '—'; return v.replace('T', ' ').substring(0, 19) }

async function load() {
  loading.value = true
  try {
    const res = await fetchAudiencePackages({ page: 0, size: 500 }) as any
    const list = Array.isArray(res) ? res : (res?.records ?? res?.list ?? res?.data ?? [])
    rows.value = list.filter((p: any) => p.status !== 0)
  } catch { rows.value = [] }
  finally { loading.value = false }
}

function openEdit(row: any) {
  editForm.value = { id: row.id, name: row.packageName || '', desc: row.description || '' }
  showEdit.value = true
}

async function doUpdate() {
  try {
    await updateAudiencePackage(editForm.value.id, { packageName: editForm.value.name, description: editForm.value.desc })
    ElMessage.success('更新成功'); showEdit.value = false; load()
  } catch { ElMessage.error('更新失败') }
}

async function doDelete(id: number) {
  try { await deleteAudiencePackage(id); ElMessage.success('已删除'); load() }
  catch { ElMessage.error('删除失败') }
}

onMounted(load)
</script>

<style scoped>
.pkg-page { padding: 28px 32px; max-width: 1100px; font-family: 'Geist','Inter','PingFang SC',sans-serif; }
.page-header { margin-bottom: 22px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: var(--art-gray-900); margin: 0 0 6px; font-family: 'Plus Jakarta Sans','Inter','PingFang SC',sans-serif; letter-spacing: -.3px; }
.page-desc { font-size: 14px; color: var(--art-gray-500); margin: 0; }
.pkg-table { border: 1px solid var(--default-border); border-radius: 12px; background: var(--default-box-color); }
</style>
