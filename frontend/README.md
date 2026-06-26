# Vue 3 前端

## 启动

```bash
# 1. 确保后端已运行在 8080
# 2. 安装依赖并启动
cd frontend
npm install
npm run dev
```

浏览器访问：**http://localhost:5173**

## 页面

| 路由 | 功能 |
|------|------|
| `/appointment` | 代码工匠智能对话（通用问答 + RAG + Tool + 流式） |
| `/codegen` | AI 代码生成（LangGraph4j 工作流 + 流式输出） |

## 技术栈

- Vue 3 + TypeScript + Vite
- Vue Router
- Fetch SSE 对接后端流式接口

## 构建

```bash
npm run build
# 产物在 frontend/dist，可部署到 Nginx
```
