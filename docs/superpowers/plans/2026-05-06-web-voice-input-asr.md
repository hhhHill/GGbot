# Web Voice Input ASR Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add browser-based voice input to the GGbot web chat so audio can be transcribed by a replaceable ASR backend, then either fill the draft box or send directly.

**Architecture:** Keep voice input as a pre-processing layer in front of the existing text chat flow. Add a dedicated synchronous web transcription endpoint plus an `AsrService` abstraction on the backend, then extend the shared `InputBox` UI and frontend API module so the result either updates `draft` or reuses the existing `handleSendMessage()` flow.

**Tech Stack:** Spring Boot MVC, Spring Web multipart upload, static React frontend, browser `MediaRecorder`, MockMvc, frontend Node-based `.mjs` tests.

---

### Task 1: Add backend transcription DTOs and controller contract tests

**Files:**
- Create: `src/main/java/org/example/ggbot/adapter/web/WebAudioTranscriptionController.java`
- Create: `src/main/java/org/example/ggbot/adapter/web/dto/WebAudioTranscriptionResponse.java`
- Create: `src/main/java/org/example/ggbot/asr/AudioTranscriptionRequest.java`
- Create: `src/main/java/org/example/ggbot/asr/AudioTranscriptionResult.java`
- Create: `src/main/java/org/example/ggbot/asr/AsrService.java`
- Create: `src/main/java/org/example/ggbot/asr/AudioUploadValidator.java`
- Test: `src/test/java/org/example/ggbot/adapter/web/WebAudioTranscriptionControllerTest.java`

- [ ] **Step 1: Write the failing controller test for a successful multipart transcription**

```java
@Test
void shouldReturnTranscriptionResult() throws Exception {
    AsrService asrService = mock(AsrService.class);
    AudioUploadValidator validator = mock(AudioUploadValidator.class);
    MockMultipartFile file = new MockMultipartFile(
            "file",
            "voice.webm",
            "audio/webm",
            "voice".getBytes(StandardCharsets.UTF_8)
    );

    when(asrService.transcribe(any())).thenReturn(new AudioTranscriptionResult(
            "帮我整理这个需求",
            "openai-compatible",
            "zh",
            1820L,
            "asr_123"
    ));

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new WebAudioTranscriptionController(validator, asrService)
    ).build();

    mockMvc.perform(multipart("/api/web/audio/transcriptions")
                    .file(file)
                    .param("mode", "fill")
                    .param("language", "zh"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.text").value("帮我整理这个需求"))
            .andExpect(jsonPath("$.data.provider").value("openai-compatible"));
}
```

- [ ] **Step 2: Write the failing controller test for invalid uploads**

```java
@Test
void shouldRejectEmptyAudioFile() throws Exception {
    AsrService asrService = mock(AsrService.class);
    AudioUploadValidator validator = mock(AudioUploadValidator.class);
    MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.webm", "audio/webm", new byte[0]);

    doThrow(new IllegalArgumentException("上传音频不能为空")).when(validator).validate(any());

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new WebAudioTranscriptionController(validator, asrService)
    ).setControllerAdvice(new GlobalExceptionHandler()).build();

    mockMvc.perform(multipart("/api/web/audio/transcriptions").file(emptyFile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("上传音频不能为空"));
}
```

- [ ] **Step 3: Run the controller test class to verify it fails**

Run:

```bash
mvn -Dtest=WebAudioTranscriptionControllerTest test
```

Expected: FAIL with compilation errors because `WebAudioTranscriptionController`, `AsrService`, `AudioTranscriptionRequest`, `AudioTranscriptionResult`, and `AudioUploadValidator` do not exist yet.

- [ ] **Step 4: Add the DTOs and controller with the minimal contract needed by the tests**

```java
public interface AsrService {

    AudioTranscriptionResult transcribe(AudioTranscriptionRequest request);
}
```

```java
public record AudioTranscriptionRequest(
        MultipartFile file,
        String language
) {
}
```

```java
public record AudioTranscriptionResult(
        String text,
        String provider,
        String language,
        Long durationMs,
        String requestId
) {
}
```

```java
public class AudioUploadValidator {

    public void validate(MultipartFile file) {
        // Validation rules are added in Task 2.
    }
}
```

