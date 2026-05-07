package org.example.ggbot.asr;

import org.springframework.core.io.ByteArrayResource;

final class AsrByteArrayResource extends ByteArrayResource {

    private final String filename;

    AsrByteArrayResource(byte[] byteArray, String filename) {
        super(byteArray);
        this.filename = filename == null || filename.isBlank() ? "voice.webm" : filename;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
