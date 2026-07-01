<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deployApp, visualEditApp } from '@/api/app'
import { getToken } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const appId = computed(() => Number(route.params.id))

const previewUrl = computed(() => `/api/app/preview/${appId.value}`)
const instruction = ref('')
const loading = ref(false)
const message = ref('')
const selectedTag = ref('')
const selectedText = ref('')
const selectedSelector = ref('')
const iframeKey = ref(0)

onMounted(() => {
  if (!getToken()) {
    router.replace('/login')
  }
})

function onIframeLoad(event: Event) {
  const iframe = event.target as HTMLIFrameElement
  try {
    const doc = iframe.contentDocument
    if (!doc) return
    doc.addEventListener('click', (e) => {
      e.preventDefault()
      e.stopPropagation()
      const el = e.target as HTMLElement
      if (!el || el === doc.body) return
      selectedTag.value = el.tagName.toLowerCase()
      selectedText.value = (el.innerText || el.textContent || '').trim().slice(0, 120)
      selectedSelector.value = buildSelector(el)
      el.style.outline = '2px solid #3b82f6'
    }, true)
  } catch {
    message.value = '无法访问预览页面，请确认应用已部署'
  }
}

function buildSelector(el: HTMLElement): string {
  if (el.id) return `#${el.id}`
  const classes = Array.from(el.classList).filter(Boolean).slice(0, 2)
  if (classes.length) return `${el.tagName.toLowerCase()}.${classes.join('.')}`
  return el.tagName.toLowerCase()
}

async function applyEdit() {
  if (!instruction.value.trim()) return
  loading.value = true
  message.value = ''
  try {
    await visualEditApp(appId.value, {
      elementSelector: selectedSelector.value,
      elementText: selectedText.value,
      instruction: instruction.value.trim(),
    })
    instruction.value = ''
    iframeKey.value += 1
    message.value = '修改已应用，预览已刷新'
  } catch (e) {
    message.value = e instanceof Error ? e.message : '修改失败'
  } finally {
    loading.value = false
  }
}

async function redeploy() {
  loading.value = true
  try {
    await deployApp(appId.value)
    iframeKey.value += 1
    message.value = '重新部署完成'
  } catch (e) {
    message.value = e instanceof Error ? e.message : '部署失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="edit-page">
    <header class="header">
      <div>
        <h2>可视化编辑</h2>
        <p>点击预览区元素，用 AI 对话快速修改页面</p>
      </div>
      <button class="ghost" @click="router.push('/apps')">返回应用列表</button>
    </header>

    <div class="workspace">
      <section class="preview-panel">
        <iframe
          :key="iframeKey"
          class="preview-frame"
          :src="previewUrl"
          title="应用预览"
          @load="onIframeLoad"
        />
      </section>

      <aside class="side-panel">
        <div class="block">
          <h3>选中元素</h3>
          <p v-if="selectedTag" class="tag">{{ selectedTag }} · {{ selectedSelector }}</p>
          <p v-else class="muted">在左侧点击页面元素</p>
          <p v-if="selectedText" class="snippet">{{ selectedText }}</p>
        </div>

        <div class="block">
          <h3>告诉 AI 如何修改</h3>
          <textarea
            v-model="instruction"
            rows="6"
            placeholder="例如：把这个按钮改成红色，文字改为「立即开始」"
            :disabled="loading"
          />
          <button class="primary" :disabled="loading || !instruction.trim()" @click="applyEdit">
            {{ loading ? '处理中…' : '应用修改' }}
          </button>
          <button class="secondary" :disabled="loading" @click="redeploy">重新部署</button>
        </div>

        <p v-if="message" class="msg">{{ message }}</p>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.edit-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border);
}

.header h2 {
  font-size: 18px;
}

.header p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.ghost {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-muted);
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
}

.workspace {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 360px;
  overflow: hidden;
}

.preview-panel {
  background: #111;
  display: flex;
}

.preview-frame {
  flex: 1;
  border: none;
  background: white;
}

.side-panel {
  border-left: 1px solid var(--border);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.block h3 {
  font-size: 14px;
  margin-bottom: 8px;
}

.tag {
  font-family: var(--mono);
  font-size: 12px;
  color: var(--accent);
}

.snippet {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 8px;
}

textarea {
  width: 100%;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text);
  font-size: 14px;
  margin-bottom: 10px;
}

.primary {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 8px;
  background: var(--accent);
  color: white;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 8px;
}

.secondary {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
}

.muted {
  color: var(--text-muted);
  font-size: 13px;
}

.msg {
  font-size: 13px;
  color: var(--success);
}
</style>
