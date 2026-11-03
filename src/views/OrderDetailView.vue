<script setup>
import {
  ArrowLeft,
  CheckCircle2,
  ChevronRight,
  Clock3,
  Copy,
  MapPin,
  MessageCircle,
  PackageCheck,
  Phone,
  RotateCcw,
  ShieldCheck,
  Store,
  Truck,
  UserRound,
  XCircle,
} from 'lucide-vue-next'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PaymentQrModal from '../components/PaymentQrModal.vue'
import AfterSaleModal from '../components/AfterSaleModal.vue'
import {
  addToCart,
  cancelOrder,
  confirmReceipt,
  formatCurrency,
  productById,
  showToast,
  state,
  userOrders,
} from '../store'

const route = useRoute()
const router = useRouter()
const paymentNo = ref('')
const afterSaleModalOpen = ref(false)
const order = computed(
  () => userOrders.value.find((item) => item.id === route.params.id) ?? null,
)
const merchant = computed(
  () => state.merchants.find((item) => item.id === order.value?.merchantId) ?? null,
)
const orderLines = computed(
  () =>
    order.value?.items.map((item) => ({
      ...item,
      product: productById(item.productId),
    })) ?? [],
)
const subtotal = computed(() =>
  orderLines.value.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0),
)
const afterSale = computed(
  () => state.afterSales.find((item) => item.orderId === order.value?.id) ?? null,
)
const canApplyAfterSale = computed(
  () =>
    ['待发货', '待收货', '已完成'].includes(order.value?.status) &&
    !afterSale.value,
)

const statusMeta = computed(() => {
  const status = order.value?.status
  if (status === '待支付') {
    return {
      title: '等待支付',
      description: '订单尚待支付，请尽快完成付款。',
      icon: Clock3,
      tone: 'pending',
    }
  }
  if (status === '待发货') {
    return {
      title: '等待商家发货',
      description: '订单已支付，商家正在准备商品。',
      icon: PackageCheck,
      tone: 'processing',
    }
  }
  if (status === '待收货') {
    return {
      title: '商家已发货',
      description: '商品正在配送中，请留意收货。',
      icon: Truck,
      tone: 'shipping',
    }
  }
  if (status === '已完成') {
    return {
      title: '交易已完成',
      description: '订单已确认收货，感谢你的购买。',
      icon: CheckCircle2,
      tone: 'complete',
    }
  }
  return {
    title: '订单已取消',
    description: '订单已取消，库存与经营数据已回滚。',
    icon: XCircle,
    tone: 'cancelled',
  }
})

const timeline = computed(() => {
  const status = order.value?.status
  const paid = !['待支付', '已取消'].includes(status)
  const shipped = ['待收货', '已完成'].includes(status)
  return [
    {
      label: '提交订单',
      detail: order.value?.createdAt,
      active: true,
    },
    {
      label: '完成支付',
      detail: paid ? '支付已确认' : '等待完成支付',
      active: paid,
    },
    {
      label: '商家发货',
      detail: shipped
        ? `${order.value?.shippingCompany || '商家'}已揽收${order.value?.shippedAt ? ` · ${order.value.shippedAt}` : ''}`
        : '等待商家安排发货',
      active: shipped,
    },
    {
      label: '确认收货',
      detail: status === '已完成' ? '订单交易完成' : '等待确认收货',
      active: status === '已完成',
    },
  ]
})

async function copyOrderId() {
  try {
    await navigator.clipboard.writeText(order.value.id)
    showToast('订单号已复制')
  } catch {
    showToast('浏览器未允许复制，请手动选择订单号', 'warning')
  }
}

async function copyTrackingNo() {
  try {
    await navigator.clipboard.writeText(order.value.trackingNo)
    showToast('运单号已复制')
  } catch {
    showToast('浏览器未允许复制，请手动选择运单号', 'warning')
  }
}

async function buyAgain() {
  for (const item of order.value.items) {
    await addToCart(item.productId, item.quantity)
  }
  router.push('/cart')
}

async function cancelCurrentOrder() {
  await cancelOrder(order.value.id)
}

async function confirmCurrentOrder() {
  await confirmReceipt(order.value.id)
}

function contactMerchant() {
  router.push({
    name: 'support',
    query: { merchant: order.value.merchantId },
  })
}

