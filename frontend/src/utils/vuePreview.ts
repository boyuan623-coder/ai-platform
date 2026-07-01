/**
 * 将 Vue 工程项目打包为可在 iframe 中预览的 HTML（Vue 3 运行时）
 */
export function buildVueProjectPreview(files: Record<string, string>): string | null {
  const indexHtml = files['index.html']
  if (indexHtml?.trim() && canPreviewIndexHtml(indexHtml)) {
    return patchIndexHtml(indexHtml, files)
  }

  const appPath = findAppVuePath(files)
  if (!appPath) {
    return null
  }

  return buildRuntimePreviewHtml(files, appPath)
}

function findAppVuePath(files: Record<string, string>): string | null {
  if (files['src/App.vue']) return 'src/App.vue'
  return Object.keys(files).find((p) => /App\.vue$/i.test(p)) ?? null
}

function canPreviewIndexHtml(html: string): boolean {
  const lower = html.toLowerCase()
  if (lower.includes('type="module"') || lower.includes("type='module'")) return false
  if (/\/src\/main\.(ts|js)/i.test(html)) return false
  if (lower.includes('@vite') || lower.includes('vite/client')) return false
  return true
}

interface ComponentDef {
  name: string
  template: string
  setup: string
  style: string
}

function buildRuntimePreviewHtml(files: Record<string, string>, appPath: string): string {
  const appParsed = parseVueSfc(files[appPath])
  const childDefs: ComponentDef[] = []

  for (const [path, content] of Object.entries(files)) {
    if (!path.endsWith('.vue') || path === appPath) continue
    const parsed = parseVueSfc(content)
    if (parsed.isOptionsApi) continue
    childDefs.push({
      name: componentNameFromPath(path),
      template: parsed.template,
      setup: buildSetupScript(parsed.scriptBody),
      style: parsed.style,
    })
  }

  const globalCss = collectGlobalCss(files)
  const appSetup = appParsed.isOptionsApi ? null : buildSetupScript(appParsed.scriptBody)

  const appLiteral = appParsed.isOptionsApi && appParsed.optionsApiLiteral
    ? `${appParsed.optionsApiLiteral}`
    : `{
        name: 'App',
        components: __components,
        template: ${JSON.stringify(appParsed.template)},
        setup() {
          ${appSetup ?? 'return {};'};
        }
      }`

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <script src="https://unpkg.com/vue@3/dist/vue.global.prod.js"><\/script>
  <style>${escapeStyle(globalCss)}</style>
  <style>${escapeStyle(appParsed.style)}</style>
  ${childDefs.map((c) => `<style>${escapeStyle(c.style)}</style>`).join('\n  ')}
  <style>
    body { margin: 0; padding: 16px; font-family: system-ui, sans-serif; background: #fff; color: #111; }
    #app { min-height: 120px; }
    .preview-error { color: #b91c1c; background: #fef2f2; padding: 12px; border-radius: 8px; white-space: pre-wrap; font-size: 13px; }
  </style>
</head>
<body>
  <div id="app"></div>
  <script>
    const { createApp, ref, reactive, computed, watch, onMounted, onUnmounted } = Vue;

    function __execSetup(code) {
      return new Function('ref', 'reactive', 'computed', 'watch', 'onMounted', 'onUnmounted', code)(
        ref, reactive, computed, watch, onMounted, onUnmounted
      );
    }

    const __childDefs = ${JSON.stringify(childDefs)};
    const __components = {};
    for (const def of __childDefs) {
      __components[def.name] = {
        name: def.name,
        template: def.template,
        setup() {
          return __execSetup(def.setup);
        }
      };
    }

    try {
      const App = ${appLiteral};
      createApp(App).mount('#app');
    } catch (e) {
      document.getElementById('app').innerHTML =
        '<div class="preview-error"><strong>预览渲染失败</strong>\\n' + (e && e.message ? e.message : String(e)) + '</div>';
    }
  </script>
  <p style="color:#888;font-size:12px;padding:8px;border-top:1px solid #eee;margin-top:16px">
    Vue 3 运行时预览 · 完整 Vite 工程请下载 ZIP 后执行 npm install && npm run dev
  </p>
</body>
</html>`
}

interface ParsedSfc {
  template: string
  style: string
  scriptBody: string
  isSetup: boolean
  isOptionsApi: boolean
  optionsApiLiteral?: string
}

function parseVueSfc(content: string): ParsedSfc {
  const templateMatch = content.match(/<template[^>]*>([\s\S]*?)<\/template>/i)
  const styleMatches = [...content.matchAll(/<style[^>]*>([\s\S]*?)<\/style>/gi)]
  const setupMatch = content.match(/<script[^>]*setup[^>]*>([\s\S]*?)<\/script>/is)
  const scriptMatch = content.match(/<script(?![^>]*setup)[^>]*>([\s\S]*?)<\/script>/is)

  const template = templateMatch ? templateMatch[1].trim() : '<p>Vue App</p>'
  const style = styleMatches.map((m) => m[1].trim()).join('\n')
  const scriptBody = (setupMatch?.[1] ?? scriptMatch?.[1] ?? '').trim()
  const isSetup = Boolean(setupMatch)
  const isOptionsApi = !isSetup && /export\s+default/.test(scriptBody)

  let optionsApiLiteral: string | undefined
  if (isOptionsApi) {
    optionsApiLiteral = scriptBody
      .replace(/export\s+default\s+/, '')
      .replace(/import\s+[\s\S]*?from\s+['"][^'"]+['"];?/g, '')
      .replace(/import\s+['"][^'"]+['"];?/g, '')
      .trim()
    if (optionsApiLiteral.endsWith(';')) {
      optionsApiLiteral = optionsApiLiteral.slice(0, -1)
    }
  }

  return { template, style, scriptBody, isSetup, isOptionsApi, optionsApiLiteral }
}

function buildSetupScript(body: string): string {
  if (!body.trim()) return 'return {};'

  const stripped = stripImports(body)
    .replace(/defineProps\s*\([^)]*\)/g, '({})')
    .replace(/defineEmits\s*\([^)]*\)/g, '() => {}')
    .replace(/defineExpose\s*\([^)]*\)/g, '')

  const bindings = extractBindingNames(stripped)
  const returnStmt = bindings.length > 0 ? `return { ${bindings.join(', ')} };` : 'return {};'
  return `${stripped}\n${returnStmt}`
}

function stripImports(body: string): string {
  return body
    .replace(/import\s+type\s+[\s\S]*?from\s+['"][^'"]+['"];?/g, '')
    .replace(/import\s+[\s\S]*?from\s+['"][^'"]+['"];?/g, '')
    .replace(/import\s+['"][^'"]+['"];?/g, '')
}

function extractBindingNames(body: string): string[] {
  const names = new Set<string>()
  const patterns = [
    /(?:const|let)\s+([A-Za-z_$][\w$]*)\s*=/g,
    /function\s+([A-Za-z_$][\w$]*)\s*\(/g,
  ]
  for (const re of patterns) {
    let m: RegExpExecArray | null
    while ((m = re.exec(body)) !== null) {
      names.add(m[1])
    }
  }
  return [...names]
}

function componentNameFromPath(path: string): string {
  const base = path.split('/').pop()?.replace(/\.vue$/i, '') ?? 'Component'
  return base.charAt(0).toUpperCase() + base.slice(1)
}

function collectGlobalCss(files: Record<string, string>): string {
  return Object.entries(files)
    .filter(([p]) => p.endsWith('.css'))
    .map(([, c]) => c)
    .join('\n')
}

function escapeStyle(css: string): string {
  return css.replace(/<\/style/gi, '<\\/style')
}

function patchIndexHtml(indexHtml: string, files: Record<string, string>): string {
  let html = indexHtml
  for (const [path, content] of Object.entries(files)) {
    if (path.endsWith('.html')) continue
    if (path.endsWith('.css')) {
      const linkPattern = new RegExp(`href=["']/?${escapeRegExp(path)}["']`, 'g')
      if (linkPattern.test(html)) {
        html = html.replace(
          linkPattern,
          `href="data:text/css;base64,${btoa(unescape(encodeURIComponent(content)))}"`,
        )
      }
      continue
    }
    const srcPattern = new RegExp(`src=["']/?${escapeRegExp(path)}["']`, 'g')
    if (srcPattern.test(html)) {
      const dataUri = `data:text/javascript;base64,${btoa(unescape(encodeURIComponent(content)))}`
      html = html.replace(srcPattern, `src="${dataUri}"`)
    }
  }
  if (!html.includes('vue.global') && !html.includes('vue@3')) {
    html = html.replace(
      '</head>',
      '  <script src="https://unpkg.com/vue@3/dist/vue.global.prod.js"><\/script>\n</head>',
    )
  }
  return html
}

function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export function sortFilePaths(files: Record<string, string>): string[] {
  return Object.keys(files).sort((a, b) => {
    const depthA = a.split('/').length
    const depthB = b.split('/').length
    if (depthA !== depthB) return depthA - depthB
    return a.localeCompare(b)
  })
}
