<script setup>
import {
  ArrowDownWideNarrow,
  ArrowRight,
  ArrowUpRight,
  ArrowUpNarrowWide,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Flame,
  RefreshCw,
  Search,
  Sparkles,
  Store,
  Star,
  TrendingUp,
} from 'lucide-vue-next'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ProductCard from '../components/ProductCard.vue'
import {
  formatCurrency,
  initializeStorefront,
  merchantName,
  onlineProducts,
  refreshBootstrap,
  session,
  state,
  showToast,
} from '../store'

const router = useRouter()
const search = ref('')
const category = ref('全部')
const merchantFilter = ref('all')
const sortMode = ref('featured')
const sortOpen = ref(false)
const sortControl = ref(null)
const heroIndex = ref(0)
const heroPaused = ref(false)
const pullDistance = ref(0)
const isRefreshing = ref(false)
const refreshCompleted = ref(false)
const featuredOrder = ref([])

const PULL_THRESHOLD = 72
const MAX_PULL_DISTANCE = 108

const categories = ['全部', '数码', '家居', '服饰', '文具', '图书', '运动', '美妆', '厨具']
const sortOptions = [
  {
    value: 'featured',
    label: '综合推荐',
    description: '每次刷新随机换序',
    icon: Sparkles,
  },
  {
    value: 'popular',
    label: '销量优先',
    description: '先看大家都在买',
    icon: Flame,
  },
  {
    value: 'price-asc',
    label: '价格从低到高',
    description: '优先控制预算',
    icon: ArrowUpNarrowWide,
  },
  {
    value: 'price-desc',
    label: '价格从高到低',
    description: '先看高价款',
    icon: ArrowDownWideNarrow,
  },
]

const selectedSort = computed(
  () => sortOptions.find((option) => option.value === sortMode.value) ?? sortOptions[0],
)

const pullReady = computed(() => pullDistance.value >= PULL_THRESHOLD)
const pullLabel = computed(() => {
  if (isRefreshing.value) return '正在刷新精选'
  if (refreshCompleted.value) return '精选已更新'
  if (pullReady.value) return '松开刷新'
  return '下拉刷新'
})
const pullIndicatorStyle = computed(() => ({
  opacity: isRefreshing.value ? 1 : Math.min(1, pullDistance.value / 22),
  transform: `translate3d(-50%, ${Math.min(pullDistance.value, 82)}px, 0)`,
}))

const featuredOrderLookup = computed(
  () => new Map(featuredOrder.value.map((productId, index) => [productId, index])),
)

const featuredHeroProducts = computed(() =>
  [...onlineProducts.value].sort((a, b) => b.sold - a.sold).slice(0, 6),
)

const currentHeroProduct = computed(
  () => featuredHeroProducts.value[heroIndex.value] ?? featuredHeroProducts.value[0],
)

const merchantProducts = computed(() => {
  let items = onlineProducts.value.filter((product) => {
    const activeMerchant = state.merchants.find(
      (merchant) => merchant.id === product.merchantId,
    )
    return activeMerchant?.status === 'active'
  })

  if (category.value !== '全部') {
    items = items.filter((product) => product.category === category.value)
  }
  if (merchantFilter.value !== 'all') {
    items = items.filter((product) => product.merchantId === merchantFilter.value)
  }
  if (search.value.trim()) {
    const keyword = search.value.trim().toLowerCase()
    items = items.filter(
      (product) =>
        product.name.toLowerCase().includes(keyword) ||
        product.description.toLowerCase().includes(keyword) ||
        product.tags.some((tag) => tag.toLowerCase().includes(keyword)),
    )
  }

  if (sortMode.value === 'price-asc') {
    return [...items].sort((a, b) => a.price - b.price)
  }
  if (sortMode.value === 'price-desc') {
    return [...items].sort((a, b) => b.price - a.price)
  }
  if (sortMode.value === 'popular') {
    return [...items].sort((a, b) => b.sold - a.sold)
  }
  return [...items].sort(
    (a, b) =>
      (featuredOrderLookup.value.get(a.id) ?? Number.MAX_SAFE_INTEGER) -
      (featuredOrderLookup.value.get(b.id) ?? Number.MAX_SAFE_INTEGER),
  )
})

const activeMerchants = computed(() =>
  state.merchants.filter((merchant) => merchant.status === 'active'),
)

const totalSold = computed(() =>
  onlineProducts.value.reduce((sum, product) => sum + product.sold, 0),
)

function selectMerchant(merchantId) {
  const isSelected = merchantFilter.value === merchantId
  merchantFilter.value = isSelected ? 'all' : merchantId
  category.value = '全部'
  search.value = ''
  sortMode.value = 'featured'
  scrollToProducts()
}

