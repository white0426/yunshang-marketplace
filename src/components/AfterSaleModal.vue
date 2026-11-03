<script setup>
import { RotateCcw, X } from 'lucide-vue-next'
import { reactive, ref } from 'vue'
import { createAfterSale } from '../store'

const props = defineProps({
  order: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['close', 'saved'])
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({
  requestType: '仅退款',
  reason: '',
  description: '',
})

async function submit() {
  if (submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  const result = await createAfterSale(props.order.id, form)
  submitting.value = false
  if (!result) {
    errorMessage.value = '售后申请提交失败，请确认订单状态'
    return
  }
  emit('saved')
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop" @click.self="emit('close')">
      <section
        class="form-modal after-sale-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="after-sale-title"
      >
        <header class="form-modal__header">
          <div>
            <span class="section-kicker">AFTER-SALES SERVICE</span>
            <h2 id="after-sale-title">申请售后</h2>
            <p>订单号 {{ order.id }}</p>
          </div>
          <button
            class="icon-button"
            type="button"
            aria-label="关闭售后窗口"
            @click="emit('close')"
          >
            <X :size="19" />
          </button>
        </header>
        <form class="after-sale-form" @submit.prevent="submit">
          <label>
            <span>售后类型</span>
            <select v-model="form.requestType">
              <option value="仅退款">仅退款</option>
              <option value="退货退款">退货退款</option>
            </select>
          </label>
          <label>
            <span>售后原因</span>
            <input
              v-model.trim="form.reason"
              type="text"
              maxlength="300"
              placeholder="例如：商品与描述不符"
              required
            />
          </label>
          <label>
            <span>问题描述</span>
            <textarea
              v-model.trim="form.description"
              maxlength="500"
              rows="4"
              placeholder="请补充商品问题或退款原因"
            />
          </label>
          <p v-if="errorMessage" class="login-error" role="alert">{{ errorMessage }}</p>
          <button class="button button--dark" type="submit" :disabled="submitting">
            <RotateCcw :size="17" />
            {{ submitting ? '正在提交...' : '提交售后申请' }}
          </button>
        </form>
      </section>
    </div>
  </Teleport>
</template>
