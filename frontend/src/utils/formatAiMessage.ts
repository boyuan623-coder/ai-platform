/**
 * 将 AI 挤在一起的回复按常见模式自动分行，提升可读性。
 */
export function formatAiMessage(text: string): string {
  if (!text) return ''

  let s = text.trim()

  // emoji / 问候语后换行
  s = s.replace(/([😊👋✨！!])\s*(?=[我您你])/g, '$1\n\n')

  // 「如下：1.」类列表前换行
  s = s.replace(/：\s*(\d+\.)/g, '：\n\n$1')
  s = s.replace(/([。！？])\s*(\d+\.)/g, '$1\n\n$2')

  // 数字列表项前换行
  s = s.replace(/(?<!\n)(\d+\.)\s*(\*\*)/g, '\n$1 $2')

  // 「等2.」「吗3.」等紧贴的数字列表前换行
  s = s.replace(/([^\d\n])(\d+\.\s*\*\*)/g, '$1\n\n$2')

  // 「**标题**—内容」格式
  s = s.replace(/\*\*([^*]+)\*\*—([^*\d]+?)(?=\d+\.)/g, '**$1** — $2\n\n')
  s = s.replace(/\*\*([^*]+)\*\*—/g, '**$1** — ')

  // 「·」列表每项单独一行
  s = s.replace(/：\s*·/g, '：\n\n·')
  s = s.replace(/([^\n])\s*·\s*/g, '$1\n· ')

  // 服务时间等关键信息单独成行
  s = s.replace(/(服务时间[：:])/g, '\n\n$1')
  s = s.replace(/(可预约[^：]{0,20}[：:])/g, '\n$1')

  // 结尾问候单独成行
  s = s.replace(/([。！？])\s*(有什么|请问|还需要|还有其他)/g, '$1\n\n$2')

  // 预约查询结果
  s = formatAppointmentQuery(s)

  // 合并多余空行
  s = s.replace(/\n{3,}/g, '\n\n')

  return s.trim()
}

/** 预约查询类回复的专用分行 */
function formatAppointmentQuery(s: string): string {
  // 查询引导句与结果之间分段
  s = s.replace(/。(查询到|未找到)/g, '。\n\n$1')
  s = s.replace(/(预约记录)\*\*：/g, '$1**：\n')
  s = s.replace(/(预约记录)：(?!\n)/g, '$1：\n')

  // 分隔线单独成行
  s = s.replace(/：\s*---+/g, '：\n\n---')
  s = s.replace(/---+/g, '\n---\n')

  // 「预约N」标题前换行
  s = s.replace(/([^\n])(\*\*预约\s*\d+\*\*)/g, '$1\n\n$2')

  // 「-**字段：**」转为列表项
  s = s.replace(/-\*\*([^*]+)：\*\*/g, '\n- **$1：**')

  // 字段值后接下一条「-**」时换行
  s = s.replace(/([^-\n])-\*\*/g, '$1\n- **')

  // 状态结束后接下一条预约前换行
  s = s.replace(/(已确认|待确认|已取消|已完成)(\*\*预约)/g, '$1\n\n$2')

  // 工具返回的「订单N | ...」格式
  s = s.replace(/([^\n])(订单\d+\s*\|)/g, '$1\n$2')

  // 取消提示单独成段（热线保持在括号内）
  s = s.replace(/(如需取消预约)/g, '\n\n$1')

  return s
}
