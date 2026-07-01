import { parseSseStream } from './sse'
import { authHeaders } from '@/utils/auth'

export interface CodeGenStreamResult {
  phase: string
  phaseLog: string
  analysis: string
  plan: string
  review: string
  code: string
  optimized: string
}

export interface VueProjectStreamResult {
  phase: string
  phaseLog: string
  sessionId: string
  entryPath: string
  summary: string
  reply: string
  files: Record<string, string>
  activeFile: string
}

export interface WorkflowGraphNode {
  id: string
  label: string
  description: string
}

export interface WorkflowGraphEdge {
  from: string
  to: string
  condition: string | null
}

export interface WorkflowGraph {
  name: string
  description: string
  nodes: WorkflowGraphNode[]
  edges: WorkflowGraphEdge[]
  maxRetries: number
}

export async function fetchWorkflowGraph(): Promise<WorkflowGraph> {
  const res = await fetch('/api/codegen/workflow/graph', { headers: authHeaders() })
  if (!res.ok) throw new Error('获取工作流定义失败')
  const json = await res.json()
  if (json.code !== 200) throw new Error(json.message || '获取工作流定义失败')
  return json.data
}

function appendTokenToSection(state: CodeGenStreamResult, nodeId: string, text: string) {
  if (!text) return
  switch (nodeId) {
    case 'understand': state.analysis += text; break
    case 'plan': state.plan += text; break
    case 'generate': state.code += text; break
    case 'review': state.review += text; break
    case 'optimize': state.optimized += text; break
    default: state.phaseLog += text
  }
}

export interface CodeRunResult {
  success: boolean
  language: string
  stdout: string
  stderr: string
  exitCode: number
  durationMs: number
  message: string
}

export async function runCode(code: string, language = 'auto'): Promise<CodeRunResult> {
  const res = await fetch('/api/codegen/run', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ code, language }),
  })
  if (!res.ok) {
    throw new Error(`运行请求失败 (${res.status})`)
  }
  const json = await res.json()
  if (json.code !== 200) {
    throw new Error(json.message || '运行失败')
  }
  return json.data
}

export async function generateStream(
  requirement: string,
  onUpdate: (result: CodeGenStreamResult) => void,
  signal?: AbortSignal,
): Promise<void> {
  const state: CodeGenStreamResult = {
    phase: 'INIT',
    phaseLog: '',
    analysis: '',
    plan: '',
    review: '',
    code: '',
    optimized: '',
  }

  let section: 'none' | 'analysis' | 'plan' | 'review' | 'code' | 'optimized' = 'none'

  const params = new URLSearchParams({ requirement })
  await parseSseStream(
    `/api/codegen/generate/stream?${params}`,
    (chunk) => {
      const lines = chunk.split('\n')
      for (const line of lines) {
        if (!line) continue

        const phaseMatch = line.match(/^\[phase:([^\]]+)\]\s*(.*)$/)
        if (phaseMatch) {
          state.phase = phaseMatch[1]
          if (phaseMatch[2]) {
            state.phaseLog += phaseMatch[2] + '\n'
          }
          section = 'none'
          onUpdate({ ...state })
          continue
        }

        const tokenMatch = line.match(/^\[token:([^\]]+)\](.*)$/)
        if (tokenMatch) {
          appendTokenToSection(state, tokenMatch[1], tokenMatch[2])
          onUpdate({ ...state })
          continue
        }

        if (line === '[analysis]') {
          section = 'analysis'
          continue
        }
        if (line === '[plan]') {
          section = 'plan'
          continue
        }
        if (line === '[review]') {
          section = 'review'
          continue
        }
        if (line === '[code]') {
          section = 'code'
          continue
        }
        if (line === '[optimized]') {
          section = 'optimized'
          continue
        }
        if (line.startsWith('[error]')) {
          state.phaseLog += line + '\n'
          state.phase = 'ERROR'
          onUpdate({ ...state })
          continue
        }
        if (line === '[done]') {
          section = 'none'
          continue
        }

        if (section === 'analysis') state.analysis += line + '\n'
        else if (section === 'plan') state.plan += line + '\n'
        else if (section === 'review') state.review += line + '\n'
        else if (section === 'code') state.code += line + '\n'
        else if (section === 'optimized') state.optimized += line + '\n'
        else state.phaseLog += line + '\n'

        onUpdate({ ...state })
      }
    },
    signal,
    authHeaders(),
  )
}

