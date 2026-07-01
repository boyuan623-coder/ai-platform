<script setup lang="ts">
defineOptions({ name: 'CodeGenView' })

import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  generateStream,
  generateVueProjectStream,
  generateSmartStream,
  fetchWorkflowGraph,
  downloadVueProjectZip,
  type WorkflowGraph,
  runCode,
  type CodeGenStreamResult,
  type CodeRunResult,
  type VueProjectStreamResult,
  type SmartStreamState,
} from '@/api/codegen'
import { saveAppProject } from '@/api/app'
import {
  detectCodeType,
  ensureFullHtml,
  extractRunnableCode,
  extractScriptFromHtml,
} from '@/utils/codeDetect'
import { buildVueProjectPreview, sortFilePaths } from '@/utils/vuePreview'

type GenMode = 'smart' | 'vue' | 'single'

const route = useRoute()
const appId = computed(() => {
  const id = route.query.appId
  return id ? Number(id) : null
})

const genMode = ref<GenMode>('smart')
const requirement = ref('创建一个待办事项 Vue 应用，支持添加和删除任务，界面简洁美观')
const loading = ref(false)
const running = ref(false)
const result = ref<CodeGenStreamResult>({
  phase: 'INIT',
  phaseLog: '',
  analysis: '',
  plan: '',
  review: '',
  code: '',
  optimized: '',
})
const vueResult = ref<VueProjectStreamResult>({
  phase: 'INIT',
  phaseLog: '',
  sessionId: '',
  entryPath: 'src/App.vue',
  summary: '',
  reply: '',
  files: {},
  activeFile: '',
})
const smartState = ref<SmartStreamState>({
  route: '',
  routeReason: '',
  vue: { phase: 'INIT', phaseLog: '', sessionId: '', entryPath: 'src/App.vue', summary: '', reply: '', files: {}, activeFile: '' },
  single: { phase: 'INIT', phaseLog: '', analysis: '', plan: '', review: '', code: '', optimized: '' },
})
const workflowGraph = ref<WorkflowGraph | null>(null)
const saveMessage = ref('')
const activeTab = ref<'optimized' | 'code' | 'analysis' | 'plan' | 'review'>('optimized')
const runLanguage = ref('auto')
const runOutput = ref<CodeRunResult | null>(null)
const htmlPreview = ref('')
const showConsole = ref(false)
const genError = ref('')
let abortController: AbortController | null = null

const workflowPhases = [
  { key: 'UNDERSTAND', label: '需求理解' },
  { key: 'PLAN', label: '方案规划' },
  { key: 'GENERATE', label: '代码生成' },
  { key: 'REVIEW', label: '代码审查' },
  { key: 'OPTIMIZE', label: '结果优化' },
]

function workflowDoneKeys(stream: CodeGenStreamResult) {
  const done = new Set<string>()
  if (stream.analysis) done.add('UNDERSTAND')
  if (stream.plan) done.add('PLAN')
  if (stream.code) done.add('GENERATE')
  if (stream.review) done.add('REVIEW')
  if (stream.optimized || stream.phase === 'CACHE_HIT') done.add('OPTIMIZE')
  return done
}

const phases = computed(() => {
  if (genMode.value === 'smart') {
    const route = smartState.value.route
    if (route === 'VUE_PROJECT') {
      const n = Object.keys(smartState.value.vue.files).length
      return [
        { key: 'ROUTE', label: '智能路由', done: true },
        { key: 'GENERATE', label: 'Tool Calling', done: n > 0 },
        { key: 'DONE', label: '完成', done: smartState.value.vue.phase === 'DONE' },
      ]
    }
    const single = smartState.value.single
    const doneKeys = workflowDoneKeys(single)
    return [
      { key: 'ROUTE', label: '智能路由', done: !!route },
      ...workflowPhases.map((p) => ({ ...p, done: doneKeys.has(p.key) })),
    ]
  }
  if (genMode.value === 'vue') {
    const fileCount = Object.keys(vueResult.value.files).length
    return [
      { key: 'GENERATE', label: 'Tool Calling 生成', done: fileCount > 0 || vueResult.value.phase === 'DONE' },
      { key: 'DONE', label: '项目完成', done: vueResult.value.phase === 'DONE' },
    ]
  }
  const doneKeys = workflowDoneKeys(result.value)
  return workflowPhases.map((p) => ({
    ...p,
    done: doneKeys.has(p.key),
    active: result.value.phase === p.key,
  }))
})

