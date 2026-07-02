<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../../api/request'
import { notify } from '../../utils/notify'
import { clearSession } from '../../utils/session'

const router = useRouter()
const route = useRoute()
const userId = Number(localStorage.getItem('userId'))
const username = localStorage.getItem('username')
const savedAvatarUrl = localStorage.getItem('avatarUrl')
const avatarUrl = savedAvatarUrl && !savedAvatarUrl.includes('favicon.svg') ? savedAvatarUrl : ''
const cartKey = `cart:${userId}`

const loading = ref(false)
const products = ref([])
const categories = ref([])
const pickupPoints = ref([])
const orders = ref([])
const notices = ref([])
const activities = ref([])
const openGroups = ref([])
const myGroups = ref([])
const cart = ref([])
const userProfile = ref({})
const keyword = ref('')
const categoryId = ref()
const activeTab = ref(typeof route.query.tab === 'string' ? route.query.tab : 'market')
const hiddenGroupIds = ref([])
const hiddenGroupOrderIds = ref([])
const recommendations = ref([])
const recommendationsLoading = ref(false)

const checkout = reactive({
  visible: false,
  product: null,
  quantity: 1,
  pickupPointId: null,
  orderType: 'SINGLE',
  groupId: null,
})

const payment = reactive({
  visible: false,
  order: null,
  draft: null,
  method: '微信支付',
  password: '',
})

const refund = reactive({
  visible: false,
  order: null,
  reason: '',
  amount: 0,
})

const profileForm = reactive({
  phone: '',
  realName: '',
  communityName: '',
  address: '',
  avatarUrl: '',
})

const activityByProduct = computed(() => {
  const map = new Map()
  activities.value.forEach((item) => map.set(Number(item.product_id || item.productId), item))
  return map
})

const selectedCategoryName = computed(() => {
  if (!categoryId.value) return '全部好物'
  const item = categories.value.find((category) => Number(category.category_id || category.categoryId) === Number(categoryId.value))
  return item?.category_name || item?.categoryName || '分类好物'
})

const pendingPayOrders = computed(() => orders.value.filter((order) => order.payStatus === 0 && order.orderStatus !== 7))
const pickupOrders = computed(() => orders.value.filter((order) => order.pickupStatus === 1 || order.orderStatus === 5))
const displayOrders = computed(() => orders.value.filter((order) => order.orderStatus !== 7))
const cartTotal = computed(() => cart.value.reduce((sum, item) => sum + Number(item.singlePrice || 0) * Number(item.quantity || 1), 0))
const liveGroupCount = computed(() => openGroups.value.length)
const featuredProducts = computed(() => products.value.slice(0, 5))
const freeGroupCount = computed(() => userProfile.value.freeGroupCount ?? 0)
const rawGroupOrders = computed(() => orders.value.filter((order) => order.orderType === 'GROUP' || order.orderType === 'FREE_GROUP'))
const groupOrders = computed(() => rawGroupOrders.value.filter((order) => !hiddenGroupOrderIds.value.includes(Number(order.orderId))))
const visibleMyGroups = computed(() => myGroups.value.filter((group) => !hiddenGroupIds.value.includes(groupKey(group))))

function categoryKey(item) {
  return item.category_id || item.categoryId
}

async function selectCategory(id) {
  categoryId.value = id
  await loadAll()
}

function loadCart() {
  cart.value = JSON.parse(localStorage.getItem(cartKey) || '[]')
}

function saveCart() {
  localStorage.setItem(cartKey, JSON.stringify(cart.value))
}

function loadHiddenGroups() {
  hiddenGroupIds.value = JSON.parse(localStorage.getItem(`hiddenGroups:${userId}`) || '[]')
}

function saveHiddenGroups() {
  localStorage.setItem(`hiddenGroups:${userId}`, JSON.stringify(hiddenGroupIds.value))
}

