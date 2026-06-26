# 🛠️ 代码工匠 (Code Craftsman)

> RAG 智能助手 + 代码生成工作流 — 基于 LangChain4j 与 Spring Boot 3 的模块化 AI 平台

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-4fc08d)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

---

## ✨ 功能特色

| 模块 | 能力 |
|------|------|
| 🤖 **智能对话** | 通用问答 + RAG 文档检索 + Tool Calling 预约 |
| 🧠 **RAG 检索增强** | PDF/文档 Embedding → 向量相似度搜索 → 精准回答 |
| 📝 **代码生成** | LangGraph4j 工作流引擎，分阶段流式 SSE 输出 |
| 🔄 **流式交互** | 实时 Token 级别流式推送，打字机效果 |
| 💾 **多级缓存** | Caffeine L1 + Redis L2，会话记忆持久化 |
| 🗄️ **工具调用** | AI 自动识别意图 → 调用预约 Tool → 写入数据库 |

---

## 🏗️ 架构

```
┌─────────────────────────────────────────────┐
│                 Frontend (Vue 3)              │
│           SSE 流式 / REST API 交互             │
└────────────────────┬────────────────────────┘
                     │
┌────────────────────▼────────────────────────┐
│          platform-bootstrap (启动入口)          │
│   ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│   │ module-  │  │ module-  │  │ platform-│  │
│   │appointment│  │codegen  │  │  rag     │  │
│   └─────┬────┘  └────┬────┘  └────┬─────┘  │
│         │            │            │         │
│   ┌─────▼────────────▼────────────▼─────┐   │
│   │       platform-ai-core              │   │
│   │  LangChain4j / DeepSeek / Memory    │   │
│   └────────────────┬───────────────────┘   │
│   ┌────────────────▼───────────────────┐   │
│   │  platform-infra / platform-cache   │   │
│   │  MyBatis-Plus / Redis / Caffeine   │   │
│   └────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

## 🚀 快速启动

### 环境要求

- **JDK 21+**
- **Maven 3.9+**
- **Docker**（MySQL 8.0 + Redis 7）

### 1. 启动基础设施

```bash
docker-compose up -d
```

### 2. 配置 API Key

```bash
# Windows
set DEEPSEEK_API_KEY=你的DeepSeek密钥

# Linux / macOS
export DEEPSEEK_API_KEY=你的DeepSeek密钥
```

> 💡 可在 [DeepSeek 开放平台](https://platform.deepseek.com/) 获取 API Key

### 3. 编译运行

```bash
mvn clean package -DskipTests
java -jar platform-bootstrap/target/platform-bootstrap-1.0.0-SNAPSHOT.jar
```

或使用启动脚本（Windows）：

```bash
run.bat
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 **http://localhost:5173**

---

## 📡 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/appointment/chat` | 同步对话 + RAG + Tool |
| `GET` | `/api/appointment/chat/stream` | 流式对话 + RAG + Tool |
| `POST` | `/api/codegen/generate` | 代码生成工作流 |
| `GET` | `/api/codegen/generate/stream` | 流式代码生成 |
| `GET` | `/actuator/health` | 健康检查 |

---

## 📂 项目结构

```
ai-platform/
├── platform-common        # 公共 API、常量、异常
├── platform-infra         # MyBatis-Plus、Redis 基础设施
├── platform-cache         # Caffeine + Redis 多级缓存
├── platform-ai-core       # LangChain4j 核心（模型、记忆、作用域）
├── platform-rag           # Easy RAG（Embedding → 向量检索）
├── platform-workflow      # LangGraph4j 工作流引擎
├── module-appointment     # 智能对话 + 预约 Tool Calling
├── module-codegen         # 代码生成 + 流式 SSE
├── platform-bootstrap     # 统一启动入口（模块化单体）
├── frontend               # Vue 3 + Vite 前端
├── scripts                # 辅助脚本
└── docs                   # 文档
```

---

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| 框架 | Spring Boot 3.5、LangChain4j 1.0.1-beta6 |
| 工作流 | LangGraph4j 1.8.19 |
| AI 模型 | DeepSeek (OpenAI 兼容) |
| 向量/Embedding | all-MiniLM-L6-v2 (本地) |
| 数据库 | MySQL 8.0 + MyBatis-Plus 3.5 |
| 缓存 | Redis 7 + Caffeine |
| 前端 | Vue 3 + TypeScript + Vite |
| 监控 | Spring Actuator + Prometheus + Grafana |

---

## 🔒 安全提示

- **API Key 通过环境变量 `DEEPSEEK_API_KEY` 注入，不要硬编码到配置文件中**
- 仓库中的 `application.yml` 仅包含占位符 `your-deepseek-api-key-here`
- `.env` 文件已加入 `.gitignore`，不会被提交

---

## 📋 演进路线

- [x] Phase 1 — 模块化单体
- [ ] Phase 2 — 微服务拆分（Nacos + Gateway + Dubbo）
- [ ] Phase 3 — Docker 一键部署 + 全生命周期监控

---

## 📄 License

Apache 2.0 © 2025
