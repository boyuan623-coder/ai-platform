<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, register } from '@/api/user'
import { setAuth } from '@/utils/auth'

const router = useRouter()
const isRegister = ref(false)
const username = ref('')
const password = ref('')
const nickname = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  loading.value = true
  try {
    if (isRegister.value) {
      await register(username.value, password.value, nickname.value || username.value)
      const result = await login(username.value, password.value)
      setAuth(result.token, result.user)
    } else {
      const result = await login(username.value, password.value)
      setAuth(result.token, result.user)
    }
    router.push('/apps')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '操作失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1>AI 零代码平台</h1>
      <p class="subtitle">登录后开始创建你的 AI 应用</p>

      <form @submit.prevent="submit">
        <label>
          用户名
          <input v-model="username" required autocomplete="username" />
        </label>
        <label v-if="isRegister">
          昵称
          <input v-model="nickname" autocomplete="nickname" />
        </label>
        <label>
          密码
          <input v-model="password" type="password" required autocomplete="current-password" />
        </label>

        <p v-if="error" class="error">{{ error }}</p>

        <button type="submit" :disabled="loading">
          {{ loading ? '处理中…' : isRegister ? '注册并登录' : '登录' }}
        </button>
      </form>

      <button type="button" class="link" @click="isRegister = !isRegister">
        {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
      </button>

      <p class="hint">演示账号：demo / 123456</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 32px;
}

h1 {
  font-size: 22px;
  margin-bottom: 8px;
}

.subtitle {
  color: var(--text-muted);
  font-size: 14px;
  margin-bottom: 24px;
}

form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: var(--text-muted);
}

input {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg);
  color: var(--text);
}

button[type='submit'] {
  padding: 12px;
  border-radius: 8px;
  border: none;
  background: var(--accent);
  color: white;
  font-weight: 600;
  cursor: pointer;
}

button[type='submit']:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.link {
  margin-top: 16px;
  background: none;
  border: none;
  color: var(--accent);
  cursor: pointer;
  font-size: 14px;
}

.error {
  color: #f87171;
  font-size: 13px;
}

.hint {
  margin-top: 20px;
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
}
</style>
