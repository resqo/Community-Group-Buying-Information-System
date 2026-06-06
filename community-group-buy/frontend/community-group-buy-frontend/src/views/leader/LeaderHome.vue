<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../../api/request'
import { notify } from '../../utils/notify'
import { clearSession } from '../../utils/session'

const router = useRouter()
const route = useRoute()
const leaderId = Number(localStorage.getItem('userId'))
const username = localStorage.getItem('username')
const savedAvatarUrl = localStorage.getItem('avatarUrl')
const avatarUrl = savedAvatarUrl && !savedAvatarUrl.includes('favicon.svg') ? savedAvatarUrl : ''
const loading = ref(false)
const activeTab = ref(typeof route.query.tab === 'string' ? route.query.tab : 'dashboard')
const points = ref([])
const selectedPointId = ref(null)
const orders = ref([])
const verifyCode = ref('')

const pointForm = reactive({
  pickupPointId: null,
  leaderId,
  pointName: '',
  communityName: '',
  address: '',
  phone: '',
  businessHours: '09:00-20:00',
  status: 1,
})

const currentPoint = computed(() => points.value.find((item) => item.pickupPointId === selectedPointId.value))
const waitPickup = computed(() => orders.value.filter((item) => item.pickupStatus === 1))
const inTransit = computed(() => orders.value.filter((item) => item.orderStatus === 4))
const finished = computed(() => orders.value.filter((item) => item.pickupStatus === 2))

async function loadPoints() {
  points.value = await api.leaderPickupPoints(leaderId)
  if (!selectedPointId.value && points.value.length) selectedPointId.value = points.value[0].pickupPointId
  if (selectedPointId.value) await loadOrders()
}

async function loadOrders() {
  orders.value = selectedPointId.value ? await api.leaderOrders(selectedPointId.value) : []
}

function editPoint(point) {
  Object.assign(pointForm, point)
  activeTab.value = 'pickup'
}

function newPoint() {
  Object.assign(pointForm, {
    pickupPointId: null,
    leaderId,
    pointName: '',
    communityName: '',
    address: '',
    phone: '',
    businessHours: '09:00-20:00',
    status: 1,
  })
  activeTab.value = 'pickup'
}

async function savePoint() {
  const saved = await api.savePickupPoint(pointForm)
  notify.success('自提点已保存')
  selectedPointId.value = saved.pickupPointId
  await loadPoints()
}

async function verify() {
  if (!verifyCode.value) {
    notify.warning('请输入取件码')
    return
  }
  await api.verifyOrder(verifyCode.value)
  notify.success('核销成功')
  verifyCode.value = ''
  await loadOrders()
}

async function sendNotice(order) {
  await api.sendPickupNotice(order.orderId)
  notify.success('已确认到达自提点并发送取件通知')
  await loadOrders()
}

function avatarText(value, fallback = '客') {
  return String(value || fallback).trim().slice(0, 1).toUpperCase()
}

function logout() {
  clearSession('LEADER')
  router.push('/login')
}

onMounted(async () => {
  loading.value = true
  try {
    await loadPoints()
  } finally {
    loading.value = false
  }
})

watch(activeTab, (value) => {
  router.replace({ query: { ...route.query, tab: value } })
})
</script>

