<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import type { ChatMessage } from '@/types/api'
import { chat, chatStream } from '@/api/appointment'
import { formatAiMessage } from '@/utils/formatAiMessage'

const SESSION_KEY = 'chat-session-id'

const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const useStream = ref(true)
const sessionId = ref('')
const chatBox = ref<HTMLElement | null>(null)
let abortController: AbortController | null = null

onMounted(() => {
  const saved = localStorage.getItem(SESSION_KEY)
  sessionId.value = saved || `s-${Date.now()}`
  localStorage.setItem(SESSION_KEY, sessionId.value)

  messages.value.push({
    id: 'welcome',
    role: 'assistant',
    content:
      '你好！我是代码工匠智能助手 👋\n\n我可以和你聊各类话题，例如：\n\n· 知识问答与学习辅导\n· 写作润色与翻译\n· 编程思路与技术讲解\n· 生活建议与计划安排\n\n也支持服务预约与记录查询，有需要时直接告诉我就好。',
  })
})

function scrollToBottom() {
  nextTick(() => {
    if (chatBox.value) {
      chatBox.value.scrollTop = chatBox.value.scrollHeight
    }
  })
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({
    id: `u-${Date.now()}`,
    role: 'user',
    content: text,
  })
  input.value = ''
  loading.value = true
  scrollToBottom()

  const assistantId = `a-${Date.now()}`
  messages.value.push({
    id: assistantId,
    role: 'assistant',
    content: '',
    streaming: useStream.value,
  })
  scrollToBottom()

  try {
    if (useStream.value) {
      abortController = new AbortController()
      let content = ''
      await chatStream(
        sessionId.value,
        text,
        (chunk) => {
          content += chunk
          const msg = messages.value.find((m) => m.id === assistantId)
          if (msg) {
            msg.content = content
            msg.streaming = true
          }
          scrollToBottom()
        },
        abortController.signal,
      )
      const msg = messages.value.find((m) => m.id === assistantId)
      if (msg) {
        msg.content = formatAiMessage(content)
        msg.streaming = false
      }
    } else {
      const reply = await chat(sessionId.value, text)
      const msg = messages.value.find((m) => m.id === assistantId)
      if (msg) msg.content = formatAiMessage(reply)
    }
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

function newSession() {
  sessionId.value = `s-${Date.now()}`
  localStorage.setItem(SESSION_KEY, sessionId.value)
  messages.value = [{
    id: 'welcome',
    role: 'assistant',
    content: '已开启新会话，有什么可以帮你的？',
  }]
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
    <header class="page-header">
      <div>
        <h2>代码工匠 · 智能对话</h2>
        <p>通用问答 · RAG 知识库 · Tool Calling · 多轮记忆</p>
      </div>
      <div class="header-actions">
        <label class="toggle">
          <input v-model="useStream" type="checkbox" />
          流式输出
        </label>
        <button class="btn-ghost" @click="newSession">新会话</button>
      </div>
    </header>

    <div ref="chatBox" class="chat-box">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message"
        :class="msg.role"
      >
        <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
        <div class="bubble">
          <pre class="content">{{ msg.content }}<span v-if="msg.streaming" class="cursor">▋</span></pre>
        </div>
      </div>
      <div v-if="loading && !messages.some(m => m.streaming)" class="typing">
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
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-muted);
  cursor: pointer;
}

.btn-ghost {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-muted);
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
}

.btn-ghost:hover {
  border-color: var(--accent);
  color: var(--text);
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
  letter-spacing: 0.01em;
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

.btn-send:not(:disabled):hover {
  filter: brightness(1.1);
}
</style>
