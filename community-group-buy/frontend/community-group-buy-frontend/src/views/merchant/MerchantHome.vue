<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../../api/request'
import { notify } from '../../utils/notify'
import { clearSession } from '../../utils/session'
import * as echarts from 'echarts'

const router = useRouter()
const route = useRoute()
const merchantId = Number(localStorage.getItem('userId'))
const username = localStorage.getItem('username')
const savedAvatarUrl = localStorage.getItem('avatarUrl')
const avatarUrl = savedAvatarUrl && !savedAvatarUrl.includes('favicon.svg') ? savedAvatarUrl : ''
const loading = ref(false)
const activeTab = ref(typeof route.query.tab === 'string' ? route.query.tab : 'overview')
const products = ref([])
const categories = ref([])
const orders = ref([])
const analyticsData = ref(null)
const suggestions = ref([])
const suggestionsLoading = ref(false)
let analyticsTimer = null
let chartInstances = []

const productDialog = reactive({ visible: false, editingId: null })
const productForm = reactive({
  categoryId: null,
  productName: '',
  description: '',
  mainImage: '',
  detailImages: '',
  originalPrice: 0,
  groupPrice: 0,
  singlePrice: 0,
  stock: 0,
  status: 1,
})

const myProducts = computed(() => products.value.filter((item) => Number(item.merchantId) === merchantId))
const paidWaiting = computed(() => orders.value.filter((item) => item.payStatus === 1 && item.deliveryStatus === 0))
const stockTotal = computed(() => myProducts.value.reduce((sum, item) => sum + Number(item.stock || 0), 0))
const salesAmount = computed(() => orders.value.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0))

async function loadAll() {
  loading.value = true
  try {
    const [productData, categoryData, orderData] = await Promise.all([
      api.products(),
      api.categories(),
      api.merchantOrders(merchantId),
    ])
    products.value = productData
    categories.value = categoryData
    orders.value = orderData
  } finally {
    loading.value = false
  }
}

function resetProduct() {
  Object.assign(productForm, {
    categoryId: categories.value[0]?.category_id || categories.value[0]?.categoryId || null,
    productName: '',
    description: '',
    mainImage: '',
    detailImages: '',
    originalPrice: 0,
    groupPrice: 0,
    singlePrice: 0,
    stock: 0,
    status: 1,
  })
  productDialog.editingId = null
}

function openCreate() {
  resetProduct()
  productDialog.visible = true
}

function openEdit(product) {
  Object.assign(productForm, {
    categoryId: product.categoryId,
    productName: product.productName,
    description: product.description,
    mainImage: product.mainImage,
    detailImages: product.detailImages,
    originalPrice: product.originalPrice,
    groupPrice: product.groupPrice,
    singlePrice: product.singlePrice,
    stock: product.stock,
    status: product.status,
  })
  productDialog.editingId = product.productId
  productDialog.visible = true
}

async function saveProduct() {
  const payload = { ...productForm, merchantId }
  if (productDialog.editingId) await api.updateProduct(productDialog.editingId, payload)
  else await api.createProduct(payload)
  productDialog.visible = false
  notify.success('商品已保存')
  await loadAll()
}

async function removeProduct(product) {
  await api.deleteProduct(product.productId)
  notify.success('商品已删除')
  await loadAll()
}

async function deliver(order) {
  await api.deliverOrder(order.orderId)
  notify.success('订单已发货')
  await loadAll()
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function auditText(row) {
  return row.auditStatus === 1 ? '已通过' : row.auditStatus === 2 ? '已驳回' : '待审核'
}

function avatarText(value, fallback = '商') {
  return String(value || fallback).trim().slice(0, 1).toUpperCase()
}

function logout() {
  clearSession('MERCHANT')
  router.push('/login')
}

async function loadAnalytics() {
  analyticsData.value = await api.merchantAnalyticsSales(merchantId)
  await nextTick()
  initCharts()
}

async function loadSuggestions() {
  suggestionsLoading.value = true
  try {
    suggestions.value = await api.merchantAnalyticsSuggestions(merchantId)
  } finally {
    suggestionsLoading.value = false
  }
}

function initCharts() {
  disposeCharts()
  if (activeTab.value !== 'dashboard') return

  const salesStats = analyticsData.value?.salesStats || []

  // Chart 1: Sales Quantity
  const qtyBox = document.getElementById('merchant-quantity-chart')
  if (qtyBox && salesStats.length) {
    const c1 = echarts.init(qtyBox)
    c1.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '8%', bottom: '10%', top: '8%', containLabel: true },
      xAxis: { type: 'category', data: salesStats.map(r => r.product_name), axisLabel: { rotate: 25, fontSize: 11 } },
      yAxis: { type: 'value', name: '销售量' },
      series: [{ type: 'bar', data: salesStats.map(r => r.total_quantity), itemStyle: { color: '#0f766e', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 50 }]
    })
    chartInstances.push(c1)
  }

  // Chart 2: Sales Amount
  const amtBox = document.getElementById('merchant-amount-chart')
  if (amtBox && salesStats.length) {
    const c2 = echarts.init(amtBox)
    c2.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '8%', bottom: '10%', top: '8%', containLabel: true },
      xAxis: { type: 'category', data: salesStats.map(r => r.product_name), axisLabel: { rotate: 25, fontSize: 11 } },
      yAxis: { type: 'value', name: '销售额（元）' },
      series: [{ type: 'bar', data: salesStats.map(r => (Number(r.total_amount) || 0).toFixed(2)), itemStyle: { color: '#f97316', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 50 }]
    })
    chartInstances.push(c2)
  }
}

