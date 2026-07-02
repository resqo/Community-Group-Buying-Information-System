<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../../api/request'
import { notify } from '../../utils/notify'
import { clearSession } from '../../utils/session'
import * as echarts from 'echarts'

const router = useRouter()
const route = useRoute()
const adminId = Number(localStorage.getItem('userId'))
const savedAvatarUrl = localStorage.getItem('avatarUrl')
const avatarUrl = savedAvatarUrl && !savedAvatarUrl.includes('favicon.svg') ? savedAvatarUrl : ''
const loading = ref(false)
const activeTab = ref(typeof route.query.tab === 'string' ? route.query.tab : 'dashboard')
const userRoleFilter = ref('ALL')

const users = ref([])
const products = ref([])
const orders = ref([])
const refunds = ref([])
const activities = ref([])
const payments = ref([])
const analyticsOverview = ref(null)
const adminSuggestions = ref([])
const suggestionsLoading = ref(false)
let adminTimer = null
let adminChartInstances = []

const activityDialog = reactive({ visible: false })
const activityForm = reactive({
  productId: null,
  groupPrice: 0,
  groupSize: 2,
  startTime: '',
  endTime: '',
  allowFreeGroup: 1,
  freeGroupLimit: 1,
  status: 1,
})

const noticeForm = reactive({
  userId: '',
  title: '系统公告',
  content: '',
  noticeType: 'SYSTEM',
})

const stats = computed(() => ({
  users: users.value.length,
  merchants: users.value.filter((item) => item.role === 'MERCHANT').length,
  customers: users.value.filter((item) => item.role === 'USER').length,
  products: products.value.length,
  orders: orders.value.length,
  refunds: refunds.value.filter((item) => Number(item.refund_status) === 0).length,
  amount: orders.value.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0),
}))

const filteredUsers = computed(() => userRoleFilter.value === 'ALL'
  ? users.value
  : users.value.filter((item) => item.role === userRoleFilter.value))

const userRoleOptions = [
  { label: '全部', value: 'ALL' },
  { label: '客户', value: 'USER' },
  { label: '团长', value: 'LEADER' },
  { label: '商家', value: 'MERCHANT' },
  { label: '管理员', value: 'ADMIN' },
]

async function loadAll() {
  loading.value = true
  try {
    const [userData, productData, orderData, refundData, activityData, paymentData] = await Promise.all([
      api.users(),
      api.products(),
      api.orders(),
      api.refunds(),
      api.activities(),
      api.payments(),
    ])
    users.value = userData
    products.value = productData
    orders.value = orderData
    refunds.value = refundData
    activities.value = activityData
    payments.value = paymentData
  } finally {
    loading.value = false
  }
}

async function setUserStatus(user, status) {
  await api.updateUserStatus(user.userId, status)
  notify.success('账号状态已更新')
  await loadAll()
}

async function audit(product, auditStatus) {
  await api.auditProduct(product.productId, auditStatus)
  notify.success('审核结果已保存')
  await loadAll()
}

function openActivity(product = null) {
  const now = new Date()
  const end = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
  Object.assign(activityForm, {
    productId: product?.productId || products.value[0]?.productId || null,
    groupPrice: product?.groupPrice || 0,
    groupSize: 3,
    startTime: formatDate(now),
    endTime: formatDate(end),
    allowFreeGroup: 1,
    freeGroupLimit: 1,
    status: 1,
  })
  activityDialog.visible = true
}

async function saveActivity() {
  await api.saveActivity(activityForm)
  activityDialog.visible = false
  notify.success('拼团活动已保存')
  await loadAll()
}

async function handleRefund(row, status) {
  await api.handleRefund(row.refund_id, { adminId, status })
  notify.success('退款审核已处理')
  await loadAll()
}

async function publishNotice() {
  await api.publishNotice({
    userId: noticeForm.userId || null,
    title: noticeForm.title,
    content: noticeForm.content,
    noticeType: noticeForm.noticeType,
  })
  notify.success('通知已发布')
  noticeForm.content = ''
}

