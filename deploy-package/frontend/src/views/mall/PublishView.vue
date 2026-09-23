<template>
  <el-card class="card-shadow">
    <template #header>
      <b>{{ editId ? '编辑商品' : '发布闲置' }}</b>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" style="max-width: 720px">
      <el-form-item label="商品分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 240px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="几句话描述你的宝贝" />
      </el-form-item>
      <el-form-item label="售价" prop="price">
        <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" controls-position="right" />
        <span style="margin-left: 8px; color: #999">元</span>
      </el-form-item>
      <el-form-item label="原价">
        <el-input-number v-model="form.originalPrice" :min="0.01" :precision="2" :step="1" controls-position="right" />
        <span style="margin-left: 8px; color: #999">元（选填，展示划线价）</span>
      </el-form-item>
      <el-form-item label="商品描述">
        <el-input v-model="form.description" type="textarea" :rows="5" maxlength="5000"
                  placeholder="成色、入手渠道、转让原因等，写得越清楚越容易卖出" />
      </el-form-item>
      <el-form-item label="图片上传">
        <el-upload v-model:file-list="fileList" list-type="picture-card" :auto-upload="true"
                   :http-request="customUpload" :limit="9" accept="image/*"
                   :on-preview="onPreview" :on-remove="onRemove" :on-exceed="() => ElMessage.warning('最多 9 张')">
          <el-icon><Plus /></el-icon>
        </el-upload>
        <div class="text-gray">第一张将自动作为封面图，最多 9 张，单张不超过 10MB</div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" :loading="submitting" @click="onSubmit">
          {{ editId ? '保存修改' : '发 布' }}
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
  <el-dialog v-model="previewVisible"><img :src="previewUrl" style="width: 100%" /></el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCategories, getMyProducts, getProductDetail, publishProduct, updateProduct, uploadFile } from '../../api'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const categories = ref([])
const fileList = ref([])
const previewVisible = ref(false)
const previewUrl = ref('')
const editId = computed(() => (route.query.id ? Number(route.query.id) : null))

const form = reactive({
  categoryId: null,
  title: '',
  price: null,
  originalPrice: null,
  description: ''
})

const rules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

async function customUpload({ file, onSuccess, onError }) {
  try {
    const fd = new FormData()
    fd.append('file', file)
    const url = await uploadFile(fd)
    onSuccess({ url })
  } catch (e) {
    onError(e)
  }
}

function uploadedUrls() {
  return fileList.value.map(f => f.response?.url || f.url).filter(Boolean)
}

function onPreview(file) {
  previewUrl.value = file.response?.url || file.url
  previewVisible.value = true
}

function onRemove() {}

async function loadForEdit() {
  if (!editId.value) return
  // 从我的商品里取详情（含图片）
  const p = await getProductDetail(editId.value)
  form.categoryId = p.categoryId
  form.title = p.title
  form.price = Number(p.price)
  form.originalPrice = p.originalPrice ? Number(p.originalPrice) : null
  form.description = p.description
  fileList.value = (p.images || []).map(u => ({ name: u.split('/').pop(), url: u }))
}

async function onSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const images = uploadedUrls()
    const payload = { ...form, images, coverImage: images[0] || null }
    if (editId.value) {
      await updateProduct(editId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await publishProduct(payload)
      ElMessage.success('发布成功')
    }
    router.push('/my/products')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  categories.value = await getCategories()
  loadForEdit()
})
</script>
