# 🛠️ AI 零代码应用生成平台

> 对齐编程导航 yu-ai-code-mother 架构 — LangChain4j + LangGraph4j + 微服务 AI 平台

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-4fc08d)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

---

## ✨ 功能特色

| 模块 | 能力 |
|------|------|
| 👤 **用户系统** | 注册/登录/Redis Session/权限 |
| 📦 **应用管理** | 创建/编辑/删除/精选应用 |
| 💬 **对话历史** | MySQL 持久化 + 游标分页 |
| 🤖 **智能对话** | RAG 文档检索 + Tool Calling 预约 |
| 📝 **代码生成** | LangGraph4j 工作流 + 流式 SSE |
| 🔄 **流式交互** | 实时 Token 级别流式推送 |
| 💾 **多级缓存** | Caffeine L1 + Redis L2 |

---

## 🏗️ 架构

### 模块分层

```mermaid
graph TB
    subgraph FRONTEND["🎨 表现层"]
        VUE["Vue 3 + Vite<br/>SSE 流式 / REST API"]
    end

    subgraph BOOTSTRAP["🚀 启动层"]
        APP["platform-bootstrap<br/>Spring Boot 3.5 · 统一入口"]
    end

    subgraph BUSINESS["📦 业务层"]
        direction LR
        APMT["module-appointment<br/>智能对话 · Tool Calling"]
        CG["module-codegen<br/>代码生成 · 流式 SSE"]
        RAG["platform-rag<br/>文档解析 · Embedding · 向量检索"]
        WF["platform-workflow<br/>LangGraph4j 工作流"]
    end

    subgraph CORE["🧠 AI 核心层"]
        AI["platform-ai-core<br/>LangChain4j · DeepSeek<br/>ChatMemory · Prototype 作用域"]
    end

    subgraph INFRA["⚙️ 基础设施层"]
        direction LR
        CACHE["platform-cache<br/>Caffeine L1 · Redis L2"]
        INF["platform-infra<br/>MyBatis-Plus · Redis"]
        COM["platform-common<br/>API 定义 · 异常 · 常量"]
    end

    subgraph DATA["💾 数据层"]
        direction LR
        MYSQL[("MySQL 8.0<br/>业务数据")]
        REDIS[("Redis 7<br/>会话 · 缓存 · 向量")]
    end

    VUE --> APP
    APP --> APMT
    APP --> CG
    APMT --> RAG
    CG --> WF
    APMT & CG & RAG & WF --> AI
    AI & APMT --> CACHE
    AI & CG --> INF
    CACHE --> COM
    INF --> COM
    INF --> MYSQL
    INF --> REDIS
    CACHE --> REDIS
```

### 请求链路

```mermaid
sequenceDiagram
    actor User as 👤 用户
    participant V as Vue 前端
    participant C as Controller
    participant AI as AI Core<br/>(LangChain4j)
    participant RAG as RAG 引擎
    participant LLM as DeepSeek API
    participant DB as MySQL

    User->>V: 发送消息
    V->>C: POST/GET (SSE)
    C->>AI: 构建 Prompt + 加载记忆

    alt 需要文档检索
        AI->>RAG: 语义搜索
        RAG-->>AI: 相关文档片段
    end

    AI->>LLM: 调用 DeepSeek (含 Tools)
    LLM-->>AI: 流式返回 / Tool Call

    alt Tool Calling 触发
        AI->>DB: 查询/写入业务数据
        DB-->>AI: 结果
        AI->>LLM: 回传 Tool 结果
        LLM-->>AI: 最终回答
    end

    AI->>AI: 保存会话记忆
    AI-->>C: 流式推送 Token
    C-->>V: SSE 事件流
    V-->>User: 🌊 打字机效果展示
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
| `POST` | `/api/user/register` | 用户注册 |
| `POST` | `/api/user/login` | 用户登录 |
| `GET` | `/api/user/me` | 当前用户 |
| `POST` | `/api/app` | 创建应用 |
| `GET` | `/api/app/mine` | 我的应用列表 |
| `GET` | `/api/app/featured` | 精选应用 |
| `POST` | `/api/chat/message` | 保存对话消息 |
| `GET` | `/api/chat/history` | 游标分页历史 |
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
├── platform-ai-core       # LangChain4j 核心
├── platform-rag           # Easy RAG
├── platform-workflow      # LangGraph4j 工作流
├── module-user            # 用户模块
├── module-app             # 应用模块
├── module-chat            # 对话历史模块
├── module-appointment     # 智能对话 + Tool Calling
├── module-codegen         # 代码生成 + SSE
├── platform-bootstrap     # Phase 1 单体启动
├── platform-gateway       # Phase 2 Gateway
├── service-user           # 用户/应用/对话微服务
├── service-appointment    # 对话微服务
├── service-codegen        # 代码生成微服务
├── frontend               # Vue 3 前端
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

- [x] Phase 1 — 模块化单体（用户/应用/对话/AI 生成）
- [x] Phase 2 — 微服务骨架（Nacos + Gateway + 3 服务）
- [ ] Phase 3 — 可视化编辑、一键部署、Prometheus 监控

测试账号请参见 `platform-bootstrap/src/main/resources/db/init.sql` 中的种子数据

---

## 📄 License

Apache 2.0 © 2025
