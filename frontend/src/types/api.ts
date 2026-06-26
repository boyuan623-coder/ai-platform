export interface ApiResponse<T> {
  code: number
  message: string
  data: T
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
