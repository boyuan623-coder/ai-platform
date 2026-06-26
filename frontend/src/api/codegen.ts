import { parseSseStream } from './sse'

export interface CodeGenStreamResult {
  phase: string
  phaseLog: string
  analysis: string
  code: string
  optimized: string
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
    headers: { 'Content-Type': 'application/json' },
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
    code: '',
    optimized: '',
  }

  let section: 'none' | 'analysis' | 'code' | 'optimized' = 'none'

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

        if (line === '[analysis]') {
          section = 'analysis'
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
        if (line === '[done]') {
          section = 'none'
          continue
        }

        if (section === 'analysis') state.analysis += line + '\n'
        else if (section === 'code') state.code += line + '\n'
        else if (section === 'optimized') state.optimized += line + '\n'
        else state.phaseLog += line + '\n'

        onUpdate({ ...state })
      }
    },
    signal,
  )
}
