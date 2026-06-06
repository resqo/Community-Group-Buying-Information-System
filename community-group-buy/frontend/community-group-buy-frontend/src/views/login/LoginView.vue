<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../../api/request'
import { notify } from '../../utils/notify'
import { saveSession } from '../../utils/session'

const router = useRouter()
const active = ref('login')
const loading = ref(false)
const serviceStatus = reactive({
  checking: false,
  backend: 'UNKNOWN',
  database: 'UNKNOWN',
  message: '正在检测服务状态',
})

const loginForm = reactive({
  username: 'user_1',
  password: '123456',
  role: 'USER',
})

const registerForm = reactive({
  username: '',
  password: '',
  phone: '',
  realName: '',
  role: 'USER',
  communityName: '',
  address: '',
  avatarUrl: '',
  shopName: '',
  shopAddress: '',
})

const roleOptions = [
  { label: '客户', value: 'USER' },
  { label: '团长', value: 'LEADER' },
  { label: '商家', value: 'MERCHANT' },
  { label: '管理员', value: 'ADMIN' },
]

const roleHints = {
  USER: '发现附近好物，拼团下单，到点自提。',
  LEADER: '管理自提点、到货通知和取件核销。',
  MERCHANT: '维护商品，上架图片、价格和库存。',
  ADMIN: '审核商品、管理用户和平台运营数据。',
}

const loginAvatar = computed(() => avatarFor(loginForm.username || loginForm.role))
const loginAvatarText = computed(() => ({ USER: '客', LEADER: '团', MERCHANT: '商', ADMIN: '管' }[loginForm.role] || '邻'))

function avatarFor(seed) {
  return ''
}

const homeByRole = {
  USER: '/user/home',
  LEADER: '/leader/dashboard',
  MERCHANT: '/merchant/products',
  ADMIN: '/admin/dashboard',
}

function routeRole(path) {
  if (path.startsWith('/user/')) return 'USER'
  if (path.startsWith('/leader/')) return 'LEADER'
  if (path.startsWith('/merchant/')) return 'MERCHANT'
  if (path.startsWith('/admin/')) return 'ADMIN'
  return null
}

async function checkService() {
  serviceStatus.checking = true
  try {
    const data = await api.health()
    serviceStatus.backend = data.backend
    serviceStatus.database = data.database
    serviceStatus.message = `后端 ${data.backend} · 数据库 ${data.database}`
  } catch (error) {
    serviceStatus.backend = 'DOWN'
    serviceStatus.database = 'UNKNOWN'
    serviceStatus.message = '后端服务未连接，请确认 8080 服务已启动'
  } finally {
    serviceStatus.checking = false
  }
}

async function login() {
  loading.value = true
  try {
    const data = await api.login(loginForm)
    saveSession(data)
    notify.success('登录成功')
    const redirect = router.currentRoute.value.query.redirect
    const target = typeof redirect === 'string' && routeRole(redirect) === data.role
      ? redirect
      : homeByRole[data.role] || data.homePath
    router.push(target)
  } finally {
    loading.value = false
  }
}

async function register() {
  loading.value = true
  try {
    await api.register(registerForm)
    notify.success('注册成功，请登录')
    loginForm.username = registerForm.username
    loginForm.password = registerForm.password
    loginForm.role = registerForm.role
    active.value = 'login'
  } finally {
    loading.value = false
  }
}

onMounted(checkService)
</script>

<template>
  <main class="qq-auth-page">
    <section class="qq-auth-window">
      <aside class="qq-brand-pane">
        <div class="qq-brand-top">
          <span class="brand-dot"></span>
          <strong>邻里优选</strong>
        </div>
        <div class="qq-brand-copy">
          <h1>社区团购管理信息系统</h1>
          <p>客户下单、商家发货、团长核销、管理员运营，一套流程串起来。</p>
        </div>
        <div class="qq-brand-preview">
          <div class="brand-preview-stack">
            <span>果蔬</span>
            <span>肉蛋奶</span>
            <span>粮油百货</span>
          </div>
          <div>
            <strong>今日社区专场</strong>
            <span>果蔬、蛋奶、粮油与日用百货</span>
          </div>
        </div>
      </aside>

      <section class="qq-card">
        <div class="qq-avatar-wrap">
          <el-avatar :size="86" :src="loginAvatar">{{ loginAvatarText }}</el-avatar>
          <div>
            <h2>{{ active === 'login' ? '账号登录' : '创建账号' }}</h2>
            <p>{{ roleHints[active === 'login' ? loginForm.role : registerForm.role] }}</p>
          </div>
        </div>

        <div class="service-status" :class="{ down: serviceStatus.backend !== 'UP' }">
          <el-tag :type="serviceStatus.backend === 'UP' ? 'success' : 'danger'">
            {{ serviceStatus.backend === 'UP' ? '服务在线' : '服务离线' }}
          </el-tag>
          <span>{{ serviceStatus.message }}</span>
          <el-button link :loading="serviceStatus.checking" @click="checkService">重试</el-button>
        </div>

        <el-tabs v-model="active" class="auth-tabs stretch-tabs">
          <el-tab-pane label="登录" name="login">
            <el-form label-position="top" :model="loginForm" class="auth-form">
              <el-form-item label="用户名">
                <el-input v-model="loginForm.username" size="large" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="loginForm.password" size="large" type="password" show-password placeholder="请输入密码" />
              </el-form-item>
              <el-form-item label="登录角色">
                <el-segmented v-model="loginForm.role" :options="roleOptions" />
              </el-form-item>
              <el-button type="primary" size="large" :loading="loading" @click="login">登录系统</el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form label-position="top" :model="registerForm" class="auth-form two-col">
              <el-form-item label="用户名">
                <el-input v-model="registerForm.username" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="registerForm.password" type="password" show-password />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="registerForm.phone" />
              </el-form-item>
              <el-form-item label="真实姓名">
                <el-input v-model="registerForm.realName" />
              </el-form-item>
              <el-form-item label="角色">
                <el-select v-model="registerForm.role">
                  <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="所属社区">
                <el-input v-model="registerForm.communityName" />
              </el-form-item>
              <el-form-item label="头像地址" class="span-2">
                <el-input v-model="registerForm.avatarUrl" :placeholder="avatarFor(registerForm.username || registerForm.role)" />
              </el-form-item>
              <el-form-item label="住址" class="span-2">
                <el-input v-model="registerForm.address" />
              </el-form-item>
              <el-form-item label="店铺名称">
                <el-input v-model="registerForm.shopName" :disabled="registerForm.role !== 'MERCHANT'" />
              </el-form-item>
              <el-form-item label="店铺地址">
                <el-input v-model="registerForm.shopAddress" :disabled="registerForm.role !== 'MERCHANT'" />
              </el-form-item>
              <el-button type="primary" size="large" :loading="loading" @click="register">创建账号</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </section>
    </section>
  </main>
</template>
