package org.example.ggbot.tool.ppt;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PptSpec {

    private String title;
    private List<SlideSpec> slides = new ArrayList<>();
}
