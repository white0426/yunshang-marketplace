<script setup>
import {
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
  MapPin,
  MessageCircle,
  PackageCheck,
  Pencil,
  Plus,
  RotateCcw,
  ShoppingBag,
  Sparkles,
  Trash2,
  UserRound,
  X,
} from 'lucide-vue-next'
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PaymentQrModal from '../components/PaymentQrModal.vue'
import AddressFormModal from '../components/AddressFormModal.vue'
import ProductCard from '../components/ProductCard.vue'
import {
  addToCart,
  cancelOrder,
  confirmReceipt,
  currentUser,
  formatCurrency,
  onlineProducts,
  productById,
  removeAddress,
  session,
  setDefaultAddress,
  state,
  updateUserProfile,
  userOrders,
} from '../store'

const router = useRouter()
const activeOrderStatus = ref('all')
const orderPage = ref(1)
const profileModalOpen = ref(false)
const addressModalOpen = ref(false)
const editingAddress = ref(null)
const resumePaymentNo = ref('')
const editing = ref(false)
const form = reactive({
  name: currentUser.value.name,
  phone: currentUser.value.phone,
  city: currentUser.value.city,
})
const ORDERS_PER_PAGE = 5

const totalSpend = computed(() =>
  userOrders.value
    .filter((order) => !['待支付', '已取消'].includes(order.status))
    .reduce((sum, order) => sum + order.total, 0),
)

const userAfterSales = computed(() =>
  state.afterSales.filter((item) => item.userId === session.userId),
)
const afterSaleByOrder = computed(
  () => new Map(userAfterSales.value.map((item) => [item.orderId, item])),
)

const orderStatusTabs = computed(() => [
  { id: 'all', label: '全部', count: userOrders.value.length },
  {
    id: '待支付',
    label: '待支付',
    count: userOrders.value.filter((order) => order.status === '待支付').length,
  },
  {
    id: '待发货',
    label: '待发货',
    count: userOrders.value.filter((order) => order.status === '待发货').length,
  },
  {
    id: '待收货',
    label: '待收货',
    count: userOrders.value.filter((order) => order.status === '待收货').length,
  },
  {
    id: 'after-sales',
    label: '退款/售后',
    count: userAfterSales.value.length,
  },
])

const filteredUserOrders = computed(() => {
  if (activeOrderStatus.value === 'all') return userOrders.value
  if (activeOrderStatus.value === 'after-sales') {
    const orderIds = new Set(userAfterSales.value.map((item) => item.orderId))
    return userOrders.value.filter((order) => orderIds.has(order.id))
  }
  return userOrders.value.filter((order) => order.status === activeOrderStatus.value)
})

const totalOrderPages = computed(() =>
  Math.max(1, Math.ceil(filteredUserOrders.value.length / ORDERS_PER_PAGE)),
)

const pagedUserOrders = computed(() => {
  const start = (orderPage.value - 1) * ORDERS_PER_PAGE
  return filteredUserOrders.value.slice(start, start + ORDERS_PER_PAGE)
})

const orderPageNumbers = computed(() =>
  Array.from({ length: totalOrderPages.value }, (_, index) => index + 1),
)

watch(totalOrderPages, (pages) => {
  if (orderPage.value > pages) orderPage.value = pages
})

const recommendedProducts = computed(() => {
  const purchasedProductIds = new Set(
    userOrders.value.flatMap((order) => order.items.map((item) => item.productId)),
  )
  const categoryAffinity = new Map()
  const merchantAffinity = new Map()

  userOrders.value.forEach((order) => {
    merchantAffinity.set(
      order.merchantId,
      (merchantAffinity.get(order.merchantId) ?? 0) + 1,
    )
    order.items.forEach((item) => {
      const product = productById(item.productId)
      if (!product) return
      categoryAffinity.set(
        product.category,
        (categoryAffinity.get(product.category) ?? 0) + item.quantity,
      )
    })
  })

  return [...onlineProducts.value]
    .filter((product) => {
      const merchant = state.merchants.find(
        (candidate) => candidate.id === product.merchantId,
      )
      return merchant?.status === 'active'
    })
    .map((product) => ({
      product,
      score:
        (categoryAffinity.get(product.category) ?? 0) * 1000 +
        (merchantAffinity.get(product.merchantId) ?? 0) * 180 +
        Number(product.sold) / 100 -
        (purchasedProductIds.has(product.id) ? 2000 : 0),
    }))
    .sort((a, b) => b.score - a.score || a.product.id.localeCompare(b.product.id))
    .slice(0, 4)
    .map(({ product }) => product)
})

