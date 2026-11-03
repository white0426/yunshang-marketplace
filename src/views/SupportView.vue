<script setup>
import {
  ArrowUpRight,
  LoaderCircle,
  MessageCircle,
  Send,
  Store,
} from 'lucide-vue-next'
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  currentUser,
  loadSupportConversations,
  loadSupportMessages,
  sendSupportMessage,
} from '../store'

const route = useRoute()
const router = useRouter()
const conversations = ref([])
const messagesByMerchant = reactive({})
const selectedMerchantId = ref('')
const draft = ref('')
const loading = ref(true)
const sending = ref(false)
const chatBody = ref(null)
let pollTimer

const selectedConversation = computed(() =>
  conversations.value.find((item) => item.merchantId === selectedMerchantId.value),
)
const selectedMessages = computed(
  () => messagesByMerchant[selectedMerchantId.value] ?? [],
)

const quickQuestions = [
  '这款商品现在有货吗？',
  '下单后多久可以发货？',
  '支持退换货吗？',
]

function formatTime(value) {
  if (!value) return ''
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBody.value) chatBody.value.scrollTop = chatBody.value.scrollHeight
  })
}

function selectMerchant(merchantId) {
  selectedMerchantId.value = merchantId
  router.replace({ name: 'support', query: { merchant: merchantId } })
  loadMessages(merchantId)
  scrollToBottom()
}

async function loadMessages(merchantId = selectedMerchantId.value) {
  if (!merchantId) return
  try {
    const detail = await loadSupportMessages(merchantId)
    messagesByMerchant[merchantId] = detail.messages ?? []
    const index = conversations.value.findIndex(
      (item) => item.merchantId === merchantId,
    )
    if (index >= 0 && detail.conversation) {
      conversations.value[index] = detail.conversation
    }
    scrollToBottom()
  } catch (error) {
    console.error('加载客服消息失败', error)
  }
}

async function loadConversations() {
  try {
    const data = await loadSupportConversations()
    conversations.value = data
    const requested = String(route.query.merchant || '')
    if (!selectedMerchantId.value) {
      selectedMerchantId.value = data.some((item) => item.merchantId === requested)
        ? requested
        : data[0]?.merchantId ?? ''
    }
    if (selectedMerchantId.value) {
      await loadMessages(selectedMerchantId.value)
    }
  } catch (error) {
    console.error('加载客服会话失败', error)
  } finally {
    loading.value = false
  }
}

async function sendMessage(text = draft.value) {
  const content = text.trim()
  const merchantId = selectedMerchantId.value
  if (!content || !merchantId || sending.value) return
  sending.value = true
  try {
    const detail = await sendSupportMessage(merchantId, content)
    messagesByMerchant[merchantId] = detail.messages ?? []
    const index = conversations.value.findIndex(
      (item) => item.merchantId === merchantId,
    )
    if (index >= 0 && detail.conversation) {
      conversations.value[index] = detail.conversation
    }
    draft.value = ''
    scrollToBottom()
  } catch (error) {
    console.error('发送客服消息失败', error)
  } finally {
    sending.value = false
  }
}

function handleComposerKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

function goToMerchant() {
  if (selectedConversation.value) {
    router.push({
      name: 'shop',
      params: { id: selectedConversation.value.merchantId },
    })
  }
}

onMounted(() => {
  loadConversations()
  pollTimer = window.setInterval(loadConversations, 3000)
})

watch(
  () => route.query.merchant,
  (merchantId) => {
    if (
      merchantId &&
      conversations.value.some((item) => item.merchantId === merchantId) &&
      selectedMerchantId.value !== merchantId
    ) {
      selectedMerchantId.value = String(merchantId)
      loadMessages(String(merchantId))
    }
  },
)

onBeforeUnmount(() => {
  window.clearInterval(pollTimer)
})
</script>

<template>
  <main class="support-page page-width">
    <header class="support-page__heading">
      <div>
        <span class="section-kicker">MERCHANT SUPPORT</span>
        <h1>联系店家客服</h1>
        <p>选择店铺，咨询商品、库存、发货和售后问题。</p>
      </div>
      <span class="support-page__status">
        <MessageCircle :size="16" />
        {{ conversations.length }} 家店铺可联系
      </span>
    </header>

    <section v-if="!loading && conversations.length" class="support-shell">
      <aside class="support-sidebar">
        <header>
          <div>
            <span class="section-kicker">CONVERSATIONS</span>
            <h2>店铺会话</h2>
          </div>
          <span>{{ conversations.length }}</span>
        </header>
        <div class="support-merchant-list">
          <button
            v-for="conversation in conversations"
            :key="conversation.merchantId"
            type="button"
            :class="{ active: selectedMerchantId === conversation.merchantId }"
            @click="selectMerchant(conversation.merchantId)"
          >
            <img
              :src="conversation.merchantBanner"
              :alt="conversation.merchantName"
            />
            <span class="support-merchant-list__copy">
              <strong>{{ conversation.merchantName }}</strong>
              <small>{{ conversation.lastMessage || '开始咨询这家店铺' }}</small>
            </span>
            <time>{{ conversation.lastMessageAt || '' }}</time>
          </button>
        </div>
      </aside>

      <section v-if="selectedConversation" class="support-chat">
        <header class="support-chat__header">
          <span class="support-chat__logo">
            <Store :size="21" />
          </span>
          <span>
            <strong>{{ selectedConversation.merchantName }}</strong>
            <small><i /> 消息实时同步 · 等待商家回复</small>
          </span>
          <button type="button" @click="goToMerchant">
            进入店铺
            <ArrowUpRight :size="15" />
          </button>
        </header>

        <div ref="chatBody" class="support-chat__body">
          <div
            v-for="message in selectedMessages"
            :key="message.id"
            class="support-message"
            :class="`support-message--${message.senderRole === 'MERCHANT' ? 'merchant' : 'user'}`"
          >
            <span class="support-message__avatar">
              {{
                message.senderRole === 'MERCHANT'
                  ? selectedConversation.merchantName.slice(0, 1)
                  : currentUser.avatar
              }}
            </span>
            <div>
              <p>{{ message.content }}</p>
              <small>{{ formatTime(message.createdAt) }}</small>
            </div>
          </div>
          <div v-if="!selectedMessages.length" class="support-chat__empty">
            <MessageCircle :size="26" />
            <span>还没有消息，发送第一条咨询开始对话。</span>
          </div>
        </div>

        <div class="support-quick-questions">
          <button
            v-for="question in quickQuestions"
            :key="question"
            type="button"
            @click="sendMessage(question)"
          >
            {{ question }}
          </button>
        </div>

        <div class="support-composer">
          <textarea
            v-model="draft"
            rows="2"
            placeholder="输入想咨询的问题，Enter 发送"
            @keydown="handleComposerKeydown"
          />
          <button
            type="button"
            aria-label="发送消息"
            :disabled="!draft.trim() || sending"
            @click="sendMessage()"
          >
            <LoaderCircle v-if="sending" class="spin" :size="18" />
            <Send v-else :size="18" />
          </button>
        </div>
      </section>
    </section>

    <div v-else-if="loading" class="empty-state">
      <LoaderCircle class="spin" :size="30" />
      <h2>正在加载会话</h2>
    </div>

    <div v-else class="empty-state">
      <MessageCircle :size="30" />
      <h2>暂无可联系的店铺</h2>
      <p>商家恢复营业后，就可以在这里发起咨询。</p>
    </div>
  </main>
</template>
