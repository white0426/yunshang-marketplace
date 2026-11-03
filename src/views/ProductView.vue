<script setup>
import {
  ArrowLeft,
  Check,
  ChevronRight,
  MessageSquareText,
  MessageCircle,
  Minus,
  Plus,
  ShieldCheck,
  ShoppingBag,
  Star,
  Store,
  Truck,
} from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductCard from '../components/ProductCard.vue'
import ReviewModal from '../components/ReviewModal.vue'
import {
  addToCart,
  formatCurrency,
  initializeStorefront,
  onlineProducts,
  productById,
  refreshBootstrap,
  session,
  state,
  userOrders,
} from '../store'

const route = useRoute()
const router = useRouter()
const quantity = ref(1)
const reviewModalOpen = ref(false)

const product = computed(() => productById(route.params.id))
const merchant = computed(() =>
  state.merchants.find((item) => item.id === product.value?.merchantId),
)
const merchantProductCount = computed(
  () =>
    onlineProducts.value.filter(
      (item) => item.merchantId === product.value?.merchantId,
    ).length,
)
const relatedProducts = computed(() =>
  onlineProducts.value
    .filter(
      (item) =>
        item.id !== product.value?.id &&
        (item.category === product.value?.category ||
          item.merchantId === product.value?.merchantId),
    )
    .slice(0, 4),
)
const productReviews = computed(() =>
  state.reviews.filter(
    (review) => review.reviewType === 'PRODUCT' && review.productId === product.value?.id,
  ),
)
const averageRating = computed(() => {
  if (!productReviews.value.length) return '0.0'
  return (
    productReviews.value.reduce((sum, review) => sum + review.rating, 0) /
    productReviews.value.length
  ).toFixed(1)
})
const canReviewProduct = computed(
  () =>
    session.loggedIn &&
    session.role === 'user' &&
    userOrders.value.some(
      (order) =>
        order.status === '已完成' &&
        order.items.some((item) => item.productId === product.value?.id),
    ) &&
    !productReviews.value.some((review) => review.userId === session.userId),
)

async function addToBag() {
  if (!session.loggedIn) {
    requestLogin('add')
    return
  }
  await addToCart(product.value.id, quantity.value)
}

async function buyNow() {
  if (!session.loggedIn) {
    requestLogin('buy')
    return
  }
  const added = await addToCart(product.value.id, quantity.value)
  if (added) router.push('/cart')
}

function requestLogin(action) {
  router.replace({
    name: route.name,
    params: route.params,
    query: {
      ...route.query,
      login: '1',
      redirect: route.fullPath,
      action,
      productId: product.value.id,
      quantity: quantity.value,
    },
  })
}

function openMerchant() {
  if (merchant.value) {
    router.push({ name: 'shop', params: { id: merchant.value.id } })
  }
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
  <main v-if="product" class="product-page page-width">
    <button class="back-link" type="button" @click="router.back()">
      <ArrowLeft :size="16" />
      返回商城
    </button>

    <section class="product-detail">
      <div class="product-detail__media">
        <img :src="product.image" :alt="product.name" />
        <div class="media-caption">
          <span>{{ product.category }} / {{ merchant.name }}</span>
          <span>{{ product.sold }} 人已购买</span>
        </div>
      </div>

      <div class="product-detail__info">
        <div class="product-detail__eyebrow">
          <button class="product-merchant-link" type="button" @click="openMerchant">
            {{ merchant.name }}
          </button>
          <span class="rating-inline">
            <Star :size="14" fill="currentColor" />
            {{ merchant.rating }}
          </span>
        </div>
        <h1>{{ product.name }}</h1>
        <p class="product-detail__description">{{ product.description }}</p>
        <div class="tag-row">
          <span v-for="tag in product.tags" :key="tag" class="soft-tag">{{ tag }}</span>
        </div>

        <div class="product-detail__price">
          <strong>{{ formatCurrency(product.price) }}</strong>
          <del v-if="product.originalPrice > product.price">
            {{ formatCurrency(product.originalPrice) }}
          </del>
          <span>含税价</span>
        </div>

        <div class="quantity-row">
          <span>数量</span>
          <div class="quantity-stepper">
            <button
              type="button"
              aria-label="减少数量"
              @click="quantity = Math.max(1, quantity - 1)"
            >
              <Minus :size="16" />
            </button>
            <input v-model.number="quantity" type="number" min="1" :max="product.stock" />
            <button
              type="button"
              aria-label="增加数量"
              @click="quantity = Math.min(product.stock, quantity + 1)"
            >
              <Plus :size="16" />
            </button>
          </div>
          <span class="stock-note">库存 {{ product.stock }} 件</span>
        </div>

        <div class="product-detail__actions">
          <button class="button button--outline" type="button" @click="addToBag">
            <ShoppingBag :size="18" />
            加入购物车
          </button>
          <button class="button button--dark" type="button" @click="buyNow">
            立即购买
            <ChevronRight :size="18" />
          </button>
        </div>

        <div class="service-list">
          <span><Truck :size="17" /> 商家 48 小时内发货</span>
          <span><ShieldCheck :size="17" /> 平台交易数据保障</span>
          <span><Check :size="17" /> 7 天无理由退换</span>
        </div>

        <button class="merchant-inline" type="button" @click="openMerchant">
          <span class="merchant-inline__logo"><Store :size="21" /></span>
          <span>
            <small>{{ merchantProductCount }} 件在售 · 进入店铺</small>
            <strong>{{ merchant.name }}</strong>
          </span>
          <ChevronRight :size="18" />
        </button>
        <button class="product-support-link" type="button" @click="openSupport">
          <MessageCircle :size="16" />
          联系商家客服
        </button>
      </div>
    </section>

    <section class="product-reviews-section">
      <div class="section-heading product-reviews-heading">
        <div>
          <span class="section-kicker">CUSTOMER REVIEWS</span>
          <h2>商品评价</h2>
          <p>来自真实消费者的购买与使用反馈。</p>
        </div>
        <button
          v-if="canReviewProduct"
          class="button button--outline button--compact"
          type="button"
          @click="reviewModalOpen = true"
        >
          <MessageSquareText :size="16" />
          写评价
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
          <small>{{ productReviews.length }} 条评价</small>
        </div>
        <div v-if="productReviews.length" class="review-list">
          <article v-for="review in productReviews" :key="review.id" class="review-item">
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
          <span>暂无商品评价，完成购买后可以发布第一条评价。</span>
        </div>
      </div>
    </section>

    <section v-if="relatedProducts.length" class="related-products">
      <div class="section-heading">
        <div>
          <span class="section-kicker">MORE TO DISCOVER</span>
          <h2>你可能也会喜欢</h2>
        </div>
      </div>
      <div class="product-grid">
        <ProductCard
          v-for="item in relatedProducts"
          :key="item.id"
          :product="item"
        />
      </div>
    </section>

    <ReviewModal
      v-if="reviewModalOpen"
      type="product"
      :target-id="product.id"
      :target-name="product.name"
      @close="reviewModalOpen = false"
      @saved="reviewModalOpen = false"
    />
  </main>

  <main v-else class="not-found page-width">
    <h1>商品不存在</h1>
    <p>该商品可能已经被删除。</p>
    <button class="button button--dark" type="button" @click="router.push('/')">
      返回商城
    </button>
  </main>
</template>
