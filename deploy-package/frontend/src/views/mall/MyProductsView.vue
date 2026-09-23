<template>
  <el-card class="card-shadow">
    <template #header><b>我发布的商品</b></template>
    <el-table :data="products" v-loading="loading">
      <el-table-column label="商品" min-width="320">
        <template #default="{ row }">
          <div class="p-cell">
            <el-image :src="row.coverImage" fit="cover" class="p-img">
              <template #error><div class="p-img p-img-empty"><el-icon><Picture /></el-icon></div></template>
            </el-image>
            <div>
              <div class="ellipsis" style="max-width: 240px; color: #333">{{ row.title }}</div>
              <div class="text-gray">{{ row.categoryName || '未分类' }} · {{ row.viewCount }} 浏览</div>
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
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/publish?id=${row.id}`)">编辑</el-button>
          <el-button v-if="row.status === 1" size="small" type="warning" plain @click="offShelf(row)">下架</el-button>
          <el-button v-if="row.status === 2" size="small" type="success" plain @click="onShelf(row)">上架</el-button>
          <el-popconfirm title="确定删除该商品吗？" @confirm="del(row)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <div style="display: flex; justify-content: center; margin-top: 16px" v-if="total > pageSize">
      <el-pagination background layout="prev, pager, next" :total="total"
                     :page-size="pageSize" v-model:current-page="pageNum" @current-change="load" />
    </div>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyProducts, deleteProduct, changeProductStatus } from '../../api'

const products = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

const statusType = s => ({ 0: 'warning', 1: 'success', 2: 'info', 3: 'danger' }[s] || 'info')

async function load() {
  loading.value = true
  try {
    const data = await getMyProducts({ pageNum: pageNum.value, pageSize })
    products.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

async function offShelf(row) {
  await changeProductStatus(row.id, 2)
  ElMessage.success('已下架')
  load()
}

async function onShelf(row) {
  await changeProductStatus(row.id, 1)
  ElMessage.success('已上架')
  load()
}

async function del(row) {
  await deleteProduct(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.p-cell { display: flex; align-items: center; gap: 10px; }
.p-img { width: 56px; height: 56px; border-radius: 6px; flex-shrink: 0; }
.p-img-empty {
  display: flex; align-items: center; justify-content: center;
  background: #f5f5f5; color: #ccc;
}
</style>
