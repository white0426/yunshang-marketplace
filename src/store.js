import { computed, reactive } from 'vue'

const apiHost =
  typeof window !== 'undefined' &&
  !['127.0.0.1', 'localhost'].includes(window.location.hostname)
    ? window.location.hostname
    : '127.0.0.1'
const API_BASE = import.meta.env.VITE_API_BASE || `http://${apiHost}:8080/api`
const TOKEN_KEY = 'yunshang_market_jwt'
const DEMO_PASSWORD = '123456'

const emptySession = () => ({
  loggedIn: false,
  role: 'guest',
  userId: '',
  merchantId: '',
  accountName: '',
  email: '',
  token: localStorage.getItem(TOKEN_KEY) || '',
})

export const session = reactive(emptySession())

export const state = reactive({
  merchants: [],
  users: [],
  products: [],
  cart: [],
  addresses: [],
  reviews: [],
  afterSales: [],
  operationLogs: [],
  orders: [],
})

const toastState = reactive({
  visible: false,
  message: '',
  tone: 'success',
})

let toastTimer
let initializing = null
let storefrontLoaded = false
let storefrontInitializing = null

export function showToast(message, tone = 'success') {
  clearTimeout(toastTimer)
  toastState.message = message
  toastState.tone = tone
  toastState.visible = true
  toastTimer = window.setTimeout(() => {
    toastState.visible = false
  }, 2800)
}

export function useToast() {
  return toastState
}

async function apiRequest(path, options = {}, withAuth = true) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  if (withAuth && session.token) {
    headers.Authorization = `Bearer ${session.token}`
  }

  let response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
    })
  } catch {
    throw new Error('无法连接后端服务，请确认 Spring Boot 已启动')
  }

  let data = null
  if (response.status !== 204) {
    try {
      data = await response.json()
    } catch {
      data = null
    }
  }

  if (!response.ok) {
    if (response.status === 401 && path !== '/auth/login') {
      clearSession()
    }
    throw new Error(data?.message || `请求失败（${response.status}）`)
  }
  return data
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  Object.assign(session, {
    loggedIn: false,
    role: 'guest',
    userId: '',
    merchantId: '',
    accountName: '',
    email: '',
    token: '',
  })
  clearState()
}

function clearState() {
  storefrontLoaded = false
  state.merchants.splice(0)
  state.users.splice(0)
  state.products.splice(0)
  state.cart.splice(0)
  state.addresses.splice(0)
  state.reviews.splice(0)
  state.afterSales.splice(0)
  state.operationLogs.splice(0)
  state.orders.splice(0)
}

function hydrateState(data) {
  storefrontLoaded = true
  state.merchants.splice(0, state.merchants.length, ...(data.merchants || []))
  state.users.splice(0, state.users.length, ...(data.users || []))
  state.products.splice(0, state.products.length, ...(data.products || []))
  state.cart.splice(0, state.cart.length, ...(data.cart || []))
  state.addresses.splice(0, state.addresses.length, ...(data.addresses || []))
  state.reviews.splice(0, state.reviews.length, ...(data.reviews || []))
  state.afterSales.splice(0, state.afterSales.length, ...(data.afterSales || []))
  state.operationLogs.splice(0, state.operationLogs.length, ...(data.operationLogs || []))
  state.orders.splice(0, state.orders.length, ...(data.orders || []))

  const account = data.account
  Object.assign(session, {
    loggedIn: true,
    role: data.role,
    userId: account.id,
    merchantId: account.merchantId || '',
    accountName: account.name,
    email: account.email,
  })
}

export async function refreshBootstrap() {
  if (!session.token) return false
  try {
    const data = await apiRequest('/bootstrap')
    hydrateState(data)
    return true
  } catch (error) {
    console.error('刷新后端数据失败', error)
    if (error.message.includes('登录状态')) clearSession()
    throw error
  }
}

export function initializeStore() {
  if (initializing) return initializing
  initializing = (async () => {
    if (!session.token) return false
    try {
      await refreshBootstrap()
      return true
    } catch (error) {
      console.error('初始化登录会话失败', error)
      clearSession()
      return false
    }
  })()
  return initializing
}

