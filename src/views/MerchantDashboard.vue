<script setup>
import {
  AlertTriangle,
  ArrowUpRight,
  BarChart3,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  CircleDollarSign,
  ClipboardList,
  Eye,
  EyeOff,
  ImagePlus,
  LayoutDashboard,
  Lightbulb,
  LoaderCircle,
  MapPin,
  Menu,
  MessageCircle,
  Package,
  PackageCheck,
  PackagePlus,
  Pencil,
  Phone,
  Plus,
  Search,
  Send,
  Settings2,
  ShoppingBag,
  Store,
  TrendingUp,
  Truck,
  UsersRound,
  RotateCcw,
  X,
} from 'lucide-vue-next'
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
  watchEffect,
} from 'vue'
import { useRouter } from 'vue-router'
import DemandDonut from '../components/DemandDonut.vue'
import SalesChart from '../components/SalesChart.vue'
import ShippingModal from '../components/ShippingModal.vue'
import {
  addProduct,
  currentMerchant,
  formatCurrency,
  loadMerchantSupportConversations,
  loadMerchantSupportMessages,
  merchantName,
  processAfterSale,
  sendMerchantSupportMessage,
  showToast,
  state,
  toggleProductStatus,
  updateMerchantProfile,
  updateProductPrice,
  updateProductDetails,
} from '../store'

const router = useRouter()
const activeTab = ref('overview')
const currentDateLabel = new Date().toLocaleDateString('zh-CN')
const sidebarOpen = ref(false)
const addModalOpen = ref(false)
const productSearch = ref('')
const orderSearch = ref('')
const orderStatusFilter = ref('all')
const orderPage = ref(1)
const orderPageSize = 6
const orderDetailOpen = ref(false)
const selectedOrder = ref(null)
const shippingModalOpen = ref(false)
const shippingTarget = ref(null)
const priceDrafts = reactive({})
const stockDrafts = reactive({})
const supportConversations = ref([])
const supportMessages = reactive({})
const selectedSupportUserId = ref('')
const supportDraft = ref('')
const supportSending = ref(false)
const supportLoading = ref(false)
const afterSaleReplies = reactive({})
let supportPollTimer

const tabs = [
  { id: 'overview', label: '经营概览', icon: LayoutDashboard },
  { id: 'products', label: '商品管理', icon: Package },
  { id: 'orders', label: '订单处理', icon: ClipboardList },
  { id: 'after-sales', label: '售后处理', icon: RotateCcw },
  { id: 'support', label: '客服消息', icon: MessageCircle },
  { id: 'profile', label: '店铺资料', icon: Settings2 },
]

function parseOrderDate(value) {
  const [datePart, timePart = '00:00'] = value.split(' ')
  return new Date(`${datePart}T${timePart}:00`)
}

function shortLabel(value, maxLength = 9) {
  if (!value || value.length <= maxLength) return value
  return `${value.slice(0, maxLength)}…`
}

const merchantProducts = computed(() =>
  state.products.filter(
    (product) => product.merchantId === currentMerchant.value.id,
  ),
)

const merchantOrders = computed(() =>
  state.orders
    .filter((order) => order.merchantId === currentMerchant.value.id)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
)

const merchantAfterSales = computed(() =>
  state.afterSales
    .filter((item) => item.merchantId === currentMerchant.value.id)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
)

const pendingAfterSaleCount = computed(
  () => merchantAfterSales.value.filter((item) => item.status === '待商家处理').length,
)

const filteredProducts = computed(() => {
  const keyword = productSearch.value.trim().toLowerCase()
  if (!keyword) return merchantProducts.value
  return merchantProducts.value.filter(
    (product) =>
      product.name.toLowerCase().includes(keyword) ||
      product.category.toLowerCase().includes(keyword),
  )
})

const grossSales = computed(() =>
  currentMerchant.value.weeklySales.reduce((sum, value) => sum + value, 0),
)

const orderCount = computed(() =>
  currentMerchant.value.weeklyOrders.reduce((sum, value) => sum + value, 0),
)

const onlineCount = computed(
  () => merchantProducts.value.filter((product) => product.status === 'online').length,
)

const averageOrder = computed(() =>
  orderCount.value ? Math.round(grossSales.value / orderCount.value) : 0,
)

const topProducts = computed(() =>
  [...merchantProducts.value].sort((a, b) => b.sold - a.sold).slice(0, 4),
)

const recentWindowStart = computed(() => {
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  start.setDate(start.getDate() - 6)
  return start
})

const recentWindowLabel = computed(() => {
  const end = new Date()
  const format = (date) => `${date.getMonth() + 1}/${date.getDate()}`
  return `${format(recentWindowStart.value)} - ${format(end)}`
})

const recentMerchantOrders = computed(() =>
  merchantOrders.value.filter((order) => {
    const createdAt = parseOrderDate(order.createdAt)
    return createdAt >= recentWindowStart.value && createdAt <= new Date()
  }),
)

const recentDemandRows = computed(() => {
  const demand = new Map()
  recentMerchantOrders.value.forEach((order) => {
    order.items.forEach((item) => {
      const product = state.products.find((candidate) => candidate.id === item.productId)
      const current = demand.get(item.productId) ?? {
        productId: item.productId,
        product,
        quantity: 0,
        revenue: 0,
        orderCount: 0,
      }
      current.quantity += item.quantity
      current.revenue += Number(item.price) * item.quantity
      current.orderCount += 1
      demand.set(item.productId, current)
    })
  })
  return [...demand.values()].sort(
    (a, b) =>
      b.quantity - a.quantity ||
      (b.product?.sold ?? 0) - (a.product?.sold ?? 0),
  )
})

const recentDemandUnits = computed(() =>
  recentDemandRows.value.reduce((sum, row) => sum + row.quantity, 0),
)