```java
@RestController
@RequestMapping("/api/web/audio")
@RequiredArgsConstructor
public class WebAudioTranscriptionController {

    private final AudioUploadValidator audioUploadValidator;
    private final AsrService asrService;

    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<WebAudioTranscriptionResponse> transcribe(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "mode", required = false, defaultValue = "fill") String mode,
            @RequestParam(value = "language", required = false, defaultValue = "zh") String language
    ) {
        audioUploadValidator.validate(file);
        AudioTranscriptionResult result = asrService.transcribe(new AudioTranscriptionRequest(file, language));
        return ApiResponse.success(WebAudioTranscriptionResponse.from(result));
    }
}
```

- [ ] **Step 5: Run the controller test class to verify it passes**

Run:

```bash
mvn -Dtest=WebAudioTranscriptionControllerTest test
```

Expected: PASS with 2 tests in `WebAudioTranscriptionControllerTest`.

- [ ] **Step 6: Commit the controller contract**

```bash
git add src/main/java/org/example/ggbot/adapter/web/WebAudioTranscriptionController.java src/main/java/org/example/ggbot/adapter/web/dto/WebAudioTranscriptionResponse.java src/main/java/org/example/ggbot/asr/AudioTranscriptionRequest.java src/main/java/org/example/ggbot/asr/AudioTranscriptionResult.java src/main/java/org/example/ggbot/asr/AsrService.java src/main/java/org/example/ggbot/asr/AudioUploadValidator.java src/test/java/org/example/ggbot/adapter/web/WebAudioTranscriptionControllerTest.java
git commit -m "feat: add web audio transcription controller contract"
```

### Task 2: Add upload validation and ASR provider backend implementation

**Files:**
- Create: `src/main/java/org/example/ggbot/asr/AudioUploadValidator.java`
- Create: `src/main/java/org/example/ggbot/asr/AsrProperties.java`
- Create: `src/main/java/org/example/ggbot/asr/AsrProviderException.java`
- Create: `src/main/java/org/example/ggbot/asr/ProviderAsrService.java`
- Modify: `src/main/java/org/example/ggbot/config/AppConfig.java`
- Modify: `src/main/resources/application.yml`
- Test: `src/test/java/org/example/ggbot/asr/AudioUploadValidatorTest.java`
- Test: `src/test/java/org/example/ggbot/asr/ProviderAsrServiceTest.java`

- [ ] **Step 1: Write the failing validator test for empty file, unsupported type, and oversize file**

```java
@Test
void shouldRejectUnsupportedAudioContentType() {
    AsrProperties properties = new AsrProperties();
    properties.setAllowedContentTypes(List.of("audio/webm", "audio/mpeg"));
    properties.setMaxFileSize(DataSize.ofMegabytes(5));
    AudioUploadValidator validator = new AudioUploadValidator(properties);

    MockMultipartFile file = new MockMultipartFile("file", "voice.txt", "text/plain", "bad".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("暂不支持该音频格式");
}
```

- [ ] **Step 2: Write the failing provider test for successful response mapping and timeout mapping**

```java
@Test
void shouldMapProviderResponseToAudioTranscriptionResult() {
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClient).build();
    server.expect(requestTo("https://asr.example.com/audio/transcriptions"))
            .andRespond(withSuccess("""
                    {"text":"你好 GGbot","language":"zh","duration_ms":640,"request_id":"req-1"}
                    """, MediaType.APPLICATION_JSON));

    AudioTranscriptionResult result = service.transcribe(request());

    assertThat(result.text()).isEqualTo("你好 GGbot");
    assertThat(result.provider()).isEqualTo("openai-compatible");
    assertThat(result.requestId()).isEqualTo("req-1");
}
```

```java
@Test
void shouldWrapProviderErrors() {
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClient).build();
    server.expect(anything()).andRespond(withServerError());

    assertThatThrownBy(() -> service.transcribe(request()))
            .isInstanceOf(AsrProviderException.class)
            .hasMessage("语音转写服务暂时不可用");
}
```

- [ ] **Step 3: Run the ASR test classes to verify they fail**

Run:

```bash
mvn -Dtest=AudioUploadValidatorTest,ProviderAsrServiceTest test
```

Expected: FAIL because `AudioUploadValidator`, `AsrProperties`, `AsrProviderException`, and `ProviderAsrService` do not exist yet.

- [ ] **Step 4: Implement the validator and properties with strict server-side checks**

```java
@ConfigurationProperties(prefix = "asr")
public class AsrProperties {

    private boolean enabled = false;
    private String provider = "openai-compatible";
    private String baseUrl;
    private String apiKey;
    private String model = "whisper-1";
    private Duration timeout = Duration.ofSeconds(20);
    private DataSize maxFileSize = DataSize.ofMegabytes(5);
    private List<String> allowedContentTypes = List.of("audio/webm", "audio/mp4", "audio/mpeg", "audio/wav");
}
```

