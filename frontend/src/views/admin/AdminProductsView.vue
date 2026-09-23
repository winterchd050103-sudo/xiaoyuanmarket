<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="load(true)">
        <el-option label="待审核" :value="0" />
        <el-option label="在售" :value="1" />
        <el-option label="已下架" :value="2" />
        <el-option label="已售出" :value="3" />
      </el-select>
      <el-input v-model="keyword" placeholder="商品标题搜索" clearable style="width: 220px" @keyup.enter="load(true)" />
      <el-button type="primary" @click="load(true)">搜索</el-button>
    </div>
    <el-table :data="products" v-loading="loading">
      <el-table-column label="商品" min-width="300">
        <template #default="{ row }">
          <div class="p-cell">
            <el-image :src="row.coverImage" fit="cover" class="p-img">
              <template #error><div class="p-img p-img-empty"><el-icon><Picture /></el-icon></div></template>
            </el-image>
            <div>
              <div class="ellipsis" style="max-width: 200px; color: #333">{{ row.title }}</div>
              <div class="text-gray">卖家：{{ row.sellerNickname }} · {{ row.viewCount }} 浏览</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="110">
        <template #default="{ row }"><span class="price">￥{{ row.price }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="170">
        <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" type="success" size="small" @click="audit(row)">审核通过</el-button>
          <el-button v-if="row.status === 1 || row.status === 0" type="danger" plain size="small"
                     @click="offline(row)">违规下架</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminGetProducts, adminAuditProduct, adminOfflineProduct } from '../../api'

const products = ref([])
const status = ref(null)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

const statusType = s => ({ 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }[s] || 'info')

async function load(reset = false) {
  if (reset) pageNum.value = 1
  loading.value = true
  try {
    const data = await adminGetProducts({
      status: status.value ?? undefined,
      keyword: keyword.value || undefined,
      pageNum: pageNum.value,
      pageSize
    })
    products.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

async function audit(row) {
  await adminAuditProduct(row.id)
  ElMessage.success('审核通过，商品已上架')
  load()
}

async function offline(row) {
  await ElMessageBox.confirm(`确定将「${row.title}」违规下架吗？`, '提示', { type: 'warning' })
  await adminOfflineProduct(row.id)
  ElMessage.success('已下架')
  load()
}

onMounted(() => load())
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; }
.p-cell { display: flex; align-items: center; gap: 10px; }
.p-img { width: 52px; height: 52px; border-radius: 6px; flex-shrink: 0; }
.p-img-empty { display: flex; align-items: center; justify-content: center; background: #f5f5f5; color: #ccc; }
</style>
