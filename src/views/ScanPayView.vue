<script setup>
import {
  CheckCircle2,
  Clock3,
  LoaderCircle,
  QrCode,
  ShieldCheck,
  ShoppingBag,
} from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchPayment,
  formatCurrency,
  payPayment,
  session,
} from '../store'

const route = useRoute()
const router = useRouter()
const payment = ref(null)
const loading = ref(true)
const paying = ref(false)
const errorMessage = ref('')

const isPaid = computed(() => payment.value?.status === 'PAID')
const isExpired = computed(() => payment.value?.status === 'EXPIRED')

async function loadPayment() {
  try {
    payment.value = await fetchPayment(route.params.paymentNo)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function confirmPayment() {
  if (paying.value || isPaid.value || isExpired.value) return
  paying.value = true
  try {
    payment.value = await payPayment(route.params.paymentNo)
  } catch (error) {
    errorMessage.value = error.message
    await loadPayment()
  } finally {
    paying.value = false
  }
}

function returnToStore() {
  router.push(session.loggedIn ? '/' : '/login')
}

onMounted(loadPayment)
</script>

<template>
  <main class="scan-pay-page">
    <header class="scan-pay-brand">
      <span class="brand-mark"><QrCode :size="19" /></span>
      <div>
        <strong>云商集</strong>
        <small>SECURE CHECKOUT</small>
      </div>
    </header>

    <section class="scan-pay-panel">
      <div v-if="loading" class="payment-state">
        <LoaderCircle class="spin" :size="30" />
        <strong>正在读取支付单</strong>
      </div>

      <template v-else-if="payment">
        <div v-if="isPaid" class="scan-pay-result scan-pay-result--success">
          <CheckCircle2 :size="52" />
          <span class="section-kicker">PAYMENT COMPLETE</span>
          <h1>支付成功</h1>
          <p>订单已提交给商家，电脑端支付页面会自动跳转。</p>
          <div class="scan-pay-amount">{{ formatCurrency(payment.amount) }}</div>
          <button class="button button--dark" type="button" @click="returnToStore">
            <ShoppingBag :size="17" />
            返回商城
          </button>
        </div>

        <template v-else>
          <div class="scan-pay-heading">
            <span class="section-kicker">MERCHANT CHECKOUT</span>
            <h1>{{ isExpired ? '支付已过期' : '确认支付' }}</h1>
            <p>
              {{
                isExpired
                  ? '二维码已超过有效期，订单仍为待支付状态。'
                  : '请核对订单金额后完成支付。'
              }}
            </p>
          </div>

          <div class="scan-pay-amount">{{ formatCurrency(payment.amount) }}</div>
          <div class="scan-pay-meta">
            <span>支付单号</span>
            <strong>{{ payment.paymentNo }}</strong>
          </div>
          <div class="scan-pay-meta">
            <span>商家订单</span>
            <strong>{{ payment.orderIds.length }} 笔</strong>
          </div>
          <div class="scan-pay-meta">
            <span>支付有效期</span>
            <strong>{{ payment.expiresAt }}</strong>
          </div>

          <p v-if="errorMessage" class="payment-error">{{ errorMessage }}</p>
          <button
            v-if="!isExpired"
            class="button button--dark button--full"
            type="button"
            :disabled="paying"
            @click="confirmPayment"
          >
            <LoaderCircle v-if="paying" class="spin" :size="17" />
            <ShieldCheck v-else :size="17" />
            {{ paying ? '支付处理中' : '确认支付' }}
          </button>
          <button
            v-else
            class="button button--outline button--full"
            type="button"
            @click="returnToStore"
          >
            <Clock3 :size="17" />
            返回订单查看
          </button>
        </template>
      </template>

      <div v-else class="payment-state payment-state--error">
        <Clock3 :size="38" />
        <strong>无法读取支付单</strong>
        <p>{{ errorMessage }}</p>
      </div>
    </section>
  </main>
</template>