```java
@RequiredArgsConstructor
public class AudioUploadValidator {

    private final AsrProperties properties;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传音频不能为空");
        }
        if (!properties.getAllowedContentTypes().contains(file.getContentType())) {
            throw new IllegalArgumentException("暂不支持该音频格式");
        }
        if (file.getSize() > properties.getMaxFileSize().toBytes()) {
            throw new IllegalArgumentException("音频文件超过大小限制");
        }
    }
}
```

- [ ] **Step 5: Implement the provider adapter and Spring wiring**

```java
@Slf4j
@RequiredArgsConstructor
public class ProviderAsrService implements AsrService {

    private final RestClient restClient;
    private final AsrProperties properties;

    @Override
    public AudioTranscriptionResult transcribe(AudioTranscriptionRequest request) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(
                    request.file().getInputStream(),
                    request.file().getOriginalFilename()
            ));
            body.add("model", properties.getModel());
            body.add("language", request.language());

            ProviderAsrResponse response = restClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(ProviderAsrResponse.class);

            return new AudioTranscriptionResult(
                    response.text(),
                    properties.getProvider(),
                    response.language(),
                    response.durationMs(),
                    response.requestId()
            );
        } catch (Exception exception) {
            log.warn("ASR transcription failed provider={}", properties.getProvider(), exception);
            throw new AsrProviderException("语音转写服务暂时不可用", exception);
        }
    }
}
```

```java
@Bean
@ConditionalOnProperty(prefix = "asr", name = "enabled", havingValue = "true")
AsrService asrService(RestClient.Builder restClientBuilder, AsrProperties properties) {
    RestClient restClient = restClientBuilder
            .baseUrl(properties.getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
            .build();
    return new ProviderAsrService(restClient, properties);
}
```

- [ ] **Step 6: Add the minimal ASR configuration block**

```yaml
asr:
  enabled: ${ASR_ENABLED:false}
  provider: ${ASR_PROVIDER:openai-compatible}
  base-url: ${ASR_BASE_URL:}
  api-key: ${ASR_API_KEY:}
  model: ${ASR_MODEL:whisper-1}
  timeout: ${ASR_TIMEOUT:20s}
  max-file-size: ${ASR_MAX_FILE_SIZE:5MB}
  allowed-content-types:
    - audio/webm
    - audio/mp4
    - audio/mpeg
    - audio/wav
```

- [ ] **Step 7: Run the ASR backend tests to verify they pass**

Run:

```bash
mvn -Dtest=AudioUploadValidatorTest,ProviderAsrServiceTest,WebAudioTranscriptionControllerTest test
```

Expected: PASS with validator, provider, and controller tests green.

- [ ] **Step 8: Commit the backend ASR implementation**

```bash
git add src/main/java/org/example/ggbot/asr src/main/java/org/example/ggbot/config/AppConfig.java src/main/resources/application.yml src/test/java/org/example/ggbot/asr src/test/java/org/example/ggbot/adapter/web/WebAudioTranscriptionControllerTest.java
git commit -m "feat: add replaceable ASR backend for web uploads"
```

### Task 3: Add frontend transcription API and shared voice recorder state tests

**Files:**
- Create: `src/main/resources/static/services/voice-input.js`
- Modify: `src/main/resources/static/services/session-api.js`
- Modify: `src/test/frontend/app-merge-session.test.mjs`
- Create: `src/test/frontend/voice-input-service.test.mjs`

- [ ] **Step 1: Write the failing frontend service test for successful upload mapping**

```javascript
const calls = [];
global.fetch = async (url, options) => {
    calls.push({ url, options });
    return {
        ok: true,
        async json() {
            return {
                success: true,
                data: {
                    text: "整理一下这个需求",
                    provider: "openai-compatible",
                    language: "zh",
                    durationMs: 900,
                    requestId: "req-1"
                }
            };
        }
    };
};

const result = await transcribeAudio(new Blob(["voice"], { type: "audio/webm" }), { mode: "fill", language: "zh" });

assert.equal(calls[0].url, "/api/web/audio/transcriptions");
assert.equal(result.text, "整理一下这个需求");
assert.equal(result.provider, "openai-compatible");
console.log("voice-input upload mapping test passed");
```

- [ ] **Step 2: Write the failing frontend service test for browser support detection**

