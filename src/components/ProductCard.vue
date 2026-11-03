<script setup>
import { ArrowUpRight, Plus } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import {
  addToCart,
  formatCurrency,
  merchantName,
  session,
} from '../store'

const props = defineProps({
  product: {
    type: Object,
    required: true,
  },
})

const router = useRouter()
const route = useRoute()

function openProduct() {
  router.push(`/product/${props.product.id}`)
}

async function add(event) {
  event.stopPropagation()
  if (!session.loggedIn) {
    router.replace({
      name: route.name,
      params: route.params,
      query: {
        ...route.query,
        login: '1',
        redirect: route.fullPath,
        action: 'add',
        productId: props.product.id,
        quantity: 1,
      },
    })
    return
  }
  await addToCart(props.product.id)
}
</script>

<template>
  <article class="product-card" tabindex="0" @click="openProduct" @keydown.enter="openProduct">
    <div class="product-card__media">
      <img :src="product.image" :alt="product.name" loading="lazy" />
      <span v-if="product.originalPrice > product.price" class="sale-badge">
        省 {{ formatCurrency(product.originalPrice - product.price) }}
      </span>
      <button
        class="product-card__add"
        type="button"
        :aria-label="`将${product.name}加入购物车`"
        @click="add"
      >
        <Plus :size="19" :stroke-width="2.3" />
      </button>
    </div>
    <div class="product-card__body">
      <div class="product-card__meta">
        <span>{{ product.category }}</span>
        <span>{{ merchantName(product.merchantId) }}</span>
      </div>
      <h3>{{ product.name }}</h3>
      <p>{{ product.description }}</p>
      <div class="product-card__footer">
        <div>
          <strong>{{ formatCurrency(product.price) }}</strong>
          <del v-if="product.originalPrice > product.price">
            {{ formatCurrency(product.originalPrice) }}
          </del>
        </div>
        <span class="product-card__link">
          查看
          <ArrowUpRight :size="15" />
        </span>
      </div>
    </div>
  </article>
</template>