const filePaths = computed(() => {
  if (genMode.value === 'smart' && smartState.value.route === 'VUE_PROJECT') {
    return sortFilePaths(smartState.value.vue.files)
  }
  if (genMode.value === 'vue') return sortFilePaths(vueResult.value.files)
  return []
})

const activeVueState = computed(() =>
  genMode.value === 'smart' ? smartState.value.vue : vueResult.value,
)

const activeFileContent = computed(() => {
  const f = activeVueState.value.activeFile
  return f ? activeVueState.value.files[f] || '' : ''
})

const runnableCode = computed(() => {
  if (genMode.value === 'smart') {
    if (smartState.value.route === 'VUE_PROJECT') return activeFileContent.value
    if (activeTab.value === 'analysis') return ''
    if (activeTab.value === 'plan') return ''
    if (activeTab.value === 'review') return ''
    return smartState.value.single.optimized || smartState.value.single.code
  }
  if (genMode.value === 'vue') return activeFileContent.value
  if (activeTab.value === 'analysis' || activeTab.value === 'plan' || activeTab.value === 'review') return ''
  return result.value.optimized || result.value.code
})

const displayCode = computed(() => {
  if (genMode.value === 'smart') {
    if (smartState.value.route === 'VUE_PROJECT') return activeFileContent.value
    if (activeTab.value === 'optimized') return smartState.value.single.optimized || smartState.value.single.code
    if (activeTab.value === 'code') return smartState.value.single.code
    if (activeTab.value === 'plan') return smartState.value.single.plan
    if (activeTab.value === 'review') return smartState.value.single.review
    return smartState.value.single.analysis
  }
  if (genMode.value === 'vue') return activeFileContent.value
  if (activeTab.value === 'optimized') return result.value.optimized || result.value.code
  if (activeTab.value === 'code') return result.value.code
  if (activeTab.value === 'plan') return result.value.plan
  if (activeTab.value === 'review') return result.value.review
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

function selectFile(path: string) {
  vueResult.value.activeFile = path
}

async function generate() {
  const text = requirement.value.trim()
  if (!text || loading.value) return

  loading.value = true
  genError.value = ''
  runOutput.value = null
  htmlPreview.value = ''
  showConsole.value = false
  abortController = new AbortController()

  if (genMode.value === 'smart') {
    smartState.value = {
      route: '',
      routeReason: '',
      vue: { phase: 'INIT', phaseLog: '', sessionId: '', entryPath: 'src/App.vue', summary: '', reply: '', files: {}, activeFile: '' },
      single: { phase: 'INIT', phaseLog: '', analysis: '', plan: '', review: '', code: '', optimized: '' },
    }
    try {
      await generateSmartStream(text, (update) => { smartState.value = { ...update } }, abortController.signal)
    } catch (e) {
      const msg = e instanceof Error ? e.message : '未知错误'
      genError.value = msg
      if (smartState.value.route === 'VUE_PROJECT') {
        smartState.value.vue.phaseLog += `\n错误：${msg}`
        smartState.value.vue.phase = 'ERROR'
      } else {
        smartState.value.single.phaseLog += `\n错误：${msg}`
        smartState.value.single.phase = 'ERROR'
      }
    } finally {
      loading.value = false
      abortController = null
    }
    return
  }

  if (genMode.value === 'vue') {
    vueResult.value = {
      phase: 'INIT',
      phaseLog: '',
      sessionId: '',
      entryPath: 'src/App.vue',
      summary: '',
      reply: '',
      files: {},
      activeFile: '',
    }
    try {
      await generateVueProjectStream(
        text,
        (update) => {
          vueResult.value = { ...update }
        },
        abortController.signal,
      )
    } catch (e) {
      const msg = e instanceof Error ? e.message : '未知错误'
      genError.value = msg
      vueResult.value.phaseLog += `\n错误：${msg}`
      vueResult.value.phase = 'ERROR'
    } finally {
      loading.value = false
      abortController = null
    }
    return
  }

  result.value = { phase: 'INIT', phaseLog: '', analysis: '', plan: '', review: '', code: '', optimized: '' }
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
    const msg = e instanceof Error ? e.message : '未知错误'
    genError.value = msg
    result.value.phaseLog += `\n错误：${msg}`
    result.value.phase = 'ERROR'
  } finally {
    loading.value = false
    abortController = null
  }
}

async function runGeneratedCode() {
  const raw = runnableCode.value.trim()
  if (!raw || running.value) return

  if (genMode.value === 'vue' || (genMode.value === 'smart' && smartState.value.route === 'VUE_PROJECT')) {
    running.value = true
    showConsole.value = true
    runOutput.value = null
    const files = genMode.value === 'smart' ? smartState.value.vue.files : vueResult.value.files
    const preview = buildVueProjectPreview(files)
    if (preview) {
      htmlPreview.value = preview
      runOutput.value = {
        success: true,
        language: 'vue',
        stdout: 'Vue 项目预览已加载（基于 App.vue 模板预览）',
        stderr: '',
        exitCode: 0,
        durationMs: 0,
        message: 'Vue 项目预览',
      }
    } else {
      runOutput.value = {
        success: false,
        language: 'vue',
        stdout: '',
        stderr: '无法生成预览，请检查是否包含 src/App.vue',
        exitCode: 1,
        durationMs: 0,
        message: '预览失败',
      }
    }
    running.value = false
    return
  }

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
        stdout: '已在下方加载页面预览。',
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

async function downloadProject() {
  const sessionId = genMode.value === 'smart' ? smartState.value.vue.sessionId : vueResult.value.sessionId
  if (!sessionId) return
  try {
    await downloadVueProjectZip(sessionId)
  } catch (e) {
    saveMessage.value = e instanceof Error ? e.message : '下载失败'
  }
}

async function saveToApp() {
  if (!appId.value) {
    saveMessage.value = '请从「我的应用」进入以绑定应用'
    return
  }
  try {
    if (genMode.value === 'smart' && smartState.value.route !== 'VUE_PROJECT') {
      const code = smartState.value.single.optimized || smartState.value.single.code
      if (!code.trim()) {
        saveMessage.value = '暂无代码可保存'
        return
      }
      const isHtml = smartState.value.route === 'HTML_PAGE'
      await saveAppProject(appId.value, {
        projectType: isHtml ? 'HTML' : 'CODE',
        entryPath: isHtml ? 'index.html' : 'main.txt',
        files: isHtml ? { 'index.html': code } : { 'main.txt': code },
      })
    } else {
      const vue = genMode.value === 'smart' ? smartState.value.vue : vueResult.value
      if (Object.keys(vue.files).length === 0) {
        saveMessage.value = '暂无项目文件可保存'
        return
      }
      await saveAppProject(appId.value, {
        projectType: 'VUE',
        entryPath: vue.entryPath,
        files: vue.files,
      })
    }
    saveMessage.value = '已保存到应用'
  } catch (e) {
    saveMessage.value = e instanceof Error ? e.message : '保存失败'
  }
}

onMounted(async () => {
  try {
    workflowGraph.value = await fetchWorkflowGraph()
  } catch {
    // 工作流定义可选，失败不影响生成
  }
})
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2>AI 代码生成</h2>
        <p>{{ genMode === 'vue' ? 'Vue 工程项目 · Tool Calling 多文件生成' : genMode === 'smart' ? 'AI 智能路由 · LangGraph4j 工作流编排' : 'LangGraph4j 工作流 · 五节点条件编排' }}</p>
      </div>
    </header>

    <p v-if="genError" class="gen-error">{{ genError }}</p>
    <p v-else-if="loading" class="gen-hint">生成中，Vue 工程约 1～3 分钟，单文件工作流约需 2～5 分钟，请耐心等待…</p>

    <div class="workspace">
      <section class="input-panel">
        <div class="mode-switch">
          <button :class="{ active: genMode === 'smart' }" @click="genMode = 'smart'">AI 智能路由</button>
          <button :class="{ active: genMode === 'vue' }" @click="genMode = 'vue'">Vue 工程项目</button>
          <button :class="{ active: genMode === 'single' }" @click="genMode = 'single'">单文件生成</button>
        </div>

        <label>需求描述</label>
        <textarea
          v-model="requirement"
          rows="8"
          :placeholder="genMode === 'vue' ? '描述你想生成的 Vue 应用，例如：待办清单、个人主页' : '描述你想生成的代码'"
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
            :class="{ done: p.done, active: ('active' in p && p.active) || result.phase === p.key || smartState.single.phase === p.key }"
          >
            <span class="phase-num">{{ i + 1 }}</span>
            <span>{{ p.label }}</span>
          </div>
        </div>

        <p v-if="workflowGraph && genMode !== 'vue'" class="workflow-hint">
          工作流：{{ workflowGraph.nodes.map(n => n.label).join(' → ') }}
        </p>

        <pre v-if="genMode === 'smart' && smartState.routeReason" class="phase-log">{{ smartState.routeReason }}</pre>
        <pre v-else-if="genMode === 'vue' && vueResult.summary" class="phase-log">{{ vueResult.summary }}</pre>
        <p v-if="saveMessage" class="save-msg">{{ saveMessage }}</p>
      </section>

      <section class="output-panel">
        <div class="tabs">
          <template v-if="genMode === 'single' || (genMode === 'smart' && smartState.route !== 'VUE_PROJECT')">
            <button :class="{ active: activeTab === 'optimized' }" @click="activeTab = 'optimized'">优化代码</button>
            <button :class="{ active: activeTab === 'code' }" @click="activeTab = 'code'">原始代码</button>
            <button :class="{ active: activeTab === 'analysis' }" @click="activeTab = 'analysis'">需求分析</button>
            <button :class="{ active: activeTab === 'plan' }" @click="activeTab = 'plan'">方案规划</button>
            <button :class="{ active: activeTab === 'review' }" @click="activeTab = 'review'">审查意见</button>
          </template>
          <template v-else>
            <span class="tab-label">文件 {{ filePaths.length }} 个</span>
          </template>

          <button
            v-if="filePaths.length > 0"
            class="btn-secondary"
            @click="downloadProject"
          >
            下载 ZIP
          </button>
          <button
            v-if="appId && filePaths.length > 0"
            class="btn-secondary"
            @click="saveToApp"
          >
            保存到应用
          </button>

          <select v-if="genMode === 'single'" v-model="runLanguage" class="lang-select" title="运行语言">
            <option value="auto">自动检测</option>
            <option value="html">HTML 页面</option>
            <option value="java">Java</option>
            <option value="javascript">JavaScript</option>
            <option value="python">Python</option>
          </select>

          <button
            class="btn-run"
            :disabled="!runnableCode || running || (genMode === 'single' && (activeTab === 'analysis' || activeTab === 'plan' || activeTab === 'review')) || (genMode === 'smart' && smartState.route !== 'VUE_PROJECT' && (activeTab === 'analysis' || activeTab === 'plan' || activeTab === 'review'))"
            @click="runGeneratedCode"
          >
            {{ running ? '运行中…' : (genMode === 'vue' || (genMode === 'smart' && smartState.route === 'VUE_PROJECT')) ? '▶ 预览' : '▶ 运行' }}
          </button>
          <button class="btn-copy" :disabled="!displayCode" @click="copyCode">复制</button>
        </div>

        <div class="code-area" :class="{ 'with-console': showConsole, 'with-filetree': filePaths.length > 0 }">
          <aside v-if="filePaths.length > 0" class="file-tree">
            <div class="file-tree-title">项目文件</div>
            <button
              v-for="path in filePaths"
              :key="path"
              class="file-item"
              :class="{ active: activeVueState.activeFile === path }"
              @click="genMode === 'smart' ? (smartState.vue.activeFile = path) : selectFile(path)"
            >
              {{ path }}
            </button>
          </aside>

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
              title="预览"
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

.gen-error {
  margin: 0 28px 12px;
  padding: 10px 14px;
  border-radius: 8px;
  background: rgba(248, 113, 113, 0.12);
  border: 1px solid rgba(248, 113, 113, 0.35);
  color: #fca5a5;
  font-size: 13px;
}

.gen-hint {
  margin: 0 28px 12px;
  font-size: 13px;
  color: var(--accent);
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

.mode-switch {
  display: flex;
  gap: 8px;
}

.mode-switch button {
  flex: 1;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
}

.mode-switch button.active {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent);
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

.tab-label {
  font-size: 13px;
  color: var(--text-muted);
  padding: 8px 14px;
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

.btn-secondary {
  background: var(--bg-card) !important;
  border: 1px solid var(--border) !important;
  color: var(--text) !important;
  font-size: 12px !important;
  padding: 8px 12px !important;
}

.save-msg {
  font-size: 12px;
  color: var(--success);
}

.workflow-hint {
  font-size: 11px;
  color: var(--text-muted);
  line-height: 1.5;
}

.code-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.code-area.with-filetree {
  flex-direction: row;
}

.file-tree {
  width: 220px;
  border-right: 1px solid var(--border);
  background: var(--bg-elevated);
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-tree-title {
  font-size: 12px;
  color: var(--text-muted);
  padding: 8px 10px;
  font-weight: 600;
}

.file-item {
  text-align: left;
  background: transparent;
  border: none;
  color: var(--text-muted);
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-family: var(--mono);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-item:hover {
  background: rgba(255, 255, 255, 0.04);
  color: var(--text);
}

.file-item.active {
  background: var(--accent-soft);
  color: var(--accent);
}

.code-area.with-filetree .code-block {
  flex: 1;
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
  flex: 1;
}

.with-filetree .console-panel {
  width: 100%;
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