```javascript
const previous = global.MediaRecorder;
delete global.MediaRecorder;

assert.equal(isVoiceInputSupported(), false);

global.MediaRecorder = previous;
console.log("voice-input support detection test passed");
```

- [ ] **Step 3: Run the frontend service tests to verify they fail**

Run:

```bash
node src/test/frontend/voice-input-service.test.mjs
```

Expected: FAIL because `voice-input.js` and the exported helpers do not exist yet.

- [ ] **Step 4: Implement the frontend transcription helpers**

```javascript
export function isVoiceInputSupported() {
    return typeof window !== "undefined" && typeof window.MediaRecorder !== "undefined" && !!navigator.mediaDevices?.getUserMedia;
}

export async function transcribeAudio(blob, { mode = "fill", language = "zh" } = {}) {
    const formData = new FormData();
    formData.append("file", blob, "voice.webm");
    formData.append("mode", mode);
    formData.append("language", language);

    const response = await fetch("/api/web/audio/transcriptions", {
        method: "POST",
        body: formData
    });
    const payload = await response.json();
    if (!response.ok || !payload.success) {
        throw new Error(payload.message || "语音转写失败");
    }
    return payload.data;
}
```

- [ ] **Step 5: Re-export the transcription entry from the existing frontend API surface**

```javascript
export { transcribeAudio } from "./voice-input.js";
```

- [ ] **Step 6: Run the frontend service tests to verify they pass**

Run:

```bash
node src/test/frontend/voice-input-service.test.mjs
```

Expected: PASS with the upload mapping and support detection tests green.

- [ ] **Step 7: Commit the frontend voice service layer**

```bash
git add src/main/resources/static/services/voice-input.js src/main/resources/static/services/session-api.js src/test/frontend/voice-input-service.test.mjs
git commit -m "feat: add frontend voice input service"
```

### Task 4: Integrate voice input into the shared InputBox with draft-fill and direct-send flows

**Files:**
- Modify: `src/main/resources/static/components/InputBox.js`
- Modify: `src/main/resources/static/components/ChatArea.js`
- Modify: `src/main/resources/static/components/HomePage.js`
- Modify: `src/main/resources/static/components/ConversationPage.js`
- Modify: `src/main/resources/static/app.js`
- Modify: `src/main/resources/static/app.css`
- Test: `src/test/frontend/input-box-voice-state.test.mjs`
- Test: `src/test/frontend/app-merge-session.test.mjs`

- [ ] **Step 1: Write the failing UI state test for transcription success filling the draft**

```javascript
const state = reducer(
    {
        draft: "",
        voiceMode: "fill",
        voiceState: "transcribing",
        error: ""
    },
    {
        type: "voice/result",
        text: "帮我输出项目方案"
    }
);

assert.equal(state.draft, "帮我输出项目方案");
assert.equal(state.voiceState, "idle");
console.log("voice draft fill reducer test passed");
```

- [ ] **Step 2: Write the failing UI state test for direct-send mode invoking the existing send handler**

```javascript
let sentMessage = "";

await handleVoiceResult("send", "直接发出去", async (message) => {
    sentMessage = message;
});

assert.equal(sentMessage, "直接发出去");
console.log("voice direct send handler test passed");
```

- [ ] **Step 3: Run the frontend UI tests to verify they fail**

Run:

```bash
node src/test/frontend/input-box-voice-state.test.mjs
node src/test/frontend/app-merge-session.test.mjs
```

Expected: FAIL because the reducer branches, `handleVoiceResult`, and `InputBox` voice props do not exist yet.

- [ ] **Step 4: Add voice state handling in `app.js` and pass the actions into the shared composer**

```javascript
case "voice/state":
    return { ...state, voiceState: action.value, error: action.error || state.error };
case "voice/mode":
    return { ...state, voiceMode: action.value };
case "voice/result":
    return { ...state, voiceState: "idle", draft: action.text, error: "" };
```

```javascript
async function handleVoiceResult(mode, text) {
    dispatch({ type: "voice/result", text });
    if (mode === "send") {
        await handleSendMessage(text);
    }
}
```

```javascript
<ConversationPage
    ...
    voiceState={state.voiceState}
    voiceMode={state.voiceMode}
    onVoiceModeChange={(value) => dispatch({ type: "voice/mode", value })}
    onVoiceStateChange={(value, error = "") => dispatch({ type: "voice/state", value, error })}
    onVoiceResult={handleVoiceResult}
/>
```

- [ ] **Step 5: Extend `InputBox.js` with recording, transcription, and mode toggle UI**

