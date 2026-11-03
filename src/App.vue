<script setup>
import { useRoute } from 'vue-router'
import StoreHeader from './components/StoreHeader.vue'
import ToastMessage from './components/ToastMessage.vue'

const route = useRoute()
</script>

<template>
  <div class="app-shell">
    <StoreHeader v-if="route.meta.layout === 'store'" />
    <router-view v-slot="{ Component }">
      <transition name="page" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
    <ToastMessage />
  </div>
</template>
