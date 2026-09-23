<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="admin-aside">
      <div class="admin-logo">
        <el-icon :size="24" color="#fff"><Setting /></el-icon>
        <span>管理后台</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#001529" text-color="#a6adb4"
               active-text-color="#fff" class="admin-menu">
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon><span>数据统计</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/products">
          <el-icon><Goods /></el-icon><span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><List /></el-icon><span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><Menu /></el-icon><span>分类管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <span class="admin-title">{{ $route.meta.title }}</span>
        <div class="admin-actions">
          <el-button text @click="$router.push('/')">返回商城</el-button>
          <el-dropdown @command="onCommand">
            <span class="admin-user">
              <el-avatar :size="30" :src="userStore.userInfo?.avatar || ''">
                {{ (userStore.userInfo?.nickname || 'A').slice(0, 1) }}
              </el-avatar>
              {{ userStore.userInfo?.nickname }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

function onCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped>
.admin-layout { min-height: 100vh; }
.admin-aside { background: #001529; }
.admin-logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}
.admin-menu { border-right: none; }
.admin-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
}
.admin-title { font-size: 17px; font-weight: 600; }
.admin-actions { display: flex; align-items: center; gap: 16px; }
.admin-user { display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 14px; }
.admin-main { background: #f5f6f8; }
</style>
