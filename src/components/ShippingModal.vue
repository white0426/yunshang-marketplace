<script setup>
import { Truck, X } from 'lucide-vue-next'
import { reactive, ref } from 'vue'
import { shipOrder } from '../store'

const props = defineProps({
  order: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['close', 'shipped'])
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({
  shippingCompany: '顺丰速运',
})
const courierCodes = {
  顺丰速运: 'SF',
  圆通速递: 'YT',
  中通快递: 'ZT',
  韵达快递: 'YD',
  京东物流: 'JD',
  邮政快递: 'YZ',
}

async function submit() {
  if (submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  const result = await shipOrder(props.order.id, form)
  submitting.value = false
  if (!result) {
    errorMessage.value = '发货失败，请确认订单仍处于待发货状态'
    return
  }
  emit('shipped', result)
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop" @click.self="emit('close')">
      <section
        class="form-modal shipping-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="shipping-title"
      >
        <header class="form-modal__header">
          <div>
            <span class="section-kicker">ORDER FULFILLMENT</span>
            <h2 id="shipping-title">确认发货</h2>
            <p>订单号 {{ order.id }}</p>
          </div>
          <button
            class="icon-button"
            type="button"
            aria-label="关闭发货窗口"
            @click="emit('close')"
          >
            <X :size="19" />
          </button>
        </header>
        <form class="shipping-form" @submit.prevent="submit">
          <label>
            <span>物流公司</span>
            <select v-model="form.shippingCompany">
              <option value="顺丰速运">顺丰速运</option>
              <option value="中通快递">中通快递</option>
              <option value="圆通速递">圆通速递</option>
              <option value="韵达快递">韵达快递</option>
              <option value="京东物流">京东物流</option>
              <option value="邮政快递">邮政快递</option>
            </select>
          </label>
          <div class="auto-tracking-preview">
            <span class="section-kicker">AUTO TRACKING NUMBER</span>
            <strong>{{ courierCodes[form.shippingCompany] }}••••••••••••••••••••••</strong>
            <p>确认发货后自动生成 24 位物流单号，前两位标识物流公司。</p>
          </div>
          <p v-if="errorMessage" class="login-error" role="alert">{{ errorMessage }}</p>
          <button class="button button--dark" type="submit" :disabled="submitting">
            <Truck :size="17" />
            {{ submitting ? '正在提交...' : '确认发货' }}
          </button>
        </form>
      </section>
    </div>
  </Teleport>
</template>
