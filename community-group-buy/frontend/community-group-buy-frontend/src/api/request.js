import axios from 'axios'
import { notify } from '../utils/notify'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result && typeof result.code !== 'undefined') {
      if (result.code === 200) {
        return result.data
      }
      notify.error(result.message || '请求失败')
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return result
  },
  (error) => {
    notify.error(error.response?.data?.message || error.message || '服务连接失败')
    return Promise.reject(error)
  },
)

export const api = {
  health: () => http.get('/health'),
  login: (data) => http.post('/auth/login', data),
  register: (data) => http.post('/auth/register', data),
  users: () => http.get('/admin/users'),
  updateUserStatus: (userId, status) => http.put(`/admin/users/${userId}/status`, { status }),

  categories: () => http.get('/categories'),
  products: (params) => http.get('/products', { params }),
  productDetail: (id) => http.get(`/products/${id}`),
  createProduct: (data) => http.post('/merchant/products', data),
  updateProduct: (id, data) => http.put(`/merchant/products/${id}`, data),
  deleteProduct: (id) => http.delete(`/merchant/products/${id}`),
  auditProduct: (id, auditStatus) => http.post(`/admin/products/${id}/audit`, { auditStatus }),

  activities: () => http.get('/group-activities'),
  saveActivity: (data) =>
    data.activityId
      ? http.put(`/admin/group-activities/${data.activityId}`, data)
      : http.post('/admin/group-activities', data),
  startGroup: (activityId, userId) => http.post('/groups/start', { activityId, userId }),
  joinGroup: (groupId, userId) => http.post('/groups/join', { groupId, userId }),
  openGroups: (userId) => http.get('/groups/open', { params: { userId } }),
  myGroups: (userId) => http.get('/groups/my', { params: { userId } }),

  pickupPoints: () => http.get('/pickup-points'),
  leaderPickupPoints: (leaderId) => http.get('/leader/pickup-points', { params: { leaderId } }),
  savePickupPoint: (data) => http.put('/leader/pickup-point', data),

  createOrder: (data) => http.post('/orders', data),
  orders: (params) => http.get('/orders', { params }),
  myOrders: (userId) => http.get('/orders/my', { params: { userId } }),
  freeGroupOrder: (orderId, userId) => http.post(`/orders/${orderId}/free-group`, { userId }),
  merchantOrders: (merchantId) => http.get('/merchant/orders', { params: { merchantId } }),
  leaderOrders: (pickupPointId) => http.get('/leader/orders/wait-pickup', { params: { pickupPointId } }),
  payOrder: (orderId, payMethod = '模拟支付', paymentPassword = '') => http.post(`/orders/${orderId}/pay`, { payMethod, paymentPassword }),
  deliverOrder: (orderId) => http.post(`/merchant/orders/${orderId}/deliver`),
  sendPickupNotice: (orderId) => http.post(`/leader/orders/${orderId}/send-code`),
  verifyOrder: (pickupCode) => http.post('/leader/orders/verify', { pickupCode }),

  applyRefund: (data) => http.post('/refunds', data),
  refunds: () => http.get('/admin/refunds'),
  handleRefund: (refundId, data) => http.post(`/admin/refunds/${refundId}/handle`, data),
  payments: () => http.get('/admin/payments'),
  notices: (userId) => http.get('/notices', { params: { userId } }),
  publishNotice: (data) => http.post('/admin/notices', data),
  userProfile: (userId) => http.get(`/users/${userId}`),
  updateUserProfile: (userId, data) => http.put(`/users/${userId}`, data),
  recommendations: (userId) => http.get('/recommendations', { params: { userId } }),
  merchantAnalyticsSales: (merchantId) => http.get('/merchant/analytics/sales', { params: { merchantId } }),
  merchantAnalyticsSuggestions: (merchantId) => http.get('/merchant/analytics/suggestions', { params: { merchantId } }),
  adminAnalyticsOverview: () => http.get('/admin/analytics/overview'),
  adminAnalyticsSuggestions: () => http.get('/admin/analytics/suggestions'),
}

export default http
