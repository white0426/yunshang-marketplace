<script setup>
import {
  AlertTriangle,
  ArrowUpRight,
  BadgeDollarSign,
  BarChart3,
  ChevronRight,
  CircleDollarSign,
  Eye,
  EyeOff,
  LayoutDashboard,
  Lightbulb,
  Menu,
  Package,
  PackagePlus,
  Pencil,
  RefreshCcw,
  RotateCcw,
  ScrollText,
  Search,
  ShieldCheck,
  ShoppingBag,
  Store,
  TrendingUp,
  UsersRound,
  X,
} from 'lucide-vue-next'
import { computed, reactive, ref, watchEffect } from 'vue'
import { useRouter } from 'vue-router'
import DemandDonut from '../components/DemandDonut.vue'
import MerchantReviewModal from '../components/MerchantReviewModal.vue'
import SalesChart from '../components/SalesChart.vue'
import {
  formatCurrency,
  merchantName,
  refreshData,
  state,
  toggleMerchantStatus,
  toggleProductStatus,
  toggleUserStatus,
  updateProductPrice,
} from '../store'

const router = useRouter()
const activeTab = ref('overview')
const currentDateLabel = new Date().toLocaleDateString('zh-CN')
const sidebarOpen = ref(false)
const productSearch = ref('')
const merchantSearch = ref('')
const userSearch = ref('')
const logSearch = ref('')
const priceDrafts = reactive({})
const reviewModalOpen = ref(false)
const reviewTarget = ref(null)

const tabs = [
  { id: 'overview', label: '平台概览', icon: LayoutDashboard },
  { id: 'merchants', label: '商家管理', icon: Store },
  { id: 'merchant-review', label: '商家审核', icon: ShieldCheck },
  { id: 'products', label: '商品管理', icon: Package },
  { id: 'orders', label: '订单管理', icon: ShoppingBag },
  { id: 'after-sales', label: '售后管理', icon: RotateCcw },
  { id: 'users', label: '用户管理', icon: UsersRound },
  { id: 'logs', label: '操作日志', icon: ScrollText },
]

const pendingMerchants = computed(() =>
  state.merchants.filter((merchant) => merchant.status === 'pending'),
)
const reviewedMerchants = computed(() =>
  state.merchants.filter((merchant) =>
    ['active', 'rejected', 'disabled'].includes(merchant.status),
  ),
)
const filteredOperationLogs = computed(() => {
  const keyword = logSearch.value.trim().toLowerCase()
  if (!keyword) return state.operationLogs
  return state.operationLogs.filter((log) =>
    [
      log.operatorName,
      log.action,
      log.targetType,
      log.targetId,
      log.targetName,
      log.detail,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword)),
  )
})

function parseOrderDate(value) {
  const [datePart, timePart = '00:00'] = value.split(' ')
  return new Date(`${datePart}T${timePart}:00`)
}

function shortLabel(value, maxLength = 9) {
  if (!value || value.length <= maxLength) return value
  return `${value.slice(0, maxLength)}…`
}

const grossSales = computed(() =>
  state.merchants.reduce(
    (sum, merchant) =>
      sum + merchant.weeklySales.reduce((merchantSum, value) => merchantSum + value, 0),
    0,
  ),
)

const activeMerchants = computed(
  () => state.merchants.filter((merchant) => merchant.status === 'active').length,
)

const onlineProducts = computed(
  () => state.products.filter((product) => product.status === 'online').length,
)

const activeUsers = computed(
  () => state.users.filter((user) => user.status === 'active').length,
)

const platformTrend = computed(() =>
  Array.from({ length: 7 }, (_, index) =>
    state.merchants.reduce(
      (sum, merchant) => sum + merchant.weeklySales[index],
      0,
    ),
  ),
)

const platformTrendLabels = computed(() => {
  const dates = state.merchants.find((merchant) => merchant.weeklyDates?.length)?.weeklyDates ?? []
  if (!dates.length) return ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return dates.map((date) => `${Number(date.slice(5, 7))}/${Number(date.slice(8, 10))}`)
})

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

const recentPlatformOrders = computed(() =>
  state.orders.filter((order) => {
    const createdAt = parseOrderDate(order.createdAt)
    return createdAt >= recentWindowStart.value && createdAt <= new Date()
  }),
)

