<script setup lang="ts">
defineOptions({ name: 'AppointmentView' })

import { ref, nextTick, onMounted, computed } from 'vue'
import type { ChatMessage } from '@/types/api'
import type { AssistantSession } from '@/api/assistant'
import {
  listAssistantSessions,
  createAssistantSession,
  listAssistantMessages,
  deleteAssistantSession,
  assistantChatStream,
} from '@/api/assistant'
import { formatAiMessage } from '@/utils/formatAiMessage'

const SESSION_KEY = 'assistant-active-session-id'

const sessions = ref<AssistantSession[]>([])
const sessionId = ref('')
const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const sessionsLoading = ref(false)
const chatBox = ref<HTMLElement | null>(null)
let abortController: AbortController | null = null

const welcomeMessage = (): ChatMessage => ({
  id: 'welcome',
  role: 'assistant',
  content:
    '你好！我是代码工匠智能助手 👋\n\n我可以帮你：\n\n· 解答 AI 应用开发与平台使用问题\n· 梳理产品需求与技术方案\n· 志愿者政策咨询与服务预约\n· 查询志愿者预约记录\n\n有什么可以帮你的？',
})

const activeSession = computed(() => sessions.value.find((s) => s.id === sessionId.value))

onMounted(async () => {
  await loadSessions()
  const saved = localStorage.getItem(SESSION_KEY)
  if (saved && sessions.value.some((s) => s.id === saved)) {
    await switchSession(saved)
  } else if (sessions.value.length > 0) {
    await switchSession(sessions.value[0].id)
  } else {
    await startNewSession()
  }
})

async function loadSessions() {
  sessionsLoading.value = true
  try {
    sessions.value = await listAssistantSessions()
  } catch {
    sessions.value = []
  } finally {
    sessionsLoading.value = false
  }
}

async function loadMessages(id: string) {
  try {
    const records = await listAssistantMessages(id)
    if (records.length === 0) {
      messages.value = [welcomeMessage()]
      return
    }
    messages.value = records.map((r) => ({
      id: String(r.id),
      role: r.role === 'USER' ? 'user' : 'assistant',
      content: r.role === 'ASSISTANT' ? formatAiMessage(r.content) : r.content,
    }))
  } catch {
    messages.value = [welcomeMessage()]
  }
}

async function switchSession(id: string) {
  if (loading.value) return
  sessionId.value = id
  localStorage.setItem(SESSION_KEY, id)
  await loadMessages(id)
  scrollToBottom()
}

async function startNewSession() {
  const id = `s-${Date.now()}`
  try {
    const created = await createAssistantSession(id, '新对话')
    sessions.value = [created, ...sessions.value.filter((s) => s.id !== created.id)]
    sessionId.value = created.id
  } catch {
    sessionId.value = id
  }
  localStorage.setItem(SESSION_KEY, sessionId.value)
  messages.value = [welcomeMessage()]
}

async function removeSession(id: string, event: Event) {
  event.stopPropagation()
  if (!confirm('确定删除该对话？')) return
  try {
    await deleteAssistantSession(id)
    sessions.value = sessions.value.filter((s) => s.id !== id)
    if (sessionId.value === id) {
      if (sessions.value.length > 0) {
        await switchSession(sessions.value[0].id)
      } else {
        await startNewSession()
      }
    }
  } catch (e) {
    alert(e instanceof Error ? e.message : '删除失败')
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBox.value) {
      chatBox.value.scrollTop = chatBox.value.scrollHeight
    }
  })
}

function formatSessionTime(iso: string) {
  const d = new Date(iso)
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  if (sameDay) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value || !sessionId.value) return

  messages.value.push({ id: `u-${Date.now()}`, role: 'user', content: text })
  input.value = ''
  loading.value = true
  scrollToBottom()

  const assistantId = `a-${Date.now()}`
  messages.value.push({ id: assistantId, role: 'assistant', content: '', streaming: true })
  scrollToBottom()

  try {
    abortController = new AbortController()
    let content = ''
    await assistantChatStream(sessionId.value, text, (chunk) => {
      content += chunk
      const msg = messages.value.find((m) => m.id === assistantId)
      if (msg) {
        msg.content = content
        msg.streaming = true
      }
      scrollToBottom()
    }, abortController.signal)
    const msg = messages.value.find((m) => m.id === assistantId)
    if (msg) {
      msg.content = formatAiMessage(content)
      msg.streaming = false
    }
    await loadSessions()
  } catch (e) {
    const msg = messages.value.find((m) => m.id === assistantId)
    if (msg) {
      msg.content = `出错了：${e instanceof Error ? e.message : '未知错误'}`
      msg.streaming = false
    }
  } finally {
    loading.value = false
    abortController = null
    scrollToBottom()
  }
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}
</script>

