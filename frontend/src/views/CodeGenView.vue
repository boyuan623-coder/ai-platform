<script setup lang="ts">
import { ref, computed } from 'vue'
import { generateStream, runCode, type CodeGenStreamResult, type CodeRunResult } from '@/api/codegen'
import {
  detectCodeType,
  ensureFullHtml,
  extractRunnableCode,
  extractScriptFromHtml,
} from '@/utils/codeDetect'

const requirement = ref('写一个 Java Hello World 程序，打印 Hello World')
const loading = ref(false)
const running = ref(false)
const result = ref<CodeGenStreamResult>({
  phase: 'INIT',
  phaseLog: '',
  analysis: '',
  code: '',
  optimized: '',
})
const activeTab = ref<'optimized' | 'code' | 'analysis'>('optimized')
const runLanguage = ref('auto')
const runOutput = ref<CodeRunResult | null>(null)
const htmlPreview = ref('')
const showConsole = ref(false)
let abortController: AbortController | null = null

const phases = computed(() => [
  { key: 'UNDERSTAND', label: '需求理解', done: ['GENERATE', 'OPTIMIZE', 'CACHE_HIT'].includes(result.value.phase) || result.value.analysis.length > 0 },
  { key: 'GENERATE', label: '代码生成', done: ['OPTIMIZE', 'CACHE_HIT'].includes(result.value.phase) || result.value.code.length > 0 },
  { key: 'OPTIMIZE', label: '结果优化', done: result.value.optimized.length > 0 || result.value.phase === 'CACHE_HIT' },
])

const runnableCode = computed(() => {
  if (activeTab.value === 'analysis') return ''
  return result.value.optimized || result.value.code
})

const displayCode = computed(() => {
  if (activeTab.value === 'optimized') return result.value.optimized || result.value.code
  if (activeTab.value === 'code') return result.value.code
  return result.value.analysis
})

const consoleText = computed(() => {
  if (!runOutput.value) return ''
  const parts: string[] = []
  if (runOutput.value.message) parts.push(`[${runOutput.value.message}] ${runOutput.value.language} · ${runOutput.value.durationMs}ms · exit ${runOutput.value.exitCode}`)
  if (runOutput.value.stdout) parts.push('── stdout ──\n' + runOutput.value.stdout)
  if (runOutput.value.stderr) parts.push('── stderr ──\n' + runOutput.value.stderr)
  return parts.join('\n\n')
})

async function generate() {
  const text = requirement.value.trim()
  if (!text || loading.value) return

  loading.value = true
  runOutput.value = null
  htmlPreview.value = ''
  showConsole.value = false
  result.value = { phase: 'INIT', phaseLog: '', analysis: '', code: '', optimized: '' }
  abortController = new AbortController()

  try {
    await generateStream(
      text,
      (update) => {
        result.value = { ...update }
        if (update.optimized) activeTab.value = 'optimized'
        else if (update.code) activeTab.value = 'code'
      },
      abortController.signal,
    )
  } catch (e) {
    result.value.phaseLog += `\n错误：${e instanceof Error ? e.message : '未知错误'}`
  } finally {
    loading.value = false
    abortController = null
  }
}

async function runGeneratedCode() {
  const raw = runnableCode.value.trim()
  if (!raw || running.value) return

  const code = extractRunnableCode(raw)
  const type = runLanguage.value === 'auto' ? detectCodeType(code) : runLanguage.value

  running.value = true
  showConsole.value = true
  runOutput.value = null
  htmlPreview.value = ''

  const start = Date.now()

  try {
    if (type === 'html') {
      htmlPreview.value = ensureFullHtml(code)
      runOutput.value = {
        success: true,
        language: 'html',
        stdout: '已在下方加载页面预览，可直接操作计算器。',
        stderr: '',
        exitCode: 0,
        durationMs: Date.now() - start,
        message: 'HTML 页面预览',
      }
      return
    }

    if (type === 'javascript') {
      const js = extractScriptFromHtml(code) ?? code
      runOutput.value = runJavaScriptInBrowser(js)
      return
    }

    runOutput.value = await runCode(code, type)
  } catch (e) {
    runOutput.value = {
      success: false,
      language: type,
      stdout: '',
      stderr: e instanceof Error ? e.message : '未知错误',
      exitCode: -1,
      durationMs: Date.now() - start,
      message: '运行失败',
    }
  } finally {
    running.value = false
  }
}