const platformDemandRows = computed(() => {
  const demand = new Map()
  recentPlatformOrders.value.forEach((order) => {
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

const recentPlatformUnits = computed(() =>
  platformDemandRows.value.reduce((sum, row) => sum + row.quantity, 0),
)

const platformCategoryDemand = computed(() => {
  const demand = new Map()
  platformDemandRows.value.forEach((row) => {
    const category = row.product?.category ?? '其他'
    demand.set(category, (demand.get(category) ?? 0) + row.quantity)
  })
  return [...demand.entries()]
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
})

const platformDemandChartRows = computed(() => platformDemandRows.value.slice(0, 5))
const platformDemandChartLabels = computed(() =>
  platformDemandChartRows.value.map((row) => shortLabel(row.product?.name ?? row.productId)),
)
const platformDemandChartValues = computed(() =>
  platformDemandChartRows.value.map((row) => row.quantity),
)

const recentMerchantPerformance = computed(() => {
  const demand = new Map()
  recentPlatformOrders.value.forEach((order) => {
    const merchant = state.merchants.find((candidate) => candidate.id === order.merchantId)
    const current = demand.get(order.merchantId) ?? {
      id: order.merchantId,
      name: merchant?.name ?? '未知商家',
      banner: merchant?.banner ?? '/images/chair.jpg',
      quantity: 0,
      revenue: 0,
      orderCount: 0,
    }
    current.orderCount += 1
    current.revenue += Number(order.total)
    order.items.forEach((item) => {
      current.quantity += item.quantity
    })
    demand.set(order.merchantId, current)
  })
  return [...demand.values()].sort(
    (a, b) => b.quantity - a.quantity || b.revenue - a.revenue,
  )
})

const platformDemandRecommendations = computed(() => {
  const totalUnits = recentPlatformUnits.value || 1
  const topCategory = platformCategoryDemand.value[0]
  const topMerchant = recentMerchantPerformance.value[0]
  const topProduct = platformDemandRows.value[0]
  const recommendations = []

  if (topCategory) {
    recommendations.push({
      tone: 'green',
      icon: PackagePlus,
      title: `平台重点品类：${topCategory.name}`,
      description: `近 7 天售出 ${topCategory.value} 件，占全平台需求 ${Math.round(
        (topCategory.value / totalUnits) * 100,
      )}%。适合引导更多商家补充同类商品。`,
      action: '增加品类供给',
    })
  }

  if (topMerchant) {
    recommendations.push({
      tone: 'blue',
      icon: TrendingUp,
      title: `重点商家：${topMerchant.name}`,
      description: `近 7 天 ${topMerchant.orderCount} 单、${topMerchant.quantity} 件，贡献 ${formatCurrency(
        topMerchant.revenue,
      )}。可优先给予流量和活动资源。`,
      action: '倾斜平台资源',
    })
  }

  if (topProduct?.product) {
    recommendations.push({
      tone: 'sand',
      icon: Lightbulb,
      title: `爆品机会：${shortLabel(topProduct.product.name, 12)}`,
      description: `全平台近 7 天售出 ${topProduct.quantity} 件，累计销量 ${topProduct.product.sold} 件。建议鼓励多商家增加相似供给。`,
      action: '复制爆品供给',
    })
  }

  if (!recommendations.length) {
    recommendations.push({
      tone: 'coral',
      icon: AlertTriangle,
      title: '近期成交不足',
      description: '近 7 天暂无订单，可先检查商品供给、活动节奏和商家营业状态。',
      action: '检查经营状态',
    })
  }

  return recommendations
})

const merchantPerformance = computed(() =>
  [...state.merchants]
    .map((merchant) => ({
      ...merchant,
      gross: merchant.weeklySales.reduce((sum, value) => sum + value, 0),
      products: state.products.filter(
        (product) => product.merchantId === merchant.id,
      ).length,
    }))
    .sort((a, b) => b.gross - a.gross),
)

const topProducts = computed(() =>
  [...state.products].sort((a, b) => b.sold - a.sold).slice(0, 5),
)

const filteredProducts = computed(() => {
  const keyword = productSearch.value.trim().toLowerCase()
  if (!keyword) return state.products
  return state.products.filter(
    (product) =>
      product.name.toLowerCase().includes(keyword) ||
      product.category.toLowerCase().includes(keyword) ||
      merchantName(product.merchantId).toLowerCase().includes(keyword),
  )
})

const filteredMerchants = computed(() => {
  const keyword = merchantSearch.value.trim().toLowerCase()
  if (!keyword) return state.merchants
  return state.merchants.filter(
    (merchant) =>
      merchant.name.toLowerCase().includes(keyword) ||
      merchant.owner.toLowerCase().includes(keyword) ||
      merchant.location.toLowerCase().includes(keyword),
  )
})

const filteredUsers = computed(() => {
  const keyword = userSearch.value.trim().toLowerCase()
  if (!keyword) return state.users
  return state.users.filter(
    (user) =>
      user.name.toLowerCase().includes(keyword) ||
      user.email.toLowerCase().includes(keyword) ||
      user.city.toLowerCase().includes(keyword),
  )
})

watchEffect(() => {
  state.products.forEach((product) => {
    if (priceDrafts[product.id] === undefined) priceDrafts[product.id] = product.price
  })
})

function changeTab(tab) {
  activeTab.value = tab
  sidebarOpen.value = false
}

function openMerchantReview(merchant) {
  reviewTarget.value = merchant
  reviewModalOpen.value = true
}

function closeMerchantReview() {
  reviewModalOpen.value = false
  reviewTarget.value = null
}

function finishMerchantReview() {
  closeMerchantReview()
}

function merchantStatusLabel(status) {
  return {
    active: '营业中',
    pending: '待审核',
    rejected: '已驳回',
    disabled: '已停用',
  }[status] ?? status
}

function targetTypeLabel(type) {
  return {
    MERCHANT: '商家',
    PRODUCT: '商品',
    USER: '用户',
  }[type] ?? type
}

function savePrice(productId) {
  updateProductPrice(productId, priceDrafts[productId])
}

</script>

<template>
  <div class="dashboard-shell dashboard-shell--admin">
    <aside class="dashboard-sidebar" :class="{ open: sidebarOpen }">
      <div class="dashboard-brand">
        <span class="brand-mark"><ShieldCheck :size="20" /></span>
        <span>
          <strong>云商集</strong>
          <small>平台管理后台</small>
        </span>
      </div>

      <div class="admin-identity">
        <span><ShieldCheck :size="18" /></span>
        <div>
          <strong>超级管理员</strong>
          <small>全平台访问权限</small>
        </div>
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
        <button class="sidebar-profile" type="button" @click="router.push('/login')">
          <span>管</span>
          <span>
            <strong>平台运营中心</strong>
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
        <strong>平台管理</strong>
        <button class="mini-avatar" type="button" @click="router.push('/login')">管</button>
      </header>

      <header class="dashboard-heading">
        <div>
          <span class="section-kicker">PLATFORM CONTROL</span>
          <h1>
            {{
              activeTab === 'overview'
                ? '平台概览'
                : activeTab === 'merchants'
                  ? '商家管理'
                  : activeTab === 'merchant-review'
                    ? '商家审核'
                    : activeTab === 'products'
                      ? '商品管理'
                      : activeTab === 'orders'
                        ? '订单管理'
                        : activeTab === 'after-sales'
                          ? '售后管理'
                          : activeTab === 'logs'
                            ? '操作日志'
                            : '用户管理'
            }}
          </h1>
        </div>
        <div class="dashboard-heading__actions">
          <button class="button button--outline button--compact" type="button" @click="refreshData">
            <RefreshCcw :size="16" />
            刷新数据库数据
          </button>
          <span class="date-chip">平台数据 · {{ currentDateLabel }}</span>
        </div>
      </header>

      <section v-if="activeTab === 'overview'" class="dashboard-section">
        <div class="kpi-grid">
          <article class="kpi-card">
            <span class="kpi-card__icon kpi-card__icon--blue">
              <CircleDollarSign :size="20" />
            </span>
            <span>近 7 日平台销售额</span>
            <strong>{{ formatCurrency(grossSales) }}</strong>
            <small><ArrowUpRight :size="13" /> 来自数据库统计</small>
          </article>
          <article class="kpi-card">
            <span class="kpi-card__icon kpi-card__icon--green">
              <Store :size="20" />
            </span>
            <span>正常营业商家</span>
            <strong>{{ activeMerchants }} / {{ state.merchants.length }}</strong>
            <small>{{ state.merchants.length - activeMerchants }} 家已停用</small>
          </article>
          <article class="kpi-card">
            <span class="kpi-card__icon kpi-card__icon--coral">
              <Package :size="20" />
            </span>
            <span>在售商品</span>
            <strong>{{ onlineProducts }}</strong>
            <small>{{ state.products.length - onlineProducts }} 件已下架</small>
          </article>
          <article class="kpi-card">
            <span class="kpi-card__icon kpi-card__icon--sand">
              <UsersRound :size="20" />
            </span>
            <span>正常用户</span>
            <strong>{{ activeUsers }} / {{ state.users.length }}</strong>
            <small>{{ state.users.length - activeUsers }} 个账号停用</small>
          </article>
        </div>

        <section class="demand-insights platform-demand-insights">
          <header class="panel-heading demand-insights__heading">
            <div>
              <span class="section-kicker">PLATFORM DEMAND</span>
              <h2>全平台近期售卖洞察</h2>
              <p>汇总所有商家的近期订单，判断平台流量、品类机会和商家贡献。</p>
            </div>
            <span class="panel-badge">
              {{ recentWindowLabel }} · {{ recentPlatformOrders.length }} 单 ·
              {{ recentPlatformUnits }} 件
            </span>
          </header>

          <div class="platform-demand-grid">
            <article class="data-panel demand-chart-panel">
              <header class="panel-heading">
                <div>
                  <span class="section-kicker">TOP PRODUCTS NOW</span>
                  <h2>全平台近期热销商品</h2>
                </div>
                <BarChart3 :size="18" />
              </header>
              <SalesChart
                v-if="platformDemandChartValues.length"
                :values="platformDemandChartValues"
                :labels="platformDemandChartLabels"
                color="#315d7a"
                type="bar"
                :height="235"
              />
              <div v-else class="demand-empty">近 7 天暂无商品成交</div>
            </article>

            <article class="data-panel demand-chart-panel">
              <header class="panel-heading">
                <div>
                  <span class="section-kicker">CATEGORY DEMAND</span>
                  <h2>全平台品类需求</h2>
                </div>
              </header>
              <DemandDonut
                v-if="platformCategoryDemand.length"
                :items="platformCategoryDemand"
                :height="235"
              />
              <div v-else class="demand-empty">暂无品类成交数据</div>
            </article>

            <article class="data-panel platform-merchant-panel">
              <header class="panel-heading">
                <div>
                  <span class="section-kicker">MERCHANT MOMENTUM</span>
                  <h2>商家近期贡献</h2>
                </div>
                <Store :size="18" />
              </header>
              <div v-if="recentMerchantPerformance.length" class="platform-merchant-list">
                <div
                  v-for="(merchant, index) in recentMerchantPerformance"
                  :key="merchant.id"
                  class="platform-merchant-item"
                >
                  <span class="platform-merchant-item__rank">
                    {{ String(index + 1).padStart(2, '0') }}
                  </span>
                  <img :src="merchant.banner" :alt="merchant.name" />
                  <span class="platform-merchant-item__copy">
                    <strong>{{ merchant.name }}</strong>
                    <small>{{ merchant.orderCount }} 单 · {{ merchant.quantity }} 件</small>
                  </span>
                  <span class="platform-merchant-item__value">
                    <strong>{{ formatCurrency(merchant.revenue) }}</strong>
                    <small>
                      {{ Math.round((merchant.quantity / (recentPlatformUnits || 1)) * 100) }}%
                    </small>
                  </span>
                  <i
                    :style="{
                      '--progress': `${Math.round(
                        (merchant.quantity / (recentPlatformUnits || 1)) * 100,
                      )}%`,
                    }"
                  />
                </div>
              </div>
              <div v-else class="demand-empty">近 7 天暂无商家成交</div>
            </article>

            <article class="data-panel demand-recommendation-panel">
              <header class="panel-heading">
                <div>
                  <span class="section-kicker">PLATFORM ACTIONS</span>
                  <h2>平台运营建议</h2>
                </div>
                <Lightbulb :size="18" />
              </header>
              <div class="recommendation-list">
                <div
                  v-for="item in platformDemandRecommendations"
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
                <span class="section-kicker">PLATFORM GMV</span>
                <h2>平台销售走势</h2>
              </div>
              <span class="panel-badge">近 7 天</span>
            </header>
            <SalesChart
              :values="platformTrend"
              :labels="platformTrendLabels"
              color="#315d7a"
              :height="250"
            />
          </article>

          <article class="data-panel merchant-ranking">
            <header class="panel-heading">
              <div>
                <span class="section-kicker">MERCHANT RANKING</span>
                <h2>商家表现</h2>
              </div>
              <BarChart3 :size="18" />
            </header>
            <div class="ranking-list">
              <div v-for="(merchant, index) in merchantPerformance" :key="merchant.id">
                <span class="ranking-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <img :src="merchant.banner" :alt="merchant.name" />
                <span class="ranking-copy">
                  <strong>{{ merchant.name }}</strong>
                  <small>{{ merchant.products }} 件商品</small>
                </span>
                <strong>{{ formatCurrency(merchant.gross) }}</strong>
              </div>
            </div>
          </article>
        </div>

        <div class="dashboard-grid dashboard-grid--admin-summary">
          <article class="data-panel">
            <header class="panel-heading">
              <div>
                <span class="section-kicker">TOP SELLING</span>
                <h2>全平台热销商品</h2>
              </div>
              <button class="text-button" type="button" @click="activeTab = 'products'">
                管理商品
                <ChevronRight :size="15" />
              </button>
            </header>
            <div class="admin-product-list">
              <div v-for="product in topProducts" :key="product.id">
                <img :src="product.image" :alt="product.name" />
                <span>
                  <strong>{{ product.name }}</strong>
                  <small>{{ merchantName(product.merchantId) }}</small>
                </span>
                <span>{{ product.sold }} 件</span>
                <strong>{{ formatCurrency(product.price) }}</strong>
              </div>
            </div>
          </article>

          <article class="data-panel admin-actions-panel">
            <header class="panel-heading">
              <div>
                <span class="section-kicker">QUICK ACCESS</span>
                <h2>管理入口</h2>
              </div>
              <BadgeDollarSign :size="18" />
            </header>
            <button type="button" @click="activeTab = 'merchants'">
              <span><Store :size="19" /> 商家状态与经营概况</span>
              <ChevronRight :size="17" />
            </button>
            <button type="button" @click="activeTab = 'products'">
              <span><ShoppingBag :size="19" /> 全平台商品与价格</span>
              <ChevronRight :size="17" />
            </button>
            <button type="button" @click="activeTab = 'users'">
              <span><UsersRound :size="19" /> 用户账号与状态</span>
              <ChevronRight :size="17" />
            </button>
          </article>
        </div>

        <article class="data-panel">
          <header class="panel-heading">
            <div>
              <span class="section-kicker">RECENT ACTIVITY</span>
              <h2>最近订单</h2>
            </div>
            <span class="panel-badge">{{ state.orders.length }} 笔订单</span>
          </header>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>商家</th>
                  <th>用户</th>
                  <th>金额</th>
                  <th>状态</th>
                  <th>时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in state.orders.slice(0, 5)" :key="order.id">
                  <td class="mono">{{ order.id }}</td>
                  <td>{{ merchantName(order.merchantId) }}</td>
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

      <section v-else-if="activeTab === 'merchants'" class="dashboard-section">
        <div class="dashboard-toolbar">
          <label class="search-field">
            <Search :size="17" />
            <input v-model="merchantSearch" type="search" placeholder="搜索商家、负责人或城市" />
          </label>
          <span class="toolbar-note">{{ filteredMerchants.length }} 家商家</span>
        </div>
        <article class="data-panel">
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>商家</th>
                  <th>负责人</th>
                  <th>城市</th>
                  <th>商品数</th>
                  <th>近 7 日销售</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="merchant in filteredMerchants" :key="merchant.id">
                  <td>
                    <div class="table-product">
                      <img :src="merchant.banner" :alt="merchant.name" />
                      <div>
                        <strong>{{ merchant.name }}</strong>
                        <span>{{ merchant.email }}</span>
                      </div>
                    </div>
                  </td>
                  <td>{{ merchant.owner }}</td>
                  <td>{{ merchant.location }}</td>
                  <td>{{ state.products.filter((product) => product.merchantId === merchant.id).length }}</td>
                  <td>{{ formatCurrency(merchant.weeklySales.reduce((sum, value) => sum + value, 0)) }}</td>
                  <td>
                    <span
                      class="status-chip"
                      :class="`status-chip--${merchant.status}`"
                    >
                      {{ merchantStatusLabel(merchant.status) }}
                    </span>
                  </td>
                  <td>
                    <button
                      v-if="merchant.status === 'pending'"
                      class="table-action table-action--primary"
                      type="button"
                      @click="openMerchantReview(merchant)"
                    >
                      去审核
                    </button>
                    <button
                      v-else-if="['active', 'disabled'].includes(merchant.status)"
                      class="table-action"
                      type="button"
                      @click="toggleMerchantStatus(merchant.id)"
                    >
                      {{ merchant.status === 'active' ? '停用' : '启用' }}
                    </button>
                    <span v-else class="table-empty-text">已驳回</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section v-else-if="activeTab === 'merchant-review'" class="dashboard-section">
        <div class="kpi-grid kpi-grid--three">
          <article class="kpi-card">
            <span>待审核商家</span>
            <strong>{{ pendingMerchants.length }}</strong>
            <small>等待平台确认入驻资格</small>
          </article>
          <article class="kpi-card">
            <span>营业中商家</span>
            <strong>{{ state.merchants.filter((item) => item.status === 'active').length }}</strong>
            <small>已通过并正常经营</small>
          </article>
          <article class="kpi-card">
            <span>已驳回</span>
            <strong>{{ state.merchants.filter((item) => item.status === 'rejected').length }}</strong>
            <small>未通过入驻审核</small>
          </article>
        </div>

        <article class="data-panel">
          <header class="panel-heading">
            <div>
              <span class="section-kicker">MERCHANT APPLICATIONS</span>
              <h2>待审核店铺</h2>
            </div>
            <span class="panel-badge">{{ pendingMerchants.length }} 家</span>
          </header>
          <div class="table-wrap">
            <table class="data-table merchant-review-table">
              <thead>
                <tr>
                  <th>店铺与负责人</th>
                  <th>联系方式</th>
                  <th>地区</th>
                  <th>店铺简介</th>
                  <th>申请时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="merchant in pendingMerchants" :key="merchant.id">
                  <td>
                    <div class="user-cell">
                      <span>{{ merchant.name.slice(0, 1) }}</span>
                      <div>
                        <strong>{{ merchant.name }}</strong>
                        <small>{{ merchant.owner }}</small>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div class="merchant-contact-cell">
                      <span>{{ merchant.email }}</span>
                      <small>{{ merchant.phone || '未填写电话' }}</small>
                    </div>
                  </td>
                  <td>{{ merchant.location }}</td>
                  <td>
                    <p class="merchant-application-copy">{{ merchant.description }}</p>
                  </td>
                  <td>{{ merchant.joinedAt }}</td>
                  <td>
                    <button
                      class="table-action table-action--primary"
                      type="button"
                      @click="openMerchantReview(merchant)"
                    >
                      <ShieldCheck :size="14" />
                      审核
                    </button>
                  </td>
                </tr>
                <tr v-if="!pendingMerchants.length">
                  <td colspan="6">
                    <div class="table-empty">当前没有待审核的商家申请</div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article v-if="state.merchants.some((item) => item.status === 'rejected')" class="data-panel review-history-panel">
          <header class="panel-heading">
            <div>
              <span class="section-kicker">REVIEW HISTORY</span>
              <h2>驳回记录</h2>
            </div>
          </header>
          <div class="review-history-list">
            <div v-for="merchant in state.merchants.filter((item) => item.status === 'rejected')" :key="merchant.id">
              <strong>{{ merchant.name }}</strong>
              <span>{{ merchant.reviewNote || '未填写审核说明' }}</span>
              <time>{{ merchant.reviewedAt }}</time>
            </div>
          </div>
        </article>
      </section>

      <section v-else-if="activeTab === 'products'" class="dashboard-section">
        <div class="dashboard-toolbar">
          <label class="search-field">
            <Search :size="17" />
            <input v-model="productSearch" type="search" placeholder="搜索商品、分类或商家" />
          </label>
          <span class="toolbar-note">{{ filteredProducts.length }} 件商品</span>
        </div>
        <article class="data-panel">
          <div class="table-wrap">
            <table class="data-table product-admin-table">
              <thead>
                <tr>
                  <th>商品</th>
                  <th>所属商家</th>
                  <th>价格</th>
                  <th>库存</th>
                  <th>销量</th>
                  <th>状态</th>
                  <th>平台操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="product in filteredProducts" :key="product.id">
                  <td>
                    <div class="table-product">
                      <img :src="product.image" :alt="product.name" />
                      <div>
                        <strong>{{ product.name }}</strong>
                        <span>{{ product.category }} · #{{ product.id }}</span>
                      </div>
                    </div>
                  </td>
                  <td>{{ merchantName(product.merchantId) }}</td>
                  <td>
                    <div class="price-editor">
                      <span>¥</span>
                      <input v-model.number="priceDrafts[product.id]" type="number" min="0" />
                      <button type="button" aria-label="保存价格" @click="savePrice(product.id)">
                        <Pencil :size="13" />
                      </button>
                    </div>
                  </td>
                  <td>{{ product.stock }}</td>
                  <td>{{ product.sold }}</td>
                  <td>
                    <span
                      class="status-chip"
                      :class="product.status === 'online' ? 'status-chip--active' : 'status-chip--disabled'"
                    >
                      {{ product.status === 'online' ? '在售' : '已下架' }}
                    </span>
                  </td>
                  <td>
                    <button class="table-action" type="button" @click="toggleProductStatus(product.id)">
                      <EyeOff v-if="product.status === 'online'" :size="14" />
                      <Eye v-else :size="14" />
                      {{ product.status === 'online' ? '强制下架' : '允许上架' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section v-else-if="activeTab === 'orders'" class="dashboard-section">
        <article class="data-panel">
          <header class="panel-heading">
            <div>
              <span class="section-kicker">ALL ORDERS</span>
              <h2>全平台订单</h2>
            </div>
            <span class="panel-badge">{{ state.orders.length }} 笔</span>
          </header>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>订单号</th><th>商家</th><th>用户</th><th>收货人</th>
                  <th>金额</th><th>状态</th><th>时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in state.orders" :key="order.id">
                  <td class="mono">{{ order.id }}</td>
                  <td>{{ merchantName(order.merchantId) }}</td>
                  <td>{{ state.users.find((user) => user.id === order.userId)?.name }}</td>
                  <td>{{ order.receiverName }}</td>
                  <td>{{ formatCurrency(order.total) }}</td>
                  <td><span class="status-chip">{{ order.status }}</span></td>
                  <td>{{ order.createdAt }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section v-else-if="activeTab === 'after-sales'" class="dashboard-section">
        <div class="kpi-grid kpi-grid--three">
          <article class="kpi-card">
            <span>待商家处理</span>
            <strong>{{ state.afterSales.filter((item) => item.status === '待商家处理').length }}</strong>
            <small>平台持续跟踪处理进度</small>
          </article>
          <article class="kpi-card">
            <span>已同意售后</span>
            <strong>{{ state.afterSales.filter((item) => item.status === '已同意').length }}</strong>
            <small>商家已确认退款</small>
          </article>
          <article class="kpi-card">
            <span>全部售后</span>
            <strong>{{ state.afterSales.length }}</strong>
            <small>全平台售后申请记录</small>
          </article>
        </div>
        <article class="data-panel">
          <header class="panel-heading">
            <div>
              <span class="section-kicker">PLATFORM AFTER-SALES</span>
              <h2>全平台售后记录</h2>
            </div>
            <span class="panel-badge">{{ state.afterSales.length }} 笔</span>
          </header>
          <div class="table-wrap">
            <table class="data-table after-sale-table">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>商家</th>
                  <th>买家</th>
                  <th>售后类型</th>
                  <th>原因</th>
                  <th>金额</th>
                  <th>状态</th>
                  <th>申请时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="afterSale in state.afterSales" :key="afterSale.id">
                  <td class="mono">{{ afterSale.orderId }}</td>
                  <td>{{ afterSale.merchantName }}</td>
                  <td>{{ afterSale.buyerName }}</td>
                  <td>{{ afterSale.requestType }}</td>
                  <td>
                    <div class="after-sale-reason">
                      <strong>{{ afterSale.reason }}</strong>
                      <span>{{ afterSale.description || '未填写补充说明' }}</span>
                    </div>
                  </td>
                  <td>{{ formatCurrency(afterSale.orderTotal) }}</td>
                  <td>
                    <span class="after-sale-state" :class="`is-${afterSale.status}`">
                      {{ afterSale.status }}
                    </span>
                  </td>
                  <td>{{ afterSale.createdAt }}</td>
                </tr>
                <tr v-if="!state.afterSales.length">
                  <td colspan="8">
                    <div class="table-empty">暂无售后记录</div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section v-else-if="activeTab === 'logs'" class="dashboard-section">
        <div class="kpi-grid kpi-grid--three">
          <article class="kpi-card">
            <span>操作日志</span>
            <strong>{{ state.operationLogs.length }}</strong>
            <small>最近 100 条管理员操作</small>
          </article>
          <article class="kpi-card">
            <span>涉及商家</span>
            <strong>{{ state.operationLogs.filter((log) => log.targetType === 'MERCHANT').length }}</strong>
            <small>商家启停与入驻审核</small>
          </article>
          <article class="kpi-card">
            <span>涉及商品</span>
            <strong>{{ state.operationLogs.filter((log) => log.targetType === 'PRODUCT').length }}</strong>
            <small>商品改价与上下架</small>
          </article>
        </div>
        <div class="dashboard-toolbar">
          <label class="search-field">
            <Search :size="17" />
            <input
              v-model="logSearch"
              type="search"
              placeholder="搜索管理员、动作、对象或详情"
            />
          </label>
          <button class="button button--outline button--compact" type="button" @click="refreshData">
            <RefreshCcw :size="16" />
            刷新日志
          </button>
        </div>
        <article class="data-panel">
          <header class="panel-heading">
            <div>
              <span class="section-kicker">ADMIN AUDIT TRAIL</span>
              <h2>管理员操作审计</h2>
            </div>
            <span class="panel-badge">{{ filteredOperationLogs.length }} 条</span>
          </header>
          <div class="table-wrap">
            <table class="data-table operation-log-table">
              <thead>
                <tr>
                  <th>操作时间</th>
                  <th>管理员</th>
                  <th>操作动作</th>
                  <th>对象类型</th>
                  <th>对象</th>
                  <th>变更详情</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="log in filteredOperationLogs" :key="log.id">
                  <td class="mono">{{ log.createdAt }}</td>
                  <td>
                    <div class="log-operator">
                      <span>{{ log.operatorName.slice(0, 1) }}</span>
                      <strong>{{ log.operatorName }}</strong>
                    </div>
                  </td>
                  <td><span class="log-action">{{ log.action }}</span></td>
                  <td>{{ targetTypeLabel(log.targetType) }}</td>
                  <td>
                    <div class="log-target">
                      <strong>{{ log.targetName }}</strong>
                      <small>{{ log.targetId }}</small>
                    </div>
                  </td>
                  <td>
                    <p class="log-detail">{{ log.detail || '无补充说明' }}</p>
                  </td>
                </tr>
                <tr v-if="!filteredOperationLogs.length">
                  <td colspan="6">
                    <div class="table-empty">暂无管理员操作记录</div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>

      <section v-else class="dashboard-section">
        <div class="dashboard-toolbar">
          <label class="search-field">
            <Search :size="17" />
            <input v-model="userSearch" type="search" placeholder="搜索用户、邮箱或城市" />
          </label>
          <span class="toolbar-note">{{ filteredUsers.length }} 个账号</span>
        </div>
        <article class="data-panel">
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>用户</th>
                  <th>邮箱</th>
                  <th>城市</th>
                  <th>会员等级</th>
                  <th>订单数</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in filteredUsers" :key="user.id">
                  <td>
                    <div class="user-cell">
                      <span>{{ user.avatar }}</span>
                      <strong>{{ user.name }}</strong>
                    </div>
                  </td>
                  <td>{{ user.email }}</td>
                  <td>{{ user.city }}</td>
                  <td>{{ user.level }}</td>
                  <td>{{ state.orders.filter((order) => order.userId === user.id).length }}</td>
                  <td>
                    <span
                      class="status-chip"
                      :class="user.status === 'active' ? 'status-chip--active' : 'status-chip--disabled'"
                    >
                      {{ user.status === 'active' ? '正常' : '已停用' }}
                    </span>
                  </td>
                  <td>
                    <button class="table-action" type="button" @click="toggleUserStatus(user.id)">
                      {{ user.status === 'active' ? '停用账号' : '恢复账号' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>
    </main>
    <MerchantReviewModal
      v-if="reviewModalOpen && reviewTarget"
      :merchant="reviewTarget"
      @close="closeMerchantReview"
      @reviewed="finishMerchantReview"
    />
  </div>
</template>
