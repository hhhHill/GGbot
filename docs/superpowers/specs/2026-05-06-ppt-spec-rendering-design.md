# PPT Spec Rendering Design

**Goal**

把当前硬编码 demo 版 PPT 生成链路升级为：LLM 生成 `PptSpec` JSON，Java 使用 Apache POI 渲染真实 `.pptx` 文件，并返回可落地的文件路径。

**Scope**

- 保持 `PlannerService`、`PlanStepFactory`、`LangGraphAgentRunner` 主流程不变
- 改造 `DefaultExecutor` 的前置结果物化逻辑，避免 `ToolResult` / `Artifact` 对象字符串泄漏
- 新增 `PptSpec` 中间模型、解析、校验、质量检查、语义 fallback 和 PPTX 渲染器
- 重写 `GeneratePptTool` 主链路为 `instruction -> PptSpec JSON -> PptSpec -> .pptx -> PptArtifact`
- 最小修复 `GenerateDocTool` 和 `ModifyPptTool`，避免继续输出固定项目模板污染文档和 PPT
- 为新链路补充解析、校验、渲染和执行器测试

**Approach**

1. 新增 `org.example.ggbot.tool.ppt.PptSpec` 和 `SlideSpec`，作为 LLM 输出与 Java 渲染之间的唯一中间格式。
2. 新增 `PptSpecParser`，支持：
   - 严格 JSON 解析
   - 去除 ```json 代码块
   - `pageNumber` 缺失时顺序补齐
   - 空标题兜底为 `第 N 页`
   - 空 `bullets` 视为无效
3. 新增 `PptSpecValidator` 和 `ResultQualityEvaluator`，统一检查：
   - 空内容
   - 内部对象字符串
   - 固定 demo 模板组合
   - 非项目主题下的大量项目模板词
   - 是否缺少主题关键词
4. 新增 `ArtifactContentExtractor`，把 `ToolResult`、`DocumentArtifact`、`PptArtifact` 渲染为用户可读文本。`DefaultExecutor.materializeInstruction()` 改为使用该提取器拼接前置依赖结果，不再直接拼对象。
5. `GeneratePptTool` 改为依赖一个独立的 `PptSpecGenerationClient`。默认实现复用现有 Spring AI 聊天能力，使用固定 system prompt 约束模型只返回严格 JSON。
6. `GeneratePptTool` 执行顺序：
   - 组装 instruction 和前置 readable content
   - 调用 LLM 生成 `PptSpec` JSON
   - 解析、校验、做质量检查
   - 失败时带 issue 重试一次
   - 二次失败时走 `SemanticPptFallbackGenerator`
   - 生成 `PptArtifact`
   - 调用 `PptxRenderer` 输出唯一 `.pptx` 文件
   - 在 `PptArtifact` 上附带文件路径
7. `PptxRenderer` 使用 Apache POI `XMLSlideShow` / `XSLFSlide` / `XSLFTextBox`，按固定 16:9 布局渲染标题和 bullet 区域，输出到 `generated/pptx/`。
8. `GenerateDocTool` 和 `ModifyPptTool` 做最小语义化修复：
   - `GenerateDocTool` 使用 LLM 或 fallback 生成围绕主题的 Markdown，不再固定“项目方案文档”
   - `ModifyPptTool` 复用 `PptSpec` 生成、`ArtifactContentExtractor` 和 `PptxRenderer`，按原主题和修改要求重新生成

**Data Flow**

```text
用户输入 / 前置文档
    -> ArtifactContentExtractor readable content
    -> PptSpecGenerationClient
    -> JSON
    -> PptSpecParser
    -> PptSpecValidator / ResultQualityEvaluator
    -> PptxRenderer
    -> generated/pptx/ppt-*.pptx
    -> PptArtifact(filePath, slides, title)
```

**Error Handling**

- LLM 不可用、返回空串、返回非 JSON、解析失败、校验失败都不回退到旧 5 页项目模板
- 第一次失败记录 issue 并重试一次；第二次失败走 `SemanticPptFallbackGenerator`
- fallback 若仍然产出空内容或命中质量检查，则返回 `ToolResult(success=false, ...)`，交给现有 reflect / replan
- `PptxRenderer` 负责创建输出目录、关闭流、抛出明确异常，不吞掉 I/O 失败
- 中文字体仅作为首选设置，运行环境缺字体时不影响写文件

**Testing**

- `PptSpecParserTest`
  - 解析标准 JSON
  - 解析 ```json 代码块
  - 缺失 `pageNumber` 自动补齐
- `PptSpecValidatorTest`
  - 合法 spec 通过
  - 内部对象字符串失败
  - 固定 demo 模板失败
- `ArtifactContentExtractorTest`
  - `DocumentArtifact` 返回 markdown 正文
  - `PptArtifact` 返回标题和 bullet 文本
  - 不回传对象 `toString()`
- `PptxRendererTest`
  - 生成真实 `.pptx` 文件
  - 文件存在且非空
- `GeneratePptToolTest`
  - LLM 返回合法 JSON 时生成 `PptArtifact` 和文件路径
  - 首次校验失败后带 issue 重试成功
  - LLM 不可用时走语义 fallback，且不返回项目模板
- `DefaultExecutorTest`
  - 依赖结果物化使用可读内容，不暴露 `DocumentArtifact(...)`
