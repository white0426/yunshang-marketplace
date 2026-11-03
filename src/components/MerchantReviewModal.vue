<script setup>
import { ShieldCheck, Store, X } from 'lucide-vue-next'
import { ref } from 'vue'
import { reviewMerchant } from '../store'

const props = defineProps({
  merchant: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['close', 'reviewed'])
const decision = ref('APPROVE')
const note = ref('')
const submitting = ref(false)
const errorMessage = ref('')

async function submit() {
  if (submitting.value) return
  if (decision.value === 'REJECT' && !note.value.trim()) {
    errorMessage.value = '驳回商家时必须填写审核说明'
    return
  }
  submitting.value = true
  errorMessage.value = ''
  const result = await reviewMerchant(props.merchant.id, {
    decision: decision.value,
    note: note.value.trim(),
  })
  submitting.value = false
  if (!result) {
    errorMessage.value = '审核提交失败，请刷新后重试'
    return
  }
  emit('reviewed')
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop" @click.self="emit('close')">
      <section
        class="form-modal merchant-review-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="merchant-review-title"
      >
        <header class="form-modal__header">
          <div>
            <span class="section-kicker">MERCHANT APPLICATION</span>
            <h2 id="merchant-review-title">商家入驻审核</h2>
          </div>
          <button
            class="icon-button"
            type="button"
            aria-label="关闭审核窗口"
            @click="emit('close')"
          >
            <X :size="19" />
          </button>
        </header>

        <div class="merchant-review-summary">
          <span><Store :size="23" /></span>
          <div>
            <strong>{{ merchant.name }}</strong>
            <small>{{ merchant.owner }} · {{ merchant.location }}</small>
            <p>{{ merchant.description }}</p>
          </div>
        </div>

        <form class="merchant-review-form" @submit.prevent="submit">
          <div class="merchant-review-decision">
            <button
              type="button"
              :class="{ active: decision === 'APPROVE' }"
              @click="decision = 'APPROVE'"
            >
              <ShieldCheck :size="18" />
              审核通过
            </button>
            <button
              type="button"
              :class="{ active: decision === 'REJECT' }"
              @click="decision = 'REJECT'"
            >
              <X :size="18" />
              驳回申请
            </button>
          </div>
          <label>
            <span>审核说明</span>
            <textarea
              v-model.trim="note"
              maxlength="500"
              rows="4"
              :placeholder="decision === 'APPROVE' ? '可填写审核备注' : '请说明驳回原因'"
            />
          </label>
          <p v-if="errorMessage" class="login-error" role="alert">{{ errorMessage }}</p>
          <button class="button button--dark" type="submit" :disabled="submitting">
            {{ submitting ? '正在提交...' : '提交审核结果' }}
          </button>
        </form>
      </section>
    </div>
  </Teleport>
</template>
