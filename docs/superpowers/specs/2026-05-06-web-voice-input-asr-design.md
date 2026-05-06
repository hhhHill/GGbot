# Web Voice Input ASR Design

**Goal**

为 GGbot Web 工作台增加语音输入能力：用户可以在浏览器录音，服务端调用可替换的 ASR 提供方完成转写，并将结果默认回填到聊天输入框，同时支持一键直接发送。

**Scope**

- 只覆盖 Web 端语音输入，不包含飞书语音消息接入
- 新增前端录音、上传、转写结果回填与直接发送交互
- 新增服务端音频转写接口与可替换 ASR 抽象层
- 为 ASR 增加最小配置、校验、错误映射和日志观测
- 为前后端新增必要测试
- 不改造现有 Agent 任务编排、SSE 聊天主链路

**Approach**

1. Web 前端在现有聊天输入区新增麦克风入口，状态分为 `idle`、`recording`、`transcribing`、`failed`。
2. 使用浏览器 `MediaRecorder` 采集音频，停止录音后生成 `Blob`，通过 `multipart/form-data` 上传到独立接口。
3. 服务端新增同步转写接口 `POST /api/web/audio/transcriptions`，职责仅限文件接收、校验、调用 ASR、返回文本。
4. 服务端定义 `AsrService` 抽象，屏蔽不同供应商的 API 差异；首个实现为兼容 HTTP 语音转写接口的 provider adapter。
5. 转写成功后前端默认把文本写入现有 `draft`；如果用户选择直接发送，则复用现有 `handleSendMessage(text)` 主流程。
6. 失败路径按“前端可恢复、后端可观测”处理：前端展示可读错误，后端输出统一响应和结构化日志。

**Frontend Design**

- 在 `ConversationPage` 和 `HomePage` 共用的消息输入区域增加语音输入控件，避免两套实现分叉。
- 新增前端语音服务模块，职责拆分为：
  - 请求麦克风权限
  - 控制录音开始/停止
  - 将录音文件上传到转写接口
- 默认交互：
  - 点击麦克风开始录音
  - 再次点击结束录音并开始转写
  - 转写成功后把文本回填输入框
  - UI 提供“直接发送”动作，允许用户跳过手动编辑
- 录音期间禁用重复触发，转写期间展示忙碌状态，避免并发上传同一轮录音。
- 浏览器不支持 `MediaRecorder` 或用户拒绝授权时，保留原文本输入能力，不阻断聊天。

**Backend Design**

- 新增独立控制器，例如 `WebAudioTranscriptionController`，不挂在 `/api/agent` 下，避免将短同步能力混入长任务编排语义。
- 新增请求处理链路：
  - `WebAudioTranscriptionController`
  - `AudioUploadValidator`
  - `AsrService`
  - `ProviderAsrService`
- `AsrService` 作为唯一领域抽象，输入为统一的转写请求对象，输出为统一的转写结果对象。
- `ProviderAsrService` 负责：
  - 组装供应商请求
  - 传递模型、语言、超时等配置
  - 解析供应商响应
  - 将异常映射为系统内部统一错误
- 控制器返回值只暴露：
  - `text`
  - `provider`
  - `language`
  - `durationMs`
  - `requestId`
- 服务端不把供应商原始响应体直接返回前端，避免耦合外部协议。

**API Contract**

请求：

```text
POST /api/web/audio/transcriptions
Content-Type: multipart/form-data
```

表单字段建议：

- `file`: 录音文件
- `mode`: `fill` 或 `send`
- `language`: 可选，默认 `zh`

响应体建议：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "text": "帮我整理这个需求并生成汇报 PPT",
    "provider": "openai-compatible",
    "language": "zh",
    "durationMs": 1820,
    "requestId": "asr_123456"
  }
}
```

前端行为：

- `mode=fill`：将 `text` 写回 `draft`
- `mode=send`：先写回 `draft`，再立即复用现有发送逻辑提交消息

**Configuration**

新增配置项：

- `asr.enabled`
- `asr.provider`
- `asr.base-url`
- `asr.api-key`
- `asr.model`
- `asr.timeout`
- `asr.max-file-size`
- `asr.allowed-content-types`

要求：

- ASR 默认关闭，未配置时前端仍可正常使用文本聊天
- `max-file-size` 和 `allowed-content-types` 由服务端强校验，前端只做辅助提示

**Data Flow**

```text
用户点击麦克风
    -> MediaRecorder 开始录音
    -> 停止录音并生成 Blob
    -> POST /api/web/audio/transcriptions
    -> 文件校验
    -> AsrService.transcribe(...)
    -> provider adapter 调用外部 ASR
    -> 返回 text
    -> 前端回填 draft 或直接发送
```

**Error Handling**

- 浏览器不支持 `MediaRecorder`：前端提示当前浏览器不支持语音输入
- 麦克风权限被拒绝：前端提示未获得麦克风权限
- 文件为空、过大、格式不支持：服务端返回明确 `4xx` 错误与可读消息
- ASR 超时、鉴权失败、供应商异常：服务端返回统一错误结构，不向前端透出供应商细节
- 转写结果为空：前端提示未识别到有效语音，不自动发送
- 所有失败场景都不能影响现有文本输入与聊天发送主流程

服务端日志至少记录：

- `provider`
- `requestId`
- `latencyMs`
- `fileSize`
- `contentType`
- `success/failure`

**Testing**

- 后端单测：
  - `AudioUploadValidatorTest`：大小、格式、空文件校验
  - `ProviderAsrServiceTest`：成功解析、超时映射、供应商错误映射
  - `WebAudioTranscriptionControllerTest`：成功响应、参数错误、服务失败
- 前端测试：
  - 录音状态切换
  - 转写成功后回填 `draft`
  - 直接发送模式触发 `handleSendMessage`
  - 失败提示展示且不清空已有草稿
- 手工验证：
  - `录音 -> 转写 -> 回填 -> 编辑 -> 发送`
  - `录音 -> 转写 -> 直接发送`
  - 权限拒绝与浏览器不支持时的降级

**Out of Scope**

- 飞书语音消息接入
- 流式边录边转写
- 语音输出、TTS、语音播报
- 音频持久化存储与历史回放

**Implementation Notes**

- 语音输入被定义为“文本输入的前置增强”，不是新的聊天通道。
- 现有 `/api/agent/chat` 与 `/api/agent/chat/stream` 不需要因本需求改变协议。
- 后续接入飞书语音时，应直接复用 `AsrService`，只替换音频来源和身份上下文解析。
