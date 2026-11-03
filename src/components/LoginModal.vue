<script setup>
import {
  ArrowLeft,
  ArrowRight,
  Eye,
  EyeOff,
  Clock3,
  LockKeyhole,
  Mail,
  MapPin,
  Phone,
  ShieldCheck,
  Store,
  UserPlus,
  UserRound,
  X,
} from 'lucide-vue-next'
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addToCart,
  demoAccounts,
  login,
  logout,
  register as registerAccount,
  registerMerchant,
  roleHomePath,
  session,
} from '../store'

const router = useRouter()
const route = useRoute()
const emit = defineEmits(['close'])
const showPassword = ref(false)
const mode = ref('login')
const submitting = ref(false)
const form = reactive({
  email: '',
  password: '',
})
const registerForm = reactive({
  displayName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
})
const merchantForm = reactive({
  shopName: '',
  ownerName: '',
  email: '',
  phone: '',
  city: '',
  description: '',
  password: '',
  confirmPassword: '',
})
const errorMessage = ref('')
const merchantSuccessMessage = ref('')

const roleMeta = {
  user: { label: '消费者', icon: UserRound },
  merchant: { label: '商家', icon: Store },
  admin: { label: '超级管理员', icon: ShieldCheck },
}

const currentRole = computed(() => roleMeta[session.role] ?? null)
const authPanelCopy = computed(() => {
  if (mode.value === 'merchant') {
    return {
      kicker: 'BECOME A MERCHANT',
      title: '成为商家',
      description: '创建店铺与商家账号，进入经营后台。',
    }
  }
  if (mode.value === 'register') {
    return {
      kicker: 'CREATE ACCOUNT',
      title: '创建账户',
      description: '注册消费者账户，保存购物车和订单记录。',
    }
  }
  return {
    kicker: 'ACCOUNT LOGIN',
    title: '账号登录',
    description: '系统会根据账号自动进入对应身份。',
  }
})

function useAccount(account) {
  form.email = account.email
  form.password = account.password
  errorMessage.value = ''
}

async function submitLogin() {
  errorMessage.value = ''
  submitting.value = true
  const result = await login(form.email, form.password)
  submitting.value = false
  if (!result.ok) {
    errorMessage.value = result.message
    return
  }
  await completeAuth(result.path)
}

async function submitRegister() {
  errorMessage.value = ''
  if (registerForm.password !== registerForm.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }
  submitting.value = true
  const result = await registerAccount(registerForm)
  submitting.value = false
  if (!result.ok) {
    errorMessage.value = result.message
    return
  }
  await completeAuth(result.path)
}

async function submitMerchantRegister() {
  errorMessage.value = ''
  if (merchantForm.password !== merchantForm.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }
  submitting.value = true
  const result = await registerMerchant(merchantForm)
  submitting.value = false
  if (!result.ok) {
    errorMessage.value = result.message
    return
  }
  merchantSuccessMessage.value = result.message
}

async function completeAuth(fallbackPath) {
  const redirect =
    typeof route.query.redirect === 'string' &&
    route.query.redirect.startsWith('/') &&
    route.query.redirect !== '/login'
      ? route.query.redirect
      : fallbackPath
  const action = String(route.query.action || '')
  const productId = String(route.query.productId || '')
  const quantity = Math.max(1, Number(route.query.quantity) || 1)
  let destination = redirect
  if (session.role === 'user' && productId && ['add', 'buy'].includes(action)) {
    const added = await addToCart(productId, quantity)
    if (added && action === 'buy') destination = '/cart'
  }
  await router.replace(destination)
}

function switchMode(nextMode) {
  mode.value = nextMode
  errorMessage.value = ''
  showPassword.value = false
  merchantSuccessMessage.value = ''
}

function continueCurrent() {
  router.push(roleHomePath(session.role))
}