function runJavaScriptInBrowser(code: string): CodeRunResult {
  const logs: string[] = []
  const start = Date.now()
  const fakeConsole = {
    log: (...args: unknown[]) => logs.push(args.map(String).join(' ')),
    error: (...args: unknown[]) => logs.push('[error] ' + args.map(String).join(' ')),
    warn: (...args: unknown[]) => logs.push('[warn] ' + args.map(String).join(' ')),
  }
  try {
    const fn = new Function('console', code)
    fn(fakeConsole)
    return {
      success: true,
      language: 'javascript',
      stdout: logs.join('\n') || '(无输出)',
      stderr: '',
      exitCode: 0,
      durationMs: Date.now() - start,
      message: '浏览器内运行成功',
    }
  } catch (e) {
    return {
      success: false,
      language: 'javascript',
      stdout: logs.join('\n'),
      stderr: e instanceof Error ? e.message : String(e),
      exitCode: 1,
      durationMs: Date.now() - start,
      message: '浏览器内运行失败',
    }
  }
}

function stop() {
  abortController?.abort()
  loading.value = false
}

function copyCode() {
  navigator.clipboard.writeText(displayCode.value)
}
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2>代码工匠 · 代码生成</h2>
        <p>LangGraph4j 工作流 · 生成后可一键运行验证</p>
      </div>
    </header>

    <div class="workspace">
      <section class="input-panel">
        <label>需求描述</label>
        <textarea
          v-model="requirement"
          rows="8"
          placeholder="描述你想生成的代码，例如：写一个 Java Hello World"
          :disabled="loading"
        />
        <div class="actions">
          <button v-if="!loading" class="btn-primary" :disabled="!requirement.trim()" @click="generate">
            开始生成
          </button>
          <button v-else class="btn-stop" @click="stop">停止</button>
        </div>

        <div class="phases">
          <div
            v-for="(p, i) in phases"
            :key="p.key"
            class="phase"
            :class="{ done: p.done, active: result.phase === p.key }"
          >
            <span class="phase-num">{{ i + 1 }}</span>
            <span>{{ p.label }}</span>
          </div>
        </div>

        <pre v-if="result.phaseLog" class="phase-log">{{ result.phaseLog }}</pre>
      </section>

      <section class="output-panel">
        <div class="tabs">
          <button :class="{ active: activeTab === 'optimized' }" @click="activeTab = 'optimized'">优化代码</button>
          <button :class="{ active: activeTab === 'code' }" @click="activeTab = 'code'">原始代码</button>
          <button :class="{ active: activeTab === 'analysis' }" @click="activeTab = 'analysis'">需求分析</button>

          <select v-model="runLanguage" class="lang-select" title="运行语言">
            <option value="auto">自动检测</option>
            <option value="html">HTML 页面</option>
            <option value="java">Java</option>
            <option value="javascript">JavaScript</option>
            <option value="python">Python</option>
          </select>

          <button
            class="btn-run"
            :disabled="!runnableCode || running || activeTab === 'analysis'"
            @click="runGeneratedCode"
          >
            {{ running ? '运行中…' : '▶ 运行' }}
          </button>
          <button class="btn-copy" :disabled="!displayCode" @click="copyCode">复制</button>
        </div>

        <div class="code-area" :class="{ 'with-console': showConsole }">
          <pre class="code-block"><code>{{ displayCode || (loading ? '等待生成…' : '生成结果将显示在这里') }}</code></pre>

          <div v-if="showConsole" class="console-panel">
            <div class="console-header">
              <span>{{ htmlPreview ? '页面预览' : '运行输出' }}</span>
              <span v-if="runOutput" class="console-status" :class="{ ok: runOutput.success, fail: !runOutput.success }">
                {{ runOutput.success ? '成功' : '失败' }}
              </span>
              <button class="btn-close-console" @click="showConsole = false; htmlPreview = ''">收起</button>
            </div>
            <iframe
              v-if="htmlPreview"
              class="preview-frame"
              :srcdoc="htmlPreview"
              sandbox="allow-scripts allow-same-origin allow-forms"
              title="HTML 预览"
            />
            <pre v-else class="console-block" :class="{ error: runOutput && !runOutput.success }">{{ consoleText || '运行中…' }}</pre>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.page-header {
  padding: 20px 28px;
  border-bottom: 1px solid var(--border);
}

