<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { getStoredUser } from '@/utils/auth'
import type { UserVO } from '@/types/api'

const route = useRoute()
const user = getStoredUser<UserVO>()
const isAdmin = computed(() => user?.role === 'ADMIN')
const showShell = computed(() =>
  route.name !== 'login' && route.name !== 'visual-edit' && route.name !== 'app-chat',
)
</script>

<template>
  <div v-if="!showShell" class="full">
    <RouterView />
  </div>
  <div v-else class="layout">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-icon">◆</span>
        <div>
          <h1>AI 零代码平台</h1>
          <p>智能生成 · 应用管理 · 微服务架构</p>
        </div>
      </div>
      <nav>
        <RouterLink to="/apps" class="nav-item">
          <span class="nav-icon">📦</span>
          我的应用
        </RouterLink>
        <RouterLink to="/appointment" class="nav-item">
          <span class="nav-icon">💬</span>
          智能助手
        </RouterLink>
        <RouterLink to="/codegen" class="nav-item">
          <span class="nav-icon">⚡</span>
          代码生成
        </RouterLink>
        <RouterLink v-if="isAdmin" to="/admin" class="nav-item">
          <span class="nav-icon">🛡️</span>
          管理后台
        </RouterLink>
      </nav>
      <footer class="sidebar-footer">
        <span class="status-dot"></span>
        后端 localhost:8080
      </footer>
    </aside>
    <main class="main">
      <RouterView v-slot="{ Component, route }">
        <KeepAlive :include="['AppointmentView', 'CodeGenView']">
          <component
            :is="Component"
            :key="route.name === 'appointment' || route.name === 'codegen' ? String(route.name) : route.fullPath"
          />
        </KeepAlive>
      </RouterView>
    </main>
  </div>
</template>

<style scoped>
.full {
  height: 100%;
}

.layout {
  display: flex;
  height: 100%;
}

.sidebar {
  width: 240px;
  background: var(--bg-elevated);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 32px;
  padding: 0 8px;
}

.brand-icon {
  font-size: 24px;
  color: var(--accent);
}

.brand h1 {
  font-size: 16px;
  font-weight: 600;
}

.brand p {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: var(--radius);
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 500;
  transition: all 0.15s;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.04);
  color: var(--text);
}

.nav-item.router-link-active {
  background: var(--accent-soft);
  color: var(--accent);
}

.nav-icon {
  font-size: 16px;
}

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-muted);
  padding: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--success);
  box-shadow: 0 0 8px var(--success);
}

.main {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
