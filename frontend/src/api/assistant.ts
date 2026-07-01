import type { ApiResponse } from '@/types/api'
import { parseSseStream } from './sse'
import { authHeaders } from '@/utils/auth'

export interface AssistantSession {
  id: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface AssistantMessageRecord {
  id: number
  sessionId: string
  role: 'USER' | 'ASSISTANT'
  content: string
  createdAt: string
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...authHeaders(), ...init?.headers },
  })
  if (!res.ok) {
    throw new Error(`请求失败 (${res.status})`)
  }
  const json: ApiResponse<T> = await res.json()
  if (json.code !== 200) {
    throw new Error(json.message || '服务异常')
  }
  return json.data
}

export function listAssistantSessions() {
  return request<AssistantSession[]>('/api/assistant/sessions')
}

export function createAssistantSession(sessionId?: string, title = '新对话') {
  return request<AssistantSession>('/api/assistant/sessions', {
    method: 'POST',
    body: JSON.stringify({ sessionId, title }),
  })
}

export function listAssistantMessages(sessionId: string) {
  return request<AssistantMessageRecord[]>(`/api/assistant/sessions/${encodeURIComponent(sessionId)}/messages`)
}

export function deleteAssistantSession(sessionId: string) {
  return request<void>(`/api/assistant/sessions/${encodeURIComponent(sessionId)}`, { method: 'DELETE' })
}

export async function assistantChatStream(
  sessionId: string,
  message: string,
  onChunk: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  const params = new URLSearchParams({ sessionId, message })
  await parseSseStream(`/api/assistant/chat/stream?${params}`, onChunk, signal, authHeaders())
}