const statusIcon = {
  待支付: Clock3,
  待发货: Clock3,
  待收货: PackageCheck,
  已完成: CheckCircle2,
  已取消: RotateCcw,
}

async function saveProfile() {
  await updateUserProfile(form)
  editing.value = false
}

async function buyAgain(order) {
  for (const item of order.items) {
    await addToCart(item.productId, item.quantity)
  }
  router.push('/cart')
}

async function cancelCurrentOrder(order) {
  await cancelOrder(order.id)
}

async function confirmCurrentOrder(order) {
  await confirmReceipt(order.id)
}

function contactMerchant(order) {
  router.push({
    name: 'support',
    query: { merchant: order.merchantId },
  })
}

function openNewAddress() {
  editingAddress.value = null
  addressModalOpen.value = true
}

function openEditAddress(address) {
  editingAddress.value = address
  addressModalOpen.value = true
}

function closeAddressModal() {
  addressModalOpen.value = false
  editingAddress.value = null
}

function addressSaved() {
  closeAddressModal()
}

async function makeDefaultAddress(addressId) {
  await setDefaultAddress(addressId)
}

async function deleteSavedAddress(addressId) {
  await removeAddress(addressId)
}

function selectOrderStatus(status) {
  activeOrderStatus.value = status
  orderPage.value = 1
}

function goToOrderPage(page) {
  orderPage.value = Math.min(Math.max(page, 1), totalOrderPages.value)
}

function continuePayment(order) {
  resumePaymentNo.value = order.paymentNo
}

function closeResumePayment() {
  resumePaymentNo.value = ''
}

function finishResumePayment() {
  const paymentNo = resumePaymentNo.value
  resumePaymentNo.value = ''
  router.push({
    name: 'payment-complete',
    params: { paymentNo },
  })
}

function openProfileModal() {
  editing.value = false
  profileModalOpen.value = true
}

function closeProfileModal() {
  profileModalOpen.value = false
  editing.value = false
}
</script>