function formatDate(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function roleText(role) {
  return { USER: '客户', LEADER: '团长', MERCHANT: '商家', ADMIN: '管理员' }[role] || role
}

function auditText(status) {
  return Number(status) === 1 ? '通过' : Number(status) === 2 ? '驳回' : '待审'
}

function orderStatusText(status) {
  const map = ['待支付', '已支付', '拼团中', '待发货', '运输中', '待自提', '已完成', '已取消', '退款中', '已退货']
  return map[Number(status)] || status
}

function avatarText(value, fallback = '用') {
  return String(value || fallback).trim().slice(0, 1).toUpperCase()
}

function logout() {
  clearSession('ADMIN')
  router.push('/login')
}

async function loadAnalytics() {
  analyticsOverview.value = await api.adminAnalyticsOverview()
  await nextTick()
  initAdminCharts()
}

async function loadAdminSuggestions() {
  suggestionsLoading.value = true
  try {
    adminSuggestions.value = await api.adminAnalyticsSuggestions()
  } finally {
    suggestionsLoading.value = false
  }
}

function initAdminCharts() {
  disposeAdminCharts()
  if (activeTab.value !== 'dashboard') return

  const salesStats = analyticsOverview.value?.salesStats || []
  const userStats = analyticsOverview.value?.userStats || []

  // Chart 1: Product Sales Quantity
  const qtyBox = document.getElementById('admin-quantity-chart')
  if (qtyBox && salesStats.length) {
    const c1 = echarts.init(qtyBox)
    c1.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '8%', bottom: '10%', top: '8%', containLabel: true },
      xAxis: { type: 'category', data: salesStats.map(r => r.product_name), axisLabel: { rotate: 25, fontSize: 11 } },
      yAxis: { type: 'value', name: '销售量' },
      series: [{ type: 'bar', data: salesStats.map(r => r.total_quantity), itemStyle: { color: '#0f766e', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 50 }]
    })
    adminChartInstances.push(c1)
  }

  // Chart 2: Product Sales Amount
  const amtBox = document.getElementById('admin-amount-chart')
  if (amtBox && salesStats.length) {
    const c2 = echarts.init(amtBox)
    c2.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '8%', bottom: '10%', top: '8%', containLabel: true },
      xAxis: { type: 'category', data: salesStats.map(r => r.product_name), axisLabel: { rotate: 25, fontSize: 11 } },
      yAxis: { type: 'value', name: '销售额（元）' },
      series: [{ type: 'bar', data: salesStats.map(r => (Number(r.total_amount) || 0).toFixed(2)), itemStyle: { color: '#f97316', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 50 }]
    })
    adminChartInstances.push(c2)
  }

  // Chart 3: User Purchase Amount (horizontal bar)
  const userBox = document.getElementById('admin-user-chart')
  if (userBox && userStats.length) {
    const c3 = echarts.init(userBox)
    c3.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '2%', right: '8%', bottom: '6%', top: '6%', containLabel: true },
      yAxis: { type: 'category', data: userStats.map(r => r.username).reverse(), axisLabel: { fontSize: 12 } },
      xAxis: { type: 'value', name: '购买额（元）' },
      series: [{ type: 'bar', data: userStats.map(r => (Number(r.total_amount) || 0).toFixed(2)).reverse(), itemStyle: { color: '#2563eb', borderRadius: [0, 4, 4, 0] }, barMaxWidth: 32 }]
    })
    adminChartInstances.push(c3)
  }
}

function disposeAdminCharts() {
  adminChartInstances.forEach(c => c.dispose())
  adminChartInstances = []
}

async function updateFreeGroupCount(user, count) {
  await api.updateUserProfile(user.userId, {
    username: user.username,
    password: user.password,
    phone: user.phone || '',
    realName: user.realName || '',
    role: user.role,
    communityName: user.communityName || '',
    address: user.address || '',
    avatarUrl: user.avatarUrl || '',
    shopName: user.shopName || '',
    shopAddress: user.shopAddress || '',
    freeGroupCount: count,
    status: user.status ?? 1,
  })
  notify.success(`已更新 ${user.username} 的免拼次数为 ${count}`)
}

function startAdminTimer() {
  stopAdminTimer()
  adminTimer = setInterval(async () => {
    if (activeTab.value === 'dashboard') {
      await loadAnalytics()
      await loadAdminSuggestions()
    }
  }, 30000)
}

function stopAdminTimer() {
  if (adminTimer) { clearInterval(adminTimer); adminTimer = null }
}

onMounted(async () => {
  await loadAll()
  if (activeTab.value === 'dashboard') {
    await loadAnalytics()
    await loadAdminSuggestions()
  }
  startAdminTimer()
})

onUnmounted(() => {
  disposeAdminCharts()
  stopAdminTimer()
})

