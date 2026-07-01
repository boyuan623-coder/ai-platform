import { parseSseStream } from './sse'
import { authHeaders } from '@/utils/auth'
import type { ApiResponse } from '@/types/api'

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
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

export interface ChatHistoryItem {
  id: number
  appId: number
  role: string
  content: string
  createdAt: string
}

export interface ChatHistoryPage {
  records: ChatHistoryItem[]
  nextCursor: number | null
  hasMore: boolean
}

export async function listChatHistory(appId: number, cursor?: number, size = 20) {
  const params = new URLSearchParams({ appId: String(appId), size: String(size) })
  if (cursor) params.set('cursor', String(cursor))
  return request<ChatHistoryPage>(`/api/chat/history?${params}`)
}

export async function clearChatHistory(appId: number) {
  return request<void>(`/api/chat/history?appId=${appId}`, { method: 'DELETE' })
}

export async function sendAppChat(appId: number, sessionId: string, message: string) {
  return request<ChatHistoryItem>(`/api/chat/${appId}/message`, {
    method: 'POST',
    body: JSON.stringify({ sessionId, message }),
  })
}

export async function appChatStream(
  appId: number,
  sessionId: string,
  message: string,
  onChunk: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  const params = new URLSearchParams({ sessionId, message })
  await parseSseStream(
    `/api/chat/${appId}/stream?${params}`,
    onChunk,
    signal,
    authHeaders(),
  )
}