export function initializeStorefront(force = false) {
  if (session.loggedIn) return Promise.resolve(true)
  if (storefrontLoaded && !force) return Promise.resolve(true)
  if (storefrontInitializing) return storefrontInitializing

  storefrontInitializing = (async () => {
    try {
      const data = await apiRequest('/storefront', {}, false)
      state.merchants.splice(0, state.merchants.length, ...(data.merchants || []))
      state.products.splice(0, state.products.length, ...(data.products || []))
      state.users.splice(0)
      state.cart.splice(0)
      state.addresses.splice(0)
      state.reviews.splice(0, state.reviews.length, ...(data.reviews || []))
      state.afterSales.splice(0)
      state.operationLogs.splice(0)
      state.orders.splice(0)
      storefrontLoaded = true
      return true
    } catch (error) {
      console.error('加载商城公开数据失败', error)
      return false
    } finally {
      storefrontInitializing = null
    }
  })()
  return storefrontInitializing
}

export const currentUser = computed(() => {
  const stored = state.users.find((item) => item.id === session.userId)
  if (stored) return stored
  return {
    id: session.userId,
    name: session.accountName,
    displayName: session.accountName,
    email: session.email,
    avatar: session.accountName?.slice(0, 1) || '用',
    phone: '',
    city: '',
    level: '',
    joinedAt: '',
  }
})

export const currentMerchant = computed(
  () =>
    state.merchants.find((item) => item.id === session.merchantId) ??
    state.merchants[0] ?? {
      id: '',
      name: '',
      owner: '',
      banner: '/images/chair.jpg',
      accent: '#56715f',
      weeklyDates: [],
      weeklySales: [],
      weeklyOrders: [],
    },
)

export const onlineProducts = computed(() =>
  state.products.filter((item) => item.status === 'online'),
)

export const cartItems = computed(() =>
  state.cart
    .map((item) => ({
      ...item,
      product: state.products.find((product) => product.id === item.productId),
    }))
    .filter((item) => item.product),
)

export const cartCount = computed(() =>
  state.cart.reduce((total, item) => total + item.quantity, 0),
)

export const cartTotal = computed(() =>
  cartItems.value.reduce(
    (total, item) => total + Number(item.product.price) * item.quantity,
    0,
  ),
)

export const defaultAddress = computed(
  () =>
    state.addresses.find((address) => address.defaultAddress) ??
    state.addresses[0] ??
    null,
)

export const userOrders = computed(() =>
  [...state.orders]
    .filter((order) => order.userId === session.userId)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
)

export function merchantName(merchantId) {
  return (
    state.merchants.find((merchant) => merchant.id === merchantId)?.name ??
    '未知商家'
  )
}

export function productById(productId) {
  return state.products.find((product) => product.id === productId)
}

export const demoAccounts = [
  {
    role: 'user',
    label: '消费者',
    name: '林知夏',
    email: 'user@demo.com',
    password: DEMO_PASSWORD,
    path: '/',
  },
  {
    role: 'merchant',
    label: '商家',
    name: '山岚生活',
    email: 'merchant@demo.com',
    password: DEMO_PASSWORD,
    path: '/merchant',
  },
  {
    role: 'admin',
    label: '超级管理员',
    name: '平台运营中心',
    email: 'admin@demo.com',
    password: DEMO_PASSWORD,
    path: '/admin',
  },
]

export function roleHomePath(role) {
  if (role === 'merchant') return '/merchant'
  if (role === 'admin') return '/admin'
  return '/'
}

export async function login(email, password) {
  try {
    const data = await apiRequest(
      '/auth/login',
      {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      },
      false,
    )
    session.token = data.token
    localStorage.setItem(TOKEN_KEY, data.token)
    await refreshBootstrap()
    return { ok: true, role: data.role, path: data.path }
  } catch (error) {
    clearSession()
    return { ok: false, message: error.message }
  }
}

export async function register(payload) {
  try {
    const data = await apiRequest(
      '/auth/register',
      {
        method: 'POST',
        body: JSON.stringify({
          displayName: payload.displayName,
          email: payload.email,
          phone: payload.phone,
          password: payload.password,
        }),
      },
      false,
    )
    session.token = data.token
    localStorage.setItem(TOKEN_KEY, data.token)
    await refreshBootstrap()
    return { ok: true, role: data.role, path: data.path }
  } catch (error) {
    clearSession()
    return { ok: false, message: error.message }
  }
}

export async function registerMerchant(payload) {
  try {
    const data = await apiRequest(
      '/auth/register/merchant',
      {
        method: 'POST',
        body: JSON.stringify({
          shopName: payload.shopName,
          ownerName: payload.ownerName,
          email: payload.email,
          phone: payload.phone,
          city: payload.city,
          description: payload.description,
          password: payload.password,
        }),
      },
      false,
    )
    return {
      ok: true,
      pending: Boolean(data.pending),
      message: data.message,
    }
  } catch (error) {
    return { ok: false, message: error.message }
  }
}