watch(activeTab, async (value) => {
  router.replace({ query: { ...route.query, tab: value } })
  if (value === 'dashboard') {
    await nextTick()
    if (!analyticsOverview.value) await loadAnalytics()
    else initAdminCharts()
    if (!adminSuggestions.value.length) await loadAdminSuggestions()
  } else {
    disposeAdminCharts()
  }
})

</script>

<template>
  <main class="shell" v-loading="loading">
    <header class="topbar admin-hero">
      <div class="topbar-identity">
        <el-avatar class="role-avatar admin-avatar" :size="58" :src="avatarUrl || undefined">管</el-avatar>
        <div><h1>管理员工作台</h1></div>
      </div>
      <div class="topbar-actions">
        <button type="button" class="dashboard-header-btn" @click="activeTab = 'dashboard'">数据大屏</button>
        <el-button type="primary" @click="openActivity()">创建拼团</el-button>
        <el-button @click="logout">退出</el-button>
      </div>
    </header>

    <section class="stat-grid">
      <div class="stat accent-stat"><span>用户总数</span><strong>{{ stats.users }}</strong></div>
      <div class="stat"><span>普通客户</span><strong>{{ stats.customers }}</strong></div>
      <div class="stat"><span>商家</span><strong>{{ stats.merchants }}</strong></div>
      <div class="stat"><span>商品</span><strong>{{ stats.products }}</strong></div>
      <div class="stat"><span>交易额</span><strong>¥{{ money(stats.amount) }}</strong></div>
    </section>

    <el-tabs v-model="activeTab" class="workspace admin-workspace stretch-tabs">
      <el-tab-pane label="数据大屏" name="dashboard">
        <div v-if="activeTab === 'dashboard'" class="dashboard-shell">
          <div class="dashboard-stats">
            <div class="dashboard-stat-card accent"><span class="stat-label">总交易额</span><span class="stat-value">¥{{ money(analyticsOverview?.overview?.total_revenue || 0) }}</span></div>
            <div class="dashboard-stat-card"><span class="stat-label">总订单数</span><span class="stat-value">{{ analyticsOverview?.overview?.order_count || 0 }}</span></div>
            <div class="dashboard-stat-card"><span class="stat-label">付费用户</span><span class="stat-value">{{ analyticsOverview?.overview?.user_count || 0 }} 人</span></div>
            <div class="dashboard-stat-card"><span class="stat-label">待处理退款</span><span class="stat-value">{{ stats.refunds }}</span></div>
          </div>

          <div class="dashboard-charts">
            <div class="dashboard-chart-panel">
              <h3>全平台商品销售量排行</h3>
              <div id="admin-quantity-chart" class="dashboard-chart-box"></div>
            </div>
            <div class="dashboard-chart-panel">
              <h3>全平台商品销售额排行</h3>
              <div id="admin-amount-chart" class="dashboard-chart-box"></div>
            </div>
          </div>

          <div class="dashboard-charts full">
            <div class="dashboard-chart-panel">
              <h3>用户购买额排行</h3>
              <div id="admin-user-chart" class="dashboard-chart-box tall"></div>
            </div>
          </div>

          <div class="dashboard-suggestions">
            <h3>智能运营建议</h3>
            <div v-if="suggestionsLoading" style="color: var(--muted)">正在分析平台数据...</div>
            <div v-else class="suggestion-list">
              <div v-for="(s, idx) in adminSuggestions" :key="idx" :class="['suggestion-item', `sug-${idx + 1}`]">
                <span class="sug-idx">{{ idx + 1 }}</span>
                <span>{{ s }}</span>
              </div>
            </div>
            <div class="dashboard-actions" style="margin-top: 16px">
              <el-button type="primary" @click="activeTab = 'notices'">发布促销通知</el-button>
              <el-button @click="openActivity()">创建拼团活动</el-button>
              <el-button @click="activeTab = 'refunds'">处理退款</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="总览" name="overview">
        <div class="admin-dashboard-grid">
          <section class="mini-panel">
            <h2>最近订单</h2>
            <el-table :data="orders.slice(0, 8)" empty-text="暂无订单">
              <el-table-column prop="orderNo" label="订单号" min-width="160" />
              <el-table-column prop="productName" label="商品" min-width="140" />
              <el-table-column label="客户" width="130"><template #default="{ row }"><div class="table-user"><el-avatar :size="26" :src="row.userAvatar || undefined">{{ avatarText(row.username) }}</el-avatar><span>{{ row.username }}</span></div></template></el-table-column>
              <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag>{{ orderStatusText(row.orderStatus) }}</el-tag></template></el-table-column>
              <el-table-column label="金额" width="110"><template #default="{ row }">¥{{ money(row.totalAmount) }}</template></el-table-column>
            </el-table>
          </section>
          <section class="mini-panel">
            <h2>商品审核</h2>
            <div class="audit-list audit-list-scroll">
              <article v-for="product in products.slice(0, 6)" :key="product.productId">
                <img :src="product.mainImage || '/favicon.svg'" :alt="product.productName" />
                <div><strong>{{ product.productName }}</strong><span>{{ product.merchantName }} · 库存 {{ product.stock }}</span></div>
                <el-tag :type="product.auditStatus === 1 ? 'success' : product.auditStatus === 2 ? 'danger' : 'warning'">{{ auditText(product.auditStatus) }}</el-tag>
              </article>
            </div>
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="用户管理" name="users">
        <div class="toolbar"><el-segmented v-model="userRoleFilter" :options="userRoleOptions" /></div>
        <el-table :data="filteredUsers" empty-text="暂无用户">
          <el-table-column label="用户" min-width="190"><template #default="{ row }"><div class="table-user"><el-avatar :size="32" :src="row.avatarUrl || undefined">{{ avatarText(row.realName || row.username) }}</el-avatar><div><strong>{{ row.username }}</strong><span>{{ row.realName }}</span></div></div></template></el-table-column>
          <el-table-column label="角色" width="100"><template #default="{ row }">{{ roleText(row.role) }}</template></el-table-column>
          <el-table-column prop="phone" label="电话" min-width="130" />
          <el-table-column prop="communityName" label="社区" min-width="120" />
          <el-table-column prop="shopName" label="店铺" min-width="130" />
          <el-table-column label="免拼次数" width="160"><template #default="{ row }"><el-input-number v-if="row.role === 'USER'" v-model="row.freeGroupCount" :min="0" :max="99" size="small" controls-position="right" @change="(val) => updateFreeGroupCount(row, val)" /><span v-else style="color: var(--muted)">-</span></template></el-table-column>
          <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="150"><template #default="{ row }"><el-button v-if="row.status === 1" size="small" type="danger" @click="setUserStatus(row, 0)">禁用</el-button><el-button v-else size="small" type="primary" @click="setUserStatus(row, 1)">启用</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="商品审核" name="products">
        <div class="toolbar"><el-button type="primary" @click="openActivity()">创建拼团活动</el-button><el-button @click="loadAll">刷新</el-button></div>
        <div class="table-scroll audit-table-scroll">
          <el-table :data="products" empty-text="暂无商品">
            <el-table-column label="商品" min-width="320"><template #default="{ row }"><div class="cart-product"><img :src="row.mainImage || '/favicon.svg'" :alt="row.productName" /><div><strong>{{ row.productName }}</strong><p>{{ row.description }}</p></div></div></template></el-table-column>
            <el-table-column label="商家" min-width="170"><template #default="{ row }"><div class="table-user"><el-avatar :size="26" :src="row.merchantAvatar || undefined">{{ avatarText(row.merchantName, '商') }}</el-avatar><span>{{ row.merchantName }}</span></div></template></el-table-column>
            <el-table-column label="价格" width="110"><template #default="{ row }">¥{{ money(row.singlePrice) }}</template></el-table-column>
            <el-table-column prop="stock" label="库存" width="80" />
            <el-table-column label="审核" width="100"><template #default="{ row }"><el-tag :type="row.auditStatus === 1 ? 'success' : row.auditStatus === 2 ? 'danger' : 'warning'">{{ auditText(row.auditStatus) }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="250"><template #default="{ row }"><el-button size="small" type="success" @click="audit(row, 1)">通过</el-button><el-button size="small" type="danger" @click="audit(row, 2)">驳回</el-button><el-button size="small" @click="openActivity(row)">拼团</el-button></template></el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="拼团活动" name="groups">
        <el-table :data="activities" empty-text="暂无拼团活动">
          <el-table-column prop="product_name" label="商品" min-width="150" />
          <el-table-column label="拼团价" width="100"><template #default="{ row }">¥{{ money(row.group_price) }}</template></el-table-column>
          <el-table-column prop="group_size" label="成团人数" width="100" />
          <el-table-column prop="start_time" label="开始时间" min-width="170" />
          <el-table-column prop="end_time" label="结束时间" min-width="170" />
          <el-table-column label="免拼" width="80"><template #default="{ row }">{{ row.allow_free_group === 1 ? '允许' : '不允许' }}</template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="订单管理" name="orders">
        <el-table :data="orders" empty-text="暂无订单">
          <el-table-column prop="orderNo" label="订单号" min-width="160" />
          <el-table-column prop="productName" label="商品" min-width="140" />
          <el-table-column prop="username" label="客户" width="100" />
          <el-table-column prop="merchantName" label="商家" min-width="120" />
          <el-table-column prop="pointName" label="自提点" min-width="120" />
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag>{{ orderStatusText(row.orderStatus) }}</el-tag></template></el-table-column>
          <el-table-column label="金额" width="110"><template #default="{ row }">¥{{ money(row.totalAmount) }}</template></el-table-column>
          <el-table-column prop="pickupCode" label="取件码" width="100" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="支付流水" name="payments">
        <el-table :data="payments" empty-text="暂无支付记录">
          <el-table-column prop="pay_no" label="流水号" min-width="180" />
          <el-table-column prop="order_no" label="订单号" min-width="160" />
          <el-table-column prop="username" label="客户" width="100" />
          <el-table-column prop="product_name" label="商品" min-width="140" />
          <el-table-column prop="pay_method" label="方式" width="110" />
          <el-table-column label="金额" width="110"><template #default="{ row }">¥{{ money(row.pay_amount) }}</template></el-table-column>
          <el-table-column prop="pay_time" label="支付时间" min-width="170" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="售后退款" name="refunds">
        <el-table :data="refunds" empty-text="暂无退款">
          <el-table-column prop="refund_id" label="ID" width="70" />
          <el-table-column prop="order_id" label="订单ID" width="90" />
          <el-table-column prop="refund_reason" label="原因" min-width="170" />
          <el-table-column label="金额" width="110"><template #default="{ row }">¥{{ money(row.refund_amount) }}</template></el-table-column>
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="Number(row.refund_status) === 0 ? 'warning' : Number(row.refund_status) === 3 ? 'success' : 'danger'">{{ Number(row.refund_status) === 0 ? '待审核' : Number(row.refund_status) === 3 ? '已退货' : '已拒绝' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="160"><template #default="{ row }"><el-button v-if="Number(row.refund_status) === 0" size="small" type="success" @click="handleRefund(row, 1)">通过</el-button><el-button v-if="Number(row.refund_status) === 0" size="small" type="danger" @click="handleRefund(row, 2)">拒绝</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="通知公告" name="notices">
        <el-form :model="noticeForm" label-position="top" class="form-grid notice-panel">
          <el-form-item label="接收用户ID"><el-input v-model="noticeForm.userId" placeholder="留空表示系统公告" /></el-form-item>
          <el-form-item label="通知类型"><el-select v-model="noticeForm.noticeType"><el-option label="系统公告" value="SYSTEM" /><el-option label="订单通知" value="ORDER" /><el-option label="拼团通知" value="GROUP" /><el-option label="取件通知" value="PICKUP" /><el-option label="退款通知" value="REFUND" /></el-select></el-form-item>
          <el-form-item label="标题" class="span-2"><el-input v-model="noticeForm.title" /></el-form-item>
          <el-form-item label="内容" class="span-2"><el-input v-model="noticeForm.content" type="textarea" :rows="5" /></el-form-item>
          <el-button type="primary" @click="publishNotice">发布通知</el-button>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="activityDialog.visible" title="拼团活动" width="680px">
      <el-form :model="activityForm" label-position="top" class="form-grid">
        <el-form-item label="商品"><el-select v-model="activityForm.productId"><el-option v-for="product in products" :key="product.productId" :label="product.productName" :value="product.productId" /></el-select></el-form-item>
        <el-form-item label="拼团价"><el-input-number v-model="activityForm.groupPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="成团人数"><el-input-number v-model="activityForm.groupSize" :min="2" /></el-form-item>
        <el-form-item label="免拼次数"><el-input-number v-model="activityForm.freeGroupLimit" :min="0" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="activityForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="activityForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="允许免拼"><el-switch v-model="activityForm.allowFreeGroup" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="activityDialog.visible = false">取消</el-button><el-button type="primary" @click="saveActivity">保存</el-button></template>
    </el-dialog>
  </main>
</template>