function openPayment() {
  paymentNo.value = order.value.paymentNo
}

function finishPayment() {
  paymentNo.value = ''
  showToast('支付成功，订单已进入待发货')
}
</script>

<template>
  <main v-if="order" class="order-detail-page page-width">
    <button class="back-link" type="button" @click="router.push('/profile')">
      <ArrowLeft :size="16" />
      返回我的订单
    </button>

    <section class="order-status-hero" :class="`is-${statusMeta.tone}`">
      <component :is="statusMeta.icon" :size="32" />
      <div>
        <span class="section-kicker">ORDER STATUS</span>
        <h1>{{ statusMeta.title }}</h1>
        <p>{{ statusMeta.description }}</p>
      </div>
      <div class="order-status-hero__meta">
        <span>订单号</span>
        <strong>{{ order.id }}</strong>
        <button type="button" @click="copyOrderId">
          <Copy :size="14" />
          复制
        </button>
      </div>
    </section>

    <section class="order-detail-card order-store-card">
      <div class="order-store-card__identity">
        <span><Store :size="21" /></span>
        <div>
          <strong>{{ merchant?.name || '商家店铺' }}</strong>
          <small>{{ merchant?.location || '商家所在地' }} · 平台认证商家</small>
        </div>
      </div>
      <button
        class="text-button"
        type="button"
        @click="router.push({ name: 'shop', params: { id: order.merchantId } })"
      >
        进入店铺
        <ChevronRight :size="15" />
      </button>
    </section>

    <section class="order-detail-card">
      <header class="order-detail-card__header">
        <div>
          <span class="section-kicker">PRODUCTS</span>
          <h2>商品明细</h2>
        </div>
        <span>共 {{ orderLines.reduce((sum, item) => sum + item.quantity, 0) }} 件</span>
      </header>

      <div class="order-detail-products">
        <button
          v-for="item in orderLines"
          :key="item.productId"
          class="order-detail-product"
          type="button"
          @click="router.push(`/product/${item.productId}`)"
        >
          <img :src="item.product?.image" :alt="item.product?.name || '商品'" />
          <span>
            <strong>{{ item.product?.name || '商品已下架' }}</strong>
            <small>{{ item.product?.category || '历史订单商品' }}</small>
            <em>{{ formatCurrency(item.price) }} × {{ item.quantity }}</em>
          </span>
          <strong>{{ formatCurrency(Number(item.price) * item.quantity) }}</strong>
        </button>
      </div>

      <div class="order-price-summary">
        <div><span>商品总价</span><strong>{{ formatCurrency(subtotal) }}</strong></div>
        <div><span>配送费</span><strong>¥0</strong></div>
        <div><span>优惠</span><strong>-¥0</strong></div>
        <div class="order-price-summary__total">
          <span>实付款</span>
          <strong>{{ formatCurrency(order.total) }}</strong>
        </div>
      </div>
    </section>

    <div class="order-detail-columns">
      <section class="order-detail-card">
        <header class="order-detail-card__header">
          <div>
            <span class="section-kicker">DELIVERY</span>
            <h2>收货信息</h2>
          </div>
          <MapPin :size="18" />
        </header>
        <div class="order-contact-row">
          <UserRound :size="15" />
          <span>{{ order.receiverName }}</span>
        </div>
        <div class="order-contact-row">
          <Phone :size="15" />
          <span>{{ order.receiverPhone }}</span>
        </div>
        <div class="order-contact-row order-contact-row--address">
          <MapPin :size="15" />
          <span>{{ order.receiverAddress }}</span>
        </div>
      </section>

      <section class="order-detail-card">
        <header class="order-detail-card__header">
          <div>
            <span class="section-kicker">ORDER INFO</span>
            <h2>订单信息</h2>
          </div>
          <ShieldCheck :size="18" />
        </header>
        <dl class="order-info-list">
          <div><dt>订单编号</dt><dd>{{ order.id }}</dd></div>
          <div><dt>下单时间</dt><dd>{{ order.createdAt }}</dd></div>
          <div><dt>支付单号</dt><dd>{{ order.paymentNo || '无需支付' }}</dd></div>
          <div><dt>订单状态</dt><dd>{{ order.status }}</dd></div>
        </dl>
      </section>
    </div>

    <section
      v-if="order.shippingCompany || order.trackingNo"
      class="order-detail-card logistics-card"
    >
      <header class="order-detail-card__header">
        <div>
          <span class="section-kicker">DELIVERY TRACKING</span>
          <h2>物流信息</h2>
        </div>
        <Truck :size="19" />
      </header>
      <div class="logistics-info">
        <span class="logistics-info__icon"><Truck :size="24" /></span>
        <div>
          <strong>{{ order.shippingCompany }}</strong>
          <span>运单号：{{ order.trackingNo }}</span>
          <small v-if="order.shippedAt">发货时间：{{ order.shippedAt }}</small>
        </div>
        <button class="table-action" type="button" @click="copyTrackingNo">
          <Copy :size="14" />
          复制运单号
        </button>
      </div>
      <div class="logistics-timeline">
        <span class="active">商家已发货</span>
        <span :class="{ active: order.status === '待收货' }">运输中</span>
        <span :class="{ active: order.status === '已完成' }">已签收</span>
      </div>
    </section>

    <section class="order-detail-card order-timeline-card">
      <header class="order-detail-card__header">
        <div>
          <span class="section-kicker">ORDER PROGRESS</span>
          <h2>订单进度</h2>
        </div>
      </header>
      <div class="order-progress">
        <div
          v-for="step in timeline"
          :key="step.label"
          class="order-progress__step"
          :class="{ active: step.active }"
        >
          <i />
          <strong>{{ step.label }}</strong>
          <span>{{ step.detail }}</span>
        </div>
      </div>
    </section>

    <section v-if="afterSale" class="order-detail-card after-sale-status-card">
      <header class="order-detail-card__header">
        <div>
          <span class="section-kicker">AFTER-SALES SERVICE</span>
          <h2>售后进度</h2>
        </div>
        <span class="after-sale-state" :class="`is-${afterSale.status}`">
          {{ afterSale.status }}
        </span>
      </header>
      <dl class="order-info-list">
        <div><dt>售后类型</dt><dd>{{ afterSale.requestType }}</dd></div>
        <div><dt>申请原因</dt><dd>{{ afterSale.reason }}</dd></div>
        <div v-if="afterSale.description"><dt>问题描述</dt><dd>{{ afterSale.description }}</dd></div>
        <div><dt>申请时间</dt><dd>{{ afterSale.createdAt }}</dd></div>
        <div v-if="afterSale.merchantReply"><dt>商家处理</dt><dd>{{ afterSale.merchantReply }}</dd></div>
      </dl>
    </section>

    <section class="order-detail-actions">
      <button class="button button--outline" type="button" @click="contactMerchant">
        <MessageCircle :size="17" />
        联系商家
      </button>
      <button class="button button--outline" type="button" @click="buyAgain">
        <RotateCcw :size="17" />
        再次购买
      </button>
      <button
        v-if="order.status === '待支付' && order.paymentNo"
        class="button button--dark"
        type="button"
        @click="openPayment"
      >
        继续支付
      </button>
      <button
        v-if="['待支付', '待发货'].includes(order.status)"
        class="button button--outline"
        type="button"
        @click="cancelCurrentOrder"
      >
        取消订单
      </button>
      <button
        v-if="order.status === '待收货'"
        class="button button--dark"
        type="button"
        @click="confirmCurrentOrder"
      >
        <CheckCircle2 :size="17" />
        确认收货
      </button>
      <button
        v-if="canApplyAfterSale"
        class="button button--outline"
        type="button"
        @click="afterSaleModalOpen = true"
      >
        <RotateCcw :size="17" />
        申请售后
      </button>
    </section>

    <PaymentQrModal
      v-if="paymentNo"
      :payment-no="paymentNo"
      @close="paymentNo = ''"
      @paid="finishPayment"
    />
    <AfterSaleModal
      v-if="afterSaleModalOpen"
      :order="order"
      @close="afterSaleModalOpen = false"
      @saved="afterSaleModalOpen = false"
    />
  </main>

  <main v-else class="not-found page-width">
    <h1>订单不存在</h1>
    <p>该订单可能已被删除，或不属于当前账号。</p>
    <button class="button button--dark" type="button" @click="router.push('/profile')">
      返回我的订单
    </button>
  </main>
</template>
