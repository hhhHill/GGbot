package org.example.ggbot.asr;

public class AsrProviderException extends RuntimeException {

    public AsrProviderException(String message) {
        super(message);
    }

    public AsrProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
