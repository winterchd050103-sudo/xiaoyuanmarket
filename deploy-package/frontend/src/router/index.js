import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/mall/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/mall/RegisterView.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('../layouts/MallLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../views/mall/HomeView.vue'), meta: { title: '首页' } },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('../views/mall/ProductDetailView.vue'), meta: { title: '商品详情' } },
      { path: 'publish', name: 'Publish', component: () => import('../views/mall/PublishView.vue'), meta: { title: '发布商品', requiresAuth: true } },
      { path: 'my/products', name: 'MyProducts', component: () => import('../views/mall/MyProductsView.vue'), meta: { title: '我的商品', requiresAuth: true } },
      { path: 'orders', name: 'Orders', component: () => import('../views/mall/OrdersView.vue'), meta: { title: '我的订单', requiresAuth: true } },
      { path: 'favorites', name: 'Favorites', component: () => import('../views/mall/FavoritesView.vue'), meta: { title: '我的收藏', requiresAuth: true } },
      { path: 'profile', name: 'Profile', component: () => import('../views/mall/ProfileView.vue'), meta: { title: '个人中心', requiresAuth: true } }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/DashboardView.vue'), meta: { title: '数据统计' } },
      { path: 'users', name: 'AdminUsers', component: () => import('../views/admin/AdminUsersView.vue'), meta: { title: '用户管理' } },
      { path: 'products', name: 'AdminProducts', component: () => import('../views/admin/AdminProductsView.vue'), meta: { title: '商品管理' } },
      { path: 'orders', name: 'AdminOrders', component: () => import('../views/admin/AdminOrdersView.vue'), meta: { title: '订单管理' } },
      { path: 'categories', name: 'AdminCategories', component: () => import('../views/admin/AdminCategoriesView.vue'), meta: { title: '分类管理' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(to => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '校园二手交易平台'
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  if (to.meta.requiresAuth && !token) {
    ElMessage.warning('请先登录')
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && userInfo?.role !== 1) {
    ElMessage.error('无管理员权限')
    return { path: '/' }
  }
  return true
})

export default router