function disposeCharts() {
  chartInstances.forEach(c => c.dispose())
  chartInstances = []
}

function startAnalyticsTimer() {
  stopAnalyticsTimer()
  analyticsTimer = setInterval(async () => {
    if (activeTab.value === 'dashboard') {
      await loadAnalytics()
      await loadSuggestions()
    }
  }, 30000)
}

function stopAnalyticsTimer() {
  if (analyticsTimer) { clearInterval(analyticsTimer); analyticsTimer = null }
}

onMounted(async () => {
  await loadAll()
  if (activeTab.value === 'dashboard') {
    await loadAnalytics()
    await loadSuggestions()
  }
  startAnalyticsTimer()
})

onUnmounted(() => {
  disposeCharts()
  stopAnalyticsTimer()
})

watch(activeTab, async (value) => {
  router.replace({ query: { ...route.query, tab: value } })
  if (value === 'dashboard') {
    await nextTick()
    if (!analyticsData.value) await loadAnalytics()
    else initCharts()
    if (!suggestions.value.length) await loadSuggestions()
  } else {
    disposeCharts()
  }
})

</script>

<template>
  <main class="shell" v-loading="loading">
    <header class="topbar rich-topbar">
      <div class="topbar-identity">
        <el-avatar class="role-avatar merchant-avatar" :size="58" :src="avatarUrl || undefined">商</el-avatar>
        <div>
          <h1>商家工作台</h1>
        </div>
      </div>
      <div class="topbar-actions">
        <button type="button" class="dashboard-header-btn" @click="activeTab = 'dashboard'">数据大屏</button>
        <el-button type="primary" @click="openCreate">新增商品</el-button>
        <el-button @click="logout">退出</el-button>
      </div>
    </header>

    <section class="stat-grid">
      <div class="stat accent-stat"><span>我的商品</span><strong>{{ myProducts.length }}</strong></div>
      <div class="stat"><span>待发货</span><strong>{{ paidWaiting.length }}</strong></div>
      <div class="stat"><span>总库存</span><strong>{{ stockTotal }}</strong></div>
      <div class="stat"><span>订单数</span><strong>{{ orders.length }}</strong></div>
      <div class="stat"><span>订单金额</span><strong>¥{{ money(salesAmount) }}</strong></div>
    </section>

    <el-tabs v-model="activeTab" class="workspace stretch-tabs">
      <el-tab-pane label="数据大屏" name="dashboard">
        <div v-if="activeTab === 'dashboard'" class="dashboard-shell">
          <div class="dashboard-stats">
            <div class="dashboard-stat-card accent"><span class="stat-label">总销售额</span><span class="stat-value">¥{{ money(analyticsData?.totalRevenue || 0) }}</span></div>
            <div class="dashboard-stat-card"><span class="stat-label">总销量</span><span class="stat-value">{{ analyticsData?.totalQuantity || 0 }} 件</span></div>
            <div class="dashboard-stat-card"><span class="stat-label">商品种类</span><span class="stat-value">{{ analyticsData?.salesStats?.length || 0 }}</span></div>
            <div class="dashboard-stat-card"><span class="stat-label">库存总量</span><span class="stat-value">{{ stockTotal }}</span></div>
          </div>

          <div class="dashboard-charts">
            <div class="dashboard-chart-panel">
              <h3>商品销售量排行</h3>
              <div id="merchant-quantity-chart" class="dashboard-chart-box"></div>
            </div>
            <div class="dashboard-chart-panel">
              <h3>商品销售额排行</h3>
              <div id="merchant-amount-chart" class="dashboard-chart-box"></div>
            </div>
          </div>

          <div class="dashboard-suggestions">
            <h3>智能经营建议</h3>
            <div v-if="suggestionsLoading" style="color: var(--muted)">正在分析经营数据...</div>
            <div v-else class="suggestion-list">
              <div v-for="(s, idx) in suggestions" :key="idx" :class="['suggestion-item', `sug-${idx + 1}`]">
                <span class="sug-idx">{{ idx + 1 }}</span>
                <span>{{ s }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="经营概览" name="overview">
        <div class="merchant-overview">
          <section class="mini-panel">
            <h2>上架商品</h2>
            <div class="merchant-product-list">
              <article v-for="product in myProducts.slice(0, 6)" :key="product.productId" class="merchant-product-item">
                <img :src="product.mainImage || '/favicon.svg'" :alt="product.productName" />
                <div>
                  <strong>{{ product.productName }}</strong>
                  <span>{{ product.categoryName }} · 库存 {{ product.stock }}</span>
                  <em>¥{{ money(product.singlePrice) }} / 拼团 ¥{{ money(product.groupPrice) }}</em>
                </div>
              </article>
            </div>
          </section>
          <section class="mini-panel">
            <h2>待处理订单</h2>
            <el-table :data="paidWaiting.slice(0, 6)" empty-text="暂无待发货订单">
              <el-table-column prop="orderNo" label="订单号" min-width="150" />
              <el-table-column prop="productName" label="商品" min-width="130" />
              <el-table-column label="金额" width="100"><template #default="{ row }">¥{{ money(row.totalAmount) }}</template></el-table-column>
              <el-table-column label="操作" width="90"><template #default="{ row }"><el-button size="small" type="primary" @click="deliver(row)">发货</el-button></template></el-table-column>
            </el-table>
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="商品管理" name="products">
        <div class="toolbar">
          <el-button type="primary" @click="openCreate">新增商品</el-button>
          <el-button @click="loadAll">刷新</el-button>
        </div>
        <el-table :data="myProducts" empty-text="暂无商品">
          <el-table-column label="商品" min-width="260">
            <template #default="{ row }">
              <div class="cart-product"><img :src="row.mainImage || '/favicon.svg'" :alt="row.productName" /><div><strong>{{ row.productName }}</strong><p>{{ row.description }}</p></div></div>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="110" />
          <el-table-column label="单买价" width="100"><template #default="{ row }">¥{{ money(row.singlePrice) }}</template></el-table-column>
          <el-table-column label="拼团价" width="100"><template #default="{ row }">¥{{ money(row.groupPrice) }}</template></el-table-column>
          <el-table-column prop="stock" label="库存" width="80" />
          <el-table-column label="审核" width="100"><template #default="{ row }"><el-tag :type="row.auditStatus === 1 ? 'success' : row.auditStatus === 2 ? 'danger' : 'warning'">{{ auditText(row) }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="180"><template #default="{ row }"><el-button size="small" @click="openEdit(row)">编辑</el-button><el-button size="small" type="danger" @click="removeProduct(row)">删除</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="发货管理" name="orders">
        <el-table :data="orders" empty-text="暂无订单">
          <el-table-column prop="orderNo" label="订单号" min-width="160" />
          <el-table-column prop="productName" label="商品" min-width="140" />
          <el-table-column label="客户" width="130"><template #default="{ row }"><div class="table-user"><el-avatar :size="26" :src="row.userAvatar || undefined">{{ avatarText(row.username, '客') }}</el-avatar><span>{{ row.username }}</span></div></template></el-table-column>
          <el-table-column prop="pointName" label="自提点" min-width="130" />
          <el-table-column label="金额" width="100"><template #default="{ row }">¥{{ money(row.totalAmount) }}</template></el-table-column>
          <el-table-column label="支付" width="90"><template #default="{ row }"><el-tag :type="row.payStatus === 1 ? 'success' : 'info'">{{ row.payStatus === 1 ? '已支付' : '未支付' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="140"><template #default="{ row }"><el-button v-if="row.payStatus === 1 && row.deliveryStatus === 0" size="small" type="primary" @click="deliver(row)">发货</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="productDialog.visible" title="商品信息" width="720px">
      <el-form :model="productForm" label-position="top" class="form-grid">
        <el-form-item label="商品名称"><el-input v-model="productForm.productName" /></el-form-item>
        <el-form-item label="分类"><el-select v-model="productForm.categoryId"><el-option v-for="item in categories" :key="item.category_id || item.categoryId" :label="item.category_name || item.categoryName" :value="item.category_id || item.categoryId" /></el-select></el-form-item>
        <el-form-item label="原价"><el-input-number v-model="productForm.originalPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="单买价"><el-input-number v-model="productForm.singlePrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="拼团价"><el-input-number v-model="productForm.groupPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="productForm.stock" :min="0" /></el-form-item>
        <el-form-item label="主图地址" class="span-2"><el-input v-model="productForm.mainImage" /></el-form-item>
        <el-form-item label="商品描述" class="span-2"><el-input v-model="productForm.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="productDialog.visible = false">取消</el-button><el-button type="primary" @click="saveProduct">保存</el-button></template>
    </el-dialog>
  </main>
</template>
