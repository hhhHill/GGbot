package org.example.ggbot.asr;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DashScopeAsrService implements AsrService {

    private final AsrProperties properties;

    public DashScopeAsrService(AsrProperties properties) {
        this.properties = properties;
    }

    @Override
    public AudioTranscriptionResult transcribe(AudioTranscriptionRequest request) {
        long startedAt = System.currentTimeMillis();
        Path sourceFile = null;
        Path wavFile = null;
        Recognition recognizer = new Recognition();
        try {
            sourceFile = Files.createTempFile("ggbot-asr-", ".webm");
            request.file().transferTo(sourceFile.toFile());

            wavFile = convertToWav(sourceFile);

            RecognitionParam param = RecognitionParam.builder()
                    .apiKey(properties.getApiKey())
                    .model(properties.getModel())
                    .format("wav")
                    .sampleRate(16000)
                    .parameter("language_hints", new String[]{request.language()})
                    .build();

            log.info("Starting ASR recognition model={} format=wav fileSize={}",
                    properties.getModel(), request.file().getSize());

            String text = recognizer.call(param, wavFile.toFile());

            long latencyMs = System.currentTimeMillis() - startedAt;
            log.info("ASR recognition succeeded requestId={} latencyMs={} fileSize={} textLength={} text=[{}]",
                    recognizer.getLastRequestId(), latencyMs, request.file().getSize(),
                    text != null ? text.length() : 0, text);

            return new AudioTranscriptionResult(
                    text,
                    properties.getProvider(),
                    request.language(),
                    latencyMs,
                    recognizer.getLastRequestId()
            );
        } catch (Exception e) {
            log.warn("ASR recognition failed fileSize={}", request.file().getSize(), e);
            throw new AsrProviderException("语音转写服务暂时不可用", e);
        } finally {
            recognizer.getDuplexApi().close(1000, "bye");
            deleteQuietly(sourceFile);
            deleteQuietly(wavFile);
        }
    }

    private Path convertToWav(Path source) throws IOException, InterruptedException {
        Path wavFile = Files.createTempFile("ggbot-asr-", ".wav");
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", source.toString(),
                "-c:a", "pcm_s16le",
                "-ar", "16000",
                "-ac", "1",
                wavFile.toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String ffmpegOutput = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("ffmpeg conversion timed out");
        }
        if (process.exitValue() != 0) {
            log.warn("ffmpeg conversion failed exitCode={} sourceSize={} output={}",
                    process.exitValue(), Files.size(source), ffmpegOutput);
            throw new IOException("ffmpeg conversion failed");
        }
        log.info("Converted audio to WAV sourceSize={} wavSize={}", Files.size(source), Files.size(wavFile));
        return wavFile;
    }

    private void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Failed to delete temp file {}", path, e);
            }
        }
    }
}
