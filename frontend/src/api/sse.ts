export async function parseSseStream(
  url: string,
  onData: (data: string) => void,
  signal?: AbortSignal,
  headers?: Record<string, string>,
): Promise<void> {
  const res = await fetch(url, { signal, headers })
  if (!res.ok) {
    throw new Error(`请求失败 (${res.status})`)
  }
  if (!res.body) {
    throw new Error('响应体为空')
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split('\n\n')
    buffer = events.pop() ?? ''

    for (const event of events) {
      for (const line of event.split('\n')) {
        if (line.startsWith('data:')) {
          const data = line.slice(5).trimStart()
          if (data) onData(data)
        }
      }
    }
  }

  if (buffer.trim()) {
    for (const line of buffer.split('\n')) {
      if (line.startsWith('data:')) {
        const data = line.slice(5).trimStart()
        if (data) onData(data)
      }
    }
  }
}
