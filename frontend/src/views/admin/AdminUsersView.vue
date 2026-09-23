<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="用户名 / 昵称搜索" clearable style="width: 240px"
                @keyup.enter="load(true)" />
      <el-button type="primary" @click="load(true)">搜索</el-button>
    </div>
    <el-table :data="users" v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="头像" width="70">
        <template #default="{ row }">
          <el-avatar :size="36" :src="row.avatar || ''">{{ (row.nickname || 'U').slice(0, 1) }}</el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="角色" width="90">
        <template #default="{ row }">
          <el-tag :type="row.role === 1 ? 'danger' : 'info'" size="small">
            {{ row.role === 1 ? '管理员' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="170">
        <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0"
                     :disabled="row.role === 1" active-text="" @change="toggle(row)" />
        </template>
      </el-table-column>
    </el-table>
    <div style="display: flex; justify-content: center; margin-top: 16px" v-if="total > pageSize">
      <el-pagination background layout="prev, pager, next" :total="total"
                     :page-size="pageSize" v-model:current-page="pageNum" @current-change="load()" />
    </div>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminGetUsers, adminSetUserStatus } from '../../api'

const users = ref([])
const keyword = ref('')
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

async function load(reset = false) {
  if (reset) pageNum.value = 1
  loading.value = true
  try {
    const data = await adminGetUsers({ keyword: keyword.value || undefined, pageNum: pageNum.value, pageSize })
    users.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

async function toggle(row) {
  await adminSetUserStatus(row.id, row.status)
  ElMessage.success(row.status === 1 ? '已启用' : '已禁用（用户缓存已清除，立即生效）')
}

onMounted(() => load())
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; }
</style>
