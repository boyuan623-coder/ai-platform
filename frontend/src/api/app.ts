import type { ApiResponse, AppItem, PageResult } from '@/types/api'
import { authHeaders } from '@/utils/auth'

const BASE = ''

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE}${url}`, {
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

export function listMyApps(page = 1, size = 10) {
  return request<PageResult<AppItem>>(`/api/app/mine?page=${page}&size=${size}`)
}

export function listFeatured(limit = 10) {
  return request<AppItem[]>(`/api/app/featured?limit=${limit}`)
}

export function listAllAppsAdmin(page = 1, size = 20) {
  return request<PageResult<AppItem>>(`/api/app/admin/all?page=${page}&size=${size}`)
}

export function setAppFeatured(id: number, featured: boolean) {
  return request<AppItem>(`/api/app/${id}/featured`, {
    method: 'POST',
    body: JSON.stringify({ featured }),
  })
}

export function createApp(data: {
  name: string
  description?: string
  codeType?: string
  codeContent?: string
}) {
  return request<AppItem>('/api/app', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function deleteApp(id: number) {
  return request<void>(`/api/app/${id}`, { method: 'DELETE' })
}

export function saveAppProject(id: number, payload: {
  projectType: string
  entryPath: string
  files: Record<string, string>
}) {
  return request<AppItem>(`/api/app/${id}/project`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function generateAppCover(id: number) {
  return request<AppItem>(`/api/app/${id}/cover`, { method: 'POST' })
}

export function deployApp(id: number) {
  return request<AppItem>(`/api/app/${id}/deploy`, { method: 'POST' })
}

export function getCoverImageUrl(id: number) {
  return `/api/app/${id}/cover/image`
}

export async function visualEditApp(id: number, body: {
  filePath?: string
  elementSelector?: string
  elementText?: string
  instruction: string
}) {
  return request<AppItem>(`/api/app/${id}/visual-edit`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export async function downloadAppZip(id: number, filename = 'app.zip') {
  const res = await fetch(`/api/app/${id}/download`, { headers: authHeaders() })
  if (!res.ok) throw new Error('下载失败')
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
