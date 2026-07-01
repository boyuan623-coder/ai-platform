# Phase 2 微服务拆分

## 基础设施

```bash
docker-compose up -d   # MySQL + Redis + Nacos
```

## 启动顺序

1. Nacos — http://localhost:8848/nacos
2. `service-user` — 8083（用户/应用/对话）
3. `service-appointment` — 8081（RAG 对话）
4. `service-codegen` — 8082（代码生成）
5. `platform-gateway` — 8080（统一入口，前端无需改端口）

## 构建

```bash
mvn clean package -DskipTests -pl platform-gateway,service-user,service-appointment,service-codegen -am
```

## Gateway 路由

| 路径前缀 | 目标服务 |
|----------|----------|
| `/api/user/**` `/api/app/**` `/api/chat/**` | service-user |
| `/api/appointment/**` | service-appointment |
| `/api/codegen/**` | service-codegen |

## Docker 全栈（可选）

取消 `docker-compose.yml` 中微服务相关注释，先执行 `mvn package`，再 `docker-compose up --build`。
