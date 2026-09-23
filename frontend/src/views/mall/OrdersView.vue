<template>
  <el-card class="card-shadow">
    <template #header>
      <el-tabs v-model="tab" @tab-change="load">
        <el-tab-pane label="我买到的" name="bought" />
        <el-tab-pane label="我卖出的" name="sold" />
      </el-tabs>
    </template>

    <div v-loading="loading">
      <div v-if="orders.length" class="order-list">
        <div v-for="o in orders" :key="o.id" class="order-card">
          <div class="o-top">
            <span class="text-gray">订单号：{{ o.orderNo }}</span>
            <span class="text-gray">{{ (o.createTime || '').replace('T', ' ').slice(0, 16) }}</span>
          </div>
          <div class="o-body">
            <el-image :src="o.productCover" fit="cover" class="o-img">
              <template #error><div class="o-img o-img-empty"><el-icon><Picture /></el-icon></div></template>
            </el-image>
            <div class="o-info" @click="goProduct(o)">
              <div class="ellipsis o-title">{{ o.productTitle }}</div>
              <div class="text-gray">{{ o.role === 'buyer' ? `卖家：${o.sellerNickname}` : `买家：${o.buyerNickname}` }}</div>
              <div v-if="o.receiver" class="text-gray">收货：{{ o.receiver }} {{ o.receiverPhone }} · {{ o.receiverAddress }}</div>
            </div>
            <div class="o-right">
              <div class="price">￥{{ o.amount }}</div>
              <el-tag :type="statusType(o.status)" size="small" style="margin-top: 6px">{{ o.statusText }}</el-tag>
            </div>
            <div class="o-actions">
              <template v-if="o.role === 'buyer'">
                <el-button v-if="o.status === 0" type="primary" size="small" @click="pay(o)">去支付</el-button>
                <el-button v-if="o.status === 0" size="small" @click="cancel(o)">取消订单</el-button>
                <el-button v-if="o.status === 2" type="success" size="small" @click="receive(o)">确认收货</el-button>
                <el-button v-if="o.status === 1 || o.status === 2" type="warning" plain size="small" @click="refund(o)">申请退款</el-button>
                <el-button v-if="o.status === 3 && !o.commented" size="small" type="success" plain @click="openComment(o)">发表评价</el-button>
              </template>
              <template v-else>
                <el-button v-if="o.status === 1" type="primary" size="small" @click="deliver(o)">发货</el-button>
              </template>
              <el-button size="small" text type="primary" @click="goProduct(o)">查看商品</el-button>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无订单" />
      <div style="display: flex; justify-content: center; margin-top: 16px" v-if="total > pageSize">
        <el-pagination background layout="prev, pager, next" :total="total"
                       :page-size="pageSize" v-model:current-page="pageNum" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="commentVisible" title="发表评价" width="440px">
      <el-form label-width="60px">
        <el-form-item label="评分">
          <el-rate v-model="commentForm.rating" />
        </el-form-item>
        <el-form-item label="评价">
          <el-input v-model="commentForm.content" type="textarea" :rows="3" maxlength="500" show-word-limit
                    placeholder="宝贝描述与实物相符吗？卖家服务怎么样？" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="commentVisible = false">取消</el-button>
        <el-button type="primary" :loading="commenting" @click="submitComment">提交评价</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBoughtOrders, getSoldOrders, cancelOrder, deliverOrder, receiveOrder, refundOrder, mockPay, addComment } from '../../api'

const route = useRoute()
const router = useRouter()
const tab = ref(route.query.tab === 'sold' ? 'sold' : 'bought')
const orders = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const commentVisible = ref(false)
const commenting = ref(false)
const commentForm = reactive({ orderNo: '', productId: null, rating: 5, content: '' })

const statusType = s => ({ 0: 'warning', 1: 'primary', 2: 'primary', 3: 'success', 4: 'info', 5: 'danger' }[s] || 'info')

async function load() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize }
    const data = tab.value === 'bought' ? await getBoughtOrders(params) : await getSoldOrders(params)
    orders.value = data.records
    total.value = Number(data.total)
    orders.value.forEach(o => { o.commented = false })
  } finally {
    loading.value = false
  }
}

function goProduct(o) {
  router.push(`/product/${o.productId}`)
}

async function pay(o) {
  await ElMessageBox.confirm(`确认支付 ￥${o.amount} 吗？（模拟支付）`, '模拟支付', { type: 'warning' })
  await mockPay(o.orderNo)
  ElMessage.success('支付成功')
  load()
}

async function cancel(o) {
  await ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
  await cancelOrder(o.orderNo)
  ElMessage.success('已取消')
  load()
}

async function deliver(o) {
  await deliverOrder(o.orderNo)
  ElMessage.success('已发货')
  load()
}

async function receive(o) {
  await receiveOrder(o.orderNo)
  ElMessage.success('交易完成，别忘了评价哦')
  load()
}

async function refund(o) {
  await ElMessageBox.confirm('确定申请退款吗？退款后商品将重新上架。', '申请退款', { type: 'warning' })
  await refundOrder(o.orderNo)
  ElMessage.success('退款成功')
  load()
}

function openComment(o) {
  commentForm.orderNo = o.orderNo
  commentForm.productId = o.productId
  commentForm.rating = 5
  commentForm.content = ''
  commentVisible.value = true
}

async function submitComment() {
  if (!commentForm.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  commenting.value = true
  try {
    await addComment({
      productId: commentForm.productId,
      content: commentForm.content,
      rating: commentForm.rating
    })
    commentVisible.value = false
    ElMessage.success('评价成功')
    load()
  } finally {
    commenting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.order-list { display: flex; flex-direction: column; gap: 14px; }
.order-card { border: 1px solid #eee; border-radius: 8px; padding: 12px 16px; }
.o-top { display: flex; justify-content: space-between; padding-bottom: 8px; border-bottom: 1px dashed #f0f0f0; }
.o-body { display: flex; align-items: center; gap: 14px; padding-top: 12px; flex-wrap: wrap; }
.o-img { width: 72px; height: 72px; border-radius: 8px; flex-shrink: 0; }
.o-img-empty { display: flex; align-items: center; justify-content: center; background: #f5f5f5; color: #ccc; }
.o-info { flex: 1; min-width: 200px; cursor: pointer; }
.o-title { font-size: 15px; color: #333; }
.o-right { text-align: right; min-width: 90px; }
.o-actions { display: flex; gap: 8px; align-items: center; }
</style>