async function loadAll() {
  loading.value = true
  try {
    const [catData, productData, pickupData, orderData, noticeData, activityData, groupData, myGroupData, profileData] = await Promise.all([
      api.categories(),
      api.products({ categoryId: categoryId.value, keyword: keyword.value }),
      api.pickupPoints(),
      api.myOrders(userId),
      api.notices(userId),
      api.activities(),
      api.openGroups(userId),
      api.myGroups(userId),
      api.userProfile(userId),
    ])
    categories.value = catData
    products.value = productData
    pickupPoints.value = pickupData
    orders.value = orderData
    notices.value = noticeData
    activities.value = activityData
    openGroups.value = groupData
    myGroups.value = myGroupData
    userProfile.value = profileData || {}
    Object.assign(profileForm, {
      phone: userProfile.value.phone || '',
      realName: userProfile.value.realName || '',
      communityName: userProfile.value.communityName || '',
      address: userProfile.value.address || '',
      avatarUrl: userProfile.value.avatarUrl || avatarUrl || '',
    })
    if (!checkout.pickupPointId && pickupData.length) checkout.pickupPointId = pickupData[0].pickupPointId
  } finally {
    loading.value = false
  }
}

async function loadRecommendations() {
  recommendationsLoading.value = true
  try {
    recommendations.value = await api.recommendations(userId)
  } finally {
    recommendationsLoading.value = false
  }
}

function openCheckout(product, orderType = 'SINGLE', groupId = null, quantity = 1) {
  if (orderType === 'FREE_GROUP' && freeGroupCount.value <= 0) {
    notify.warning('免拼次数不足')
    return
  }
  checkout.product = product
  checkout.quantity = quantity
  checkout.orderType = orderType
  checkout.groupId = groupId
  checkout.visible = true
}

function addCart(product) {
  const existing = cart.value.find((item) => Number(item.productId) === Number(product.productId))
  if (existing) {
    existing.quantity += 1
  } else {
    cart.value.push({
      productId: product.productId,
      productName: product.productName,
      mainImage: product.mainImage,
      singlePrice: product.singlePrice,
      stock: product.stock,
      quantity: 1,
    })
  }
  saveCart()
  notify.success('已加入购物车')
}

function removeCartItem(productId) {
  cart.value = cart.value.filter((item) => Number(item.productId) !== Number(productId))
  saveCart()
}

function updateCartQuantity() {
  saveCart()
}

async function startGroup(product) {
  const activity = activityOf(product)
  if (!activity) {
    notify.warning('该商品暂无拼团活动')
    return
  }
  const group = await api.startGroup(activity.activity_id || activity.activityId, userId)
  openCheckout(product, 'GROUP', group.groupId)
}

function isActivityItem(group) {
  return Number(group.is_activity || group.isActivity) === 1
}

async function startFromActivity(item) {
  const activityId = item.activity_id || item.activityId
  const group = await api.startGroup(activityId, userId)
  openCheckout({
    productId: item.product_id || item.productId,
    productName: item.product_name || item.productName,
    groupPrice: item.group_price || item.groupPrice,
  }, 'GROUP', group.groupId)
}

async function submitOrder() {
  if (!checkout.pickupPointId) {
    notify.warning('请先选择自提点')
    return
  }
  payment.order = null
  payment.draft = {
    payload: buildCheckoutPayload(),
    totalAmount: checkoutTotal(),
    orderType: checkout.orderType,
  }
  payment.method = '微信支付'
  payment.password = ''
  checkout.visible = false
  payment.visible = true
  activeTab.value = 'pending'
}

function openPayment(order) {
  payment.order = order
  payment.draft = null
  payment.method = '微信支付'
  payment.password = ''
  payment.visible = true
}

async function deferPayment() {
  if (payment.draft) {
    await api.createOrder(payment.draft.payload)
  }
  payment.visible = false
  payment.password = ''
  payment.order = null
  payment.draft = null
  await loadAll()
  activeTab.value = 'pending'
}

async function confirmPay() {
  if (!payment.password) {
    notify.warning('请输入支付密码')
    return
  }
  if (payment.password !== '123456') {
    notify.warning('支付密码错误')
    return
  }
  let order = payment.order
  if (payment.draft) {
    order = await api.createOrder(payment.draft.payload)
  }
  await api.payOrder(order.orderId, payment.method, payment.password)
  payment.visible = false
  payment.password = ''
  payment.order = null
  payment.draft = null
  notify.success(`${payment.method}成功`)
  await loadAll()
  activeTab.value = 'orders'
}

