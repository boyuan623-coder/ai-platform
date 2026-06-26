/** 从生成结果中提取可运行代码（优先识别 html / java / js 代码块） */
export function extractRunnableCode(raw: string): string {
  const text = raw.trim()
  if (!text) return ''

  const htmlBlock = text.match(/```html\s*([\s\S]*?)```/i)
  if (htmlBlock) return htmlBlock[1].trim()

  const jsBlock = text.match(/```(?:javascript|js)\s*([\s\S]*?)```/i)
  if (jsBlock) return jsBlock[1].trim()

  const javaBlock = text.match(/```(?:java)\s*([\s\S]*?)```/i)
  if (javaBlock) return javaBlock[1].trim()

  const pyBlock = text.match(/```(?:python|py)\s*([\s\S]*?)```/i)
  if (pyBlock) return pyBlock[1].trim()

  const anyBlock = text.match(/```\w*\s*([\s\S]*?)```/)
  if (anyBlock) return anyBlock[1].trim()

  return text
}

export type CodeRunType = 'html' | 'javascript' | 'java' | 'python'

export function detectCodeType(code: string): CodeRunType {
  const lower = code.trim().toLowerCase()

  if (
    lower.includes('<!doctype html') ||
    lower.includes('<html') ||
    (lower.includes('<head') && lower.includes('<body')) ||
    (lower.includes('<div') && lower.includes('<script') && lower.includes('</script>'))
  ) {
    return 'html'
  }

  if (code.includes('public class') || code.includes('import java.')) {
    return 'java'
  }

  if (lower.includes('def ') && (lower.includes('print(') || lower.includes('import '))) {
    return 'python'
  }

  return 'javascript'
}

/** 补全不完整的 HTML 片段 */
export function ensureFullHtml(code: string): string {
  const trimmed = code.trim()
  if (trimmed.toLowerCase().includes('<html')) {
    return trimmed
  }
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>预览</title>
</head>
<body>
${trimmed}
</body>
</html>`
}

/** 从 HTML 中提取 script 内容（纯 JS 场景） */
export function extractScriptFromHtml(html: string): string | null {
  const scripts = [...html.matchAll(/<script[^>]*>([\s\S]*?)<\/script>/gi)]
  if (scripts.length === 0) return null
  return scripts.map((m) => m[1].trim()).join('\n\n')
}
