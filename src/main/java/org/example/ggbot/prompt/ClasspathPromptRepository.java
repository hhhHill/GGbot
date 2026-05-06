package org.example.ggbot.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ClasspathPromptRepository {

    public String load(String name) {
        ClassPathResource resource = new ClassPathResource("prompts/" + name);
        if (!resource.exists()) {
            throw new IllegalStateException("Prompt resource not found: " + name);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load prompt resource: " + name, exception);
        }
    }

    public String load(String name, Map<String, String> variables) {
        String template = load(name);
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() == null ? "" : entry.getValue();
            rendered = rendered.replace(placeholder, value);
        }
        return rendered;
    }
}
