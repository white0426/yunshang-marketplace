<script setup>
import {
  Home,
  MapPin,
  Phone,
  Save,
  UserRound,
  X,
} from 'lucide-vue-next'
import { reactive, ref } from 'vue'
import {
  addAddress,
  updateAddress,
} from '../store'

const props = defineProps({
  address: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['close', 'saved'])
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({
  receiverName: props.address?.receiverName || '',
  receiverPhone: props.address?.receiverPhone || '',
  region: props.address?.region || '',
  detail: props.address?.detail || '',
  defaultAddress: props.address?.defaultAddress ?? false,
})

async function save() {
  if (submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  const payload = {
    receiverName: form.receiverName.trim(),
    receiverPhone: form.receiverPhone.trim(),
    region: form.region.trim(),
    detail: form.detail.trim(),
    defaultAddress: form.defaultAddress,
  }
  const result = props.address
    ? await updateAddress(props.address.id, payload)
    : await addAddress(payload)
  submitting.value = false
  if (!result) {
    errorMessage.value = '地址保存失败，请检查填写内容'
    return
  }
  emit('saved', result)
}
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop" @click.self="emit('close')">
      <section
        class="form-modal address-form-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="address-form-title"
      >
        <header class="form-modal__header">
          <div>
            <span class="section-kicker">DELIVERY ADDRESS</span>
            <h2 id="address-form-title">{{ address ? '编辑收货地址' : '新增收货地址' }}</h2>
          </div>
          <button
            class="icon-button"
            type="button"
            aria-label="关闭地址窗口"
            @click="emit('close')"
          >
            <X :size="19" />
          </button>
        </header>

        <form class="login-form address-form" @submit.prevent="save">
          <div class="address-form-row">
            <label>
              <span>收货人</span>
              <div class="login-input">
                <UserRound :size="17" />
                <input
                  v-model.trim="form.receiverName"
                  type="text"
                  maxlength="50"
                  placeholder="请输入收货人姓名"
                  required
                />
              </div>
            </label>
            <label>
              <span>联系电话</span>
              <div class="login-input">
                <Phone :size="17" />
                <input
                  v-model.trim="form.receiverPhone"
                  type="tel"
                  maxlength="30"
                  placeholder="请输入联系电话"
                  required
                />
              </div>
            </label>
          </div>
          <label>
            <span>所在地区</span>
            <div class="login-input">
              <MapPin :size="17" />
              <input
                v-model.trim="form.region"
                type="text"
                maxlength="120"
                placeholder="例如：上海市 浦东新区"
                required
              />
            </div>
          </label>
          <label>
            <span>详细地址</span>
            <div class="login-input login-input--address">
              <Home :size="17" />
              <textarea
                v-model.trim="form.detail"
                maxlength="240"
                rows="3"
                placeholder="街道、门牌号、楼栋和房间号"
                required
              />
            </div>
          </label>
          <label class="address-default-toggle">
            <input v-model="form.defaultAddress" type="checkbox" />
            <span>设为默认收货地址</span>
          </label>
          <p v-if="errorMessage" class="login-error" role="alert">{{ errorMessage }}</p>
          <button class="button button--dark" type="submit" :disabled="submitting">
            <Save :size="17" />
            {{ submitting ? '正在保存...' : '保存地址' }}
          </button>
        </form>
      </section>
    </div>
  </Teleport>
</template>
