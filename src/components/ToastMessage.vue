<script setup>
import { Check, CircleAlert } from 'lucide-vue-next'
import { useToast } from '../store'

const toast = useToast()
</script>

<template>
  <Teleport to="body">
    <transition name="toast">
      <div
        v-if="toast.visible"
        class="toast-message"
        :class="`toast-message--${toast.tone}`"
        role="status"
      >
        <CircleAlert v-if="toast.tone === 'warning'" :size="18" />
        <Check v-else :size="18" />
        <span>{{ toast.message }}</span>
      </div>
    </transition>
  </Teleport>
</template>
