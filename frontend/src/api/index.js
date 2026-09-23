import request from './request'

// ---------- 认证 ----------
export const login = data => request.post('/auth/login', data)
export const register = data => request.post('/auth/register', data)

// ---------- 用户 ----------
export const getProfile = () => request.get('/user/profile')
export const updateProfile = data => request.put('/user/profile', data)
export const changePassword = data => request.put('/user/password', data)
export const uploadAvatar = formData => request.post('/user/avatar', formData)

// ---------- 文件 ----------
export const uploadFile = formData => request.post('/files/upload', formData)

// ---------- 分类 ----------
export const getCategories = () => request.get('/categories')

// ---------- 商品 ----------
export const getProducts = params => request.get('/products', { params })
export const getProductDetail = id => request.get(`/products/${id}`)
export const getMyProducts = params => request.get('/products/my', { params })
export const publishProduct = data => request.post('/products', data)
export const updateProduct = (id, data) => request.put(`/products/${id}`, data)
export const deleteProduct = id => request.delete(`/products/${id}`)
export const changeProductStatus = (id, status) => request.put(`/products/${id}/status`, { status })

// ---------- 收藏 ----------
export const addFavorite = productId => request.post(`/favorites/${productId}`)
export const removeFavorite = productId => request.delete(`/favorites/${productId}`)
export const getFavorites = params => request.get('/favorites', { params })

// ---------- 评论 ----------
export const getComments = params => request.get('/comments', { params })
export const addComment = data => request.post('/comments', data)

// ---------- 地址 ----------
export const getAddresses = () => request.get('/address/list')
export const addAddress = data => request.post('/address', data)
export const updateAddress = (id, data) => request.put(`/address/${id}`, data)
export const deleteAddress = id => request.delete(`/address/${id}`)
export const setDefaultAddress = id => request.put(`/address/${id}/default`)

// ---------- 订单 ----------
export const createOrder = data => request.post('/orders', data)
export const getBoughtOrders = params => request.get('/orders/bought', { params })
export const getSoldOrders = params => request.get('/orders/sold', { params })
export const getOrderDetail = orderNo => request.get(`/orders/${orderNo}`)
export const cancelOrder = orderNo => request.put(`/orders/${orderNo}/cancel`)
export const deliverOrder = orderNo => request.put(`/orders/${orderNo}/deliver`)
export const receiveOrder = orderNo => request.put(`/orders/${orderNo}/receive`)
export const refundOrder = orderNo => request.put(`/orders/${orderNo}/refund`)

// ---------- 支付 ----------
export const mockPay = orderNo => request.post(`/pay/mock/${orderNo}`)

// ---------- 管理后台 ----------
export const adminGetUsers = params => request.get('/admin/users', { params })
export const adminSetUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, { status })
export const adminGetProducts = params => request.get('/admin/products', { params })
export const adminAuditProduct = id => request.put(`/admin/products/${id}/audit`)
export const adminOfflineProduct = id => request.put(`/admin/products/${id}/offline`)
export const adminGetOrders = params => request.get('/admin/orders', { params })
export const adminGetStats = () => request.get('/admin/stats')
export const adminGetCategories = () => request.get('/admin/categories')
export const adminAddCategory = data => request.post('/admin/categories', data)
export const adminUpdateCategory = (id, data) => request.put(`/admin/categories/${id}`, data)
export const adminDeleteCategory = id => request.delete(`/admin/categories/${id}`)
