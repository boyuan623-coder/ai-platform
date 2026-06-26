<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-icon">◆</span>
        <div>
          <h1>代码工匠</h1>
          <p>智能对话 · 代码生成</p>
        </div>
      </div>
      <nav>
        <RouterLink to="/appointment" class="nav-item">
          <span class="nav-icon">💬</span>
          智能对话
        </RouterLink>
        <RouterLink to="/codegen" class="nav-item">
          <span class="nav-icon">⚡</span>
          代码生成
        </RouterLink>
      </nav>
      <footer class="sidebar-footer">
        <span class="status-dot"></span>
        后端 localhost:8080
      </footer>
    </aside>
    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
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
