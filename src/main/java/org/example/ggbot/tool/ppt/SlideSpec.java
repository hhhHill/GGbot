package org.example.ggbot.tool.ppt;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlideSpec {

    private int pageNumber;
    private String title;
    private String subtitle;
    private List<String> bullets = new ArrayList<>();
    private String speakerNotes;
}
