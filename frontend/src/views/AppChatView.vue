<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import type { ChatMessage } from '@/types/api'
import { appChatStream, listChatHistory, clearChatHistory } from '@/api/chat'

const route = useRoute()
const appId = computed(() => Number(route.params.id))
const sessionId = ref(`session-${Date.now()}`)
const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const chatBox = ref<HTMLElement | null>(null)

const appName = computed(() => (route.query.name as string) || `应用 #${appId.value}`)

onMounted(async () => {
  try {
    const page = await listChatHistory(appId.value)
    messages.value = page.records
      .reverse()
      .map((m) => ({
        id: String(m.id),
        role: m.role.toLowerCase() as 'user' | 'assistant',
        content: m.content,
      }))
  } catch {
    // empty history
  }
})

function scrollBottom() {
  requestAnimationFrame(() => {
    if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
  })
}

async function clearHistory() {
  if (!confirm('确定清空该应用的对话历史？')) return
  try {
    await clearChatHistory(appId.value)
    messages.value = []
    sessionId.value = `session-${Date.now()}`
  } catch (e) {
    alert(e instanceof Error ? e.message : '清空失败')
  }
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ id: `u-${Date.now()}`, role: 'user', content: text })
  input.value = ''
  loading.value = true
  scrollBottom()

  const assistantId = `a-${Date.now()}`
  messages.value.push({ id: assistantId, role: 'assistant', content: '', streaming: true })

  try {
    await appChatStream(
      appId.value,
      sessionId.value,
      text,
      (chunk) => {
        const m = messages.value.find((x) => x.id === assistantId)
        if (m) m.content += chunk
        scrollBottom()
      },
    )
  } catch (e) {
    const m = messages.value.find((x) => x.id === assistantId)
    if (m) m.content = e instanceof Error ? e.message : '发送失败'
  } finally {
    const m = messages.value.find((x) => x.id === assistantId)
    if (m) m.streaming = false
    loading.value = false
    scrollBottom()
  }
}
</script>

<template>
  <div class="page">
    <header class="header">
      <RouterLink to="/apps" class="back">← 我的应用</RouterLink>
      <div class="header-row">
        <div>
          <h2>{{ appName }} · AI 助手</h2>
          <p>与应用开发助手对话，获取修改建议与实现思路</p>
        </div>
        <button class="btn-clear" type="button" @click="clearHistory">清空历史</button>
      </div>
    </header>

    <div ref="chatBox" class="chat-box">
      <div v-for="m in messages" :key="m.id" class="msg" :class="m.role">
        <div class="bubble">{{ m.content }}<span v-if="m.streaming" class="cursor">▍</span></div>
      </div>
      <p v-if="messages.length === 0" class="empty">开始与应用 AI 助手对话吧</p>
    </div>

    <footer class="input-bar">
      <textarea
        v-model="input"
        rows="2"
        placeholder="描述你想如何改进这个应用…"
        :disabled="loading"
        @keydown.enter.exact.prevent="send"
      />
      <button :disabled="loading || !input.trim()" @click="send">发送</button>
    </footer>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 900px;
  margin: 0 auto;
}

.header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
}

.back {
  font-size: 13px;
  color: var(--text-muted);
  text-decoration: none;
}

.header h2 {
  font-size: 18px;
  margin-top: 8px;
}

.header p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-top: 8px;
}

.btn-clear {
  padding: 8px 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
}

.btn-clear:hover {
  color: var(--text);
  border-color: var(--text-muted);
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty {
  color: var(--text-muted);
  text-align: center;
  margin-top: 40px;
}

.msg.user {
  align-self: flex-end;
}

.msg.assistant {
  align-self: flex-start;
}

.bubble {
  max-width: 85%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.user .bubble {
  background: var(--accent);
  color: white;
}

.assistant .bubble {
  background: var(--bg-card);
  border: 1px solid var(--border);
}

.cursor {
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.input-bar {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
  background: var(--bg-elevated);
}

.input-bar textarea {
  flex: 1;
  resize: none;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px;
  background: var(--bg-card);
  color: var(--text);
}

.input-bar button {
  padding: 0 24px;
  border: none;
  border-radius: 8px;
  background: var(--accent);
  color: white;
  font-weight: 600;
  cursor: pointer;
}

.input-bar button:disabled {
  opacity: 0.5;
}
</style>