<template>
  <div class="page">
    <aside class="history-panel">
      <div class="history-header">
        <h3>历史对话</h3>
        <button class="btn-new" type="button" @click="startNewSession">+ 新对话</button>
      </div>
      <p class="history-hint">保留最近 30 天</p>
      <div v-if="sessionsLoading" class="history-empty">加载中…</div>
      <div v-else-if="sessions.length === 0" class="history-empty">暂无历史，开始新对话吧</div>
      <ul v-else class="session-list">
        <li
          v-for="item in sessions"
          :key="item.id"
          class="session-item"
          :class="{ active: item.id === sessionId }"
          @click="switchSession(item.id)"
        >
          <div class="session-main">
            <span class="session-title">{{ item.title }}</span>
            <span class="session-time">{{ formatSessionTime(item.updatedAt) }}</span>
          </div>
          <button class="btn-del" type="button" title="删除" @click="removeSession(item.id, $event)">×</button>
        </li>
      </ul>
    </aside>

    <section class="chat-panel">
      <header class="page-header">
        <div>
          <h2>代码工匠 · 智能助手</h2>
          <p>{{ activeSession?.title || '通用 AI 对话 · 多轮记忆 · 历史保存' }}</p>
        </div>
      </header>

      <div ref="chatBox" class="chat-box">
        <div v-for="msg in messages" :key="msg.id" class="message" :class="msg.role">
          <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
          <div class="bubble">
            <pre class="content">{{ msg.content }}<span v-if="msg.streaming" class="cursor">▋</span></pre>
          </div>
        </div>
        <div v-if="loading && !messages.some((m) => m.streaming)" class="typing">
          <span></span><span></span><span></span>
        </div>
      </div>

      <div class="input-area">
        <textarea
          v-model="input"
          placeholder="输入消息，Enter 发送，Shift+Enter 换行"
          rows="2"
          :disabled="loading"
          @keydown="onKeydown"
        />
        <button class="btn-send" :disabled="loading || !input.trim()" @click="send">
          {{ loading ? '生成中…' : '发送' }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  height: 100%;
}

.history-panel {
  width: 260px;
  flex-shrink: 0;
  border-right: 1px solid var(--border);
  background: var(--bg-elevated);
  display: flex;
  flex-direction: column;
  padding: 16px 12px;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.history-header h3 {
  font-size: 14px;
  font-weight: 600;
}

.btn-new {
  border: 1px solid var(--border);
  background: transparent;
  color: var(--accent);
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 12px;
  cursor: pointer;
}

.history-hint {
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.history-empty {
  font-size: 13px;
  color: var(--text-muted);
  padding: 12px 8px;
}

.session-list {
  list-style: none;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
}

.session-item:hover {
  background: rgba(255, 255, 255, 0.04);
}

.session-item.active {
  background: var(--accent-soft);
  border-color: var(--accent);
}

.session-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-title {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  font-size: 11px;
  color: var(--text-muted);
}

.btn-del {
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  padding: 0 4px;
  opacity: 0;
}

.session-item:hover .btn-del {
  opacity: 1;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.page-header {
  padding: 20px 28px;
  border-bottom: 1px solid var(--border);
}

.page-header h2 {
  font-size: 18px;
  font-weight: 600;
}

.page-header p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  display: flex;
  gap: 12px;
  max-width: 820px;
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  background: var(--bg-card);
  border: 1px solid var(--border);
}

.message.user .avatar {
  background: var(--user-bubble);
  border-color: transparent;
}

.bubble {
  padding: 14px 18px;
  border-radius: var(--radius);
  background: var(--assistant-bubble);
  border: 1px solid var(--border);
  max-width: 100%;
}

.message.user .bubble {
  background: var(--user-bubble);
  border-color: transparent;
}

.content {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font);
  font-size: 14px;
  line-height: 1.75;
  margin: 0;
}

.cursor {
  animation: blink 1s step-end infinite;
  color: var(--accent);
}

@keyframes blink {
  50% { opacity: 0; }
}

.typing {
  display: flex;
  gap: 4px;
  padding-left: 48px;
}

.typing span {
  width: 8px;
  height: 8px;
  background: var(--text-muted);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.typing span:nth-child(1) { animation-delay: -0.32s; }
.typing span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.input-area {
  display: flex;
  gap: 12px;
  padding: 16px 28px 24px;
  border-top: 1px solid var(--border);
  background: var(--bg-elevated);
}

.input-area textarea {
  flex: 1;
  resize: none;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 12px 16px;
  color: var(--text);
  font-size: 14px;
  line-height: 1.5;
  outline: none;
}

.input-area textarea:focus {
  border-color: var(--accent);
}

.btn-send {
  align-self: flex-end;
  background: var(--accent);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: var(--radius);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  min-width: 96px;
}

.btn-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
