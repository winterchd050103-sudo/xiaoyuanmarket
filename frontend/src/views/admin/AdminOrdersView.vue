<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="load(true)">
        <el-option v-for="(text, code) in statusMap" :key="code" :label="text" :value="Number(code)" />
      </el-select>
    </div>
    <el-table :data="orders" v-loading="loading">
      <el-table-column prop="orderNo" label="订单号" min-width="230" />
      <el-table-column prop="productTitle" label="商品" min-width="200" show-overflow-tooltip />
      <el-table-column label="买家" prop="buyerNickname" width="110" />
      <el-table-column label="卖家" prop="sellerNickname" width="110" />
      <el-table-column label="金额" width="100">
        <template #default="{ row }"><span class="price">￥{{ row.amount }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付时间" width="170">
        <template #default="{ row }">{{ row.payTime ? row.payTime.replace('T', ' ').slice(0, 16) : '-' }}</template>
      </el-table-column>
      <el-table-column label="下单时间" width="170">
        <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 16) }}</template>
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
import { adminGetOrders } from '../../api'

const orders = ref([])
const status = ref(null)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

const statusMap = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '已退款' }
const statusType = s => ({ 0: 'warning', 1: 'primary', 2: 'primary', 3: 'success', 4: 'info', 5: 'danger' }[s] || 'info')

async function load(reset = false) {
  if (reset) pageNum.value = 1
  loading.value = true
  try {
    const data = await adminGetOrders({ status: status.value ?? undefined, pageNum: pageNum.value, pageSize })
    orders.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.toolbar { margin-bottom: 14px; }
</style>