export async function logout() {
  try {
    if (session.token) {
      await apiRequest('/auth/logout', { method: 'POST' })
    }
  } catch {
    // JWT logout is client-side token removal.
  }
  clearSession()
}

async function mutate(path, options, successMessage) {
  try {
    const data = await apiRequest(path, options)
    await refreshBootstrap()
    if (successMessage) showToast(successMessage)
    return data
  } catch (error) {
    showToast(error.message, 'warning')
    return null
  }
}

export async function addToCart(productId, quantity = 1) {
  const result = await mutate(
    '/cart/items',
    {
      method: 'POST',
      body: JSON.stringify({ productId, quantity }),
    },
    '已加入购物车',
  )
  return Boolean(result)
}

export async function updateCartQuantity(productId, quantity) {
  return mutate(`/cart/items/${productId}`, {
    method: 'PATCH',
    body: JSON.stringify({ quantity: Math.max(1, Number(quantity) || 1) }),
  })
}

export async function removeCartItem(productId) {
  return mutate(
    `/cart/items/${productId}`,
    { method: 'DELETE' },
    '商品已移出购物车',
  )
}

export async function checkout(receiver = {}) {
  try {
    const payment = await apiRequest('/orders/checkout', {
      method: 'POST',
      body: JSON.stringify({
        receiverName: receiver.receiverName || session.accountName,
        receiverPhone: receiver.receiverPhone || '未填写',
        receiverAddress: receiver.receiverAddress || '未填写',
      }),
    })
    await refreshBootstrap()
    showToast(`订单已创建，共生成 ${payment.orderIds.length} 笔商家订单`)
    return payment
  } catch (error) {
    showToast(error.message, 'warning')
    return null
  }
}

export async function fetchPayment(paymentNo) {
  return apiRequest(`/payments/${encodeURIComponent(paymentNo)}`, {}, false)
}

export async function payPayment(paymentNo) {
  return apiRequest(
    `/payments/${encodeURIComponent(paymentNo)}/pay`,
    { method: 'POST' },
    false,
  )
}

export async function renewPayment(paymentNo) {
  return apiRequest(
    `/orders/payments/${encodeURIComponent(paymentNo)}/renew`,
    { method: 'POST' },
  )
}

export async function addAddress(payload) {
  return mutate(
    '/addresses',
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    '收货地址已保存',
  )
}

export async function updateAddress(addressId, payload) {
  return mutate(
    `/addresses/${addressId}`,
    {
      method: 'PATCH',
      body: JSON.stringify(payload),
    },
    '收货地址已更新',
  )
}

export async function setDefaultAddress(addressId) {
  return mutate(
    `/addresses/${addressId}/default`,
    { method: 'PATCH' },
    '默认地址已更新',
  )
}

export async function removeAddress(addressId) {
  return mutate(
    `/addresses/${addressId}`,
    { method: 'DELETE' },
    '收货地址已删除',
  )
}

export async function submitProductReview(productId, payload) {
  return mutate(
    `/reviews/products/${productId}`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    '商品评价已发布',
  )
}

export async function submitMerchantReview(merchantId, payload) {
  return mutate(
    `/reviews/merchants/${merchantId}`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    '店铺评价已发布',
  )
}

export async function loadSupportConversations() {
  return apiRequest('/support/conversations')
}

export async function loadSupportMessages(merchantId) {
  return apiRequest(`/support/conversations/${encodeURIComponent(merchantId)}`)
}

export async function sendSupportMessage(merchantId, content) {
  return apiRequest(
    `/support/conversations/${encodeURIComponent(merchantId)}/messages`,
    {
      method: 'POST',
      body: JSON.stringify({ content }),
    },
  )
}

export async function loadMerchantSupportConversations() {
  return apiRequest('/merchant/support/conversations')
}

export async function loadMerchantSupportMessages(userId) {
  return apiRequest(`/merchant/support/conversations/${encodeURIComponent(userId)}`)
}

export async function sendMerchantSupportMessage(userId, content) {
  return apiRequest(
    `/merchant/support/conversations/${encodeURIComponent(userId)}/messages`,
    {
      method: 'POST',
      body: JSON.stringify({ content }),
    },
  )
}

export async function createAfterSale(orderId, payload) {
  return mutate(
    `/after-sales/orders/${orderId}`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    '售后申请已提交',
  )
}

export async function processAfterSale(afterSaleId, payload) {
  return mutate(
    `/merchant/after-sales/${afterSaleId}`,
    {
      method: 'PATCH',
      body: JSON.stringify(payload),
    },
    `售后申请已${payload.decision === 'APPROVE' ? '同意' : '拒绝'}`,
  )
}

