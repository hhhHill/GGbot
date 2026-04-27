# GGbot

GGbot 是一个基于 Java 17、Spring Boot 3.4 和 Spring AI 构建的 AI Agent 后端工程。  
它的目标不是做一个只绑定某个平台的机器人 Demo，而是作为一个可继续演进的 Agent 基础工程：

- Feishu 只是 Adapter
- Web API 是另一个入口
- Agent 核心独立于接入渠道
- Spring AI 负责模型、工具、记忆等基础设施接入

## 当前状态

当前仓库已经不再是“纯 API 骨架”，而是一个可以直接启动并看到效果的 MVP：

- `GET /`：浏览器可直接打开的单页聊天控制台
- `GET /health`：探活接口
- `POST /api/agent/chat`：普通 Web Agent 调用入口
- `POST /feishu/webhook`：Feishu Webhook 入口

当前主链路已经具备：

- 循环式 Agent 运行模型
- 规则规划
- 执行反馈
- 反思与重规划
- 文档 / PPT / 总结类工具
- Spring AI `ChatClient`
- Spring AI `ChatMemory`
- Spring AI tool objects / `ToolCallbackProvider`

## 架构图

```text
 Browser MVP / Feishu / API Client
             |
             v
   +--------------------------+
   | adapter.web / feishu     |
   +------------+-------------+
                |
                v
        +---------------+
        | AgentService  |
        +-------+-------+
                |
                v
        +---------------+
        | AgentRunner   |
        | loop engine   |
        +---+---+---+---+
            |   |   |
            |   |   +--------------------+
            |   |                        |
            v   v                        v
       Planner Executor              Reflector
                         \            /
                          \          /
                           v        v
                           RePlanner

 Spring AI infrastructure
 - ChatClient
 - ChatMemory
 - ToolCallbackProvider
```

## 项目结构

```text
org.example.ggbot
├── adapter
│   ├── feishu
│   └── web
├── agent
│   ├── execution
│   ├── reflection
│   └── replan
├── ai
├── common
├── config
├── memory
├── planner
├── task
└── tool
```

## 模块说明

### `adapter.web`

负责普通 HTTP 接口和当前的浏览器 MVP 交互入口。  
后端 API 入口仍然是：

- `POST /api/agent/chat`
- `GET /health`

前端静态页放在 `src/main/resources/static`，不单独起前端工程。

### `adapter.feishu`

负责 Feishu Webhook 协议接入、事件解析和消息回发。  
核心 Agent 不依赖 Feishu 类型。

### `agent`

这是项目的核心编排层。当前已经升级成循环式 Agent 架构：

```java
while (!done) {
    plan = planner.plan(state);
    result = executor.execute(state, plan);
    reflection = reflector.analyze(state, result);
    state = state.update(result, reflection);
}
```

这里包含：

- `AgentState`
- `AgentRunner`
- `Executor`
- `Reflector`
- `RePlanner`

### `planner`

负责把用户输入转成 `Plan`。  
当前还是规则驱动，但结构已经为后续 LLM Planner 留好了扩展口。

### `tool`

工具层已经不再使用旧的 `ToolRegistry`。  
当前结构是：

- 具体工具类作为 Spring Bean
- 工具方法使用 Spring AI `@Tool`
- 项目内部通过 `SpringAiToolExecutor` 执行
- `ToolCallbackProvider` 已经注册，后续可直接接入 `ChatClient.tools(...)`

当前工具包括：

- 生成文档
- 生成 PPT
- 修改 PPT
- 总结

### `memory`

记忆层已经迁移到 Spring AI `ChatMemory`。  
项目仍保留 `ConversationMemoryService` 作为语义层，避免业务层直接依赖 Spring AI API。

### `ai`

这里是项目内部对 Spring AI 的轻量接入层。  
当前包含 `SpringAiChatService`，用于统一聊天模型调用入口。

## Web MVP

当前已经提供一个可以直接验证主链路的浏览器页面：

- 地址：`http://localhost:8080/`

页面能力：

- 自动探测服务状态
- 直接输入需求并发起 Agent 请求
- 展示聊天消息流
- 展示最近一次执行结果摘要
  - `taskId`
  - `intentType`
  - `artifactSummaries`

这不是独立前端工程，而是一个验证型 MVP，目的是让你现在就能看见、能试、能判断主链路是否跑通。

当前行为：

- 普通聊天类请求会优先走真实大模型对话
- 文档 / PPT 类请求仍然先走现有工具链

## 快速开始

### 1. 启动项目

```bash
mvn -s .mvn/local-settings.xml spring-boot:run
```

### 2. 打开浏览器

```text
http://localhost:8080/
```

### 3. 也可以直接测 API

#### 健康检查

```bash
curl http://localhost:8080/health
```

#### Web Chat

```bash
curl -X POST "http://localhost:8080/api/agent/chat" ^
  -H "Content-Type: application/json" ^
  -d "{\"conversationId\":\"web-session-1\",\"userId\":\"user-1\",\"message\":\"帮我做一个项目方案文档和汇报PPT\"}"
```

#### Feishu Challenge

```bash
curl -X POST "http://localhost:8080/feishu/webhook" ^
  -H "Content-Type: application/json" ^
  -d "{\"challenge\":\"demo-challenge\"}"
```

## 配置说明

主配置在 [application.yml](/D:/GGbot/src/main/resources/application.yml:1)。

### Spring AI

当前项目使用 Spring AI 官方配置入口：

- `spring.ai.openai.base-url`
- `spring.ai.openai.api-key`
- `spring.ai.openai.chat.options.model`

默认情况下项目为了本地开发稳定性，不强依赖真实模型可用。

项目现在已经支持自动读取根目录 `.env` 文件。这个能力由 `spring-dotenv` 提供，适合你继续往 `.env` 里加入其他本地开发配置。

如果你要让 Web MVP 里的普通聊天真正走 OpenAI-compatible 模型，至少需要提供：

`.env` 示例：

```dotenv
SPRING_AI_OPENAI_BASE_URL=https://your-openai-compatible-base-url
SPRING_AI_OPENAI_API_KEY=your-api-key
SPRING_AI_OPENAI_MODEL=your-model-name
```

然后再启动：

```bash
mvn -s .mvn/local-settings.xml spring-boot:run
```

这样浏览器里普通聊天问题会优先返回真实模型回复；如果模型不可用，则自动降级到模板回复。

注意：

- `.env` 只用于本地开发便利，不应提交到仓库
- 仓库里提供了 [.env.example](/D:/GGbot/.env.example:1) 作为模板

### Feishu

Feishu 相关配置仍然通过：

- `app.feishu.enabled`
- `app.feishu.mock-send`
- `app.feishu.app-id`
- `app.feishu.app-secret`

控制。

## 验证

运行测试：

```bash
mvn -s .mvn/local-settings.xml test
```

## Roadmap

接下来更值得做的方向：

- 让 `Executor` 真正消费 `ChatClient`、`ChatMemory` 和 Spring AI tool context
- 增加流式输出
- 增加任务查询页面
- 接入持久化存储
- 引入 LLM Planner / LLM Reflector
- 增加更完整的前端状态管理和会话历史
