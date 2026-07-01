<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  listFeatured,
  listMyApps,
  createApp,
  deleteApp,
  generateAppCover,
  deployApp,
  downloadAppZip,
  getCoverImageUrl,
} from '@/api/app'
import { logout } from '@/api/user'
import type { AppItem, UserVO } from '@/types/api'
import { clearAuth, getStoredUser, getToken } from '@/utils/auth'

const router = useRouter()
const user = ref<UserVO | null>(getStoredUser<UserVO>())
const myApps = ref<AppItem[]>([])
const featuredApps = ref<AppItem[]>([])
const loading = ref(true)
const error = ref('')
const actionMsg = ref('')
const newAppName = ref('')

onMounted(async () => {
  if (!getToken()) {
    router.replace('/login')
    return
  }
  try {
    const [mine, featured] = await Promise.all([listMyApps(), listFeatured()])
    myApps.value = mine.records || []
    featuredApps.value = featured
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
})

async function handleCreate() {
  if (!newAppName.value.trim()) return
  try {
    const app = await createApp({ name: newAppName.value.trim(), codeType: 'HTML' })
    myApps.value.unshift(app)
    newAppName.value = ''
  } catch (e) {
    error.value = e instanceof Error ? e.message : '创建失败'
  }
}

async function handleDelete(id: number) {
  if (!confirm('确定删除该应用？')) return
  await deleteApp(id)
  myApps.value = myApps.value.filter((a) => a.id !== id)
}

async function handleCover(app: AppItem) {
  try {
    const updated = await generateAppCover(app.id)
    const idx = myApps.value.findIndex((a) => a.id === app.id)
    if (idx >= 0) myApps.value[idx] = { ...myApps.value[idx], ...updated }
    actionMsg.value = '封面已生成'
  } catch (e) {
    actionMsg.value = e instanceof Error ? e.message : '封面生成失败'
  }
}

async function handleDeploy(app: AppItem) {
  try {
    const updated = await deployApp(app.id)
    const idx = myApps.value.findIndex((a) => a.id === app.id)
    if (idx >= 0) myApps.value[idx] = { ...myApps.value[idx], ...updated }
    actionMsg.value = '部署成功'
  } catch (e) {
    actionMsg.value = e instanceof Error ? e.message : '部署失败'
  }
}

async function handleDownload(app: AppItem) {
  try {
    await downloadAppZip(app.id, `${app.name}.zip`)
    actionMsg.value = '下载已开始'
  } catch (e) {
    actionMsg.value = e instanceof Error ? e.message : '下载失败'
  }
}

function openDeploy(app: AppItem) {
  if (app.deployUrl) {
    window.open(app.deployUrl, '_blank')
  }
}

async function handleLogout() {
  const token = getToken()
  if (token) await logout(token).catch(() => {})
  clearAuth()
  router.push('/login')
}
</script>

<template>
  <div class="apps-page">
    <header class="page-header">
      <div>
        <h2>我的应用</h2>
        <p v-if="user">欢迎，{{ user.nickname || user.username }}</p>
      </div>
      <button class="ghost" @click="handleLogout">退出登录</button>
    </header>

    <section class="create-bar">
      <input v-model="newAppName" placeholder="输入应用名称，快速创建" @keyup.enter="handleCreate" />
      <button @click="handleCreate">创建应用</button>
    </section>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="actionMsg" class="ok-msg">{{ actionMsg }}</p>
    <p v-if="loading" class="muted">加载中…</p>

    <section v-if="!loading" class="grid">
      <article v-for="app in myApps" :key="app.id" class="app-card">
        <div v-if="app.coverUrl" class="cover-wrap">
          <img :src="getCoverImageUrl(app.id)" alt="封面" class="cover-img" />
        </div>
        <div v-else class="cover-placeholder" @click="handleCover(app)">点击生成封面</div>
        <h3>{{ app.name }}</h3>
        <p>{{ app.description || '暂无描述' }}</p>
        <div class="meta">
          <span>{{ app.codeType }}</span>
          <span>{{ app.status }}</span>
        </div>
        <div class="actions">
          <RouterLink :to="`/codegen?appId=${app.id}`">去生成</RouterLink>
          <RouterLink :to="{ path: `/app/${app.id}/chat`, query: { name: app.name } }">AI 对话</RouterLink>
          <button @click="handleCover(app)">封面</button>
          <button @click="handleDeploy(app)">部署</button>
          <button v-if="app.deployUrl" @click="openDeploy(app)">访问</button>
          <RouterLink v-if="app.status === 'DEPLOYED'" :to="`/app/${app.id}/edit`">可视化编辑</RouterLink>
          <button @click="handleDownload(app)">下载</button>
          <button class="danger" @click="handleDelete(app.id)">删除</button>
        </div>
      </article>
    </section>

    <section v-if="featuredApps.length" class="featured">
      <h3>精选应用</h3>
      <div class="grid">
        <article v-for="app in featuredApps" :key="app.id" class="app-card featured-card">
          <div v-if="app.coverUrl" class="cover-wrap">
            <img :src="getCoverImageUrl(app.id)" alt="封面" class="cover-img" />
          </div>
          <h3>{{ app.name }}</h3>
          <p>{{ app.description || '暂无描述' }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.apps-page {
  padding: 24px 32px;
  overflow-y: auto;
  height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 20px;
}

.page-header p {
  color: var(--text-muted);
  font-size: 13px;
  margin-top: 4px;
}

.create-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.create-bar input {
  flex: 1;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-elevated);
  color: var(--text);
}

.create-bar button,
.ghost {
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--accent);
  color: white;
  cursor: pointer;
}

.ghost {
  background: transparent;
  color: var(--text-muted);
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.app-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px;
}

.cover-wrap {
  margin-bottom: 12px;
  border-radius: 8px;
  overflow: hidden;
  height: 120px;
  background: var(--bg-card);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  margin-bottom: 12px;
  height: 80px;
  border-radius: 8px;
  background: var(--bg-card);
  border: 1px dashed var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-muted);
  cursor: pointer;
}

.app-card h3 {
  font-size: 16px;
  margin-bottom: 8px;
}

.app-card p {
  color: var(--text-muted);
  font-size: 13px;
  margin-bottom: 12px;
}

.meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.actions a {
  color: var(--accent);
  font-size: 13px;
  text-decoration: none;
}

.actions button {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-muted);
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}

.danger {
  border: none !important;
  color: #f87171 !important;
}

.featured {
  margin-top: 32px;
}

.featured h3 {
  margin-bottom: 16px;
}

.featured-card {
  border-color: var(--accent);
}

.error {
  color: #f87171;
  margin-bottom: 12px;
}

.ok-msg {
  color: var(--success);
  margin-bottom: 12px;
  font-size: 13px;
}

.muted {
  color: var(--text-muted);
}
</style>
