# IntelliJ IDEA 导入与运行指南

## 1. 环境准备

| 工具 | 版本 |
|------|------|
| IntelliJ IDEA | 2023.3+（推荐 Ultimate，Community 也可） |
| JDK | 21（`C:\Program Files\Java\jdk-21.0.11`） |
| Maven | IDEA 内置或 3.9+ |
| 插件 | **Lombok**（Settings → Plugins 搜索安装） |

## 2. 导入项目

1. 打开 IDEA → **File → Open**
2. 选择 `f:\java\chatbot\pom.xml`（选根目录 pom，不要只开子模块）
3. 选择 **Open as Project**
4. 等待 Maven 依赖下载完成（右下角进度条）

## 3. 配置 JDK 21

1. **File → Project Structure → Project**
2. SDK 选择 **21**（若无则 Add SDK → 选 `C:\Program Files\Java\jdk-21.0.11`）
3. Language level 选 **21**

## 4. 启用 Lombok

1. **Settings → Build → Compiler → Annotation Processors**
2. 勾选 **Enable annotation processing**

## 5. 初始化 MySQL 数据库

**方式一（推荐）：IDEA 运行 DbInit**

1. 确认 MySQL80 服务已启动（`services.msc` → MySQL80 → 运行中）
2. 右上角运行配置选 **DbInit** → 运行
3. 若 root 密码不是 `root`，在运行配置 Environment variables 加：
   ```
   MYSQL_PASSWORD=你的密码
   ```

**方式二：命令行脚本**

```bat
scripts\init-mysql.bat
```

将自动创建：
- 数据库 `volunteer`
- 表 `appointment_order`
- 6 条示例预约数据

## 6. 运行项目

项目已预置运行配置 **AiPlatformApplication**：

1. 右上角运行配置下拉 → 选 **AiPlatformApplication**
2. 编辑配置 → Environment variables 填入：
   ```
   DEEPSEEK_API_KEY=sk-你的密钥
   ```
3. Active profiles 已设为 `local`（使用 `application-local.yml`）
4. 点击绿色运行按钮

或直接打开 `platform-bootstrap/src/main/java/com/chatbot/AiPlatformApplication.java`，点击类旁绿色三角运行。

## 7. Maven 面板

IDEA 右侧 **Maven** 面板可看到 9 个子模块：

```
ai-platform
├── platform-common
├── platform-infra
├── platform-cache
├── platform-ai-core
├── platform-rag
├── platform-workflow
├── module-appointment      ← 预约业务代码
├── module-codegen          ← 代码生成业务代码
└── platform-bootstrap      ← 启动模块（运行这个）
```

## 8. 开发时改哪个模块

| 需求 | 编辑模块 |
|------|----------|
| 预约 Tool / 对话 | `module-appointment` |
| RAG 文档检索 | `platform-rag` |
| 代码生成工作流 | `platform-workflow` |
| LangChain4j 配置 | `platform-ai-core` |
| 接口与启动配置 | `platform-bootstrap` |

## 9. 常见问题

**Q: 模块报红 / 找不到符号**
→ Maven 面板点刷新按钮，或 **File → Invalidate Caches → Restart**

**Q: Lombok getter/setter 报红**
→ 确认 Lombok 插件已安装且 Annotation Processing 已开启

**Q: 运行报 MySQL / Redis 连接失败**
→ 先启动本地服务：
  - Redis：双击 `scripts\start-redis.bat`（使用 `F:\Redis-x64-3.2.100`）
  - MySQL：创建 `volunteer` 库并执行 `platform-bootstrap/src/main/resources/db/schema.sql`
→ 或修改 `application-local.yml` 中的连接信息

**Q: Redis 3.2 能否用于 RAG 向量库？**
→ 不能。Redis 3.2 缺少 RediSearch 模块。`local` profile 已自动改用内存向量库，Redis 仅用于会话记忆和缓存。
→ Project Structure 中确认 SDK 为 21，不要用 11 或 17
