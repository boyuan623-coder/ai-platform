import type { ApiResponse } from '@/types/api'
import { parseSseStream } from './sse'
import { authHeaders } from '@/utils/auth'

export async function chat(sessionId: string, message: string): Promise<string> {
  const res = await fetch('/api/appointment/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ sessionId, message }),
  })
  if (!res.ok) {
    throw new Error(`请求失败 (${res.status})`)
  }
  const json: ApiResponse<string> = await res.json()
  if (json.code !== 200) {
    throw new Error(json.message || '服务异常')
  }
  return json.data
}

export async function chatStream(
  sessionId: string,
  message: string,
  onChunk: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  const params = new URLSearchParams({ sessionId, message })
  await parseSseStream(`/api/appointment/chat/stream?${params}`, onChunk, signal, authHeaders())
}