const categoryDemand = computed(() => {
  const demand = new Map()
  recentDemandRows.value.forEach((row) => {
    const category = row.product?.category ?? '其他'
    demand.set(category, (demand.get(category) ?? 0) + row.quantity)
  })
  return [...demand.entries()]
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
})

const demandChartRows = computed(() => recentDemandRows.value.slice(0, 5))
const demandChartLabels = computed(() =>
  demandChartRows.value.map((row) => shortLabel(row.product?.name ?? row.productId)),
)
const demandChartValues = computed(() =>
  demandChartRows.value.map((row) => row.quantity),
)

const demandRecommendations = computed(() => {
  const recommendations = []
  const totalUnits = recentDemandUnits.value || 1
  const topCategory = categoryDemand.value[0]
  const topDemand = recentDemandRows.value[0]

  if (topCategory) {
    recommendations.push({
      tone: 'green',
      icon: PackagePlus,
      title: `优先扩充：${topCategory.name}`,
      description: `近 7 天售出 ${topCategory.value} 件，占近期需求 ${Math.round(
        (topCategory.value / totalUnits) * 100,
      )}%。建议补充同类价格带和风格的商品。`,
      action: '增加同类商品',
    })
  }

  if (topDemand?.product) {
    recommendations.push({
      tone: 'blue',
      icon: TrendingUp,
      title: `围绕 ${shortLabel(topDemand.product.name, 12)} 扩展`,
      description: `近 7 天售出 ${topDemand.quantity} 件，累计销量 ${topDemand.product.sold} 件。可增加相似功能、规格或套装。`,
      action: '新增相似 SKU',
    })
  }

  const lowStockDemand = merchantProducts.value
    .filter((product) => product.status === 'online')
    .map((product) => {
      const demand = recentDemandRows.value.find((row) => row.productId === product.id)
      return {
        product,
        recentQuantity: demand?.quantity ?? 0,
        coverDays: demand?.quantity ? product.stock / demand.quantity : Infinity,
      }
    })
    .filter((item) => item.recentQuantity > 0 && item.coverDays < 7)
    .sort((a, b) => a.coverDays - b.coverDays)[0]

  if (lowStockDemand) {
    recommendations.push({
      tone: 'coral',
      icon: AlertTriangle,
      title: `补货提醒：${shortLabel(lowStockDemand.product.name, 12)}`,
      description: `当前库存 ${lowStockDemand.product.stock} 件，近 7 天售出 ${lowStockDemand.recentQuantity} 件，库存覆盖不足 7 天。`,
      action: '优先补库存',
    })
  } else {
    recommendations.push({
      tone: 'sand',
      icon: Lightbulb,
      title: '库存节奏健康',
      description: '近期热销商品库存覆盖较充足，可把资源优先投入新品扩充和内容运营。',
      action: '保持补货节奏',
    })
  }

  return recommendations
})

const spotlightProduct = computed(() => topProducts.value[0] ?? {
  id: '',
  name: '暂未添加商品',
  description: '创建商品后，这里会展示本店主推商品。',
  image: '/images/chair.jpg',
  sold: 0,
  stock: 0,
  price: 0,
})

const orderStatusCounts = computed(() => {
  const result = { 待发货: 0, 待收货: 0, 已完成: 0, 已取消: 0 }
  merchantOrders.value.forEach((order) => {
    result[order.status] = (result[order.status] ?? 0) + 1
  })
  return result
})

const orderStatusBreakdown = computed(() => {
  const total = merchantOrders.value.length || 1
  return ['待发货', '待收货', '已完成', '已取消'].map((status) => {
    const count = orderStatusCounts.value[status] ?? 0
    return {
      status,
      count,
      progress: Math.round((count / total) * 100),
    }
  })
})

const filteredMerchantOrders = computed(() => {
  const keyword = orderSearch.value.trim().toLowerCase()
  return merchantOrders.value.filter((order) => {
    if (orderStatusFilter.value !== 'all' && order.status !== orderStatusFilter.value) {
      return false
    }
    if (!keyword) return true
    const buyer = state.users.find((user) => user.id === order.userId)
    const productNames = order.items
      .map((item) => state.products.find((product) => product.id === item.productId)?.name)
      .filter(Boolean)
    return [
      order.id,
      order.receiverName,
      order.receiverPhone,
      order.receiverAddress,
      buyer?.name,
      buyer?.email,
      ...productNames,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword))
  })
})

const orderPageCount = computed(() =>
  Math.max(1, Math.ceil(filteredMerchantOrders.value.length / orderPageSize)),
)

const paginatedOrders = computed(() => {
  const start = (orderPage.value - 1) * orderPageSize
  return filteredMerchantOrders.value.slice(start, start + orderPageSize)
})

const weeklyLabels = computed(() => {
  const dates = currentMerchant.value.weeklyDates ?? []
  if (!dates.length) return ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return dates.map((date) => `${Number(date.slice(5, 7))}/${Number(date.slice(8, 10))}`)
})

const selectedOrderBuyer = computed(() =>
  state.users.find((user) => user.id === selectedOrder.value?.userId),
)

const selectedOrderLines = computed(() =>
  (selectedOrder.value?.items ?? []).map((item) => ({
    ...item,
    product: state.products.find((product) => product.id === item.productId),
  })),
)

const selectedSupportConversation = computed(() =>
  supportConversations.value.find(
    (conversation) => conversation.userId === selectedSupportUserId.value,
  ),
)
const selectedSupportMessages = computed(
  () => supportMessages[selectedSupportUserId.value] ?? [],
)

const profileForm = reactive({
  name: currentMerchant.value.name,
  owner: currentMerchant.value.owner,
  phone: currentMerchant.value.phone,
  location: currentMerchant.value.location,
  description: currentMerchant.value.description,
})

const newProduct = reactive({
  name: '',
  category: '家居',
  price: '',
  originalPrice: '',
  stock: '',
  status: 'online',
  image: '/images/lamp.jpg',
  description: '',
  tags: '',
})

