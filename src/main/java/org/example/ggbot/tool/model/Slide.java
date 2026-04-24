package org.example.ggbot.tool.model;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Slide {

    private final int pageNumber;
    private final String title;
    private final List<String> bullets;
}
