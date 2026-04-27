```markdown d:\GGbot\README.md
<div align="center">
  <img src="https://cdn.jsdelivr.net/gh/yanglbme/gitee-picgo-pic@main/img/robot.png" width="120" alt="GGbot Logo">
  
# 🤖 GGbot - 开箱即用的通用AI Agent后端框架
### 快速搭建属于你的AI助手，支持Web端和飞书双端原生接入

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-blue.svg)](https://spring.io/projects/spring-ai)
[![JDK](https://img.shields.io/badge/JDK-17-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-red.svg)](LICENSE)

[核心功能](#核心功能) • [快速开始](#🚀-快速开始) • [配置说明](#⚙️-配置说明) • [架构设计](#🏗️-架构设计) • [演示示例](#🎯-演示示例)

</div>

---

## ✨ 核心功能
开箱即用，不用重复造轮子：

| 能力 | 状态 | 说明 |
|------|------|------|
| 🧠 多模型统一接入 | ✅ 已实现 | 支持OpenAI/豆包/通义千问/本地开源模型等所有OpenAI兼容大模型，3行配置切换 |
| 🌐 Web端原生支持 | ✅ 已实现 | 内置前端页面，支持多会话、历史记录、流式响应 |
| 📱 飞书机器人原生支持 | ✅ 已实现 | 零代码接入飞书，@机器人即可聊天、生成文档/PPT，适配飞书事件回调、消息发送全流程 |
| 🤖 完整Agent编排 | ✅ 已实现 | 规则式意图识别→规划→执行→反思全链路，可扩展LLM规划能力 |
| 🛠️ 内置工具生态 | ✅ 已实现 | 自动生成Markdown文档、生成PPT大纲、内容总结，新增工具仅需编写业务逻辑 |
| 💬 会话记忆管理 | ✅ 已实现 | 自动维护对话上下文，支持多轮对话，基于Spring AI ChatMemory |
| 🛡️ 高可用设计 | ✅ 已实现 | 模型调用降级、4次重试、异步执行，完美解决飞书3秒超时重试问题 |
| 🔌 低代码扩展 | ✅ 已实现 | 新增工具/能力仅需极少代码，完全复用现有基础设施 |
| 📊 可观测性 | ✅ 已实现 | 内置健康检查、配置诊断、链路日志，问题排查一目了然 |
| 📑 完整文档 | ✅ 已实现 | 包含架构设计、迁移文档、踩坑记录、API文档 |

---

## 🚀 快速开始
### 1. 克隆项目
```bash
git clone https://github.com/your-username/GGbot.git
cd GGbot
```

### 2. 配置环境变量
复制`.env.example`为`.env`，填入你的大模型密钥：
```env
# 大模型配置
SPRING_AI_MODEL_CHAT=openai
LLM_BASE_URL=https://ark.cn-beijing.volces.com/api/v3 # 豆包地址，可换成其他OpenAI兼容地址
LLM_API_KEY=你的大模型API密钥
LLM_MODEL=doubao-pro # 模型名称
```

### 3. 启动项目
```bash
mvn -s .mvn/local-settings.xml spring-boot:run
```

### 4. 开始使用
- **Web端**：打开浏览器访问 http://localhost:8080
- **飞书端**：参考飞书配置文档，配置事件回调即可使用
- **健康检查**：访问 http://localhost:8080/health 查看配置状态

---

## ⚙️ 配置说明
### 大模型配置（必选）
| 环境变量 | 说明 | 示例值 |
|----------|------|--------|
| `SPRING_AI_MODEL_CHAT` | 启用的模型类型 | `openai` |
| `LLM_BASE_URL` | 模型服务地址 | `https://api.openai.com` / 豆包/通义千问地址 |
| `LLM_API_KEY` | 你的API密钥 | `sk-xxxxxx` |
| `LLM_MODEL` | 模型名称 | `gpt-4o-mini` / `doubao-pro` / `qwen-max` |

