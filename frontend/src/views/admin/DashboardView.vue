<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic title="用户总数" :value="stats.userCount || 0">
            <template #prefix><el-icon color="#409eff"><User /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic title="商品总数" :value="stats.productCount || 0">
            <template #prefix><el-icon color="#67c23a"><Goods /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic title="订单总数" :value="stats.orderCount || 0">
            <template #prefix><el-icon color="#e6a23c"><List /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic title="成交额（元）" :value="Number(stats.gmv || 0)" :precision="2">
            <template #prefix><el-icon color="#f56c6c"><Money /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header><b>近 7 日订单量趋势</b></template>
          <div ref="orderChartRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><b>近 7 日商品发布量趋势</b></template>
          <div ref="productChartRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { adminGetStats } from '../../api'

const stats = ref({})
const loading = ref(false)
const orderChartRef = ref()
const productChartRef = ref()

function renderLine(el, title, data) {
  if (!el) return
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.date) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ name: title, type: 'line', smooth: true, data: data.map(d => d.count), areaStyle: { opacity: 0.15 } }]
  })
}

onMounted(async () => {
  loading.value = true
  try {
    stats.value = await adminGetStats()
    await nextTick()
    renderLine(orderChartRef.value, '订单量', stats.value.orderTrend || [])
    renderLine(productChartRef.value, '发布量', stats.value.productTrend || [])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-card :deep(.el-statistic__head) { color: #999; }
.chart { height: 300px; }
</style>
