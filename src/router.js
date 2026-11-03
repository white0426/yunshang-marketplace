import { createRouter, createWebHistory } from 'vue-router'
import HomeView from './views/HomeView.vue'
import ProductView from './views/ProductView.vue'
import StoreView from './views/StoreView.vue'
import SupportView from './views/SupportView.vue'
import CartView from './views/CartView.vue'
import ProfileView from './views/ProfileView.vue'
import OrderDetailView from './views/OrderDetailView.vue'
import ScanPayView from './views/ScanPayView.vue'
import PaymentCompleteView from './views/PaymentCompleteView.vue'
import MerchantDashboard from './views/MerchantDashboard.vue'
import AdminDashboard from './views/AdminDashboard.vue'
import {
  initializeStore,
  initializeStorefront,
  roleHomePath,
  session,
} from './store'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      redirect: (to) => ({
        name: 'home',
        query: {
          ...to.query,
          login: '1',
        },
      }),
    },
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { layout: 'store', storefront: true },
    },
    {
      path: '/product/:id',
      name: 'product',
      component: ProductView,
      meta: { layout: 'store', storefront: true },
    },
    {
      path: '/shop/:id',
      name: 'shop',
      component: StoreView,
      meta: { layout: 'store', storefront: true },
    },
    {
      path: '/support',
      name: 'support',
      component: SupportView,
      meta: { layout: 'store', role: 'user', requiresAuth: true },
    },
    {
      path: '/cart',
      name: 'cart',
      component: CartView,
      meta: { layout: 'store', role: 'user', requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: ProfileView,
      meta: { layout: 'store', role: 'user', requiresAuth: true },
    },
    {
      path: '/order/:id',
      name: 'order-detail',
      component: OrderDetailView,
      meta: { layout: 'store', role: 'user', requiresAuth: true },
    },
    {
      path: '/pay/:paymentNo',
      name: 'scan-payment',
      component: ScanPayView,
      meta: { layout: 'payment' },
    },
    {
      path: '/payment-complete/:paymentNo',
      name: 'payment-complete',
      component: PaymentCompleteView,
      meta: { layout: 'payment', role: 'user', requiresAuth: true },
    },
    {
      path: '/merchant',
      name: 'merchant',
      component: MerchantDashboard,
      meta: { layout: 'dashboard', role: 'merchant', requiresAuth: true },
    },
    {
      path: '/admin',
      name: 'admin',
      component: AdminDashboard,
      meta: { layout: 'dashboard', role: 'admin', requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: () => (session.loggedIn ? roleHomePath(session.role) : '/'),
    },
  ],
  scrollBehavior(to) {
    if (to.hash) {
      return { el: to.hash, top: 88, behavior: 'smooth' }
    }
    return { top: 0, behavior: 'smooth' }
  },
})

router.beforeEach(async (to) => {
  if (session.token && !session.loggedIn) {
    await initializeStore()
  }
  if (to.meta.storefront) {
    await initializeStorefront()
  }
  if (to.meta.requiresAuth && !session.loggedIn) {
    const current = router.currentRoute.value
    if (current.name && !current.meta.requiresAuth) {
      return {
        name: current.name,
        params: current.params,
        query: {
          ...current.query,
          login: '1',
          redirect: to.fullPath,
        },
        hash: current.hash,
      }
    }
    return {
      name: 'home',
      query: { login: '1', redirect: to.fullPath },
    }
  }
  if (to.meta.role && session.role !== to.meta.role) {
    return roleHomePath(session.role)
  }
  return true
})

export default router