.page-header h2 {
  font-size: 18px;
  font-weight: 600;
}

.page-header p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.workspace {
  flex: 1;
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 0;
  overflow: hidden;
}

.input-panel {
  padding: 24px;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.input-panel label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
}

.input-panel textarea {
  flex: 1;
  min-height: 160px;
  resize: vertical;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 14px;
  color: var(--text);
  font-size: 14px;
  line-height: 1.6;
  outline: none;
}

.input-panel textarea:focus {
  border-color: var(--accent);
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-primary {
  flex: 1;
  background: var(--accent);
  color: white;
  border: none;
  padding: 12px;
  border-radius: var(--radius);
  font-weight: 600;
  cursor: pointer;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-stop {
  flex: 1;
  background: #dc2626;
  color: white;
  border: none;
  padding: 12px;
  border-radius: var(--radius);
  font-weight: 600;
  cursor: pointer;
}

.phases {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.phase {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  font-size: 13px;
  color: var(--text-muted);
}

.phase.done {
  border-color: var(--success);
  color: var(--success);
}

.phase.active {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

.phase-num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
}

.phase.done .phase-num {
  background: var(--success);
  color: white;
}

.phase.active .phase-num {
  background: var(--accent);
  color: white;
}

.phase-log {
  font-size: 12px;
  color: var(--text-muted);
  white-space: pre-wrap;
  background: var(--bg);
  padding: 10px;
  border-radius: 8px;
  max-height: 120px;
  overflow-y: auto;
}

.output-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-elevated);
  flex-wrap: wrap;
}

.tabs button {
  background: transparent;
  border: none;
  color: var(--text-muted);
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
}

.tabs button.active {
  background: var(--accent-soft);
  color: var(--accent);
}

.lang-select {
  margin-left: auto;
  background: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--text);
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 12px;
}

.btn-run {
  background: var(--success) !important;
  color: white !important;
  font-weight: 600;
  padding: 8px 16px !important;
}

.btn-run:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-copy {
  border: 1px solid var(--border) !important;
}

.code-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.code-area.with-console .code-block {
  flex: 1;
  min-height: 40%;
}

.code-block {
  flex: 1;
  margin: 0;
  padding: 20px 24px;
  overflow: auto;
  background: #0d1117;
  font-family: var(--mono);
  font-size: 13px;
  line-height: 1.7;
  color: #c9d1d9;
}

.code-block code {
  white-space: pre-wrap;
  word-break: break-word;
}

.console-panel {
  border-top: 1px solid var(--border);
  background: #0a0e14;
  display: flex;
  flex-direction: column;
  max-height: 50%;
  min-height: 200px;
}

.preview-frame {
  flex: 1;
  width: 100%;
  min-height: 280px;
  border: none;
  background: white;
}

.console-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  font-size: 12px;
  color: var(--text-muted);
  border-bottom: 1px solid var(--border);
}

.console-status.ok {
  color: var(--success);
}

.console-status.fail {
  color: #f87171;
}

.btn-close-console {
  margin-left: auto;
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 12px;
}

.console-block {
  flex: 1;
  margin: 0;
  padding: 14px 16px;
  overflow: auto;
  font-family: var(--mono);
  font-size: 12px;
  line-height: 1.6;
  color: #7ee787;
  white-space: pre-wrap;
  word-break: break-word;
}

.console-block.error {
  color: #f85149;
}
</style>
