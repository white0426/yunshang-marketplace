<script setup>
import {
  ArrowLeft,
  MapPin,
  MessageCircle,
  MessageSquareText,
  Package,
  Star,
  Store,
  TrendingUp,
} from 'lucide-vue-next'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductCard from '../components/ProductCard.vue'
import ReviewModal from '../components/ReviewModal.vue'
import {
  onlineProducts,
  refreshBootstrap,
  session,
  state,
  userOrders,
  initializeStorefront,
} from '../store'

const route = useRoute()
const router = useRouter()
const activeCategory = ref('全部')
const reviewModalOpen = ref(false)

const merchant = computed(() =>
  state.merchants.find((item) => item.id === route.params.id),
)

const storeProducts = computed(() =>
  onlineProducts.value.filter((product) => product.merchantId === merchant.value?.id),
)

const categories = computed(() => [
  '全部',
  ...new Set(storeProducts.value.map((product) => product.category)),
])

const filteredProducts = computed(() =>
  activeCategory.value === '全部'
    ? storeProducts.value
    : storeProducts.value.filter((product) => product.category === activeCategory.value),
)

const totalSold = computed(() =>
  storeProducts.value.reduce((total, product) => total + product.sold, 0),
)
const merchantReviews = computed(() =>
  state.reviews.filter(
    (review) =>
      review.reviewType === 'MERCHANT' && review.merchantId === merchant.value?.id,
  ),
)
const averageRating = computed(() => {
  if (!merchantReviews.value.length) return '0.0'
  return (
    merchantReviews.value.reduce((sum, review) => sum + review.rating, 0) /
    merchantReviews.value.length
  ).toFixed(1)
})
const canReviewMerchant = computed(
  () =>
    session.loggedIn &&
    session.role === 'user' &&
    userOrders.value.some(
      (order) =>
        order.status === '已完成' && order.merchantId === merchant.value?.id,
    ) &&
    !merchantReviews.value.some((review) => review.userId === session.userId),
)

watch(
  () => route.params.id,
  () => {
    activeCategory.value = '全部'
  },
)

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/')
}

function openSupport() {
  if (merchant.value) {
    router.push({ name: 'support', query: { merchant: merchant.value.id } })
  }
}

onMounted(() => {
  if (state.reviews.length === 0) {
    if (session.loggedIn) refreshBootstrap()
    else initializeStorefront(true)
  }
})
</script>

<template>
  <main v-if="merchant" class="merchant-store page-width">
    <button class="back-link" type="button" @click="goBack">
      <ArrowLeft :size="16" />
      返回上一页
    </button>

    <section class="merchant-store__hero">
      <img :src="merchant.banner" :alt="`${merchant.name}店铺封面`" />
      <div class="merchant-store__veil" />
      <div class="merchant-store__hero-content">
        <span class="merchant-store__kicker">MERCHANT STOREFRONT</span>
        <div class="merchant-store__identity">
          <span class="merchant-store__logo">
            <Store :size="25" />
          </span>
          <div>
            <h1>{{ merchant.name }}</h1>
            <p>{{ merchant.description }}</p>
          </div>
          <span class="merchant-store__rating">
            <Star :size="16" fill="currentColor" />
            {{ merchant.rating }}
          </span>
        </div>
        <div class="merchant-store__meta">
          <span><MapPin :size="15" />{{ merchant.location }}</span>
          <span><Package :size="15" />{{ storeProducts.length }} 件在售</span>
          <span><TrendingUp :size="15" />{{ totalSold.toLocaleString('zh-CN') }} 件已售</span>
          <span>入驻于 {{ merchant.joinedAt }}</span>
        </div>
        <div class="merchant-store__actions">
          <button class="button button--light button--compact" type="button" @click="openSupport">
            <MessageCircle :size="16" />
            联系客服
          </button>
        </div>
      </div>
    </section>

    <section class="merchant-store__catalog">
      <div class="section-heading section-heading--products">
        <div>
          <span class="section-kicker">ALL PRODUCTS</span>
          <h2>店铺在售商品</h2>
          <p>浏览 {{ merchant.name }} 当前上架的全部商品。</p>
        </div>
        <span class="merchant-store__count">{{ filteredProducts.length }} 件商品</span>
      </div>

      <div class="category-pills merchant-store__categories">
        <button
          v-for="categoryName in categories"
          :key="categoryName"
          :class="{ active: activeCategory === categoryName }"
          type="button"
          @click="activeCategory = categoryName"
        >
          {{ categoryName }}
        </button>
      </div>

      <div v-if="filteredProducts.length" class="product-grid">
        <ProductCard
          v-for="product in filteredProducts"
          :key="product.id"
          :product="product"
        />
      </div>

      <div v-else class="empty-state">
        <Package :size="28" />
        <h3>这个分类暂时没有商品</h3>
        <p>切换其他分类，或者返回商城继续浏览。</p>
        <button class="button button--dark" type="button" @click="activeCategory = '全部'">
          查看全部商品
        </button>
      </div>
    </section>

    <section class="merchant-store__reviews">
      <div class="section-heading product-reviews-heading">
        <div>
          <span class="section-kicker">STORE REVIEWS</span>
          <h2>店铺评价</h2>
          <p>看看其他消费者对店铺商品与服务的反馈。</p>
        </div>
        <button
          v-if="canReviewMerchant"
          class="button button--outline button--compact"
          type="button"
          @click="reviewModalOpen = true"
        >
          <MessageSquareText :size="16" />
          评价店铺
        </button>
      </div>

      <div class="review-overview">
        <div class="review-overview__score">
          <strong>{{ averageRating }}</strong>
          <span>
            <Star
              v-for="index in 5"
              :key="index"
              :size="15"
              :fill="index <= Math.round(Number(averageRating)) ? 'currentColor' : 'none'"
            />
          </span>
          <small>{{ merchantReviews.length }} 条评价</small>
        </div>
        <div v-if="merchantReviews.length" class="review-list">
          <article v-for="review in merchantReviews" :key="review.id" class="review-item">
            <span class="review-item__avatar">{{ review.reviewerAvatar || '用' }}</span>
            <div>
              <header>
                <strong>{{ review.reviewerName }}</strong>
                <span v-if="review.verifiedPurchase">已购验证</span>
                <time>{{ review.createdAt }}</time>
              </header>
              <div class="review-item__stars">
                <Star
                  v-for="index in 5"
                  :key="index"
                  :size="12"
                  :fill="index <= review.rating ? 'currentColor' : 'none'"
                />
              </div>
              <p>{{ review.content }}</p>
            </div>
          </article>
        </div>
        <div v-else class="review-empty">
          <MessageSquareText :size="25" />
          <span>暂无店铺评价，完成购买后可以发布第一条评价。</span>
        </div>
      </div>
    </section>

    <ReviewModal
      v-if="reviewModalOpen"
      type="merchant"
      :target-id="merchant.id"
      :target-name="merchant.name"
      @close="reviewModalOpen = false"
      @saved="reviewModalOpen = false"
    />
  </main>

  <main v-else class="not-found page-width">
    <h1>店铺不存在</h1>
    <p>该店铺可能已经停用或不存在。</p>
    <button class="button button--dark" type="button" @click="router.push('/')">
      返回商城
    </button>
  </main>
</template>
