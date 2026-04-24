package org.example.ggbot.tool.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class DocumentArtifact {

    private final String title;
    private final String markdown;
}
