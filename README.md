<div align="center">

# 🚀 GGbot · Agent-Pilot

### 从 IM 对话到演示稿的一键智能闭环

> 🧠 AI Agent 主驾驶 · 📱 多端协同 · 📄 自动生成文档与PPT

<br />

<p>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4.13-brightgreen">
  <img src="https://img.shields.io/badge/Spring%20AI-1.0.0-blue">
  <img src="https://img.shields.io/badge/JDK-17-orange">
  <img src="https://img.shields.io/badge/Agent--First-Architecture-purple">
</p>

<p>
  <a href="#-项目定位">项目定位</a> ·
  <a href="#-核心能力">核心能力</a> ·
  <a href="#-完整链路演示">完整链路</a> ·
  <a href="#-快速开始">快速开始</a> ·
  <a href="#-架构设计">架构</a>
</p>

***

### ⭐ 如果你也在做 AI Agent 落地，这个项目会对你有帮助

</div>

***

## 🎯 项目定位

GGbot 是一个围绕赛题 **Agent-Pilot** 打造的 AI Agent 系统：

> 👉 从一次 IM 对话出发，自动完成 **需求理解 → 文档生成 → 演示稿产出 → 汇报交付** 的全链路闭环

它解决的核心问题：

- ❌ IM → 文档 → PPT 过程割裂
- ❌ 重复手动整理内容
- ❌ 多工具切换成本高
- ❌ 无法复用工作流

👉 **GGbot 的答案是：让 Agent 成为“主驾驶”**

***

## 🧠 核心理念（Agent First）

- **Agent = Pilot（主驾驶）**
- **GUI = Co-pilot（辅助仪表盘）**

用户只需要说一句话：

> “帮我整理这个需求，并生成汇报PPT”

系统会自动完成：

```
意图识别 → 任务拆解 → 文档生成 → PPT生成 → 输出交付
```

***

## 🔥 核心能力（对应赛题 Must-have）

### 1️⃣ IM 意图入口（场景 A）

- 支持群聊 / 单聊（飞书）
- 支持自然语言输入
- @机器人即可触发任务

***

### 2️⃣ Agent 任务理解与规划（场景 B）

- 基于 LLM 的 Planner
- 自动拆解任务为多个子步骤
- 支持动态决策执行路径

***

### 3️⃣ 文档生成与编辑（场景 C）

- 自动生成结构化文档
- 支持多轮迭代优化
- 可用于需求分析 / 方案设计

***

### 4️⃣ 演示稿生成（场景 D）

- 自动从文档生成 PPT 大纲
- 支持汇报结构组织
- 可扩展为完整 PPT 输出

***

### 5️⃣ 多端协同（场景 E）

- Web + IM 双端协同
- 状态实时同步
- 会话上下文一致

***

### 6️⃣ 总结与交付（场景 F）

- 自动生成总结
- 支持输出：
  - Markdown 文档
  - PPT 内容结构
- 可用于汇报 / 归档

***

## 🎬 完整链路演示

```text
IM输入（用户）：
👉 “帮我整理这个需求并生成汇报PPT”

↓ Agent执行

1. 意图识别
2. 任务规划
3. 生成需求文档
4. 提炼核心要点
5. 生成PPT结构
6. 输出结果

↓ 输出

📄 文档 + 📊 PPT大纲
```

***

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/your-username/GGbot.git
cd GGbot
```

***

### 2. 配置模型

```env
SPRING_AI_MODEL_CHAT=openai
LLM_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
LLM_API_KEY=你的API密钥
LLM_MODEL=doubao-pro
```

***

### 3. 启动服务

```bash
mvn -s .mvn/local-settings.xml spring-boot:run
```

***

### 4. 使用方式

- 🌐 Web：<http://localhost:8080>
- 🤖 飞书：@机器人触发
- ❤️ 健康检查：<http://localhost:8080/health>

### 5. Web 语音输入

配置以下环境变量后，Web 端输入框会显示语音转写能力：

```env
ASR_ENABLED=true
ASR_PROVIDER=openai-compatible
ASR_BASE_URL=https://your-asr-endpoint
ASR_API_KEY=your-key
ASR_MODEL=whisper-1
```

使用方式：

1. 点击输入框旁的“语音输入”开始录音
2. 再次点击结束录音并等待转写
3. 默认会回填输入框，也可以切换到“直发”模式

***

## 🏗️ 架构设计

```text
接入层（IM / Web）
        ↓
AgentService（核心编排）
        ↓
Planner → Executor → Reflector → Memory
        ↓
Spring AI 基础设施
        ↓
LLM + 工具系统
```

***

## 🧩 项目结构

```text
GGbot
├── adapter      # 接入层（Web / 飞书）
├── agent        # Agent编排
├── planner      # 意图识别 / 任务规划
├── tool         # 工具实现（文档 / PPT）
├── ai           # 模型封装
├── memory       # 会话记忆
├── job          # 异步任务
└── config       # 配置
```

***

## ⚡ 技术亮点

- ✅ Agent 全链路闭环（非 Demo 拼接）
- ✅ 多模型统一接入（OpenAI / 豆包等）
- ✅ 高可用设计（重试 + 降级）
- ✅ 低代码扩展工具体系
- ✅ 面向真实业务场景

***

## 🛣️ 路线图

- 让 `Executor` 真正消费 `ChatClient`、`ChatMemory` 和 Spring AI tool context
- 增加流式输出
- 增加任务查询页面
- 接入持久化存储
- 引入 LLM Planner / LLM Reflector
- 增加更完整的前端状态管理和会话历史

***

## 🤝 贡献

欢迎一起完善这个 Agent-Pilot 系统：

```bash
git checkout -b feature/xxx
git commit -m "feat: xxx"
git push origin feature/xxx
```

***

## 📄 License

MIT License

***

<div align="center">

### 🚀 Agent 不只是聊天，而是开始“干活”

⭐ 如果你认同这个方向，欢迎 Star！

</div>