async function signOut() {
  await logout()
  form.password = ''
  form.email = ''
  errorMessage.value = ''
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop login-modal-backdrop" @click.self="emit('close')">
      <section
        class="login-panel login-panel--modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="login-modal-title"
      >
        <button
          class="login-modal__close"
          type="button"
          aria-label="关闭登录窗口"
          @click="emit('close')"
        >
          <X :size="18" />
        </button>
      <div class="login-panel__head">
        <span class="section-kicker">{{ authPanelCopy.kicker }}</span>
        <h2 id="login-modal-title">{{ authPanelCopy.title }}</h2>
        <p>{{ authPanelCopy.description }}</p>
      </div>

      <div v-if="mode === 'login' && session.loggedIn && currentRole" class="current-account">
        <span>
          <component :is="currentRole.icon" :size="18" />
        </span>
        <div>
          <small>当前已登录为{{ currentRole.label }}</small>
          <strong>{{ session.accountName }}</strong>
        </div>
        <button type="button" @click="continueCurrent">
          返回
          <ArrowRight :size="14" />
        </button>
      </div>

      <div
        v-if="mode !== 'login'"
        class="registration-type-switch"
        role="tablist"
        aria-label="账户类型"
      >
        <button
          type="button"
          role="tab"
          :aria-selected="mode === 'register'"
          :class="{ active: mode === 'register' }"
          @click="switchMode('register')"
        >
          <UserRound :size="16" />
          消费者账户
        </button>
        <button
          type="button"
          role="tab"
          :aria-selected="mode === 'merchant'"
          :class="{ active: mode === 'merchant' }"
          @click="switchMode('merchant')"
        >
          <Store :size="16" />
          商家账户
        </button>
      </div>

      <form v-if="mode === 'login'" class="login-form" @submit.prevent="submitLogin">
        <label>
          <span>账号邮箱</span>
          <div class="login-input">
            <Mail :size="17" />
            <input
              v-model.trim="form.email"
              type="email"
              autocomplete="username"
              placeholder="请输入登录邮箱"
              required
            />
          </div>
        </label>
        <label>
          <span>登录密码</span>
          <div class="login-input">
            <LockKeyhole :size="17" />
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="请输入密码"
              required
            />
            <button
              type="button"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword"
            >
              <EyeOff v-if="showPassword" :size="17" />
              <Eye v-else :size="17" />
            </button>
          </div>
        </label>

        <p v-if="errorMessage" class="login-error" role="alert">
          {{ errorMessage }}
        </p>

        <button class="button button--dark login-submit" type="submit" :disabled="submitting">
          {{ submitting ? '正在登录...' : '登录系统' }}
          <ArrowRight :size="18" />
        </button>
      </form>

      <form
        v-else-if="mode === 'register'"
        class="login-form register-form"
        @submit.prevent="submitRegister"
      >
        <label>
          <span>姓名</span>
          <div class="login-input">
            <UserRound :size="17" />
            <input
              v-model.trim="registerForm.displayName"
              type="text"
              autocomplete="name"
              maxlength="50"
              placeholder="请输入姓名"
              required
            />
          </div>
        </label>
        <label>
          <span>账号邮箱</span>
          <div class="login-input">
            <Mail :size="17" />
            <input
              v-model.trim="registerForm.email"
              type="email"
              autocomplete="email"
              maxlength="120"
              placeholder="用于登录和找回账户"
              required
            />
          </div>
        </label>
        <label>
          <span>手机号</span>
          <div class="login-input">
            <Phone :size="17" />
            <input
              v-model.trim="registerForm.phone"
              type="tel"
              autocomplete="tel"
              maxlength="30"
              placeholder="请输入手机号"
              required
            />
          </div>
        </label>
        <label>
          <span>设置密码</span>
          <div class="login-input">
            <LockKeyhole :size="17" />
            <input
              v-model="registerForm.password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              minlength="6"
              maxlength="72"
              placeholder="至少 6 个字符"
              required
            />
            <button
              type="button"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword"
            >
              <EyeOff v-if="showPassword" :size="17" />
              <Eye v-else :size="17" />
            </button>
          </div>
        </label>
        <label>
          <span>确认密码</span>
          <div class="login-input">
            <LockKeyhole :size="17" />
            <input
              v-model="registerForm.confirmPassword"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              minlength="6"
              maxlength="72"
              placeholder="再次输入密码"
              required
            />
          </div>
        </label>

        <p v-if="errorMessage" class="login-error" role="alert">
          {{ errorMessage }}
        </p>

        <button class="button button--dark login-submit" type="submit" :disabled="submitting">
          {{ submitting ? '正在创建...' : '创建账户' }}
          <UserPlus :size="18" />
        </button>
      </form>

      <form
        v-else-if="mode === 'merchant' && !merchantSuccessMessage"
        class="login-form register-form merchant-register-form"
        @submit.prevent="submitMerchantRegister"
      >
        <label>
          <span>店铺名称</span>
          <div class="login-input">
            <Store :size="17" />
            <input
              v-model.trim="merchantForm.shopName"
              type="text"
              maxlength="100"
              placeholder="请输入店铺名称"
              required
            />
          </div>
        </label>
        <label>
          <span>负责人姓名</span>
          <div class="login-input">
            <UserRound :size="17" />
            <input
              v-model.trim="merchantForm.ownerName"
              type="text"
              autocomplete="name"
              maxlength="50"
              placeholder="请输入负责人姓名"
              required
            />
          </div>
        </label>
        <label>
          <span>商家邮箱</span>
          <div class="login-input">
            <Mail :size="17" />
            <input
              v-model.trim="merchantForm.email"
              type="email"
              autocomplete="email"
              maxlength="120"
              placeholder="用于登录商家后台"
              required
            />
          </div>
        </label>
        <label>
          <span>联系电话</span>
          <div class="login-input">
            <Phone :size="17" />
            <input
              v-model.trim="merchantForm.phone"
              type="tel"
              autocomplete="tel"
              maxlength="30"
              placeholder="请输入联系电话"
              required
            />
          </div>
        </label>
        <label>
          <span>所在城市</span>
          <div class="login-input">
            <MapPin :size="17" />
            <input
              v-model.trim="merchantForm.city"
              type="text"
              maxlength="50"
              placeholder="请输入所在城市"
              required
            />
          </div>
        </label>
        <label>
          <span>店铺简介</span>
          <textarea
            v-model.trim="merchantForm.description"
            class="login-textarea"
            maxlength="500"
            rows="3"
            placeholder="介绍一下你的店铺和主营商品"
          />
        </label>
        <label>
          <span>设置密码</span>
          <div class="login-input">
            <LockKeyhole :size="17" />
            <input
              v-model="merchantForm.password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              minlength="6"
              maxlength="72"
              placeholder="至少 6 个字符"
              required
            />
            <button
              type="button"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword"
            >
              <EyeOff v-if="showPassword" :size="17" />
              <Eye v-else :size="17" />
            </button>
          </div>
        </label>
        <label>
          <span>确认密码</span>
          <div class="login-input">
            <LockKeyhole :size="17" />
            <input
              v-model="merchantForm.confirmPassword"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              minlength="6"
              maxlength="72"
              placeholder="再次输入密码"
              required
            />
          </div>
        </label>

        <p v-if="errorMessage" class="login-error" role="alert">
          {{ errorMessage }}
        </p>

        <button class="button button--dark login-submit" type="submit" :disabled="submitting">
          {{ submitting ? '正在创建店铺...' : '提交商家入驻' }}
          <Store :size="18" />
        </button>
      </form>

      <div v-else-if="mode === 'merchant'" class="merchant-review-pending">
        <span><Clock3 :size="25" /></span>
        <h3>入驻申请已提交</h3>
        <p>{{ merchantSuccessMessage }}</p>
        <button class="button button--outline" type="button" @click="switchMode('login')">
          返回登录
        </button>
      </div>

      <div class="login-mode-switch">
        <span>{{ mode === 'login' ? '还没有账号？' : '已经有账号？' }}</span>
        <button
          type="button"
          @click="switchMode(mode === 'login' ? 'register' : 'login')"
        >
          {{ mode === 'login' ? '创建账户' : '返回登录' }}
          <ArrowRight :size="14" />
        </button>
      </div>

      <div v-if="mode === 'login'" class="demo-account-section">
        <div class="demo-account-heading">
          <span>演示账号</span>
          <small>点击账号自动填入</small>
        </div>
        <div class="demo-account-list">
          <button
            v-for="account in demoAccounts"
            :key="account.email"
            :class="`demo-account demo-account--${account.role}`"
            type="button"
            @click="useAccount(account)"
          >
            <span>
              <UserRound v-if="account.role === 'user'" :size="18" />
              <Store v-else-if="account.role === 'merchant'" :size="18" />
              <ShieldCheck v-else :size="18" />
            </span>
            <span>
              <strong>{{ account.label }}</strong>
              <small>{{ account.email }}</small>
            </span>
            <ArrowRight :size="15" />
          </button>
        </div>
        <p>统一演示密码：<strong>123456</strong></p>
      </div>

      <button
        v-if="mode === 'login' && session.loggedIn"
        class="login-signout"
        type="button"
        @click="signOut"
      >
        <ArrowLeft :size="14" />
        退出当前账号
      </button>
      </section>
    </div>
  </Teleport>
</template>
