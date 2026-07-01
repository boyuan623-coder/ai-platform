import type { ApiResponse, LoginResult, UserVO } from '@/types/api'

const BASE = ''

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE}${url}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  })
  const json = (await res.json()) as ApiResponse<T>
  if (json.code !== 200) {
    throw new Error(json.message || '请求失败')
  }
  return json.data
}

export function register(username: string, password: string, nickname?: string) {
  return request<UserVO>('/api/user/register', {
    method: 'POST',
    body: JSON.stringify({ username, password, nickname }),
  })
}

export function login(username: string, password: string) {
  return request<LoginResult>('/api/user/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function logout(token: string) {
  return request<void>('/api/user/logout', {
    method: 'POST',
    headers: { 'X-Auth-Token': token },
  })
}

export function getMe(token: string) {
  return request<UserVO>('/api/user/me', {
    headers: { 'X-Auth-Token': token },
  })
}
