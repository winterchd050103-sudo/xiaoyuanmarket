<template>
  <div>
    <el-card class="card-shadow" style="margin-bottom: 16px">
      <div class="sort-bar">
        <el-radio-group v-model="sort" @change="loadProducts(true)">
          <el-radio-button value="newest">最新发布</el-radio-button>
          <el-radio-button value="hot">最多浏览</el-radio-button>
          <el-radio-button value="price_asc">价格从低到高</el-radio-button>
          <el-radio-button value="price_desc">价格从高到低</el-radio-button>
        </el-radio-group>
        <span class="text-gray" v-if="keyword">搜索“{{ keyword }}”共 {{ total }} 件</span>
      </div>
    </el-card>

    <el-card class="card-shadow">
      <div style="display: flex; gap: 16px">
        <div class="category-side">
          <div class="cat-item" :class="{ active: !categoryId }" @click="selectCategory(null)">全部</div>
          <div v-for="c in categories" :key="c.id" class="cat-item"
               :class="{ active: categoryId === c.id }" @click="selectCategory(c.id)">
            {{ c.name }}
          </div>
        </div>
        <div style="flex: 1">
          <div v-if="products.length" class="product-grid">
            <div v-for="p in products" :key="p.id" class="product-card" @click="$router.push(`/product/${p.id}`)">
              <div class="img-box">
                <img v-if="p.coverImage" :src="p.coverImage" loading="lazy" />
                <el-icon v-else :size="40" color="#ddd"><Picture /></el-icon>
              </div>
              <div class="p-info">
                <div class="p-title ellipsis">{{ p.title }}</div>
                <div class="p-bottom">
                  <span class="price">￥{{ p.price }}</span>
                  <span class="text-gray">{{ p.viewCount }} 浏览</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无商品，快来发布第一件闲置吧" />
          <div style="display: flex; justify-content: center; margin-top: 20px" v-if="total > pageSize">
            <el-pagination background layout="prev, pager, next" :total="total"
                           :page-size="pageSize" v-model:current-page="pageNum" @current-change="loadProducts()" />
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getCategories, getProducts } from '../../api'

const route = useRoute()
const categories = ref([])
const products = ref([])
const categoryId = ref(null)
const sort = ref('newest')
const keyword = ref(route.query.keyword || '')
const pageNum = ref(1)
const pageSize = 12
const total = ref(0)

watch(() => route.query.keyword, val => {
  keyword.value = val || ''
  loadProducts(true)
})

function selectCategory(id) {
  categoryId.value = id
  loadProducts(true)
}

async function loadProducts(reset = false) {
  if (reset) pageNum.value = 1
  const data = await getProducts({
    categoryId: categoryId.value || undefined,
    keyword: keyword.value || undefined,
    sort: sort.value,
    pageNum: pageNum.value,
    pageSize
  })
  products.value = data.records
  total.value = Number(data.total)
}

onMounted(async () => {
  categories.value = await getCategories()
  loadProducts(true)
})
</script>

<style scoped>
.sort-bar { display: flex; align-items: center; justify-content: space-between; }
.category-side { width: 150px; flex-shrink: 0; }
.cat-item {
  padding: 10px 14px;
  border-radius: 6px;
  cursor: pointer;
  color: #555;
  margin-bottom: 4px;
}
.cat-item:hover { background: #f0f7ff; }
.cat-item.active { background: #e8f3ff; color: #409eff; font-weight: 600; }
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 16px;
}
.product-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all .2s;
}
.product-card:hover { transform: translateY(-3px); box-shadow: 0 6px 16px rgba(0,0,0,.08); }
.img-box {
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}
.img-box img { width: 100%; height: 100%; object-fit: cover; }
.p-info { padding: 10px 12px; }
.p-title { font-size: 14px; color: #333; }
.p-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.price { font-size: 17px; }
</style>
