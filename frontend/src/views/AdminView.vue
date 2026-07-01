<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { listAllAppsAdmin, setAppFeatured, listFeatured } from '@/api/app'
import { getStoredUser } from '@/utils/auth'
import type { AppItem, UserVO } from '@/types/api'

const user = getStoredUser<UserVO>()
const isAdmin = computed(() => user?.role === 'ADMIN')

const apps = ref<AppItem[]>([])
const featured = ref<AppItem[]>([])
const loading = ref(false)
const message = ref('')
const page = ref(1)

async function load() {
  if (!isAdmin.value) return
  loading.value = true
  try {
    const [all, feat] = await Promise.all([
      listAllAppsAdmin(page.value, 30),
      listFeatured(20),
    ])
    apps.value = all.records
    featured.value = feat
  } catch (e) {
    message.value = e instanceof Error ? e.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function toggleFeatured(app: AppItem) {
  try {
    await setAppFeatured(app.id, !app.isFeatured)
    message.value = app.isFeatured ? '已取消精选' : '已设为精选'
    await load()
  } catch (e) {
    message.value = e instanceof Error ? e.message : '操作失败'
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header>
      <h2>管理后台</h2>
      <p>精选应用管理（需管理员权限）</p>
    </header>

    <div v-if="!isAdmin" class="warn">当前账号无管理员权限</div>

    <template v-else>
      <section v-if="featured.length" class="section">
        <h3>当前精选 {{ featured.length }} 个</h3>
        <div class="grid">
          <article v-for="app in featured" :key="app.id" class="card">
            <strong>{{ app.name }}</strong>
            <span class="meta">#{{ app.id }} · {{ app.status }}</span>
            <button class="btn-secondary" @click="toggleFeatured(app)">取消精选</button>
          </article>
        </div>
      </section>

      <section class="section">
        <h3>全部应用</h3>
        <div v-if="loading" class="muted">加载中…</div>
        <div class="grid">
          <article v-for="app in apps" :key="app.id" class="card">
            <strong>{{ app.name }}</strong>
            <span class="meta">#{{ app.id }} · {{ app.isFeatured ? '精选' : '普通' }}</span>
            <button
              class="btn-primary"
              :class="{ active: app.isFeatured }"
              @click="toggleFeatured(app)"
            >
              {{ app.isFeatured ? '取消精选' : '设为精选' }}
            </button>
          </article>
        </div>
      </section>

      <p v-if="message" class="msg">{{ message }}</p>
    </template>
  </div>
</template>

<style scoped>
.page {
  padding: 24px 28px;
  max-width: 1000px;
}

header h2 {
  font-size: 18px;
}

header p {
  color: var(--text-muted);
  font-size: 13px;
  margin-top: 4px;
}

.warn {
  margin-top: 24px;
  padding: 16px;
  background: #fef3c7;
  color: #92400e;
  border-radius: 8px;
}

.section {
  margin-top: 28px;
}

.section h3 {
  font-size: 14px;
  margin-bottom: 12px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.card {
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta {
  font-size: 12px;
  color: var(--text-muted);
}

.btn-primary, .btn-secondary {
  padding: 8px 12px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-size: 12px;
}

.btn-primary {
  background: var(--accent);
  color: white;
}

.btn-primary.active {
  background: var(--text-muted);
}

.btn-secondary {
  background: var(--bg);
  border: 1px solid var(--border);
  color: var(--text);
}

.msg {
  margin-top: 16px;
  color: var(--success);
  font-size: 13px;
}

.muted {
  color: var(--text-muted);
}
</style>