export async function reviewMerchant(merchantId, payload) {
  return mutate(
    `/admin/merchants/${merchantId}/review`,
    {
      method: 'PATCH',
      body: JSON.stringify(payload),
    },
    payload.decision === 'APPROVE' ? '商家入驻审核已通过' : '商家入驻申请已驳回',
  )
}

export async function cancelOrder(orderId) {
  return mutate(`/orders/${orderId}/cancel`, { method: 'PATCH' }, '订单已取消')
}

export async function confirmReceipt(orderId) {
  return mutate(`/orders/${orderId}/confirm`, { method: 'PATCH' }, '订单已确认收货')
}

export async function updateProductPrice(productId, nextPrice) {
  const price = Number(nextPrice)
  if (!Number.isFinite(price) || price <= 0) {
    showToast('请输入有效价格', 'warning')
    return false
  }
  const path =
    session.role === 'admin'
      ? `/admin/products/${productId}/price`
      : `/merchant/products/${productId}/price`
  const result = await mutate(
    path,
    {
      method: 'PATCH',
      body: JSON.stringify({ price }),
    },
    '商品价格已更新',
  )
  return Boolean(result)
}

export async function toggleProductStatus(productId) {
  const product = productById(productId)
  if (!product) return
  const nextStatus = product.status === 'online' ? 'offline' : 'online'
  const path =
    session.role === 'admin'
      ? `/admin/products/${productId}/status`
      : `/merchant/products/${productId}/status`
  await mutate(
    path,
    {
      method: 'PATCH',
      body: JSON.stringify({ status: nextStatus }),
    },
    nextStatus === 'online' ? '商品已上架' : '商品已下架',
  )
}

export async function updateProductDetails(productId, payload) {
  return mutate(
    `/merchant/products/${productId}`,
    {
      method: 'PATCH',
      body: JSON.stringify(payload),
    },
    '商品资料已更新',
  )
}

export async function addProduct(payload) {
  const result = await mutate(
    '/merchant/products',
    {
      method: 'POST',
      body: JSON.stringify({
        name: payload.name,
        category: payload.category,
        price: Number(payload.price),
        originalPrice:
          Number(payload.originalPrice) || Number(payload.price),
        stock: Number(payload.stock),
        status: payload.status,
        image: payload.image,
        description: payload.description,
        tags: payload.tags,
      }),
    },
    '商品已创建',
  )
  return result
}

export async function updateMerchantProfile(merchantId, payload) {
  return mutate(
    '/merchant/profile',
    {
      method: 'PATCH',
      body: JSON.stringify({
        name: payload.name,
        owner: payload.owner,
        phone: payload.phone,
        location: payload.location,
        description: payload.description,
      }),
    },
    '店铺资料已保存',
  )
}

export async function toggleMerchantStatus(merchantId) {
  const merchant = state.merchants.find((item) => item.id === merchantId)
  if (!merchant) return
  const nextStatus = merchant.status === 'active' ? 'disabled' : 'active'
  await mutate(
    `/admin/merchants/${merchantId}/status`,
    {
      method: 'PATCH',
      body: JSON.stringify({ status: nextStatus }),
    },
    nextStatus === 'active' ? '商家已启用' : '商家已停用',
  )
}

export async function toggleUserStatus(userId) {
  const user = state.users.find((item) => item.id === userId)
  if (!user) return
  const nextStatus = user.status === 'active' ? 'disabled' : 'active'
  await mutate(
    `/admin/users/${userId}/status`,
    {
      method: 'PATCH',
      body: JSON.stringify({ status: nextStatus }),
    },
    nextStatus === 'active' ? '用户已启用' : '用户已停用',
  )
}

export async function updateUserProfile(payload) {
  return mutate(
    '/profile',
    {
      method: 'PATCH',
      body: JSON.stringify(payload),
    },
    '个人信息已保存',
  )
}

export async function updateOrderStatus(orderId, status) {
  return mutate(
    `/merchant/orders/${orderId}/status`,
    {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    },
    `订单状态已更新为${status}`,
  )
}

export async function shipOrder(orderId, payload) {
  try {
    const result = await apiRequest(`/merchant/orders/${orderId}/ship`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    })
    await refreshBootstrap()
    showToast(`订单已发货，运单号 ${result.trackingNo}`)
    return result
  } catch (error) {
    showToast(error.message, 'warning')
    return null
  }
}

export async function refreshData() {
  try {
    await refreshBootstrap()
    showToast('数据已刷新')
  } catch (error) {
    showToast(error.message, 'warning')
  }
}

export function formatCurrency(value) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(Number(value) || 0)
}