export async function generateVueProjectStream(
  requirement: string,
  onUpdate: (result: VueProjectStreamResult) => void,
  signal?: AbortSignal,
): Promise<void> {
  const state: VueProjectStreamResult = {
    phase: 'INIT',
    phaseLog: '',
    sessionId: '',
    entryPath: 'src/App.vue',
    summary: '',
    reply: '',
    files: {},
    activeFile: '',
  }

  let section: 'none' | 'summary' | 'reply' | 'file' = 'none'
  let currentFile = ''

  const params = new URLSearchParams({ requirement })
  await parseSseStream(
    `/api/codegen/vue/generate/stream?${params}`,
    (chunk) => {
      const lines = chunk.split('\n')
      for (const line of lines) {
        if (!line) continue

        const phaseMatch = line.match(/^\[phase:([^\]]+)\]\s*(.*)$/)
        if (phaseMatch) {
          state.phase = phaseMatch[1]
          if (phaseMatch[2]) state.phaseLog += phaseMatch[2] + '\n'
          section = 'none'
          onUpdate({ ...state })
          continue
        }

        const tokenMatch = line.match(/^\[token:assistant\](.*)$/)
        if (tokenMatch) {
          state.reply += tokenMatch[1]
          onUpdate({ ...state })
          continue
        }

        const sessionMatch = line.match(/^\[session:([^\]]+)\]$/)
        if (sessionMatch) {
          state.sessionId = sessionMatch[1]
          onUpdate({ ...state })
          continue
        }

        const entryMatch = line.match(/^\[entry:([^\]]+)\]$/)
        if (entryMatch) {
          state.entryPath = entryMatch[1]
          state.activeFile = entryMatch[1]
          onUpdate({ ...state })
          continue
        }

        const fileMatch = line.match(/^\[file:([^\]]+)\]$/)
        if (fileMatch) {
          section = 'file'
          currentFile = fileMatch[1]
          state.files[currentFile] = ''
          state.activeFile = currentFile
          onUpdate({ ...state })
          continue
        }

        if (line === '[summary]') {
          section = 'summary'
          continue
        }
        if (line === '[reply]') {
          section = 'reply'
          continue
        }
        if (line === '[project:done]' || line === '[done]') {
          section = 'none'
          state.phase = 'DONE'
          if (state.files['src/App.vue']) {
            state.activeFile = 'src/App.vue'
          } else if (state.entryPath && state.files[state.entryPath]) {
            state.activeFile = state.entryPath
          }
          onUpdate({ ...state })
          continue
        }
        if (line.startsWith('[error]')) {
          state.phaseLog += line + '\n'
          onUpdate({ ...state })
          continue
        }

        if (section === 'summary') state.summary += line + '\n'
        else if (section === 'reply') state.reply += line + '\n'
        else if (section === 'file' && currentFile) {
          state.files[currentFile] += line + '\n'
        } else {
          state.phaseLog += line + '\n'
        }

        onUpdate({ ...state })
      }
    },
    signal,
    authHeaders(),
  )
}

export interface SmartStreamState {
  route: string
  routeReason: string
  vue: VueProjectStreamResult
  single: CodeGenStreamResult
}

