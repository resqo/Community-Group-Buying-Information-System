import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/login/LoginView.vue'
import UserHome from '../views/user/UserHome.vue'
import LeaderHome from '../views/leader/LeaderHome.vue'
import MerchantHome from '../views/merchant/MerchantHome.vue'
import AdminHome from '../views/admin/AdminHome.vue'
import { applySession, getSession } from '../utils/session'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView },
  { path: '/user/home', component: UserHome, meta: { role: 'USER' } },
  { path: '/leader/dashboard', component: LeaderHome, meta: { role: 'LEADER' } },
  { path: '/merchant/products', component: MerchantHome, meta: { role: 'MERCHANT' } },
  { path: '/admin/dashboard', component: AdminHome, meta: { role: 'ADMIN' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.path === '/login') return true
  if (to.meta.role) {
    const session = getSession(to.meta.role)
    if (!session?.token) return { path: '/login', query: { redirect: to.fullPath } }
    applySession(session)
    return true
  }
  if (!getSession()?.token) return { path: '/login', query: { redirect: to.fullPath } }
  return true
})

export default router
