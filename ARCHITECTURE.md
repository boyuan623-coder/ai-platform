# 架构说明 — AI 零代码平台

## 模块

| 模块 | 职责 |
|------|------|
| platform-bootstrap | 单体启动（默认） |
| platform-gateway + service-* | 微服务（可选） |
| module-user/app/chat | 用户、应用、对话 |
| module-codegen + platform-workflow | LangGraph 工作流 + Vue 生成 |
| module-appointment + platform-rag | RAG 智能助手 |

## 启动方式

```bash
# 单体（开发推荐）
docker-compose up -d
mvn clean package -DskipTests
java -jar platform-bootstrap/target/platform-bootstrap-1.0.0-SNAPSHOT.jar

# 微服务 + 监控
mvn clean package -DskipTests
docker-compose --profile monitoring --profile microservices up -d --build
```

详见 [scripts/start-microservices.md](scripts/start-microservices.md)

## 企业级能力

| 能力 | 配置 |
|------|------|
| Dubbo 鉴权 | service-codegen/appointment 自动走 RPC |
| Zipkin 链路 | `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT` |
| Prometheus/Grafana | `--profile monitoring` |
| COS 部署 | `COS_ENABLED=true` + 云存储密钥 |
| Selenium 封面 | `SELENIUM_ENABLED=true` + Chrome |

## 演示账号

测试账号请参见种子数据 `platform-bootstrap/src/main/resources/db/init.sql`