function openRefund(order) {
  refund.order = order
  refund.reason = '申请退款'
  refund.amount = order.totalAmount
  refund.visible = true
}

async function submitRefund() {
  await api.applyRefund({
    orderId: refund.order.orderId,
    userId,
    reason: refund.reason,
    amount: refund.amount,
  })
  refund.visible = false
  notify.success('退款申请已提交')
  await loadAll()
}

async function saveProfile() {
  await api.updateUserProfile(userId, {
    ...userProfile.value,
    ...profileForm,
    role: 'USER',
    status: userProfile.value.status ?? 1,
  })
  if (profileForm.avatarUrl) localStorage.setItem('avatarUrl', profileForm.avatarUrl)
  else localStorage.removeItem('avatarUrl')
  notify.success('个人信息已保存')
  await loadAll()
}

function logout() {
  clearSession('USER')
  router.push('/login')
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function avatarFor(seed) {
  return ''
}

function avatarText(value, fallback = '客') {
  return String(value || fallback).trim().slice(0, 1).toUpperCase()
}

function activityOf(product) {
  return activityByProduct.value.get(Number(product.productId))
}

function groupPrice(product) {
  const activityPrice = activityOf(product)?.group_price || activityOf(product)?.groupPrice
  return money(Number(activityPrice) > 0 ? activityPrice : product.groupPrice)
}

function canFreeGroup(product) {
  const activity = activityOf(product)
  const allow = activity?.allow_free_group ?? activity?.allowFreeGroup
  return Boolean(activity) && Number(allow) === 1 && freeGroupCount.value > 0
}

function checkoutTotal() {
  if (!checkout.product) return 0
  const unitPrice = checkout.orderType === 'SINGLE' ? checkout.product.singlePrice : groupPrice(checkout.product)
  return Number(unitPrice || 0) * Number(checkout.quantity || 1)
}

function buildCheckoutPayload() {
  return {
    userId,
    productId: checkout.product.productId,
    quantity: checkout.quantity,
    groupId: checkout.groupId,
    orderType: checkout.orderType,
    pickupPointId: checkout.pickupPointId,
  }
}

function paymentAmount() {
  return payment.order?.totalAmount ?? payment.draft?.totalAmount ?? 0
}

function groupAvatars(group) {
  const list = [group.leader_avatar || group.leaderAvatar, ...(group.participant_avatars || group.participantAvatars || '').split(',')]
    .filter(Boolean)
  return list.length ? list : [avatarFor(group.leader_username || group.leaderUsername || 'group')]
}

function groupNames(group) {
  return [group.leader_username || group.leaderUsername, ...(group.participant_names || group.participantNames || '').split(',')]
    .filter(Boolean)
    .join('、')
}

function statusText(order) {
  const map = ['待支付', '已支付', '拼团中', '待发货', '运输中', '待自提', '已完成', '已取消', '退款中', '已退货']
  return map[order.orderStatus] || `状态${order.orderStatus}`
}

function orderTypeText(type) {
  return { SINGLE: '单买', GROUP: '拼团', FREE_GROUP: '免拼' }[type] || type
}

function groupStatusText(group) {
  const status = Number(group.status)
  if (status === 1) return '已成团'
  if (status === 2) return '已取消'
  return `${group.current_count || group.currentCount || 0}/${group.required_count || group.requiredCount || 0} 等待成团`
}

function groupKey(group) {
  return Number(group.group_id || group.groupId)
}

function canHideGroup(group) {
  return Number(group.status) === 1 || Number(group.status) === 2
}

function hideGroup(group) {
  const key = groupKey(group)
  if (!hiddenGroupIds.value.includes(key)) hiddenGroupIds.value.push(key)
  saveHiddenGroups()
}

function canHideGroupOrder(order) {
  return Number(order.orderStatus) >= 3 || order.orderType === 'FREE_GROUP'
}

function hideGroupOrder(order) {
  const key = Number(order.orderId)
  if (!hiddenGroupOrderIds.value.includes(key)) hiddenGroupOrderIds.value.push(key)
  saveHiddenGroups()
}
function orderForGroup(group) {
  const key = groupKey(group)
  return rawGroupOrders.value.find((order) => Number(order.groupId || order.group_id) === key && order.orderType === 'GROUP')
}

function canFreeGroupInMyGroup(group) {
  const order = orderForGroup(group)
  if (order) return canFreeGroupOrder(order)
  const leaderId = Number(group.leader_user_id || group.leaderUserId)
  return Number(group.status) === 0 && leaderId === userId && freeGroupCount.value > 0
}

async function freeGroupFromGroup(group) {
  const order = orderForGroup(group)
  if (order) {
    await freeGroupExistingOrder(order)
    return
  }
  openCheckout({
    productId: group.product_id || group.productId,
    productName: group.product_name || group.productName,
    groupPrice: group.group_price || group.groupPrice,
    mainImage: group.main_image || group.mainImage,
  }, 'FREE_GROUP')
}
function canFreeGroupOrder(order) {
  return order.orderType === 'GROUP' && [0, 2].includes(Number(order.orderStatus)) && freeGroupCount.value > 0
}

async function freeGroupExistingOrder(order) {
  await api.freeGroupOrder(order.orderId, userId)
  notify.success('已转为免拼，商家可继续发货')
  await loadAll()
  activeTab.value = 'myGroups'
}
function groupRelationText(group) {
  return (group.relation_type || group.relationType) === 'STARTED' ? '我发起的拼团' : '我参与的拼团'
}

onMounted(async () => {
  loadCart()
  loadHiddenGroups()
  await loadAll()
})

watch(activeTab, (value) => {
  router.replace({ query: { ...route.query, tab: value } })
})
</script>

<template>
  <main class="shell user-market-shell" v-loading="loading">
    <header class="market-header">
      <div class="market-brand user-brand-with-avatar">
          <el-avatar :size="48" :src="profileForm.avatarUrl || avatarUrl || undefined">客</el-avatar>
        <div>
          <strong>邻里优选</strong>
          <span>社区团购服务</span>
        </div>
      </div>
      <div class="market-search">
        <el-input v-model="keyword" clearable placeholder="搜索水果、粮油、日用品" @keyup.enter="loadAll" />
        <el-button type="primary" @click="loadAll">搜索</el-button>
      </div>
      <div class="market-actions">
        <button type="button" @click="loadRecommendations(); activeTab = 'recommendations'"><span>猜你喜欢</span></button>
        <button type="button" @click="activeTab = 'pending'"><span>{{ pendingPayOrders.length }}</span>待支付</button>
        <button type="button" @click="activeTab = 'myGroups'"><span>{{ freeGroupCount }}</span>免拼次数</button>
        <button type="button" @click="activeTab = 'pickup'"><span>{{ pickupOrders.length }}</span>待自提</button>
        <button type="button" @click="activeTab = 'cart'"><span>{{ cart.length }}</span>购物车</button>
        <el-button @click="logout">退出</el-button>
      </div>
    </header>

    <el-tabs v-model="activeTab" class="workspace stretch-tabs">
      <el-tab-pane label="商品选购" name="market">
        <section class="commerce-layout">
          <aside class="category-rail">
            <button :class="{ active: !categoryId }" type="button" @click="selectCategory(undefined)">
              <span>全部</span><small>今日精选</small>
            </button>
            <button v-for="item in categories" :key="categoryKey(item)" :class="{ active: Number(categoryId) === Number(categoryKey(item)) }" type="button" @click="selectCategory(categoryKey(item))">
              <span>{{ item.category_name || item.categoryName }}</span><small>社区直达</small>
            </button>
          </aside>

          <div class="market-main">
            <section class="deal-hero">
              <div class="deal-copy">
                <span class="hero-kicker">{{ selectedCategoryName }}</span>
                <h1>本日优选清单</h1>
                <p>围绕社区家庭的一日三餐和日常补给精选商品，价格、库存和自提点集中展示，下单后由商家发货、团长协同到点自提。</p>
                <div class="hero-metrics">
                  <span>{{ products.length }} 件在售</span>
                  <span>{{ liveGroupCount }} 个团购</span>
                  <span>{{ pickupPoints.length }} 个自提点</span>
                </div>
              </div>
              <el-carousel class="today-carousel" height="260px" indicator-position="outside" arrow="always">
                <el-carousel-item v-for="product in featuredProducts" :key="product.productId">
                  <div class="today-slide">
                    <img :src="product.mainImage || '/favicon.svg'" :alt="product.productName" />
                    <div>
                      <span>{{ product.categoryName || selectedCategoryName }}</span>
                      <strong>{{ product.productName }}</strong>
                      <p>{{ product.merchantName || '社区商家' }} · ¥{{ money(product.singlePrice) }}</p>
                    </div>
                  </div>
                </el-carousel-item>
              </el-carousel>
            </section>

            <div class="market-strip">
              <strong>上架商品</strong>
              <span>每件商品都标明商家、图片、单价、库存和拼团价</span>
            </div>

            <div class="product-grid market-product-grid">
              <article v-for="product in products" :key="product.productId" class="product-card market-product-card">
                <div class="product-image-wrap">
                  <img :src="product.mainImage || '/favicon.svg'" :alt="product.productName" />
                  <span v-if="activityOf(product)" class="deal-badge">拼团</span>
                </div>
                <div class="product-body">
                  <div class="product-tags">
                    <span>{{ product.categoryName || selectedCategoryName }}</span>
                    <span>{{ product.merchantName || '社区商家' }}</span>
                  </div>
                  <h2>{{ product.productName }}</h2>
                  <p>{{ product.description || '社区优选商品' }}</p>
                  <div class="merchant-line">
                    <el-avatar :size="24" :src="product.merchantAvatar || undefined">{{ avatarText(product.merchantName, '商') }}</el-avatar>
                    <span>{{ product.merchantName || '社区商家' }}</span>
                  </div>
                  <div class="price-row">
                    <div>
                      <strong>¥{{ money(product.singlePrice) }}</strong>
                      <small v-if="Number(product.originalPrice) > Number(product.singlePrice)">¥{{ money(product.originalPrice) }}</small>
                    </div>
                    <em v-if="activityOf(product)">团购 ¥{{ groupPrice(product) }}</em>
                  </div>
                  <div class="stock-meter"><span :style="{ width: `${Math.min(100, Math.max(8, Number(product.salesCount || 0) + 18))}%` }"></span></div>
                  <div class="meta-row"><span>库存 {{ product.stock }}</span><span>销量 {{ product.salesCount }}</span></div>
                  <div class="action-row">
                    <el-button type="danger" @click="openCheckout(product, 'SINGLE')">立即购买</el-button>
                    <el-button @click="addCart(product)">加入购物车</el-button>
                    <el-button :disabled="!canFreeGroup(product)" @click="openCheckout(product, 'FREE_GROUP')">免拼</el-button>
                    <el-button type="primary" plain @click="startGroup(product)">开团</el-button>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </section>
        <el-empty v-if="!products.length" description="暂无匹配商品" />
      </el-tab-pane>

      <el-tab-pane label="猜你喜欢" name="recommendations">
        <div v-loading="recommendationsLoading">
          <el-empty v-if="!recommendations.length && !recommendationsLoading" description="点击上方「猜你喜欢」按钮获取推荐" />
          <div v-else class="product-grid market-product-grid">
            <article v-for="rec in recommendations" :key="rec.productId" class="product-card market-product-card">
              <div class="product-image-wrap">
                <img :src="rec.mainImage || '/favicon.svg'" :alt="rec.productName" />
                <span v-if="activityOf(rec)" class="deal-badge">拼团</span>
              </div>
              <div class="product-body">
                <div class="recommend-reason">基于您购买的「{{ rec.basedOnProductName }}」推荐</div>
                <div class="product-tags">
                  <span>{{ rec.categoryName || '分类' }}</span>
                  <span>{{ rec.merchantName || '社区商家' }}</span>
                </div>
                <h2>{{ rec.productName }}</h2>
                <p>{{ rec.description || '社区优选商品' }}</p>
                <div class="merchant-line">
                  <el-avatar :size="24" :src="rec.merchantAvatar || undefined">{{ avatarText(rec.merchantName, '商') }}</el-avatar>
                  <span>{{ rec.merchantName || '社区商家' }}</span>
                </div>
                <div class="price-row">
                  <div>
                    <strong>¥{{ money(rec.singlePrice) }}</strong>
                    <small v-if="Number(rec.originalPrice) > Number(rec.singlePrice)">¥{{ money(rec.originalPrice) }}</small>
                  </div>
                  <em v-if="activityOf(rec)">团购 ¥{{ groupPrice(rec) }}</em>
                </div>
                <div class="stock-meter"><span :style="{ width: `${Math.min(100, Math.max(8, Number(rec.salesCount || 0) + 18))}%` }"></span></div>
                <div class="meta-row"><span>库存 {{ rec.stock }}</span><span>销量 {{ rec.salesCount }}</span></div>
                <div class="action-row">
                  <el-button type="danger" @click="openCheckout(rec, 'SINGLE')">立即购买</el-button>
                  <el-button @click="addCart(rec)">加入购物车</el-button>
                  <el-button :disabled="!canFreeGroup(rec)" @click="openCheckout(rec, 'FREE_GROUP')">免拼</el-button>
                  <el-button type="primary" plain @click="startGroup(rec)">开团</el-button>
                </div>
              </div>
            </article>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="可参与拼团" name="groups">
        <div class="group-card-grid">
          <article v-for="group in openGroups" :key="group.group_id || group.groupId || group.activity_id || group.activityId" class="group-card">
            <img :src="group.main_image || group.mainImage || '/favicon.svg'" :alt="group.product_name || group.productName" />
            <div>
              <h3>{{ group.product_name || group.productName }}</h3>
              <p v-if="isActivityItem(group)">管理员发起，快来开团</p>
              <p v-else>{{ groupNames(group) || '等待邻居加入' }}</p>
              <div class="group-avatars" v-if="!isActivityItem(group)">
                <el-avatar v-for="avatar in groupAvatars(group).slice(0, 5)" :key="avatar" :size="34" :src="avatar || undefined">团</el-avatar>
              </div>
              <div class="group-progress">
                <span>{{ group.current_count || group.currentCount || 0 }} / {{ group.required_count || group.requiredCount }} 人</span>
                <strong>¥{{ money(group.group_price || group.groupPrice) }}</strong>
              </div>
              <el-progress :percentage="Math.round(((group.current_count || group.currentCount || 0) / (group.required_count || group.requiredCount || 1)) * 100)" />
              <el-button v-if="isActivityItem(group)" type="primary" @click="startFromActivity(group)">发起拼团</el-button>
              <el-button v-else type="primary" @click="openCheckout({ productId: group.product_id || group.productId, productName: group.product_name || group.productName, groupPrice: group.group_price || group.groupPrice }, 'GROUP', group.group_id || group.groupId)">参团下单</el-button>
            </div>
          </article>
        </div>
        <el-empty v-if="!openGroups.length" description="暂无可参与拼团" />
      </el-tab-pane>

      <el-tab-pane label="我的拼团" name="myGroups">
        <div class="section-head"><div><h2>我的拼团</h2><p>剩余免拼次数 {{ freeGroupCount }} 次</p></div><el-button @click="loadAll">刷新</el-button></div>
        <div class="group-card-grid">
          <article v-for="group in visibleMyGroups" :key="group.group_id || group.groupId" class="group-card">
            <img :src="group.main_image || group.mainImage || '/favicon.svg'" :alt="group.product_name || group.productName" />
            <div>
              <h3>{{ group.product_name || group.productName }}</h3>
              <p>{{ groupRelationText(group) }} · {{ groupStatusText(group) }}</p>
              <div class="group-progress">
                <span>{{ group.participant_names || group.participantNames || '等待邻居加入' }}</span>
                <strong>¥{{ money(group.group_price || group.groupPrice) }}</strong>
              </div>
              <el-progress :percentage="Math.round(((group.current_count || group.currentCount || 0) / (group.required_count || group.requiredCount || 1)) * 100)" />
              <div class="group-card-actions"><el-button v-if="canFreeGroupInMyGroup(group)" size="small" type="primary" @click="freeGroupFromGroup(group)">免拼发货</el-button><el-button v-if="canHideGroup(group)" size="small" @click="hideGroup(group)">移除显示</el-button></div>
            </div>
          </article>
        </div>
        <el-empty v-if="!visibleMyGroups.length" description="暂无发起或参与的拼团" />
        <el-table :data="groupOrders" empty-text="暂无拼团订单">
          <el-table-column prop="orderNo" label="订单号" min-width="160" />
          <el-table-column prop="productName" label="商品" min-width="140" />
          <el-table-column label="类型" width="90"><template #default="{ row }">{{ orderTypeText(row.orderType) }}</template></el-table-column>
          <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag>{{ statusText(row) }}</el-tag></template></el-table-column>
          <el-table-column label="金额" width="110"><template #default="{ row }">¥{{ money(row.totalAmount) }}</template></el-table-column>
          <el-table-column label="操作" width="210"><template #default="{ row }"><el-button v-if="row.payStatus === 0" size="small" type="primary" @click="openPayment(row)">支付</el-button><el-button v-if="canFreeGroupOrder(row)" size="small" @click="freeGroupExistingOrder(row)">免拼发货</el-button><el-button v-if="canHideGroupOrder(row)" size="small" @click="hideGroupOrder(row)">移除显示</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="购物车" name="cart">
        <div class="section-head"><div><h2>我的购物车</h2><p>合计 ¥{{ money(cartTotal) }}</p></div><el-button @click="loadCart">刷新</el-button></div>
        <el-table :data="cart" empty-text="购物车为空">
          <el-table-column label="商品" min-width="220"><template #default="{ row }"><div class="cart-product"><img :src="row.mainImage || '/favicon.svg'" :alt="row.productName" /><strong>{{ row.productName }}</strong></div></template></el-table-column>
          <el-table-column label="单价" width="100"><template #default="{ row }">¥{{ money(row.singlePrice) }}</template></el-table-column>
          <el-table-column label="数量" width="150"><template #default="{ row }"><el-input-number v-model="row.quantity" :min="1" :max="row.stock || 99" size="small" @change="updateCartQuantity" /></template></el-table-column>
          <el-table-column label="操作" width="190"><template #default="{ row }"><el-button size="small" type="primary" @click="openCheckout(row, 'SINGLE', null, row.quantity)">去结算</el-button><el-button size="small" @click="removeCartItem(row.productId)">移除</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="待支付" name="pending">
        <el-table :data="pendingPayOrders" empty-text="暂无待支付订单">
          <el-table-column prop="orderNo" label="订单号" min-width="160" />
          <el-table-column prop="productName" label="商品" min-width="140" />
          <el-table-column label="类型" width="90"><template #default="{ row }">{{ orderTypeText(row.orderType) }}</template></el-table-column>
          <el-table-column label="金额" width="110"><template #default="{ row }">¥{{ money(row.totalAmount) }}</template></el-table-column>
          <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag>{{ statusText(row) }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="120"><template #default="{ row }"><el-button size="small" type="primary" @click="openPayment(row)">去支付</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="待自提" name="pickup">
        <el-table :data="pickupOrders" empty-text="暂无待自提订单">
          <el-table-column prop="orderNo" label="订单号" min-width="160" />
          <el-table-column prop="productName" label="商品" min-width="140" />
          <el-table-column prop="pointName" label="自提点" min-width="140" />
          <el-table-column prop="pickupCode" label="取件码" width="120" />
          <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag type="warning">{{ statusText(row) }}</el-tag></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="我的订单" name="orders">
        <el-table :data="displayOrders" empty-text="暂无订单">
          <el-table-column prop="orderNo" label="订单号" min-width="160" />
          <el-table-column prop="productName" label="商品" min-width="140" />
          <el-table-column prop="quantity" label="数量" width="70" />
          <el-table-column label="类型" width="90"><template #default="{ row }">{{ orderTypeText(row.orderType) }}</template></el-table-column>
          <el-table-column label="金额" width="110"><template #default="{ row }">¥{{ money(row.totalAmount) }}</template></el-table-column>
          <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag>{{ statusText(row) }}</el-tag></template></el-table-column>
          <el-table-column prop="pointName" label="自提点" min-width="120" />
          <el-table-column label="操作" width="180"><template #default="{ row }"><el-button v-if="row.payStatus === 0" size="small" type="primary" @click="openPayment(row)">支付</el-button><el-button v-if="row.payStatus === 1 && row.orderStatus < 6" size="small" @click="openRefund(row)">退款</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="个人中心" name="profile">
        <section class="profile-layout">
          <div class="profile-card">
            <el-avatar :size="78" :src="profileForm.avatarUrl || undefined">客</el-avatar>
            <strong>个人信息</strong>
            <span>{{ userProfile.phone || '未填写手机号' }}</span>
            <span>{{ userProfile.communityName || '未填写社区' }}</span>
            <span>{{ userProfile.address || '未填写地址' }}</span>
          </div>
          <el-form label-position="top" class="form-grid">
            <el-form-item label="真实姓名"><el-input v-model="profileForm.realName" /></el-form-item>
            <el-form-item label="联系电话"><el-input v-model="profileForm.phone" /></el-form-item>
            <el-form-item label="所属社区"><el-input v-model="profileForm.communityName" /></el-form-item>
            <el-form-item label="头像地址"><el-input v-model="profileForm.avatarUrl" /></el-form-item>
            <el-form-item label="收货地址" class="span-2"><el-input v-model="profileForm.address" /></el-form-item>
            <el-button type="primary" @click="saveProfile">保存个人信息</el-button>
          </el-form>
        </section>
      </el-tab-pane>

      <el-tab-pane label="通知" name="notices">
        <el-timeline>
          <el-timeline-item v-for="item in notices" :key="item.notice_id" :timestamp="item.create_time">
            <strong>{{ item.title }}</strong><p>{{ item.content }}</p>
          </el-timeline-item>
        </el-timeline>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="checkout.visible" title="确认订单" width="520px">
      <el-form label-position="top">
        <el-form-item label="商品">{{ checkout.product?.productName }}</el-form-item>
        <el-form-item label="购买数量"><el-input-number v-model="checkout.quantity" :min="1" /></el-form-item>
        <el-form-item label="订单类型">{{ orderTypeText(checkout.orderType) }}</el-form-item>
        <el-form-item label="自提点">
          <el-select v-model="checkout.pickupPointId" placeholder="选择自提点">
            <el-option v-for="point in pickupPoints" :key="point.pickupPointId" :label="`${point.pointName} · ${point.address}`" :value="point.pickupPointId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="checkout.visible = false">取消</el-button><el-button type="primary" @click="submitOrder">确认订单并支付</el-button></template>
    </el-dialog>

    <el-dialog v-model="payment.visible" title="选择支付方式" width="520px">
      <section class="payment-panel">
        <div><span>订单金额</span><strong>¥{{ money(paymentAmount()) }}</strong></div>
        <el-radio-group v-model="payment.method" class="pay-methods">
          <el-radio-button label="微信支付" />
          <el-radio-button label="支付宝支付" />
          <el-radio-button label="银行卡支付" />
        </el-radio-group>
        <el-input
          v-model="payment.password"
          maxlength="6"
          show-password
          type="password"
          placeholder="请输入支付密码"
          @keyup.enter="confirmPay"
        />
      </section>
      <template #footer><el-button @click="deferPayment">稍后支付</el-button><el-button type="primary" @click="confirmPay">立即支付</el-button></template>
    </el-dialog>

    <el-dialog v-model="refund.visible" title="退款申请" width="520px">
      <el-form label-position="top">
        <el-form-item label="退款原因"><el-input v-model="refund.reason" type="textarea" /></el-form-item>
        <el-form-item label="退款金额"><el-input-number v-model="refund.amount" :min="0" :precision="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="refund.visible = false">取消</el-button><el-button type="primary" @click="submitRefund">提交</el-button></template>
    </el-dialog>
  </main>
</template>