<template>
  <main class="shell" v-loading="loading">
    <header class="topbar rich-topbar">
      <div class="topbar-identity">
        <el-avatar class="role-avatar leader-avatar" :size="58" :src="avatarUrl || undefined">团</el-avatar>
        <div>
          <h1>团长工作台</h1>
        </div>
      </div>
      <div class="topbar-actions">
        <el-button type="primary" @click="newPoint">新增自提点</el-button>
        <el-button @click="logout">退出</el-button>
      </div>
    </header>

    <section class="stat-grid">
      <div class="stat accent-stat"><span>自提点</span><strong>{{ points.length }}</strong></div>
      <div class="stat"><span>运输中</span><strong>{{ inTransit.length }}</strong></div>
      <div class="stat"><span>待取货</span><strong>{{ waitPickup.length }}</strong></div>
      <div class="stat"><span>已完成</span><strong>{{ finished.length }}</strong></div>
      <div class="stat"><span>当前站点</span><strong>{{ currentPoint?.pointName || '-' }}</strong></div>
    </section>

    <el-tabs v-model="activeTab" class="workspace stretch-tabs">
      <el-tab-pane label="运营概览" name="dashboard">
        <div class="leader-dashboard">
          <section class="mini-panel">
            <h2>自提点信息</h2>
            <article v-for="point in points" :key="point.pickupPointId" class="point-card" :class="{ active: point.pickupPointId === selectedPointId }" @click="selectedPointId = point.pickupPointId; loadOrders()">
              <strong>{{ point.pointName }}</strong>
              <span>{{ point.communityName }} · {{ point.businessHours }}</span>
              <p>{{ point.address }}</p>
              <el-button size="small" @click.stop="editPoint(point)">编辑</el-button>
            </article>
          </section>
          <section class="mini-panel">
            <h2>待处理到货</h2>
            <el-table :data="[...inTransit, ...waitPickup].slice(0, 8)" empty-text="暂无待处理订单">
              <el-table-column prop="orderNo" label="订单号" min-width="150" />
              <el-table-column prop="productName" label="商品" min-width="140" />
              <el-table-column label="客户" width="130"><template #default="{ row }"><div class="table-user"><el-avatar :size="26" :src="row.userAvatar || undefined">{{ avatarText(row.username) }}</el-avatar><span>{{ row.username }}</span></div></template></el-table-column>
              <el-table-column prop="pickupCode" label="取件码" width="100" />
              <el-table-column label="操作" width="130"><template #default="{ row }"><el-button v-if="row.orderStatus === 4 || row.pickupStatus === 1" size="small" type="primary" @click="sendNotice(row)">{{ row.orderStatus === 4 ? '确认到货' : '重发通知' }}</el-button></template></el-table-column>
            </el-table>
          </section>
        </div>
      </el-tab-pane>

      <el-tab-pane label="自提点管理" name="pickup">
        <div class="toolbar">
          <el-select v-model="selectedPointId" placeholder="选择自提点" @change="loadOrders">
            <el-option v-for="point in points" :key="point.pickupPointId" :label="point.pointName" :value="point.pickupPointId" />
          </el-select>
          <el-button @click="newPoint">新建</el-button>
          <el-button v-if="currentPoint" @click="editPoint(currentPoint)">编辑当前</el-button>
        </div>
        <el-form :model="pointForm" label-position="top" class="form-grid">
          <el-form-item label="自提点名称"><el-input v-model="pointForm.pointName" /></el-form-item>
          <el-form-item label="所属社区"><el-input v-model="pointForm.communityName" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="pointForm.phone" /></el-form-item>
          <el-form-item label="营业时间"><el-input v-model="pointForm.businessHours" /></el-form-item>
          <el-form-item label="地址" class="span-2"><el-input v-model="pointForm.address" /></el-form-item>
          <el-button type="primary" @click="savePoint">保存自提点</el-button>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="到货与取货" name="orders">
        <el-table :data="orders" empty-text="暂无订单">
          <el-table-column prop="orderNo" label="订单号" min-width="160" />
          <el-table-column prop="productName" label="商品" min-width="140" />
          <el-table-column label="客户" width="130"><template #default="{ row }"><div class="table-user"><el-avatar :size="26" :src="row.userAvatar || undefined">{{ avatarText(row.username) }}</el-avatar><span>{{ row.username }}</span></div></template></el-table-column>
          <el-table-column prop="pickupCode" label="取件码" width="100" />
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.pickupStatus === 2 ? 'success' : row.pickupStatus === 1 ? 'warning' : 'info'">{{ row.pickupStatus === 2 ? '已取货' : row.pickupStatus === 1 ? '待取货' : row.orderStatus === 4 ? '运输中' : '未到货' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="130"><template #default="{ row }"><el-button v-if="row.orderStatus === 4" size="small" type="primary" @click="sendNotice(row)">确认到货</el-button><el-button v-else-if="row.pickupStatus === 1" size="small" type="primary" @click="sendNotice(row)">重发通知</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="订单核销" name="verify">
        <section class="verify-panel enhanced-verify">
          <el-input v-model="verifyCode" size="large" placeholder="输入用户取件码" />
          <el-button type="primary" size="large" @click="verify">核销订单</el-button>
        </section>
      </el-tab-pane>
    </el-tabs>
  </main>
</template>
