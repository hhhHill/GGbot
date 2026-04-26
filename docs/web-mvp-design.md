# Web MVP 设计记录

## 目标

在当前纯后端项目基础上补一个可直接打开的单页聊天控制台，用于快速验证 Agent 主链路，而不是继续只靠 Postman 或 curl。

## 设计选择

采用：

- Spring Boot `static` 静态资源页
- 直连现有 `GET /health`
- 直连现有 `POST /api/agent/chat`

不采用：

- 独立前端工程
- Thymeleaf
- 登录、历史会话管理、流式输出

## 页面结构

- 顶部信息区
  - 项目标题
  - 服务状态
  - 当前能力说明
- 中间双栏
  - 左侧：聊天消息流
  - 右侧：最近一次执行结果摘要
- 底部输入区
  - 文本框
  - 发送按钮
  - 示例问题按钮

## 交互

- 页面加载时请求 `/health`
- 发送消息时调用 `/api/agent/chat`
- 渲染用户消息与 Agent 回复
- 展示 loading / error 状态
- 展示 `taskId`、`intentType`、`artifactSummaries`

## 当前边界

这是一个验证型 MVP，不追求完整前端工程能力。

本轮不做：

- 流式输出
- 多会话切换
- 登录鉴权
- Markdown 渲染
- 持久化聊天记录

## README 同步要求

README 需要同步更新以下事实：

- 项目已迁移到 Spring AI 基础设施
- `llm`、`tool`、`memory` 的旧描述已经过时
- 项目现在包含一个可直接访问的 Web MVP 首页