const imageOptions = [
  { src: '/images/chair.jpg', label: '家居' },
  { src: '/images/lamp.jpg', label: '灯具' },
  { src: '/images/skincare.jpg', label: '美妆' },
  { src: '/images/headphones.jpg', label: '数码' },
  { src: '/images/sneaker.jpg', label: '运动' },
]

watchEffect(() => {
  merchantProducts.value.forEach((product) => {
    if (priceDrafts[product.id] === undefined) priceDrafts[product.id] = product.price
    if (stockDrafts[product.id] === undefined) stockDrafts[product.id] = product.stock
  })
})

watch([orderSearch, orderStatusFilter], () => {
  orderPage.value = 1
})

watch(orderPageCount, (count) => {
  if (orderPage.value > count) orderPage.value = count
})

function changeTab(tab) {
  activeTab.value = tab
  sidebarOpen.value = false
  if (tab === 'support') refreshSupport()
}

async function refreshSupport() {
  if (activeTab.value !== 'support' && supportConversations.value.length) return
  try {
    supportLoading.value = !supportConversations.value.length
    supportConversations.value = await loadMerchantSupportConversations()
    if (
      !selectedSupportUserId.value ||
      !supportConversations.value.some(
        (conversation) => conversation.userId === selectedSupportUserId.value,
      )
    ) {
      selectedSupportUserId.value =
        supportConversations.value[0]?.userId ?? ''
    }
    if (selectedSupportUserId.value) {
      await loadSupportMessages(selectedSupportUserId.value)
    }
  } catch (error) {
    console.error('加载商家客服会话失败', error)
  } finally {
    supportLoading.value = false
  }
}

async function loadSupportMessages(userId = selectedSupportUserId.value) {
  if (!userId) return
  try {
    const detail = await loadMerchantSupportMessages(userId)
    supportMessages[userId] = detail.messages ?? []
    const index = supportConversations.value.findIndex(
      (conversation) => conversation.userId === userId,
    )
    if (index >= 0 && detail.conversation) {
      supportConversations.value[index] = detail.conversation
    }
  } catch (error) {
    console.error('加载买家消息失败', error)
  }
}

function selectSupportConversation(userId) {
  selectedSupportUserId.value = userId
  loadSupportMessages(userId)
}

async function sendSupportReply() {
  const content = supportDraft.value.trim()
  const userId = selectedSupportUserId.value
  if (!content || !userId || supportSending.value) return
  supportSending.value = true
  try {
    const detail = await sendMerchantSupportMessage(userId, content)
    supportMessages[userId] = detail.messages ?? []
    supportDraft.value = ''
    await refreshSupport()
  } catch (error) {
    console.error('发送商家回复失败', error)
  } finally {
    supportSending.value = false
  }
}

function handleSupportKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendSupportReply()
  }
}

async function savePrice(productId) {
  await updateProductPrice(productId, priceDrafts[productId])
}

async function saveStock(product) {
  await updateProductDetails(product.id, {
    stock: Number(stockDrafts[product.id]),
    category: product.category,
    image: product.image,
    description: product.description,
  })
}

async function createProduct() {
  if (!newProduct.name.trim() || !Number(newProduct.price) || !Number(newProduct.stock)) {
    return
  }
  const created = await addProduct({
    ...newProduct,
    merchantId: currentMerchant.value.id,
  })
  if (!created) return
  Object.assign(newProduct, {
    name: '',
    category: '家居',
    price: '',
    originalPrice: '',
    stock: '',
    status: 'online',
    image: '/images/lamp.jpg',
    description: '',
    tags: '',
  })
  addModalOpen.value = false
}

async function saveProfile() {
  await updateMerchantProfile(currentMerchant.value.id, profileForm)
}

function openOrderDetails(order) {
  selectedOrder.value = order
  orderDetailOpen.value = true
}

function closeOrderDetails() {
  orderDetailOpen.value = false
  selectedOrder.value = null
}

function openShipping(order) {
  shippingTarget.value = order
  shippingModalOpen.value = true
}

function finishShipping() {
  shippingModalOpen.value = false
  shippingTarget.value = null
  closeOrderDetails()
}

async function handleAfterSale(afterSale, decision) {
  const reply = afterSaleReplies[afterSale.id]?.trim()
  if (!reply) {
    showToast('请先填写处理说明', 'warning')
    return
  }
  const result = await processAfterSale(afterSale.id, {
    decision,
    reply,
  })
  if (result) delete afterSaleReplies[afterSale.id]
}

function goOrderPage(nextPage) {
  orderPage.value = Math.min(orderPageCount.value, Math.max(1, nextPage))
}

function openAccountLogin() {
  router.push('/login')
}

onMounted(() => {
  supportPollTimer = window.setInterval(() => {
    if (activeTab.value === 'support') refreshSupport()
  }, 3000)
})

onBeforeUnmount(() => {
  window.clearInterval(supportPollTimer)
})
</script>

