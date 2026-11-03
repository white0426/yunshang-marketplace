<script setup>
import {
  ChevronRight,
  Home,
  Menu,
  MessageCircle,
  ShoppingBag,
  ShoppingCart,
  UserRound,
  X,
} from 'lucide-vue-next'
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoginModal from './LoginModal.vue'
import {
  cartCount,
  currentUser,
  session,
} from '../store'

const router = useRouter()
const route = useRoute()
const menuOpen = ref(false)
const loginModalOpen = ref(false)

function navigate(path) {
  menuOpen.value = false
  router.push(path)
}

function switchPersona() {
  menuOpen.value = false
  loginModalOpen.value = true
  if (route.query.login !== '1') {
    router.replace({
      name: route.name,
      params: route.params,
      query: {
        ...route.query,
        login: '1',
        ...(session.loggedIn ? {} : { redirect: route.fullPath }),
      },
      hash: route.hash,
    })
  }
}

function closeLoginModal() {
  loginModalOpen.value = false
  if (route.query.login === '1') {
    const query = { ...route.query }
    delete query.login
    delete query.redirect
    delete query.action
    delete query.productId
    delete query.quantity
    router.replace({
      name: route.name,
      params: route.params,
      query,
      hash: route.hash,
    })
  }
}

watch(
  () => route.query.login,
  (value) => {
    loginModalOpen.value = value === '1'
  },
  { immediate: true },
)
</script>

<template>
  <header class="store-header">
    <div class="store-header__inner">
      <button class="brand-lockup" type="button" @click="navigate('/')">
        <span class="brand-mark"><ShoppingBag :size="20" :stroke-width="2.2" /></span>
        <span>
          <strong>云商集</strong>
          <small>YUNSHANG MARKET</small>
        </span>
      </button>

      <nav class="store-nav" aria-label="商城导航">
        <button
          :class="{ active: ['home', 'product', 'shop'].includes(route.name) }"
          type="button"
          @click="navigate('/')"
        >
          首页
        </button>
        <button
          :class="{ active: route.name === 'cart' }"
          type="button"
          @click="navigate('/cart')"
        >
          购物车
          <span v-if="cartCount" class="nav-count">{{ cartCount }}</span>
        </button>
        <button
          :class="{ active: route.name === 'support' }"
          type="button"
          @click="navigate('/support')"
        >
          客服
        </button>
        <button
          :class="{ active: route.name === 'profile' }"
          type="button"
          @click="navigate('/profile')"
        >
          我的
        </button>
      </nav>

      <div class="store-actions">
        <button class="persona-button" type="button" @click="switchPersona">
          <span class="persona-avatar">{{ session.loggedIn ? currentUser.avatar : '访' }}</span>
          <span class="persona-copy">
            <strong>{{ session.loggedIn ? currentUser.name : '游客浏览' }}</strong>
            <small>{{ session.loggedIn ? '消费者身份 · 切换账号' : '登录 / 注册' }}</small>
          </span>
          <ChevronRight :size="16" />
        </button>
        <button
          class="icon-button mobile-only"
          type="button"
          aria-label="打开菜单"
          @click="menuOpen = !menuOpen"
        >
          <X v-if="menuOpen" :size="21" />
          <Menu v-else :size="21" />
        </button>
      </div>
    </div>

    <div v-if="menuOpen" class="mobile-menu">
      <button type="button" @click="navigate('/')">
        <Home :size="18" /> 商城首页
      </button>
      <button type="button" @click="navigate('/cart')">
        <ShoppingCart :size="18" /> 购物车
        <span class="nav-count">{{ cartCount }}</span>
      </button>
      <button type="button" @click="navigate('/support')">
        <MessageCircle :size="18" /> 客服消息
      </button>
      <button type="button" @click="navigate('/profile')">
        <UserRound :size="18" /> 个人中心
      </button>
      <button type="button" @click="switchPersona">
        <ShoppingBag :size="18" /> 切换演示身份
      </button>
    </div>
  </header>

  <nav class="mobile-bottom-nav" aria-label="移动端导航">
    <button
      :class="{ active: ['home', 'product', 'shop'].includes(route.name) }"
      type="button"
      @click="navigate('/')"
    >
      <Home :size="20" />
      <span>首页</span>
    </button>
    <button
      :class="{ active: route.name === 'cart' }"
      type="button"
      @click="navigate('/cart')"
    >
      <span class="mobile-cart-icon">
        <ShoppingCart :size="20" />
        <b v-if="cartCount">{{ cartCount }}</b>
      </span>
      <span>购物车</span>
    </button>
    <button
      :class="{ active: route.name === 'support' }"
      type="button"
      @click="navigate('/support')"
    >
      <MessageCircle :size="20" />
      <span>客服</span>
    </button>
    <button
      :class="{ active: route.name === 'profile' }"
      type="button"
      @click="navigate('/profile')"
    >
      <UserRound :size="20" />
      <span>我的</span>
    </button>
    <button type="button" @click="switchPersona">
      <ShoppingBag :size="20" />
      <span>身份</span>
    </button>
  </nav>

  <LoginModal v-if="loginModalOpen" @close="closeLoginModal" />
</template>
