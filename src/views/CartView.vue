<script setup>
import {
  ArrowLeft,
  Check,
  LoaderCircle,
  MapPin,
  Minus,
  Plus,
  ShoppingBag,
  Trash2,
} from 'lucide-vue-next'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import AddressFormModal from '../components/AddressFormModal.vue'
import PaymentQrModal from '../components/PaymentQrModal.vue'
import {
  cartItems,
  cartTotal,
  checkout,
  formatCurrency,
  merchantName,
  removeCartItem,
  showToast,
  state,
  updateCartQuantity,
} from '../store'

const router = useRouter()
const currentPaymentNo = ref('')
const submitting = ref(false)
const addressModalOpen = ref(false)
const selectedAddressId = ref(
  state.addresses.find((address) => address.defaultAddress)?.id ??
    state.addresses[0]?.id ??
    null,
)
const selectedAddress = computed(
  () =>
    state.addresses.find((address) => address.id === selectedAddressId.value) ??
    state.addresses.find((address) => address.defaultAddress) ??
    state.addresses[0] ??
    null,
)

async function placeOrder() {
  if (submitting.value) return
  if (!selectedAddress.value) {
    showToast('请先添加并选择收货地址', 'warning')
    return
  }
  submitting.value = true
  try {
    const payment = await checkout({
      receiverName: selectedAddress.value.receiverName,
      receiverPhone: selectedAddress.value.receiverPhone,
      receiverAddress: selectedAddress.value.fullAddress,
    })
    if (payment) currentPaymentNo.value = payment.paymentNo
  } finally {
    submitting.value = false
  }
}

function addressSaved(address) {
  selectedAddressId.value = address.id
  addressModalOpen.value = false
}

function closePayment() {
  currentPaymentNo.value = ''
}

function completePayment() {
  const paymentNo = currentPaymentNo.value
  currentPaymentNo.value = ''
  router.push({
    name: 'payment-complete',
    params: { paymentNo },
  })
}
</script>

<template>
  <main class="cart-page page-width">
    <button class="back-link" type="button" @click="router.push('/')">
      <ArrowLeft :size="16" />
      继续购物
    </button>

    <header class="page-title-row">
      <div>
        <span class="section-kicker">YOUR BAG</span>
        <h1>购物车</h1>
      </div>
      <span>{{ cartItems.length }} 种商品</span>
    </header>

    <div v-if="cartItems.length" class="cart-layout">
      <section class="cart-list">
        <article v-for="item in cartItems" :key="item.productId" class="cart-item">
          <div class="cart-item__merchant">
            <span>{{ merchantName(item.product.merchantId) }}</span>
            <span>{{ item.product.category }}</span>
          </div>
          <div class="cart-item__main">
            <button
              class="cart-item__image"
              type="button"
              @click="router.push(`/product/${item.productId}`)"
            >
              <img :src="item.product.image" :alt="item.product.name" />
            </button>
            <div class="cart-item__copy">
              <h2>{{ item.product.name }}</h2>
              <p>{{ item.product.description }}</p>
              <span class="stock-label">
                <Check :size="14" />
                现货 · 库存 {{ item.product.stock }}
              </span>
            </div>
            <div class="cart-item__controls">
              <strong>{{ formatCurrency(item.product.price) }}</strong>
              <div class="quantity-stepper quantity-stepper--small">
                <button
                  type="button"
                  aria-label="减少数量"
                  @click="updateCartQuantity(item.productId, item.quantity - 1)"
                >
                  <Minus :size="14" />
                </button>
                <input
                  :value="item.quantity"
                  type="number"
                  min="1"
                  :max="item.product.stock"
                  @change="updateCartQuantity(item.productId, $event.target.value)"
                />
                <button
                  type="button"
                  aria-label="增加数量"
                  @click="updateCartQuantity(item.productId, item.quantity + 1)"
                >
                  <Plus :size="14" />
                </button>
              </div>
              <button
                class="remove-button"
                type="button"
                @click="removeCartItem(item.productId)"
              >
                <Trash2 :size="15" />
                移除
              </button>
            </div>
          </div>
        </article>
      </section>

      <aside class="cart-summary">
        <span class="section-kicker">ORDER SUMMARY</span>
        <h2>订单汇总</h2>
        <div class="summary-row">
          <span>商品件数</span>
          <strong>{{ cartItems.reduce((sum, item) => sum + item.quantity, 0) }} 件</strong>
        </div>
        <div class="summary-row">
          <span>配送</span>
          <strong>商家承担</strong>
        </div>
        <div class="summary-row">
          <span>优惠</span>
          <strong>{{ formatCurrency(0) }}</strong>
        </div>
        <div class="summary-total">
          <span>应付合计</span>
          <strong>{{ formatCurrency(cartTotal) }}</strong>
        </div>
        <div class="checkout-address">
          <div class="checkout-address__heading">
            <span><MapPin :size="14" /> 收货地址</span>
            <button type="button" @click="addressModalOpen = true">
              <Plus :size="13" />
              新增
            </button>
          </div>
          <div v-if="state.addresses.length" class="checkout-address__list">
            <label
              v-for="address in state.addresses"
              :key="address.id"
              :class="{ active: selectedAddressId === address.id }"
            >
              <input v-model="selectedAddressId" type="radio" :value="address.id" />
              <span>
                <strong>{{ address.receiverName }} · {{ address.receiverPhone }}</strong>
                <small>{{ address.fullAddress }}</small>
              </span>
              <em v-if="address.defaultAddress">默认</em>
            </label>
          </div>
          <button
            v-else
            class="checkout-address__empty"
            type="button"
            @click="addressModalOpen = true"
          >
            <MapPin :size="17" />
            添加第一个收货地址
          </button>
          <button
            class="checkout-address__manage"
            type="button"
            @click="router.push('/profile#addresses')"
          >
            管理地址
          </button>
        </div>
        <p>
          下单后将按商家拆分为 {{ new Set(cartItems.map((item) => item.product.merchantId)).size }}
          笔订单。支付成功后进入待发货，未完成支付时保持待支付。
        </p>
        <button
          class="button button--dark button--full"
          type="button"
          :disabled="submitting"
          @click="placeOrder"
        >
          {{ submitting ? '正在创建订单' : '提交订单' }}
          <LoaderCircle v-if="submitting" class="spin" :size="18" />
          <ShoppingBag v-else :size="18" />
        </button>
        <small>演示环境使用模拟收银台，不会产生真实扣款。</small>
      </aside>
    </div>

    <div v-else class="empty-state empty-state--cart">
      <ShoppingBag :size="34" />
      <h2>购物车还是空的</h2>
      <p>回到商城挑选商品，购物车会保存刷新后的状态。</p>
      <button class="button button--dark" type="button" @click="router.push('/')">
        去逛逛
      </button>
    </div>

    <PaymentQrModal
      v-if="currentPaymentNo"
      :payment-no="currentPaymentNo"
      @close="closePayment"
      @paid="completePayment"
    />
    <AddressFormModal
      v-if="addressModalOpen"
      @close="addressModalOpen = false"
      @saved="addressSaved"
    />
  </main>
</template>
