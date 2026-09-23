<template>
  <el-card class="card-shadow">
    <template #header><b>我的收藏</b></template>
    <div v-if="products.length" class="fav-grid">
      <div v-for="p in products" :key="p.id" class="fav-card">
        <div class="img-box" @click="$router.push(`/product/${p.id}`)">
          <img v-if="p.coverImage" :src="p.coverImage" />
          <el-icon v-else :size="36" color="#ddd"><Picture /></el-icon>
        </div>
        <div class="f-info">
          <div class="ellipsis f-title" @click="$router.push(`/product/${p.id}`)">{{ p.title }}</div>
          <div class="f-bottom">
            <span class="price">￥{{ p.price }}</span>
            <el-button size="small" type="danger" text @click="unfavorite(p)">取消收藏</el-button>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-else description="还没有收藏任何宝贝" />
    <div style="display: flex; justify-content: center; margin-top: 16px" v-if="total > pageSize">
      <el-pagination background layout="prev, pager, next" :total="total"
                     :page-size="pageSize" v-model:current-page="pageNum" @current-change="load" />
    </div>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getFavorites, removeFavorite } from '../../api'

const products = ref([])
const pageNum = ref(1)
const pageSize = 12
const total = ref(0)

async function load() {
  const data = await getFavorites({ pageNum: pageNum.value, pageSize })
  products.value = data.records
  total.value = Number(data.total)
}

async function unfavorite(p) {
  await removeFavorite(p.id)
  ElMessage.success('已取消收藏')
  load()
}

onMounted(load)
</script>

<style scoped>
.fav-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; }
.fav-card { border: 1px solid #f0f0f0; border-radius: 8px; overflow: hidden; }
.img-box {
  height: 140px; display: flex; align-items: center; justify-content: center;
  background: #fafafa; cursor: pointer;
}
.img-box img { width: 100%; height: 100%; object-fit: cover; }
.f-info { padding: 10px 12px; }
.f-title { font-size: 14px; color: #333; cursor: pointer; }
.f-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
</style>