<template>
  <main class="profile-page page-width">
    <section
      class="profile-banner"
      role="button"
      tabindex="0"
      aria-label="查看个人信息"
      @click="openProfileModal"
      @keydown.enter.prevent="openProfileModal"
      @keydown.space.prevent="openProfileModal"
    >
      <div class="profile-avatar">{{ currentUser.avatar }}</div>
      <div class="profile-intro">
        <span class="section-kicker">MEMBER PROFILE</span>
        <h1>{{ currentUser.name }}</h1>
        <p>
          <MapPin :size="15" />
          {{ currentUser.city }} · {{ currentUser.level }}
        </p>
      </div>
      <div class="profile-stats">
        <div>
          <span>累计订单</span>
          <strong>{{ userOrders.length }}</strong>
        </div>
        <div>
          <span>累计消费</span>
          <strong>{{ formatCurrency(totalSpend) }}</strong>
        </div>
        <div>
          <span>会员加入</span>
          <strong>{{ currentUser.joinedAt }}</strong>
        </div>
      </div>
      <span class="profile-banner__hint">
        <UserRound :size="14" />
        个人信息
        <ChevronRight :size="14" />
      </span>
    </section>

    <section id="addresses" class="profile-addresses">
      <div class="section-heading address-heading">
        <div>
          <span class="section-kicker">DELIVERY ADDRESSES</span>
          <h2>收货地址</h2>
          <p>下单时可直接选择默认地址。</p>
        </div>
        <button class="button button--outline button--compact" type="button" @click="openNewAddress">
          <Plus :size="15" />
          新增地址
        </button>
      </div>

      <div v-if="state.addresses.length" class="address-book-grid">
        <article
          v-for="address in state.addresses"
          :key="address.id"
          class="address-book-card"
          :class="{ 'is-default': address.defaultAddress }"
        >
          <header>
            <span>
              <strong>{{ address.receiverName }}</strong>
              <small>{{ address.receiverPhone }}</small>
            </span>
            <em v-if="address.defaultAddress">默认地址</em>
          </header>
          <p><MapPin :size="15" /> {{ address.fullAddress }}</p>
          <footer>
            <button
              v-if="!address.defaultAddress"
              class="text-button"
              type="button"
              @click="makeDefaultAddress(address.id)"
            >
              设为默认
            </button>
            <button class="text-button" type="button" @click="openEditAddress(address)">
              <Pencil :size="13" />
              编辑
            </button>
            <button class="text-button danger-text" type="button" @click="deleteSavedAddress(address.id)">
              <Trash2 :size="13" />
              删除
            </button>
          </footer>
        </article>
      </div>
      <button v-else class="empty-state address-empty" type="button" @click="openNewAddress">
        <MapPin :size="28" />
        <h3>还没有收货地址</h3>
        <p>添加地址后，下单时可以直接选择。</p>
      </button>
    </section>

    <section class="profile-content">
      <div class="section-heading">
        <div>
          <span class="section-kicker">ORDER HISTORY</span>
          <h2>我的订单</h2>
        </div>
        <span>
          {{ filteredUserOrders.length }} 笔记录
          <template v-if="totalOrderPages > 1"> · {{ orderPage }}/{{ totalOrderPages }} 页</template>
        </span>
      </div>

      <div class="order-status-tabs" role="tablist" aria-label="订单状态">
        <button
          v-for="tab in orderStatusTabs"
          :key="tab.id"
          type="button"
          role="tab"
          :aria-selected="activeOrderStatus === tab.id"
          :class="{ active: activeOrderStatus === tab.id }"
          @click="selectOrderStatus(tab.id)"
        >
          {{ tab.label }}
          <strong>{{ tab.count }}</strong>
        </button>
      </div>

      <div v-if="filteredUserOrders.length" class="order-list">
        <article
          v-for="order in pagedUserOrders"
          :key="order.id"
          class="order-row order-row--clickable"
          role="button"
          tabindex="0"
          :aria-label="`查看订单 ${order.id} 详情`"
          @click="router.push(`/order/${order.id}`)"
          @keydown.enter="router.push(`/order/${order.id}`)"
          @keydown.space.prevent="router.push(`/order/${order.id}`)"
        >
          <header>
            <div>
              <strong>{{ state.merchants.find((m) => m.id === order.merchantId)?.name }}</strong>
              <span>{{ order.id }}</span>
            </div>
            <span class="status-chip" :class="`status-chip--${order.status}`">
              <component :is="statusIcon[order.status] ?? Clock3" :size="14" />
              {{ order.status }}
            </span>
            <span
              v-if="afterSaleByOrder.get(order.id)"
              class="after-sale-inline-status"
              :class="`is-${afterSaleByOrder.get(order.id).status}`"
            >
              {{ afterSaleByOrder.get(order.id).status }}
            </span>
          </header>
          <div class="order-row__body">
            <div class="order-products">
              <div v-for="item in order.items" :key="item.productId" class="order-product">
                <img
                  :src="productById(item.productId)?.image"
                  :alt="productById(item.productId)?.name"
                />
                <div>
                  <strong>{{ productById(item.productId)?.name }}</strong>
                  <span>{{ item.quantity }} 件 · {{ formatCurrency(item.price) }}</span>
                </div>
              </div>
            </div>
            <div class="order-row__total">
              <span>{{ order.createdAt }}</span>
              <strong>{{ formatCurrency(order.total) }}</strong>
              <button class="text-button" type="button" @click.stop="buyAgain(order)">
                <RotateCcw :size="14" />
                再次购买
              </button>
              <button
                v-if="order.status === '待支付' && order.paymentNo"
                class="text-button"
                type="button"
                @click.stop="continuePayment(order)"
              >
                <Clock3 :size="14" />
                继续支付
              </button>
              <button
                v-if="order.status === '待发货'"
                class="text-button"
                type="button"
                @click.stop="cancelCurrentOrder(order)"
              >
                取消订单
              </button>
              <button
                v-if="order.status === '待收货'"
                class="text-button"
                type="button"
                @click.stop="confirmCurrentOrder(order)"
              >
                确认收货
              </button>
              <button
                v-if="afterSaleByOrder.get(order.id)"
                class="text-button"
                type="button"
                @click.stop="contactMerchant(order)"
              >
                <MessageCircle :size="14" />
                联系客服
              </button>
            </div>
          </div>
        </article>
      </div>
      <div v-else class="empty-state">
        <ShoppingBag :size="30" />
        <h3>
          {{
            activeOrderStatus === 'after-sales'
              ? '暂无退款或售后记录'
              : activeOrderStatus === '待支付'
                ? '暂无待支付订单'
                : '当前状态下没有订单'
          }}
        </h3>
        <p>
          {{
            activeOrderStatus === 'after-sales'
              ? '已取消的订单会显示在这里，也可以联系店铺客服处理售后。'
              : '切换其他订单状态，或者继续去商城选购。'
          }}
        </p>
        <button class="button button--dark" type="button" @click="router.push('/')">
          去逛商城
        </button>
      </div>

      <nav
        v-if="filteredUserOrders.length && totalOrderPages > 1"
        class="order-pagination profile-order-pagination"
        aria-label="订单列表分页"
      >
        <span>每页最多 {{ ORDERS_PER_PAGE }} 笔</span>
        <div>
          <button
            type="button"
            aria-label="上一页订单"
            :disabled="orderPage === 1"
            @click="goToOrderPage(orderPage - 1)"
          >
            <ChevronLeft :size="15" />
          </button>
          <button
            v-for="page in orderPageNumbers"
            :key="page"
            type="button"
            :class="{ active: orderPage === page }"
            :aria-current="orderPage === page ? 'page' : undefined"
            :aria-label="`第 ${page} 页订单`"
            @click="goToOrderPage(page)"
          >
            {{ page }}
          </button>
          <button
            type="button"
            aria-label="下一页订单"
            :disabled="orderPage === totalOrderPages"
            @click="goToOrderPage(orderPage + 1)"
          >
            <ChevronRight :size="15" />
          </button>
        </div>
      </nav>
    </section>

    <section v-if="recommendedProducts.length" class="profile-recommendations">
      <div class="section-heading recommendation-heading">
        <div>
          <span class="section-kicker">PICKED FOR YOU</span>
          <h2>猜你喜欢</h2>
          <p>根据你的近期订单和店铺偏好挑选。</p>
        </div>
        <button class="text-button" type="button" @click="router.push('/')">
          <Sparkles :size="16" />
          去商城逛逛
        </button>
      </div>
      <div class="product-grid">
        <ProductCard
          v-for="product in recommendedProducts"
          :key="product.id"
          :product="product"
        />
      </div>
    </section>

    <Teleport to="body">
      <transition name="modal">
        <div
          v-if="profileModalOpen"
          class="modal-backdrop"
          @click.self="closeProfileModal"
        >
          <section
            class="form-modal profile-info-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="profile-info-title"
          >
            <header class="form-modal__header">
              <div>
                <span class="section-kicker">ACCOUNT DETAILS</span>
                <h2 id="profile-info-title">个人信息</h2>
              </div>
              <button
                class="icon-button"
                type="button"
                aria-label="关闭个人信息"
                @click="closeProfileModal"
              >
                <X :size="20" />
              </button>
            </header>

            <div class="account-layout profile-info-modal__content">
              <div class="account-overview">
                <div class="account-avatar">{{ currentUser.avatar }}</div>
                <h2>{{ currentUser.name }}</h2>
                <p>{{ currentUser.email }}</p>
                <span class="member-badge">{{ currentUser.level }}</span>
              </div>
              <form class="account-form" @submit.prevent="saveProfile">
                <div class="form-section-title">
                  <div>
                    <span class="section-kicker">MEMBER ACCOUNT</span>
                    <h2>账户资料</h2>
                  </div>
                  <button
                    class="text-button"
                    type="button"
                    @click="editing = !editing"
                  >
                    <Pencil :size="15" />
                    {{ editing ? '取消编辑' : '编辑资料' }}
                  </button>
                </div>
                <label>
                  <span>姓名</span>
                  <input v-model="form.name" type="text" :disabled="!editing" />
                </label>
                <label>
                  <span>手机号</span>
                  <input v-model="form.phone" type="text" :disabled="!editing" />
                </label>
                <label>
                  <span>所在城市</span>
                  <input v-model="form.city" type="text" :disabled="!editing" />
                </label>
                <label>
                  <span>登录邮箱</span>
                  <input :value="currentUser.email" type="email" disabled />
                </label>
                <button v-if="editing" class="button button--dark" type="submit">
                  保存资料
                </button>
              </form>
            </div>
          </section>
        </div>
      </transition>
    </Teleport>

    <PaymentQrModal
      v-if="resumePaymentNo"
      :payment-no="resumePaymentNo"
      @close="closeResumePayment"
      @paid="finishResumePayment"
    />
    <AddressFormModal
      v-if="addressModalOpen"
      :address="editingAddress"
      @close="closeAddressModal"
      @saved="addressSaved"
    />
  </main>
</template>
