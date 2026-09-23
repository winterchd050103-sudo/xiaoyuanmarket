<template>
  <el-card v-if="product" class="card-shadow">
    <div class="detail-wrap">
      <div class="gallery">
        <el-carousel v-if="gallery.length > 1" height="400px" :autoplay="false" indicator-position="outside">
          <el-carousel-item v-for="(img, i) in gallery" :key="i">
            <img :src="img" class="main-img" />
          </el-carousel-item>
        </el-carousel>
        <img v-else-if="gallery.length" :src="gallery[0]" class="main-img" />
        <div v-else class="no-img">
          <el-icon :size="80" color="#ddd"><Picture /></el-icon>
        </div>
      </div>
      <div class="info">
        <div class="price-row">
          <span class="big-price">￥{{ product.price }}</span>
          <span class="text-gray" v-if="product.originalPrice">原价 ￥{{ product.originalPrice }}</span>
        </div>
        <h2 class="p-title">{{ product.title }}</h2>
        <div class="meta-row">
          <el-tag size="small" type="info">{{ product.categoryName || '未分类' }}</el-tag>
          <el-tag size="small" :type="statusType">{{ statusText }}</el-tag>
          <span class="text-gray">{{ product.viewCount }} 次浏览 · {{ formatTime(product.createTime) }} 发布</span>
        </div>
        <div class="desc">{{ product.description || '卖家很懒，没有留下描述~' }}</div>
        <el-divider />
        <div class="seller-row">
          <el-avatar :size="36">{{ (product.sellerNickname || '卖').slice(0, 1) }}</el-avatar>
          <span>{{ product.sellerNickname }}</span>
          <span class="text-gray" style="margin-left: auto">卖家信誉良好</span>
        </div>
        <div class="action-row">
          <el-button size="large" :type="product.favorited ? 'warning' : 'default'" plain
                     :disabled="!userStore.isLoggedIn" @click="toggleFavorite">
            <el-icon><Star /></el-icon>&nbsp;{{ product.favorited ? '已收藏' : '收藏' }}
          </el-button>
          <el-button size="large" type="primary" :disabled="product.status !== 1"
                     :loading="buying" @click="openBuyDialog">
            {{ product.status === 1 ? '立即购买' : '已' + statusText }}
          </el-button>
        </div>
      </div>
    </div>

    <el-divider content-position="left">商品评价</el-divider>
    <div v-if="comments.length">
      <div v-for="c in comments" :key="c.id" class="comment-item">
        <el-avatar :size="34" :src="c.avatar || ''">{{ (c.nickname || 'U').slice(0, 1) }}</el-avatar>
        <div class="comment-body">
          <div class="comment-head">
            <span class="comment-nick">{{ c.nickname }}</span>
            <el-rate :model-value="c.rating" disabled size="small" />
            <span class="text-gray">{{ formatTime(c.createTime) }}</span>
          </div>
          <div class="comment-content">{{ c.content }}</div>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无评价" :image-size="80" />

    <el-dialog v-model="buyDialogVisible" title="确认订单" width="480px">
      <el-alert v-if="!addresses.length" type="warning" :closable="false"
                title="您还没有收货地址，请先到 个人中心 添加地址" style="margin-bottom: 12px" />
      <el-form label-width="80px" v-else>
        <el-form-item label="收货地址">
          <el-select v-model="selectedAddressId" placeholder="选择收货地址" style="width: 100%">
            <el-option v-for="a in addresses" :key="a.id"
                       :label="`${a.receiver} ${a.phone}（${a.province}${a.city}${a.district}${a.detail}）${a.isDefault ? ' [默认]' : ''}`"
                       :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品">
          {{ product.title }}
        </el-form-item>
        <el-form-item label="应付金额">
          <span class="price">￥{{ product.price }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="buyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="buying" :disabled="!addresses.length || !selectedAddressId"
                   @click="submitOrder">提交订单</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductDetail, getComments, getAddresses, createOrder, addFavorite, removeFavorite } from '../../api'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const userStore = useUserStore()
const product = ref(null)
const comments = ref([])
const addresses = ref([])
const selectedAddressId = ref(null)
const buyDialogVisible = ref(false)
const buying = ref(false)

const gallery = computed(() => {
  if (!product.value) return []
  const imgs = [...(product.value.images || [])]
  if (product.value.coverImage && !imgs.includes(product.value.coverImage)) {
    imgs.unshift(product.value.coverImage)
  }
  return imgs
})

const statusMap = { 0: ['待审核', 'warning'], 1: ['在售', 'success'], 2: ['已下架', 'info'], 3: ['已售出', 'danger'] }
const statusText = computed(() => statusMap[product.value?.status]?.[0] || '未知')
const statusType = computed(() => statusMap[product.value?.status]?.[1] || 'info')

function formatTime(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  const id = route.params.id
  product.value = await getProductDetail(id)
  const data = await getComments({ productId: id, pageNum: 1, pageSize: 20 })
  comments.value = data.records
}

async function toggleFavorite() {
  if (product.value.favorited) {
    await removeFavorite(product.value.id)
    product.value.favorited = false
    ElMessage.success('已取消收藏')
  } else {
    await addFavorite(product.value.id)
    product.value.favorited = true
    ElMessage.success('收藏成功')
  }
}

async function openBuyDialog() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  buying.value = true
  try {
    addresses.value = await getAddresses()
    const def = addresses.value.find(a => a.isDefault)
    selectedAddressId.value = def ? def.id : (addresses.value[0]?.id ?? null)
    buyDialogVisible.value = true
  } finally {
    buying.value = false
  }
}

async function submitOrder() {
  buying.value = true
  try {
    const clientOrderNo = (crypto.randomUUID ? crypto.randomUUID() : Date.now() + Math.random().toString(36).slice(2))
      .replace(/-/g, '').toUpperCase()
    const order = await createOrder({
      clientOrderNo,
      productId: product.value.id,
      addressId: selectedAddressId.value
    })
    buyDialogVisible.value = false
    ElMessage.success('下单成功，请尽快支付')
    location.href = '/orders?tab=bought&orderNo=' + order.orderNo
  } finally {
    buying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.detail-wrap { display: flex; gap: 32px; }
.gallery { width: 460px; flex-shrink: 0; }
.main-img { width: 100%; height: 400px; object-fit: contain; background: #fafafa; border-radius: 8px; }
.no-img {
  height: 400px; display: flex; align-items: center; justify-content: center;
  background: #fafafa; border-radius: 8px;
}
.info { flex: 1; display: flex; flex-direction: column; }
.price-row { display: flex; align-items: baseline; gap: 12px; }
.big-price { font-size: 30px; color: #f56c6c; font-weight: 700; }
.p-title { margin: 12px 0; color: #303133; }
.meta-row { display: flex; align-items: center; gap: 10px; }
.desc {
  margin-top: 16px; color: #555; line-height: 1.8; white-space: pre-wrap;
  background: #fafafa; border-radius: 8px; padding: 14px;
}
.seller-row { display: flex; align-items: center; gap: 10px; }
.action-row { margin-top: 24px; display: flex; gap: 12px; }
.comment-item { display: flex; gap: 12px; padding: 14px 0; border-bottom: 1px dashed #eee; }
.comment-body { flex: 1; }
.comment-head { display: flex; align-items: center; gap: 10px; }
.comment-nick { font-weight: 600; font-size: 14px; }
.comment-content { margin-top: 6px; color: #444; line-height: 1.6; }
</style>