function showAllProducts() {
  merchantFilter.value = 'all'
  category.value = '全部'
  search.value = ''
  sortMode.value = 'featured'
  scrollToProducts()
}

function scrollToProducts() {
  nextTick(() => {
    document.querySelector('#products-section')?.scrollIntoView({ behavior: 'smooth' })
  })
}

function toggleSortMenu() {
  sortOpen.value = !sortOpen.value
  if (sortOpen.value) {
    nextTick(() => {
      sortControl.value?.querySelector('[role="option"]')?.focus()
    })
  }
}

function selectSort(value) {
  sortMode.value = value
  sortOpen.value = false
}

function handleSortKeydown(event) {
  if (event.key === 'Escape') {
    sortOpen.value = false
    sortControl.value?.querySelector('.sort-trigger')?.focus()
    return
  }
  if (!sortOpen.value || !['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
    return
  }

  event.preventDefault()
  const options = [...sortControl.value.querySelectorAll('[role="option"]')]
  const currentIndex = options.indexOf(document.activeElement)
  let nextIndex = 0

  if (event.key === 'End') nextIndex = options.length - 1
  else if (event.key === 'ArrowUp') nextIndex = currentIndex <= 0 ? options.length - 1 : currentIndex - 1
  else if (event.key === 'ArrowDown') nextIndex = currentIndex >= options.length - 1 ? 0 : currentIndex + 1

  options[nextIndex]?.focus()
}

function handleDocumentPointerDown(event) {
  if (!sortControl.value?.contains(event.target)) sortOpen.value = false
}

function moveHero(direction) {
  const total = featuredHeroProducts.value.length
  if (!total) return
  heroIndex.value = (heroIndex.value + direction + total) % total
}

function randomizeFeaturedOrder() {
  const previousOrder = featuredOrder.value.join(',')
  const next = onlineProducts.value.map((product) => product.id)
  for (let index = next.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(Math.random() * (index + 1))
    ;[next[index], next[swapIndex]] = [next[swapIndex], next[index]]
  }
  if (next.length > 1 && next.join(',') === previousOrder) {
    ;[next[0], next[1]] = [next[1], next[0]]
  }
  featuredOrder.value = next
  heroIndex.value = 0
}

function delay(duration) {
  return new Promise((resolve) => window.setTimeout(resolve, duration))
}

async function refreshFeaturedProducts() {
  if (isRefreshing.value) return
  isRefreshing.value = true
  refreshCompleted.value = false
  pullDistance.value = 58
  try {
    await Promise.all([
      session.loggedIn ? refreshBootstrap() : initializeStorefront(true),
      delay(650),
    ])
    randomizeFeaturedOrder()
    refreshCompleted.value = true
    showToast('商品已刷新')
    await delay(420)
  } catch (error) {
    showToast(error.message, 'warning')
  } finally {
    isRefreshing.value = false
    pullDistance.value = 0
    window.setTimeout(() => {
      refreshCompleted.value = false
    }, 500)
  }
}

let pullTracking = false
let pullStartY = 0

function handleTouchStart(event) {
  if (window.scrollY > 0 || isRefreshing.value || event.touches.length !== 1) {
    pullTracking = false
    return
  }
  pullTracking = true
  pullStartY = event.touches[0].clientY
}

function handleTouchMove(event) {
  if (!pullTracking || event.touches.length !== 1) return
  if (window.scrollY > 0) {
    pullTracking = false
    pullDistance.value = 0
    return
  }

  const delta = event.touches[0].clientY - pullStartY
  if (delta <= 0) {
    pullDistance.value = 0
    return
  }

  if (event.cancelable) event.preventDefault()
  pullDistance.value = Math.min(MAX_PULL_DISTANCE, delta * 0.46)
}

function handleTouchEnd() {
  if (!pullTracking) return
  pullTracking = false
  if (pullReady.value) {
    refreshFeaturedProducts()
  } else {
    pullDistance.value = 0
  }
}

function cancelPull() {
  pullTracking = false
  if (!isRefreshing.value) pullDistance.value = 0
}

function openHeroProduct() {
  if (currentHeroProduct.value) {
    router.push(`/product/${currentHeroProduct.value.id}`)
  }
}

let heroTimer

onMounted(() => {
  randomizeFeaturedOrder()
  document.addEventListener('pointerdown', handleDocumentPointerDown)
  window.addEventListener('touchstart', handleTouchStart, { passive: true })
  window.addEventListener('touchmove', handleTouchMove, { passive: false })
  window.addEventListener('touchend', handleTouchEnd)
  window.addEventListener('touchcancel', cancelPull)
  if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    heroTimer = window.setInterval(() => {
      if (!heroPaused.value) moveHero(1)
    }, 5500)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleDocumentPointerDown)
  window.removeEventListener('touchstart', handleTouchStart)
  window.removeEventListener('touchmove', handleTouchMove)
  window.removeEventListener('touchend', handleTouchEnd)
  window.removeEventListener('touchcancel', cancelPull)
  window.clearInterval(heroTimer)
})
</script>

