<script setup>
import {
  CheckCircle2,
  Clock3,
  LoaderCircle,
  QrCode,
  RefreshCw,
  ShieldCheck,
  Smartphone,
  X,
} from 'lucide-vue-next'
import QRCode from 'qrcode'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  fetchPayment,
  formatCurrency,
  payPayment,
  renewPayment,
  refreshBootstrap,
  showToast,
} from '../store'

const props = defineProps({
  paymentNo: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['close', 'paid'])

const payment = ref(null)
const qrDataUrl = ref('')
const loading = ref(true)
const paying = ref(false)
const renewing = ref(false)
const errorMessage = ref('')
const remainingSeconds = ref(0)
const paidHandled = ref(false)

let pollTimer
let countdownTimer

const paymentBase = import.meta.env.VITE_PAYMENT_BASE || window.location.origin
const paymentUrl = computed(
  () => `${paymentBase.replace(/\/$/, '')}/pay/${props.paymentNo}`,
)
const isPending = computed(() => payment.value?.status === 'PENDING')
const isExpired = computed(() => payment.value?.status === 'EXPIRED')
const isPaid = computed(() => payment.value?.status === 'PAID')
const countdownLabel = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60)
  const seconds = remainingSeconds.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

function updateCountdown() {
  if (!payment.value?.expiresAt) {
    remainingSeconds.value = 0
    return
  }
  const expiresAt = new Date(payment.value.expiresAt.replace(' ', 'T')).getTime()
  remainingSeconds.value = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000))
}

async function loadPayment() {
  try {
    payment.value = await fetchPayment(props.paymentNo)
    errorMessage.value = ''
    updateCountdown()
    if (payment.value.status === 'PAID') {
      await finishPayment()
      return
    }
    if (payment.value.status === 'PENDING' && !qrDataUrl.value) {
      await renderQrCode()
    }
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function renderQrCode() {
  qrDataUrl.value = await QRCode.toDataURL(paymentUrl.value, {
    width: 280,
    margin: 1,
    errorCorrectionLevel: 'M',
    color: {
      dark: '#17201c',
      light: '#ffffff',
    },
  })
}

async function regeneratePayment() {
  if (renewing.value) return
  renewing.value = true
  errorMessage.value = ''
  try {
    payment.value = await renewPayment(props.paymentNo)
    updateCountdown()
    await renderQrCode()
    showToast('支付二维码已重新生成')
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    renewing.value = false
  }
}

async function finishPayment() {
  if (paidHandled.value) return
  paidHandled.value = true
  clearInterval(pollTimer)
  clearInterval(countdownTimer)
  await refreshBootstrap().catch(() => {})
  window.setTimeout(() => emit('paid', payment.value), 700)
}

async function simulatePayment() {
  if (!isPending.value || paying.value) return
  paying.value = true
  try {
    payment.value = await payPayment(props.paymentNo)
    showToast('支付成功，订单已进入待发货')
    await finishPayment()
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    paying.value = false
  }
}

onMounted(() => {
  loadPayment()
  pollTimer = window.setInterval(loadPayment, 2000)
  countdownTimer = window.setInterval(() => {
    updateCountdown()
    if (remainingSeconds.value === 0 && isPending.value) {
      payment.value = { ...payment.value, status: 'EXPIRED' }
    }
  }, 1000)
})

onBeforeUnmount(() => {
  clearInterval(pollTimer)
  clearInterval(countdownTimer)
})
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop payment-backdrop" @click.self="emit('close')">
      <section
        class="payment-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="payment-modal-title"
      >
        <header class="payment-modal__header">
          <div>
            <span class="section-kicker">SECURE PAYMENT</span>
            <h2 id="payment-modal-title">扫码支付</h2>
          </div>
          <button
            class="icon-button"
            type="button"
            aria-label="关闭支付窗口"
            @click="emit('close')"
          >
            <X :size="19" />
          </button>
        </header>

        <div v-if="loading" class="payment-state">
          <LoaderCircle class="spin" :size="30" />
          <strong>正在生成支付二维码</strong>
        </div>

        <div v-else-if="isPaid" class="payment-state payment-state--success">
          <CheckCircle2 :size="46" />
          <strong>支付成功</strong>
          <p>订单已进入待发货，正在前往完成页面。</p>
        </div>

        <div v-else class="payment-modal__body">
          <div class="payment-qr-panel">
            <div v-if="isPending && qrDataUrl" class="payment-qr">
              <img :src="qrDataUrl" alt="扫码支付二维码" />
              <span><QrCode :size="15" /> 使用手机扫码支付</span>
            </div>
            <div v-else class="payment-qr payment-qr--expired">
              <Clock3 :size="34" />
              <strong>二维码已过期</strong>
              <span>订单仍保留为待支付</span>
            </div>
          </div>

          <div class="payment-details">
            <span class="payment-details__label">应付金额</span>
            <strong class="payment-details__amount">
              {{ formatCurrency(payment?.amount) }}
            </strong>
            <div class="payment-details__line">
              <span>支付单号</span>
              <strong>{{ payment?.paymentNo }}</strong>
            </div>
            <div class="payment-details__line">
              <span>订单数量</span>
              <strong>{{ payment?.orderIds?.length || 0 }} 笔</strong>
            </div>
            <div v-if="isPending" class="payment-details__line payment-details__timer">
              <span><Clock3 :size="14" /> 支付剩余时间</span>
              <strong>{{ countdownLabel }}</strong>
            </div>
            <div class="payment-method">
              <Smartphone :size="20" />
              <div>
                <strong>云商集收银台</strong>
                <span>支持微信、支付宝扫码</span>
              </div>
              <ShieldCheck :size="18" />
            </div>
            <p v-if="errorMessage" class="payment-error">{{ errorMessage }}</p>
            <button
              v-if="isPending"
              class="button button--dark button--full"
              type="button"
              :disabled="paying"
              @click="simulatePayment"
            >
              <LoaderCircle v-if="paying" class="spin" :size="17" />
              <Smartphone v-else :size="17" />
              {{ paying ? '正在确认支付' : '模拟扫码支付' }}
            </button>
            <button
              v-else-if="isExpired"
              class="button button--dark button--full"
              type="button"
              :disabled="renewing"
              @click="regeneratePayment"
            >
              <LoaderCircle v-if="renewing" class="spin" :size="17" />
              <RefreshCw v-else :size="17" />
              {{ renewing ? '正在重新生成' : '重新生成支付二维码' }}
            </button>
            <button
              v-else
              class="button button--outline button--full"
              type="button"
              @click="emit('close')"
            >
              返回订单
            </button>
            <small>
              关闭窗口不会取消订单；未完成扫码支付的订单会保持待支付状态。
            </small>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>
