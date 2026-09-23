<template>
  <el-container class="mall-layout">
    <el-header class="mall-header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/')">
          <el-icon :size="26" color="#409eff"><ShoppingCart /></el-icon>
          <span class="logo-text">校园二手</span>
        </div>
        <div class="search-box">
          <el-input v-model="keyword" placeholder="搜索想要的宝贝..." clearable @keyup.enter="doSearch">
            <template #append>
              <el-button @click="doSearch" :icon="Search" />
            </template>
          </el-input>
        </div>
        <div class="nav-actions">
          <el-button text type="primary" @click="$router.push('/publish')">
            <el-icon><Plus /></el-icon><span class="publish-text">&nbsp;发布闲置</span>
          </el-button>
          <template v-if="userStore.isLoggedIn">
            <el-dropdown @command="onCommand">
              <span class="user-info">
                <el-avatar :size="32" :src="userStore.userInfo?.avatar || ''">
                  {{ (userStore.userInfo?.nickname || 'U').slice(0, 1) }}
                </el-avatar>
                <span class="nickname">{{ userStore.userInfo?.nickname }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="my-products">我的商品</el-dropdown-item>
                  <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>管理后台</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" plain @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    <el-main class="mall-main">
      <router-view />
    </el-main>
    <el-footer class="mall-footer">
      校园二手交易平台 · Spring Boot 3 + Vue 3 个人全栈项目
    </el-footer>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')

function doSearch() {
  router.push({ path: '/', query: { keyword: keyword.value || undefined } })
}

function onCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
    return
  }
  const map = {
    orders: '/orders',
    'my-products': '/my/products',
    favorites: '/favorites',
    profile: '/profile',
    admin: '/admin/dashboard'
  }
  router.push(map[cmd])
}
</script>

<style scoped>
.mall-layout { min-height: 100vh; }
.mall-header {
  background: #fff;
  border-bottom: 1px solid #eee;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 16px;
}
.logo { display: flex; align-items: center; gap: 8px; cursor: pointer; flex-shrink: 0; }
.logo-text { font-size: 20px; font-weight: 700; color: #409eff; white-space: nowrap; }
.search-box { flex: 1; max-width: 480px; }
.nav-actions { display: flex; align-items: center; gap: 8px; margin-left: auto; flex-shrink: 0; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.nickname { font-size: 14px; color: #333; }
.mall-main { max-width: 1200px; margin: 0 auto; width: 100%; padding: 16px; }
.mall-footer {
  text-align: center;
  color: #aaa;
  font-size: 13px;
  height: 56px;
  line-height: 56px;
}
/* ===== 手机端适配 ===== */
@media (max-width: 768px) {
  .header-inner { gap: 10px; padding: 0 12px; }
  .logo-text { font-size: 18px; }
  .search-box { display: none; }
  .nickname { display: none; }
  .publish-text { display: none; }
  .nav-actions .el-button + .el-button { margin-left: 0; }
  .mall-main { padding: 12px; }
  .mall-footer { font-size: 12px; height: 44px; line-height: 44px; }
}
</style>