<template>
  <div class="dashboard-shell dashboard-shell--merchant">
    <aside class="dashboard-sidebar" :class="{ open: sidebarOpen }">
      <div class="dashboard-brand">
        <span class="brand-mark"><Store :size="20" /></span>
        <span>
          <strong>云商集</strong>
          <small>商家中心</small>
        </span>
      </div>

      <div class="store-switcher">
        <span class="store-switcher__logo">{{ currentMerchant.name.slice(0, 1) }}</span>
        <span>
          <strong>{{ currentMerchant.name }}</strong>
          <small>商家工作台</small>
        </span>
        <ChevronDown :size="15" />
      </div>

      <nav class="dashboard-nav">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          :class="{ active: activeTab === tab.id }"
          type="button"
          @click="changeTab(tab.id)"
        >
          <component :is="tab.icon" :size="18" />
          {{ tab.label }}
        </button>
      </nav>

      <div class="dashboard-sidebar__footer">
        <button class="sidebar-profile" type="button" @click="openAccountLogin">
          <span>{{ currentMerchant.owner.slice(0, 1) }}</span>
          <span>
            <strong>{{ currentMerchant.owner }}</strong>
            <small>切换演示身份</small>
          </span>
          <ArrowUpRight :size="16" />
        </button>
      </div>
    </aside>

    <div v-if="sidebarOpen" class="sidebar-scrim" @click="sidebarOpen = false" />

    <main class="dashboard-main">
      <header class="dashboard-mobile-header">
        <button class="icon-button" type="button" @click="sidebarOpen = true">
          <Menu :size="20" />
        </button>
        <strong>商家中心</strong>
        <button class="mini-avatar" type="button" @click="openAccountLogin">
          {{ currentMerchant.owner.slice(0, 1) }}
        </button>
      </header>

      <header class="dashboard-heading">
        <div>
          <span class="section-kicker">MERCHANT WORKSPACE</span>
          <h1>
            {{
              activeTab === 'overview'
                ? '经营概览'
                : activeTab === 'products'
                  ? '商品管理'
                  : activeTab === 'orders'
                    ? '订单处理'
                    : activeTab === 'after-sales'
                      ? '售后处理'
                      : activeTab === 'support'
                        ? '客服消息'
                        : '店铺资料'
            }}
          </h1>
        </div>
        <div class="dashboard-heading__actions">
          <span class="date-chip">数据更新至 {{ currentDateLabel }}</span>
          <button
            v-if="!['support', 'after-sales'].includes(activeTab)"
            class="button button--dark button--compact"
            type="button"
            @click="activeTab = 'products'; addModalOpen = true"
          >
            <Plus :size="17" />
            新建商品
          </button>
        </div>
      </header>

      <section v-if="activeTab === 'overview'" class="dashboard-section">
        <div class="kpi-grid">
          <article class="kpi-card">
            <span class="kpi-card__icon kpi-card__icon--green">
              <CircleDollarSign :size="20" />
            </span>
            <span>近 7 日销售额</span>
            <strong>{{ formatCurrency(grossSales) }}</strong>
            <small><TrendingUp :size="13" /> 来自数据库统计</small>
          </article>
          <article class="kpi-card">
            <span class="kpi-card__icon kpi-card__icon--blue">
              <ShoppingBag :size="20" />
            </span>
            <span>近 7 日订单</span>
            <strong>{{ orderCount }}</strong>
            <small><TrendingUp :size="13" /> 来自数据库统计</small>
          </article>
          <article class="kpi-card">
            <span class="kpi-card__icon kpi-card__icon--coral">
              <PackageCheck :size="20" />
            </span>
            <span>在售商品</span>
            <strong>{{ onlineCount }}</strong>
            <small>{{ merchantProducts.length - onlineCount }} 件已下架</small>
          </article>
          <article class="kpi-card">
            <span class="kpi-card__icon kpi-card__icon--sand">
              <UsersRound :size="20" />
            </span>
            <span>平均客单价</span>
            <strong>{{ formatCurrency(averageOrder) }}</strong>
            <small>按近 7 日订单计算</small>
          </article>
        </div>

        <section class="demand-insights">
          <header class="panel-heading demand-insights__heading">
            <div>
              <span class="section-kicker">DEMAND INSIGHTS</span>
              <h2>近期需求洞察</h2>
              <p>看看最近买家更常购买什么，再决定下一批上架方向。</p>
            </div>
            <span class="panel-badge">
              {{ recentWindowLabel }} · {{ recentMerchantOrders.length }} 单 ·
              {{ recentDemandUnits }} 件
            </span>
          </header>

          <div class="demand-insight-grid">
            <article class="data-panel demand-chart-panel demand-chart-panel--wide">
              <header class="panel-heading">
                <div>
                  <span class="section-kicker">TOP SELLING NOW</span>
                  <h2>近期热销商品</h2>
                </div>
                <BarChart3 :size="18" />
              </header>
              <SalesChart
                v-if="demandChartValues.length"
                :values="demandChartValues"
                :labels="demandChartLabels"
                :color="currentMerchant.accent"
                type="bar"
                :height="235"
              />
              <div v-else class="demand-empty">
                近 7 天还没有订单，先补充商品或尝试新的推荐方式。
              </div>
            </article>

            <article class="data-panel demand-chart-panel">
              <header class="panel-heading">
                <div>
                  <span class="section-kicker">CATEGORY MIX</span>
                  <h2>品类需求</h2>
                </div>
              </header>
              <DemandDonut v-if="categoryDemand.length" :items="categoryDemand" :height="235" />
              <div v-else class="demand-empty">暂无品类成交数据</div>
            </article>

            <article class="data-panel demand-recommendation-panel">
              <header class="panel-heading">
                <div>
                  <span class="section-kicker">NEXT CATALOG MOVE</span>
                  <h2>上架建议</h2>
                </div>
                <Lightbulb :size="18" />
              </header>
              <div class="recommendation-list">
                <div
                  v-for="item in demandRecommendations"
                  :key="item.title"
                  class="recommendation-item"
                  :class="`recommendation-item--${item.tone}`"
                >
                  <span class="recommendation-item__icon">
                    <component :is="item.icon" :size="17" />
                  </span>
                  <span class="recommendation-item__copy">
                    <strong>{{ item.title }}</strong>
                    <small>{{ item.description }}</small>
                  </span>
                  <em>{{ item.action }}</em>
                </div>
              </div>
            </article>
          </div>
        </section>

        <div class="dashboard-grid dashboard-grid--wide">
          <article class="data-panel">
            <header class="panel-heading">
              <div>
                <span class="section-kicker">SALES TREND</span>
                <h2>销售走势</h2>
              </div>
              <span class="panel-badge">近 7 天</span>
            </header>
            <SalesChart
              :values="currentMerchant.weeklySales"
              :labels="weeklyLabels"
              :color="currentMerchant.accent"
              :height="240"
            />
          </article>

          <article class="data-panel product-ranking">
            <header class="panel-heading">
              <div>
                <span class="section-kicker">TOP PRODUCTS</span>
                <h2>热销商品</h2>
              </div>
              <BarChart3 :size="18" />
            </header>
            <div class="ranking-list">
              <div v-for="(product, index) in topProducts" :key="product.id">
                <span class="ranking-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <img :src="product.image" :alt="product.name" />
                <span class="ranking-copy">
                  <strong>{{ product.name }}</strong>
                  <small>{{ product.sold }} 件已售</small>
                </span>
                <strong>{{ formatCurrency(product.price) }}</strong>
              </div>
            </div>
          </article>
        </div>

        <div class="dashboard-grid dashboard-grid--spotlight">
          <article class="product-spotlight">
            <div class="product-spotlight__media">
              <img :src="spotlightProduct.image" :alt="spotlightProduct.name" />
              <span>本周主推</span>
            </div>
            <div class="product-spotlight__copy">
              <span class="section-kicker">PRODUCT SPOTLIGHT</span>
              <h2>{{ spotlightProduct.name }}</h2>
              <p>{{ spotlightProduct.description }}</p>
              <div class="spotlight-stats">
                <div>
                  <span>累计销量</span>
                  <strong>{{ spotlightProduct.sold }}</strong>
                </div>
                <div>
                  <span>当前库存</span>
                  <strong>{{ spotlightProduct.stock }}</strong>
                </div>
                <div>
                  <span>销售价格</span>
                  <strong>{{ formatCurrency(spotlightProduct.price) }}</strong>
                </div>
              </div>
              <button class="text-button" type="button" @click="activeTab = 'products'">
                管理商品信息
                <ArrowUpRight :size="15" />
              </button>
            </div>
          </article>

          <article class="data-panel order-health">
            <header class="panel-heading">
              <div>
                <span class="section-kicker">ORDER HEALTH</span>
                <h2>订单状态</h2>
              </div>
              <ClipboardList :size="18" />
            </header>
            <div class="status-breakdown">
              <div v-for="item in orderStatusBreakdown" :key="item.status">
                <span>{{ item.status }}</span>
                <strong>{{ item.count }}</strong>
                <i :style="{ '--progress': `${item.progress}%` }" />
              </div>
            </div>
            <button class="text-button" type="button" @click="activeTab = 'orders'">
              查看全部订单
              <ArrowUpRight :size="15" />
            </button>
          </article>
        </div>

        <article class="data-panel">
          <header class="panel-heading">
            <div>
              <span class="section-kicker">RECENT ORDERS</span>
              <h2>最近订单</h2>
            </div>
            <button class="text-button" type="button" @click="activeTab = 'orders'">
              全部订单
              <ArrowUpRight :size="15" />
            </button>
          </header>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>商品</th>
                  <th>用户</th>
                  <th>金额</th>
                  <th>状态</th>
                  <th>时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in merchantOrders.slice(0, 4)" :key="order.id">
                  <td class="mono">{{ order.id }}</td>
                  <td>{{ order.items.length }} 种商品</td>
                  <td>{{ state.users.find((user) => user.id === order.userId)?.name }}</td>
                  <td>{{ formatCurrency(order.total) }}</td>
                  <td><span class="status-chip">{{ order.status }}</span></td>
                  <td>{{ order.createdAt }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section v-else-if="activeTab === 'products'" class="dashboard-section">
        <div class="dashboard-toolbar">
          <label class="search-field">
            <Search :size="17" />
            <input
              v-model="productSearch"
              type="search"
              placeholder="搜索本店商品"
            />
          </label>
          <button class="button button--dark button--compact" type="button" @click="addModalOpen = true">
            <Plus :size="17" />
            新建商品
          </button>
        </div>

        <article class="data-panel">
          <div class="table-wrap">
            <table class="data-table product-admin-table">
              <thead>
                <tr>
                  <th>商品</th>
                  <th>分类</th>
                  <th>价格</th>
                  <th>库存</th>
                  <th>销量</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="product in filteredProducts" :key="product.id">
                  <td>
                    <div class="table-product">
                      <img :src="product.image" :alt="product.name" />
                      <div>
                        <strong>{{ product.name }}</strong>
                        <span>#{{ product.id }}</span>
                      </div>
                    </div>
                  </td>
                  <td>{{ product.category }}</td>
                  <td>
                    <div class="price-editor">
                      <span>¥</span>
                      <input
                        v-model.number="priceDrafts[product.id]"
                        type="number"
                        min="0"
                        step="1"
                      />
                      <button
                        type="button"
                        aria-label="保存价格"
                        @click="savePrice(product.id)"
                      >
                        <Pencil :size="13" />
                      </button>
                    </div>
                  </td>
                  <td>
                    <div class="price-editor">
                      <input v-model.number="stockDrafts[product.id]" type="number" min="0" />
                      <button type="button" aria-label="保存库存" @click="saveStock(product)">
                        <Pencil :size="13" />
                      </button>
                    </div>
                  </td>
                  <td>{{ product.sold }}</td>
                  <td>
                    <span
                      class="status-chip"
                      :class="product.status === 'online' ? 'status-chip--active' : 'status-chip--disabled'"
                    >
                      {{ product.status === 'online' ? '销售中' : '已下架' }}
                    </span>
                  </td>
                  <td>
                    <button
                      class="table-action"
                      type="button"
                      @click="toggleProductStatus(product.id)"
                    >
                      <EyeOff v-if="product.status === 'online'" :size="15" />
                      <Eye v-else :size="15" />
                      {{ product.status === 'online' ? '下架' : '上架' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section v-else-if="activeTab === 'orders'" class="dashboard-section">
        <div class="kpi-grid">
          <article class="kpi-card">
            <span>待发货</span>
            <strong>{{ orderStatusCounts['待发货'] }}</strong>
            <small>需要尽快处理</small>
          </article>
          <article class="kpi-card">
            <span>待收货</span>
            <strong>{{ orderStatusCounts['待收货'] }}</strong>
            <small>等待买家确认</small>
          </article>
          <article class="kpi-card">
            <span>已完成</span>
            <strong>{{ orderStatusCounts['已完成'] }}</strong>
            <small>历史完成订单</small>
          </article>
          <article class="kpi-card">
            <span>已取消</span>
            <strong>{{ orderStatusCounts['已取消'] }}</strong>
            <small>已回滚库存与统计</small>
          </article>
        </div>

        <div class="dashboard-toolbar order-toolbar">
          <label class="search-field">
            <Search :size="17" />
            <input
              v-model="orderSearch"
              type="search"
              placeholder="搜索订单号、买家、电话或商品"
            />
          </label>
          <label class="order-filter">
            <span>订单状态</span>
            <select v-model="orderStatusFilter">
              <option value="all">全部状态</option>
              <option value="待发货">待发货</option>
              <option value="待收货">待收货</option>
              <option value="已完成">已完成</option>
              <option value="已取消">已取消</option>
            </select>
          </label>
        </div>

        <article class="data-panel">
          <header class="panel-heading">
            <div>
              <span class="section-kicker">ORDER DESK</span>
              <h2>全部订单</h2>
            </div>
            <span class="panel-badge">{{ filteredMerchantOrders.length }} 笔</span>
          </header>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>订单与商品</th>
                  <th>买家</th>
                  <th>金额</th>
                  <th>时间</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in paginatedOrders" :key="order.id">
                  <td>
                    <div class="order-table-product">
                      <img
                        :src="state.products.find((p) => p.id === order.items[0].productId)?.image"
                        alt=""
                      />
                      <div>
                        <strong>{{ order.id }}</strong>
                        <span>
                          {{ state.products.find((p) => p.id === order.items[0].productId)?.name }}
                          <template v-if="order.items.length > 1">
                            等 {{ order.items.length }} 件
                          </template>
                        </span>
                      </div>
                    </div>
                  </td>
                  <td>{{ state.users.find((user) => user.id === order.userId)?.name }}</td>
                  <td>{{ formatCurrency(order.total) }}</td>
                  <td>{{ order.createdAt }}</td>
                  <td>
                    <span class="status-chip" :class="`status-chip--${order.status}`">
                      {{ order.status }}
                    </span>
                  </td>
                  <td>
                    <div class="order-row-actions">
                      <button class="table-action" type="button" @click="openOrderDetails(order)">
                        <Eye :size="14" />
                        详情
                      </button>
                      <button
                        v-if="order.status === '待发货'"
                        class="table-action table-action--primary"
                        type="button"
                        @click="openShipping(order)"
                      >
                        <Truck :size="14" />
                        确认发货
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="!paginatedOrders.length">
                  <td colspan="6">
                    <div class="table-empty">没有匹配的订单</div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <footer v-if="filteredMerchantOrders.length" class="order-pagination">
            <span>
              第 {{ orderPage }} / {{ orderPageCount }} 页 ·
              共 {{ filteredMerchantOrders.length }} 笔
            </span>
            <div>
              <button
                type="button"
                :disabled="orderPage === 1"
                @click="goOrderPage(orderPage - 1)"
              >
                <ChevronLeft :size="16" />
              </button>
              <button
                type="button"
                :disabled="orderPage === orderPageCount"
                @click="goOrderPage(orderPage + 1)"
              >
                <ChevronRight :size="16" />
              </button>
            </div>
          </footer>
        </article>
      </section>

      <section v-else-if="activeTab === 'after-sales'" class="dashboard-section">
        <div class="kpi-grid kpi-grid--three">
          <article class="kpi-card">
            <span>待处理售后</span>
            <strong>{{ pendingAfterSaleCount }}</strong>
            <small>需要商家尽快处理</small>
          </article>
          <article class="kpi-card">
            <span>全部售后</span>
            <strong>{{ merchantAfterSales.length }}</strong>
            <small>本店售后申请记录</small>
          </article>
          <article class="kpi-card">
            <span>已同意</span>
            <strong>{{ merchantAfterSales.filter((item) => item.status === '已同意').length }}</strong>
            <small>退款流程已确认</small>
          </article>
        </div>

        <article class="data-panel">
          <header class="panel-heading">
            <div>
              <span class="section-kicker">AFTER-SALES DESK</span>
              <h2>售后申请</h2>
            </div>
            <span class="panel-badge">{{ merchantAfterSales.length }} 笔</span>
          </header>
          <div class="table-wrap">
            <table class="data-table after-sale-table">
              <thead>
                <tr>
                  <th>订单与买家</th>
                  <th>售后类型</th>
                  <th>申请原因</th>
                  <th>申请时间</th>
                  <th>状态</th>
                  <th>处理</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="afterSale in merchantAfterSales" :key="afterSale.id">
                  <td>
                    <div class="order-table-product">
                      <span class="after-sale-avatar">{{ afterSale.buyerName.slice(0, 1) }}</span>
                      <div>
                        <strong>{{ afterSale.buyerName }}</strong>
                        <span class="mono">{{ afterSale.orderId }}</span>
                      </div>
                    </div>
                  </td>
                  <td>{{ afterSale.requestType }}</td>
                  <td>
                    <div class="after-sale-reason">
                      <strong>{{ afterSale.reason }}</strong>
                      <span>{{ afterSale.description || '未填写补充说明' }}</span>
                    </div>
                  </td>
                  <td>{{ afterSale.createdAt }}</td>
                  <td>
                    <span class="after-sale-state" :class="`is-${afterSale.status}`">
                      {{ afterSale.status }}
                    </span>
                  </td>
                  <td>
                    <div v-if="afterSale.status === '待商家处理'" class="after-sale-actions">
                      <input
                        v-model="afterSaleReplies[afterSale.id]"
                        type="text"
                        maxlength="500"
                        placeholder="填写处理说明"
                      />
                      <button
                        class="table-action table-action--primary"
                        type="button"
                        @click="handleAfterSale(afterSale, 'APPROVE')"
                      >
                        同意
                      </button>
                      <button
                        class="table-action"
                        type="button"
                        @click="handleAfterSale(afterSale, 'REJECT')"
                      >
                        拒绝
                      </button>
                    </div>
                    <span v-else class="after-sale-reply">{{ afterSale.merchantReply }}</span>
                  </td>
                </tr>
                <tr v-if="!merchantAfterSales.length">
                  <td colspan="6">
                    <div class="table-empty">暂无售后申请</div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section v-else-if="activeTab === 'support'" class="dashboard-section">
        <div v-if="supportLoading" class="empty-state merchant-support-empty">
          <LoaderCircle class="spin" :size="30" />
          <h2>正在加载客服消息</h2>
        </div>
        <div v-else-if="!supportConversations.length" class="empty-state merchant-support-empty">
          <MessageCircle :size="30" />
          <h2>还没有用户咨询</h2>
          <p>用户在店铺客服发送消息后，会自动出现在这里。</p>
        </div>
        <div v-else class="merchant-support-shell">
          <aside class="merchant-support-list">
            <header>
              <div>
                <span class="section-kicker">BUYER CHATS</span>
                <h2>用户会话</h2>
              </div>
              <span>{{ supportConversations.length }}</span>
            </header>
            <button
              v-for="conversation in supportConversations"
              :key="conversation.userId"
              type="button"
              :class="{ active: selectedSupportUserId === conversation.userId }"
              @click="selectSupportConversation(conversation.userId)"
            >
              <span class="buyer-avatar">{{ conversation.buyerAvatar || '用' }}</span>
              <span>
                <strong>{{ conversation.buyerName || '买家' }}</strong>
                <small>{{ conversation.lastMessage }}</small>
              </span>
              <time>{{ conversation.lastMessageAt }}</time>
            </button>
          </aside>

          <section v-if="selectedSupportConversation" class="merchant-support-chat">
            <header>
              <span class="buyer-avatar">
                {{ selectedSupportConversation.buyerAvatar || '用' }}
              </span>
              <div>
                <strong>{{ selectedSupportConversation.buyerName || '买家' }}</strong>
                <small>用户咨询 · 消息每 3 秒同步</small>
              </div>
            </header>

            <div class="merchant-support-messages">
              <div
                v-for="message in selectedSupportMessages"
                :key="message.id"
                class="support-message"
                :class="`support-message--${message.senderRole === 'MERCHANT' ? 'user' : 'merchant'}`"
              >
                <span class="support-message__avatar">
                  {{
                    message.senderRole === 'MERCHANT'
                      ? currentMerchant.name.slice(0, 1)
                      : selectedSupportConversation.buyerAvatar || '用'
                  }}
                </span>
                <div>
                  <p>{{ message.content }}</p>
                  <small>{{ message.createdAt }}</small>
                </div>
              </div>
              <div v-if="!selectedSupportMessages.length" class="support-chat__empty">
                <MessageCircle :size="24" />
                <span>还没有消息</span>
              </div>
            </div>

            <div class="support-composer">
              <textarea
                v-model="supportDraft"
                rows="2"
                placeholder="回复用户，Enter 发送"
                @keydown="handleSupportKeydown"
              />
              <button
                type="button"
                aria-label="发送商家回复"
                :disabled="!supportDraft.trim() || supportSending"
                @click="sendSupportReply"
              >
                <LoaderCircle v-if="supportSending" class="spin" :size="18" />
                <Send v-else :size="18" />
              </button>
            </div>
          </section>
        </div>
      </section>

      <section v-else class="dashboard-section profile-settings">
        <article class="merchant-profile-banner">
          <img :src="currentMerchant.banner" :alt="currentMerchant.name" />
          <div class="merchant-profile-banner__overlay" />
          <div class="merchant-profile-banner__content">
            <span class="merchant-profile-banner__logo">
              {{ currentMerchant.name.slice(0, 1) }}
            </span>
            <div>
              <span>{{ currentMerchant.location }} · 已入驻</span>
              <h2>{{ currentMerchant.name }}</h2>
              <p>{{ currentMerchant.description }}</p>
            </div>
          </div>
        </article>

        <article class="data-panel form-panel">
          <div class="form-section-title">
            <div>
              <span class="section-kicker">STORE PROFILE</span>
              <h2>店铺资料</h2>
            </div>
            <span class="panel-badge">商家 ID：{{ currentMerchant.id }}</span>
          </div>
          <form class="settings-form" @submit.prevent="saveProfile">
            <label>
              <span>店铺名称</span>
              <input v-model="profileForm.name" type="text" />
            </label>
            <label>
              <span>负责人</span>
              <input v-model="profileForm.owner" type="text" />
            </label>
            <label>
              <span>联系电话</span>
              <input v-model="profileForm.phone" type="text" />
            </label>
            <label>
              <span>所在城市</span>
              <input v-model="profileForm.location" type="text" />
            </label>
            <label class="full-width">
              <span>店铺介绍</span>
              <textarea v-model="profileForm.description" rows="4" />
            </label>
            <div class="form-actions">
              <button class="button button--dark" type="submit">保存修改</button>
              <span>资料更新后，商城店铺卡片同步生效。</span>
            </div>
          </form>
        </article>
      </section>
    </main>

    <Teleport to="body">
      <transition name="modal">
        <div v-if="addModalOpen" class="modal-backdrop" @click.self="addModalOpen = false">
          <section class="form-modal" role="dialog" aria-modal="true" aria-labelledby="new-product-title">
            <header class="form-modal__header">
              <div>
                <span class="section-kicker">NEW PRODUCT</span>
                <h2 id="new-product-title">新建商品</h2>
              </div>
              <button class="icon-button" type="button" aria-label="关闭新建商品窗口" @click="addModalOpen = false">
                <X :size="20" />
              </button>
            </header>
            <form class="product-form" @submit.prevent="createProduct">
              <label class="full-width">
                <span>商品名称</span>
                <input v-model="newProduct.name" type="text" required />
              </label>
              <label>
                <span>分类</span>
                <select v-model="newProduct.category">
                  <option>数码</option>
                  <option>家居</option>
                  <option>运动</option>
                  <option>美妆</option>
                </select>
              </label>
              <label>
                <span>状态</span>
                <select v-model="newProduct.status">
                  <option value="online">立即上架</option>
                  <option value="offline">暂存下架</option>
                </select>
              </label>
              <label>
                <span>销售价</span>
                <input v-model="newProduct.price" type="number" min="1" required />
              </label>
              <label>
                <span>划线价</span>
                <input v-model="newProduct.originalPrice" type="number" min="1" />
              </label>
              <label>
                <span>库存</span>
                <input v-model="newProduct.stock" type="number" min="0" required />
              </label>
              <label>
                <span>卖点标签</span>
                <input v-model="newProduct.tags" type="text" placeholder="用逗号分隔" />
              </label>
              <label class="full-width">
                <span>商品介绍</span>
                <textarea v-model="newProduct.description" rows="3" />
              </label>
              <div class="full-width">
                <span class="field-label">
                  <ImagePlus :size="15" />
                  商品图片
                </span>
                <div class="image-picker">
                  <button
                    v-for="image in imageOptions"
                    :key="image.src"
                    :class="{ active: newProduct.image === image.src }"
                    type="button"
                    @click="newProduct.image = image.src"
                  >
                    <img :src="image.src" :alt="image.label" />
                    <span>{{ image.label }}</span>
                  </button>
                </div>
              </div>
              <footer class="form-modal__footer full-width">
                <button class="button button--outline" type="button" @click="addModalOpen = false">
                  取消
                </button>
                <button class="button button--dark" type="submit">
                  创建商品
                  <Plus :size="17" />
                </button>
              </footer>
            </form>
          </section>
        </div>
      </transition>
    </Teleport>

    <Teleport to="body">
      <transition name="modal">
        <div
          v-if="orderDetailOpen && selectedOrder"
          class="modal-backdrop"
          @click.self="closeOrderDetails"
        >
          <section
            class="form-modal order-detail-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="order-detail-title"
          >
            <header class="form-modal__header">
              <div>
                <span class="section-kicker">ORDER DETAIL</span>
                <h2 id="order-detail-title">{{ selectedOrder.id }}</h2>
              </div>
              <button
                class="icon-button"
                type="button"
                aria-label="关闭订单详情"
                @click="closeOrderDetails"
              >
                <X :size="20" />
              </button>
            </header>

            <div class="order-detail-summary">
              <div>
                <span>订单金额</span>
                <strong>{{ formatCurrency(selectedOrder.total) }}</strong>
              </div>
              <div>
                <span>下单时间</span>
                <strong>{{ selectedOrder.createdAt }}</strong>
              </div>
              <span class="status-chip" :class="`status-chip--${selectedOrder.status}`">
                {{ selectedOrder.status }}
              </span>
            </div>

            <div class="order-detail-columns">
              <section class="order-detail-section">
                <h3>商品明细</h3>
                <div class="order-detail-lines">
                  <div v-for="line in selectedOrderLines" :key="line.productId">
                    <img
                      v-if="line.product"
                      :src="line.product.image"
                      :alt="line.product.name"
                    />
                    <span>
                      <strong>{{ line.product?.name ?? line.productId }}</strong>
                      <small>{{ line.quantity }} 件 · {{ formatCurrency(line.price) }}</small>
                    </span>
                    <strong>{{ formatCurrency(line.price * line.quantity) }}</strong>
                  </div>
                </div>
              </section>

              <section class="order-detail-section">
                <h3>收货信息</h3>
                <div class="order-buyer">
                  <strong>{{ selectedOrder.receiverName }}</strong>
                  <span>{{ selectedOrderBuyer?.email ?? '买家邮箱不可用' }}</span>
                </div>
                <div class="order-contact-list">
                  <span><Phone :size="14" />{{ selectedOrder.receiverPhone }}</span>
                  <span><MapPin :size="14" />{{ selectedOrder.receiverAddress }}</span>
                </div>
                <div v-if="selectedOrder.status === '已取消'" class="order-cancelled-note">
                  订单已取消，库存、销量和经营统计已回滚。
                </div>
                <div v-else class="order-detail-timeline">
                  <span class="active">订单创建</span>
                  <span :class="{ active: ['待收货', '已完成'].includes(selectedOrder.status) }">
                    商家发货
                  </span>
                  <span :class="{ active: selectedOrder.status === '已完成' }">
                    买家确认
                  </span>
                </div>
              </section>
            </div>

            <footer class="form-modal__footer order-detail-footer">
              <button class="button button--outline" type="button" @click="closeOrderDetails">
                关闭
              </button>
              <button
                v-if="selectedOrder.status === '待发货'"
                class="button button--dark"
                type="button"
                @click="openShipping(selectedOrder)"
              >
                确认发货
                <Truck :size="17" />
              </button>
            </footer>
          </section>
        </div>
      </transition>
    </Teleport>
    <ShippingModal
      v-if="shippingModalOpen && shippingTarget"
      :order="shippingTarget"
      @close="shippingModalOpen = false"
      @shipped="finishShipping"
    />
  </div>
</template>
