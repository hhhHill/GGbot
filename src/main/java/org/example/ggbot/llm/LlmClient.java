package org.example.ggbot.llm;

import java.util.Map;

public interface LlmClient {

    String chat(String prompt, Map<String, Object> options);
}
