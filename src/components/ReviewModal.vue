<script setup>
import { CheckCircle2, Star, X } from 'lucide-vue-next'
import { computed, ref } from 'vue'
import {
  submitMerchantReview,
  submitProductReview,
} from '../store'

const props = defineProps({
  type: {
    type: String,
    required: true,
  },
  targetId: {
    type: String,
    required: true,
  },
  targetName: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['close', 'saved'])
const rating = ref(5)
const content = ref('')
const submitting = ref(false)
const errorMessage = ref('')
const title = computed(() =>
  props.type === 'merchant' ? '评价这家店铺' : '评价这件商品',
)

async function submit() {
  if (!content.value.trim() || submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  const payload = {
    rating: rating.value,
    content: content.value.trim(),
  }
  const result =
    props.type === 'merchant'
      ? await submitMerchantReview(props.targetId, payload)
      : await submitProductReview(props.targetId, payload)
  submitting.value = false
  if (!result) {
    errorMessage.value = '评价发布失败，请确认订单已完成'
    return
  }
  emit('saved')
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop" @click.self="emit('close')">
      <section
        class="form-modal review-form-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="review-form-title"
      >
        <header class="form-modal__header">
          <div>
            <span class="section-kicker">VERIFIED REVIEW</span>
            <h2 id="review-form-title">{{ title }}</h2>
            <p>{{ targetName }}</p>
          </div>
          <button
            class="icon-button"
            type="button"
            aria-label="关闭评价窗口"
            @click="emit('close')"
          >
            <X :size="19" />
          </button>
        </header>

        <form class="review-form" @submit.prevent="submit">
          <fieldset class="review-rating">
            <legend>满意度</legend>
            <div>
              <button
                v-for="index in 5"
                :key="index"
                type="button"
                :class="{ active: index <= rating }"
                :aria-label="`${index} 星`"
                @click="rating = index"
              >
                <Star :size="25" fill="currentColor" />
              </button>
              <strong>{{ rating }}.0</strong>
            </div>
          </fieldset>
          <label>
            <span>评价内容</span>
            <textarea
              v-model="content"
              maxlength="500"
              rows="5"
              placeholder="分享商品质量、使用感受或店铺服务体验"
              required
            />
          </label>
          <div class="review-verified-note">
            <CheckCircle2 :size="16" />
            该评价会标记为已购验证
          </div>
          <p v-if="errorMessage" class="login-error" role="alert">{{ errorMessage }}</p>
          <button class="button button--dark" type="submit" :disabled="submitting">
            {{ submitting ? '正在发布...' : '发布评价' }}
          </button>
        </form>
      </section>
    </div>
  </Teleport>
</template>
