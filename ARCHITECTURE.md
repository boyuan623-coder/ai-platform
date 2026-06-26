# 代码工匠 架构说明

## 模块分层

```
ai-platform/
├── platform-common          # 公共 API、常量、异常
├── platform-infra           # MyBatis-Plus、Redis 基础设施
├── platform-cache           # Caffeine + Redis 多级缓存
├── platform-ai-core         # LangChain4j 核心（模型、记忆、Prototype 作用域）
├── platform-rag             # Easy RAG（PDF 切割 → Embedding → Redis 向量库）
├── platform-workflow        # LangGraph4j 工作流引擎
├── module-appointment       # 智能对话（通用问答 + RAG + 预约 Tool Calling）
├── module-codegen           # 业务二：代码生成工作流 + 流式 SSE
└── platform-bootstrap       # 统一启动入口（模块化单体）
```

## 请求路由

| 路径 | 模块 | 能力 |
|------|------|------|
| `POST /api/appointment/chat` | module-appointment | 通用对话 + RAG + Tool 同步 |
| `GET /api/appointment/chat/stream` | module-appointment | 通用对话 + RAG + Tool 流式 SSE |
| `POST /api/codegen/generate` | module-codegen | LangGraph4j 工作流生成 |
| `GET /api/codegen/generate/stream` | module-codegen | 分阶段流式输出 |

## 演进路径（单体 → 微服务）

```
Phase 1（当前）: 模块化单体
  platform-bootstrap 聚合所有模块，一个 JAR 部署

Phase 2: 服务拆分
  appointment-service  ← module-appointment + platform-rag
  codegen-service      ← module-codegen + platform-workflow
  + Nacos 注册 + Gateway 网关 + Dubbo RPC

Phase 3: 全生命周期
  + Docker 一键部署 + Nginx 预览 + 源码下载
  + Prometheus + Grafana 监控
```

## 快速启动

```bash
# 1. 启动基础设施
docker-compose up -d

# 2. 配置 API Key
set DEEPSEEK_API_KEY=sk-xxx

# 3. 编译运行
mvn clean package -DskipTests
java -jar platform-bootstrap/target/platform-bootstrap-1.0.0-SNAPSHOT.jar
```

## 环境要求

- JDK 21+
- Maven 3.9+
- Docker（MySQL + Redis）
