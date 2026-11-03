<script setup>
import { CheckCircle2, PackageCheck, ShoppingBag } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPayment, formatCurrency } from '../store'

const route = useRoute()
const router = useRouter()
const payment = ref(null)
const errorMessage = ref('')

onMounted(async () => {
  try {
    payment.value = await fetchPayment(route.params.paymentNo)
  } catch (error) {
    errorMessage.value = error.message
  }
})
</script>

<template>
  <main class="payment-complete-page">
    <section class="payment-complete-card">
      <div class="payment-complete-icon">
        <CheckCircle2 :size="52" />
      </div>
      <span class="section-kicker">PAYMENT COMPLETE</span>
      <h1>订单支付完成</h1>
      <p v-if="payment">
        {{ payment.orderIds.length }} 笔商家订单已进入待发货流程。
      </p>
      <div v-if="payment" class="payment-complete-amount">
        <span>支付金额</span>
        <strong>{{ formatCurrency(payment.amount) }}</strong>
      </div>
      <div v-if="payment" class="payment-complete-meta">
        <span>{{ payment.paymentNo }}</span>
        <span>{{ payment.paidAt }}</span>
      </div>
      <p v-if="errorMessage" class="payment-error">{{ errorMessage }}</p>
      <div class="payment-complete-actions">
        <button class="button button--dark" type="button" @click="router.push('/profile')">
          <PackageCheck :size="17" />
          查看订单
        </button>
        <button class="button button--outline" type="button" @click="router.push('/')">
          <ShoppingBag :size="17" />
          继续购物
        </button>
      </div>
    </section>
  </main>
</template>