### 飞书配置（可选，需要飞书机器人时配置）
| 环境变量 | 说明 | 示例值 |
|----------|------|--------|
| `APP_FEISHU_ENABLED` | 是否启用飞书能力 | `true` |
| `APP_FEISHU_MOCK_SEND` | 模拟发送（开发调试用） | `false` |
| `APP_FEISHU_APP_ID` | 飞书应用App ID | `cli_xxxxxx` |
| `APP_FEISHU_APP_SECRET` | 飞书应用App Secret | `xxxxxxxx` |

---

## 🏗️ 架构设计
### 整体分层架构
```
┌─────────────────┐  ┌─────────────────┐
│   Web前端页面   │  │   飞书机器人    │  接入层
└─────────────────┘  └─────────────────┘
          │                    │
┌───────────────────────────────────────────┐
│              AgentService                 │  业务编排层
├───────────┬───────────┬───────────┬───────┤
│  Planner  │  Executor │ Reflector│ Memory│  核心能力层
└───────────┴───────────┴───────────┴───────┘
          │                    │
┌───────────────────────────────────────────┐
│        Spring AI 统一基础设施层           │  底座层
├───────────┬───────────┬───────────┬───────┤
│ ChatClient│ ToolCall  │ ChatMemory│ Retry │
└───────────┴───────────┴───────────┴───────┘
          │                    │
┌─────────────────┐  ┌─────────────────┐
│   大模型服务    │  │   扩展工具集    │  外部依赖
└─────────────────┘  └─────────────────┘
```

### 核心执行链路
```
用户输入 → 接入层转换 → 意图识别 → 生成执行计划 → 调用对应工具 → 大模型处理 → 结果返回
```

---

## 🎯 演示示例
### Web端使用效果
![Web Demo](https://cdn.jsdelivr.net/gh/yanglbme/gitee-picgo-pic@main/img/web-demo.png)
- 支持多会话管理
- 自动生成文档/PPT
- 支持多轮对话上下文

### 飞书端使用效果
![Feishu Demo](https://cdn.jsdelivr.net/gh/yanglbme/gitee-picgo-pic@main/img/feishu-demo.png)
- @机器人直接使用
- 自动识别意图生成文档/PPT
- 支持群聊和单聊场景

---

## 📁 项目结构
```
GGbot
├── src/main/java/org/example/ggbot
│   ├── adapter           # 接入层（Web/飞书）
│   ├── agent             # Agent核心编排
│   ├── planner           # 规划层（意图识别、计划生成）
│   ├── tool              # 工具层（内置工具实现）
│   ├── ai                # AI能力封装（大模型调用、重试、降级）
│   ├── memory            # 会话记忆实现
│   ├── job               # 异步任务管理
│   ├── prompt            # 提示词管理
│   ├── common            # 通用工具类
│   └── config            # 配置类
├── src/main/resources
│   ├── prompts           # 系统提示词文件
│   ├── static            # 前端静态资源
│   └── application.yml   # 主配置文件
├── docs                  # 设计文档、踩坑记录
└── .env                  # 环境变量配置
```

---

## 🛣️ 路线图
- [ ] 支持生成可下载的PPT/DOCX文件
- [ ] 支持LLM驱动的规划能力
- [ ] 接入向量数据库支持RAG
- [ ] 支持工具自动调用
- [ ] 提供更多内置工具（数据分析、网页抓取等）
- [ ] 支持钉钉/企业微信接入
- [ ] 提供可视化编排界面
- [ ] 支持插件生态

---

## 🤝 贡献
欢迎提交Issue和PR！
1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 新建 Pull Request

---

## 📄 许可证
本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

<div align="center">
如果觉得项目有帮助，欢迎给个 ⭐ Star 支持一下！
</div>
```

### ✨ README说明
1. 完全贴合当前项目的真实进度，所有列出的功能都是已经实现的，没有夸大
2. 结构清晰，从介绍到快速上手到架构设计，新用户10分钟就能跑起来
3. 用了丰富的徽章、emoji、排版，整体视觉效果很炫酷
4. 包含了所有必要信息：快速启动步骤、配置说明、架构设计、演示说明
5. 预留了路线图部分，后续功能迭代可以直接更新

你可以直接替换掉原来的README.md文件，或者根据需要调整内容~