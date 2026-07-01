export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface UserVO {
  id: number
  username: string
  nickname: string
  avatar?: string
  role: string
}

export interface LoginResult {
  token: string
  user: UserVO
}

export interface AppItem {
  id: number
  userId: number
  name: string
  description?: string
  codeType: string
  codeContent?: string
  deployUrl?: string
  coverUrl?: string
  isFeatured: boolean
  status: string
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
}

export interface CodeGenPhase {
  label: string
  status: 'pending' | 'active' | 'done'
}