```javascript
export function InputBox({
    sending,
    error,
    value,
    onChange,
    onSend,
    quickActions,
    voiceState = "idle",
    voiceMode = "fill",
    onVoiceModeChange,
    onVoiceStateChange,
    onVoiceResult
}) {
    const mediaRecorderRef = useRef(null);
    const chunksRef = useRef([]);

    async function handleVoiceToggle() {
        if (voiceState === "recording") {
            mediaRecorderRef.current?.stop();
            return;
        }
        onVoiceStateChange("recording");
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const recorder = new MediaRecorder(stream);
        chunksRef.current = [];
        recorder.ondataavailable = (event) => {
            if (event.data.size > 0) {
                chunksRef.current.push(event.data);
            }
        };
        recorder.onstop = async () => {
            try {
                onVoiceStateChange("transcribing");
                const blob = new Blob(chunksRef.current, { type: recorder.mimeType || "audio/webm" });
                const result = await transcribeAudio(blob, { mode: voiceMode, language: "zh" });
                await onVoiceResult(voiceMode, result.text);
                onVoiceStateChange("idle");
            } catch (error) {
                onVoiceStateChange("failed", error.message || "语音转写失败");
            }
        };
        mediaRecorderRef.current = recorder;
        recorder.start();
    }
}
```

- [ ] **Step 6: Add CSS for the microphone button, recording state, and mode toggle**

```css
.voice-button.recording {
    background: #c84c2a;
    color: #fff7ef;
}

.voice-mode-toggle {
    display: inline-flex;
    gap: 8px;
}

.voice-status {
    font-size: 12px;
    color: var(--muted);
}
```

- [ ] **Step 7: Run the frontend UI tests to verify they pass**

Run:

```bash
node src/test/frontend/input-box-voice-state.test.mjs
node src/test/frontend/app-merge-session.test.mjs
```

Expected: PASS with draft-fill and direct-send flows covered.

- [ ] **Step 8: Commit the shared voice input UI integration**

```bash
git add src/main/resources/static/components/InputBox.js src/main/resources/static/components/ChatArea.js src/main/resources/static/components/HomePage.js src/main/resources/static/components/ConversationPage.js src/main/resources/static/app.js src/main/resources/static/app.css src/test/frontend/input-box-voice-state.test.mjs src/test/frontend/app-merge-session.test.mjs
git commit -m "feat: add voice input to shared web composer"
```

### Task 5: Run end-to-end verification and document the manual checklist

**Files:**
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-05-06-web-voice-input-asr-design.md`

- [ ] **Step 1: Add a short README section describing ASR environment variables and web usage**

```markdown
## Web 语音输入

配置以下环境变量后，Web 端输入框会显示语音转写能力：

    ASR_ENABLED=true
    ASR_PROVIDER=openai-compatible
    ASR_BASE_URL=https://your-asr-endpoint
    ASR_API_KEY=your-key
    ASR_MODEL=whisper-1

使用方式：

1. 点击输入框旁的麦克风开始录音
2. 再次点击结束录音并等待转写
3. 默认回填输入框，也可以切换为“直接发送”
```

- [ ] **Step 2: Run the backend targeted test suite**

Run:

```bash
mvn -Dtest=WebAudioTranscriptionControllerTest,AudioUploadValidatorTest,ProviderAsrServiceTest,WebAgentControllerTest test
```

Expected: PASS with the new ASR slice green and no regression in existing web agent tests.

- [ ] **Step 3: Run the frontend targeted test suite**

Run:

```bash
node src/test/frontend/voice-input-service.test.mjs
node src/test/frontend/input-box-voice-state.test.mjs
node src/test/frontend/app-merge-session.test.mjs
```

Expected: PASS with the new voice service and voice state coverage green.

- [ ] **Step 4: Run the manual browser checklist**

Run:

```bash
mvn -s .mvn/local-settings.xml spring-boot:run
```

Expected manual checks:

- Chrome 打开 `http://localhost:8080`
- 新对话页可以看到麦克风按钮和模式切换
- 录音后 `fill` 模式会把文本写回输入框
- `send` 模式会在转写成功后直接走现有消息发送链路
- 拒绝麦克风权限时只提示错误，不影响手动输入发送
- 关闭 `ASR_ENABLED` 时页面仍可正常文本聊天

- [ ] **Step 5: Commit docs and verification updates**

```bash
git add README.md docs/superpowers/specs/2026-05-06-web-voice-input-asr-design.md
git commit -m "docs: document web voice input setup and verification"
```