export async function generateSmartStream(
  requirement: string,
  onUpdate: (state: SmartStreamState) => void,
  signal?: AbortSignal,
): Promise<void> {
  const state: SmartStreamState = {
    route: '',
    routeReason: '',
    vue: {
      phase: 'INIT',
      phaseLog: '',
      sessionId: '',
      entryPath: 'src/App.vue',
      summary: '',
      reply: '',
      files: {},
      activeFile: '',
    },
    single: {
      phase: 'INIT',
      phaseLog: '',
      analysis: '',
      plan: '',
      review: '',
      code: '',
      optimized: '',
    },
  }

  let vueSection: 'none' | 'summary' | 'reply' | 'file' = 'none'
  let singleSection: 'none' | 'analysis' | 'plan' | 'review' | 'code' | 'optimized' = 'none'
  let currentFile = ''

  const params = new URLSearchParams({ requirement })
  await parseSseStream(
    `/api/codegen/smart/generate/stream?${params}`,
    (chunk) => {
      const lines = chunk.split('\n')
      for (const line of lines) {
        if (!line) continue

        const routeMatch = line.match(/^\[route:([^\]]+)\]$/)
        if (routeMatch) {
          state.route = routeMatch[1]
          onUpdate({ ...state, vue: { ...state.vue }, single: { ...state.single } })
          continue
        }
        if (line.startsWith('[route-reason]')) {
          state.routeReason = line.replace('[route-reason]', '')
          onUpdate({ ...state, vue: { ...state.vue }, single: { ...state.single } })
          continue
        }

        const fileMatch = line.match(/^\[file:([^\]]+)\]$/)
        if (fileMatch) {
          vueSection = 'file'
          currentFile = fileMatch[1]
          state.vue.files[currentFile] = ''
          state.vue.activeFile = currentFile
          onUpdate({ ...state, vue: { ...state.vue }, single: { ...state.single } })
          continue
        }
        const vueTokenMatch = line.match(/^\[token:assistant\](.*)$/)
        if (vueTokenMatch) {
          state.vue.reply += vueTokenMatch[1]
          onUpdate({ ...state, vue: { ...state.vue }, single: { ...state.single } })
          continue
        }
        const sessionMatch = line.match(/^\[session:([^\]]+)\]$/)
        if (sessionMatch) {
          state.vue.sessionId = sessionMatch[1]
          onUpdate({ ...state, vue: { ...state.vue }, single: { ...state.single } })
          continue
        }

        const phaseMatch = line.match(/^\[phase:([^\]]+)\]\s*(.*)$/)
        if (phaseMatch) {
          if (state.route === 'VUE_PROJECT' || state.route === '') {
            state.vue.phase = phaseMatch[1]
            if (phaseMatch[2]) state.vue.phaseLog += phaseMatch[2] + '\n'
          } else {
            state.single.phase = phaseMatch[1]
            if (phaseMatch[2]) state.single.phaseLog += phaseMatch[2] + '\n'
          }
          vueSection = 'none'
          singleSection = 'none'
          onUpdate({ ...state, vue: { ...state.vue }, single: { ...state.single } })
          continue
        }

        const tokenMatch = line.match(/^\[token:([^\]]+)\](.*)$/)
        if (tokenMatch && state.route !== 'VUE_PROJECT') {
          appendTokenToSection(state.single, tokenMatch[1], tokenMatch[2])
          onUpdate({ ...state, vue: { ...state.vue }, single: { ...state.single } })
          continue
        }

        if (line === '[analysis]') { singleSection = 'analysis'; continue }
        if (line === '[plan]') { singleSection = 'plan'; continue }
        if (line === '[review]') { singleSection = 'review'; continue }
        if (line === '[code]') { singleSection = 'code'; continue }
        if (line === '[optimized]') { singleSection = 'optimized'; continue }
        if (line === '[summary]') { vueSection = 'summary'; continue }
        if (line === '[reply]') { vueSection = 'reply'; continue }
        if (line.startsWith('[error]')) {
          if (state.route === 'VUE_PROJECT') {
            state.vue.phaseLog += line + '\n'
            state.vue.phase = 'ERROR'
          } else {
            state.single.phaseLog += line + '\n'
            state.single.phase = 'ERROR'
          }
          onUpdate({ ...state, vue: { ...state.vue }, single: { ...state.single } })
          continue
        }
        if (line === '[project:done]' || line === '[done]') {
          state.vue.phase = 'DONE'
          if (state.vue.files['src/App.vue']) {
            state.vue.activeFile = 'src/App.vue'
          } else if (state.vue.entryPath && state.vue.files[state.vue.entryPath]) {
            state.vue.activeFile = state.vue.entryPath
          }
          onUpdate({ ...state, vue: { ...state.vue }, single: { ...state.single } })
          continue
        }

        if (vueSection === 'file' && currentFile) {
          state.vue.files[currentFile] += line + '\n'
        } else if (vueSection === 'summary') {
          state.vue.summary += line + '\n'
        } else if (vueSection === 'reply') {
          state.vue.reply += line + '\n'
        } else if (singleSection === 'analysis') {
          state.single.analysis += line + '\n'
        } else if (singleSection === 'plan') {
          state.single.plan += line + '\n'
        } else if (singleSection === 'review') {
          state.single.review += line + '\n'
        } else if (singleSection === 'code') {
          state.single.code += line + '\n'
        } else if (singleSection === 'optimized') {
          state.single.optimized += line + '\n'
        } else if (state.route === 'VUE_PROJECT' || Object.keys(state.vue.files).length > 0) {
          state.vue.phaseLog += line + '\n'
        } else {
          state.single.phaseLog += line + '\n'
        }

        onUpdate({ ...state, vue: { ...state.vue }, single: { ...state.single } })
      }
    },
    signal,
    authHeaders(),
  )
}

export async function downloadVueProjectZip(sessionId: string) {
  const res = await fetch(`/api/codegen/vue/download/${sessionId}`, {
    headers: authHeaders(),
  })
  if (!res.ok) throw new Error('下载失败')
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `vue-project-${sessionId}.zip`
  a.click()
  URL.revokeObjectURL(url)
}