<template>
  <main class="storefront">
    <div
      class="pull-refresh-indicator"
      :class="{
        'is-ready': pullReady,
        'is-refreshing': isRefreshing,
        'is-complete': refreshCompleted,
      }"
      :style="pullIndicatorStyle"
      role="status"
      aria-live="polite"
    >
      <RefreshCw :size="17" />
      <span>{{ pullLabel }}</span>
    </div>

    <section
      class="market-hero"
      @mouseenter="heroPaused = true"
      @mouseleave="heroPaused = false"
      @focusin="heroPaused = true"
      @focusout="heroPaused = false"
    >
      <Transition name="hero-fade">
        <img
          v-if="currentHeroProduct"
          :key="currentHeroProduct.id"
          class="market-hero__image"
          :src="currentHeroProduct.image"
          :alt="currentHeroProduct.name"
        />
      </Transition>
      <div class="market-hero__veil" />
      <div class="market-hero__content page-width">
        <div class="hero-copy">
          <span class="hero-kicker">MULTI-MERCHANT MARKET</span>
          <h1>云商集</h1>
          <p>把不同商家的好物放进同一段浏览路径，选得更清楚，也买得更从容。</p>
          <div class="hero-actions">
            <button class="button button--light" type="button" @click="scrollToProducts">
              开始选购
              <ArrowRight :size="18" />
            </button>
            <button
              class="button button--ghost-light"
              type="button"
              @click="selectMerchant('m1')"
            >
              发现商家
            </button>
          </div>
        </div>
        <div class="hero-footer">
          <div v-if="currentHeroProduct" class="hero-carousel">
            <div class="hero-carousel__product">
              <span class="hero-carousel__rank">
                热销 TOP {{ String(heroIndex + 1).padStart(2, '0') }} /
                {{ String(featuredHeroProducts.length).padStart(2, '0') }}
                · {{ currentHeroProduct.category }}
              </span>
              <button class="hero-carousel__link" type="button" @click="openHeroProduct">
                <span>
                  <strong>{{ currentHeroProduct.name }}</strong>
                  <small>
                    {{ merchantName(currentHeroProduct.merchantId) }}
                    · {{ formatCurrency(currentHeroProduct.price) }}
                    · 已售 {{ currentHeroProduct.sold.toLocaleString('zh-CN') }}
                  </small>
                </span>
                <ArrowUpRight :size="21" />
              </button>
            </div>

            <div class="hero-carousel__controls" aria-label="热销商品轮播控制">
              <button type="button" aria-label="上一个热销商品" @click="moveHero(-1)">
                <ChevronLeft :size="18" />
              </button>
              <div class="hero-carousel__dots">
                <button
                  v-for="(item, index) in featuredHeroProducts"
                  :key="item.id"
                  type="button"
                  :class="{ active: heroIndex === index }"
                  :aria-label="`查看热销商品 ${index + 1}`"
                  @click="heroIndex = index"
                />
              </div>
              <button type="button" aria-label="下一个热销商品" @click="moveHero(1)">
                <ChevronRight :size="18" />
              </button>
            </div>
          </div>

          <div class="hero-numbers">
            <div>
              <span>在售商品</span>
              <strong>{{ onlineProducts.length }}</strong>
            </div>
            <div>
              <span>累计成交</span>
              <strong>{{ totalSold.toLocaleString('zh-CN') }}+</strong>
            </div>
            <div>
              <span>入驻商家</span>
              <strong>{{ activeMerchants.length }}</strong>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="category-ribbon">
      <div class="page-width category-ribbon__inner">
        <span class="section-kicker">SHOP BY CATEGORY</span>
        <div class="category-pills">
          <button
            v-for="item in categories"
            :key="item"
            :class="{ active: category === item }"
            type="button"
            @click="category = item"
          >
            {{ item }}
          </button>
        </div>
      </div>
    </section>

    <section class="merchant-showcase page-width">
      <div class="section-heading">
        <div>
          <span class="section-kicker">FEATURED STORES</span>
          <h2>逛逛不同商家</h2>
        </div>
        <button class="text-button" type="button" @click="showAllProducts">
          查看全部商品
          <ArrowRight :size="16" />
        </button>
      </div>

      <div class="merchant-grid">
        <button
          v-for="merchant in activeMerchants"
          :key="merchant.id"
          class="merchant-card"
          :class="{ active: merchantFilter === merchant.id }"
          type="button"
          @click="selectMerchant(merchant.id)"
        >
          <img :src="merchant.banner" :alt="merchant.name" />
          <span class="merchant-card__overlay" />
          <span class="merchant-card__content">
            <span class="merchant-card__top">
              <Store :size="18" />
              <span>{{ merchant.location }}</span>
            </span>
            <span class="merchant-card__bottom">
              <strong>{{ merchant.name }}</strong>
              <span>
                <Star :size="14" fill="currentColor" />
                {{ merchant.rating }}
              </span>
            </span>
          </span>
        </button>
      </div>
    </section>

    <section id="products-section" class="product-section page-width">
      <div class="section-heading section-heading--products">
        <div>
          <span class="section-kicker">CURATED FOR YOU</span>
          <h2>本周精选</h2>
          <p>来自 {{ activeMerchants.length }} 家店铺的当季好物。</p>
        </div>
        <div class="product-tools">
          <label class="search-field">
            <Search :size="17" />
            <input
              v-model="search"
              type="search"
              placeholder="搜索商品、商家或特点"
              aria-label="搜索商品"
            />
          </label>
          <div
            ref="sortControl"
            class="sort-control"
            :class="{ 'is-open': sortOpen }"
            @keydown="handleSortKeydown"
          >
            <button
              class="sort-trigger"
              type="button"
              aria-haspopup="listbox"
              :aria-expanded="sortOpen"
              @click="toggleSortMenu"
            >
              <span class="sort-trigger__icon">
                <TrendingUp :size="16" />
              </span>
              <span class="sort-trigger__copy">
                <small>排序方式</small>
                <strong>{{ selectedSort.label }}</strong>
              </span>
              <ChevronDown class="sort-trigger__chevron" :size="15" />
            </button>

            <Transition name="sort-menu">
              <div v-if="sortOpen" class="sort-menu" role="listbox" aria-label="商品排序">
                <button
                  v-for="option in sortOptions"
                  :key="option.value"
                  class="sort-option"
                  :class="{ 'is-selected': sortMode === option.value }"
                  type="button"
                  role="option"
                  :aria-selected="sortMode === option.value"
                  @click="selectSort(option.value)"
                >
                  <span class="sort-option__icon">
                    <component :is="option.icon" :size="15" />
                  </span>
                  <span class="sort-option__copy">
                    <strong>{{ option.label }}</strong>
                    <small>{{ option.description }}</small>
                  </span>
                  <Check class="sort-option__check" :size="15" />
                </button>
              </div>
            </Transition>
          </div>
        </div>
      </div>

      <div class="product-result-line">
        <span>{{ merchantProducts.length }} 件商品</span>
        <div class="product-result-actions">
          <span v-if="merchantFilter !== 'all'">
            {{ state.merchants.find((item) => item.id === merchantFilter)?.name }}
          </span>
          <button
            class="refresh-products-button"
            type="button"
            :disabled="isRefreshing"
            title="刷新精选商品"
            aria-label="刷新精选商品"
            @click="refreshFeaturedProducts"
          >
            <RefreshCw :size="14" />
            刷新
          </button>
        </div>
      </div>

      <TransitionGroup
        v-if="merchantProducts.length"
        name="product-shuffle"
        tag="div"
        class="product-grid"
      >
        <ProductCard
          v-for="product in merchantProducts"
          :key="product.id"
          :product="product"
        />
      </TransitionGroup>
      <div v-else class="empty-state">
        <Search :size="28" />
        <h3>没有找到匹配商品</h3>
        <p>换一个关键词，或者清除当前商家筛选。</p>
        <button
          class="button button--dark"
          type="button"
          @click="search = ''; category = '全部'; merchantFilter = 'all'"
        >
          清除筛选
        </button>
      </div>
    </section>

    <section class="store-promise">
      <div class="page-width store-promise__inner">
        <div>
          <span class="section-kicker">MARKETPLACE NOTE</span>
          <h2>一次浏览，发现不同店铺的选品逻辑</h2>
        </div>
        <div class="promise-grid">
          <div>
            <strong>多店聚合</strong>
            <p>商品、店铺和价格放在同一视图里比较。</p>
          </div>
          <div>
            <strong>透明价格</strong>
            <p>商家改价后，商城与购物车同步更新。</p>
          </div>
          <div>
            <strong>平台管理</strong>
            <p>商家、商品和用户状态统一受平台控制。</p>
          </div>
        </div>
      </div>
    </section>

    <footer class="store-footer page-width">
      <div class="brand-lockup">
        <span class="brand-mark"><Store :size="20" /></span>
        <span>
          <strong>云商集</strong>
          <small>多商家电商运营平台原型</small>
        </span>
      </div>
      <span>{{ formatCurrency(totalSold * 128) }} 累计交易展示额</span>
    </footer>
  </main>
</template>
