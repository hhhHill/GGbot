package org.example.ggbot.tool.ppt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.springframework.stereotype.Component;

@Component
public class PptxRenderer {

    private static final List<String> FONT_CANDIDATES = List.of("Microsoft YaHei", "SimSun", "Arial");
    private final Path outputDirectory;

    public PptxRenderer() {
        this(Path.of("generated", "pptx"));
    }

    public PptxRenderer(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public Path render(PptSpec spec) {
        try {
            Files.createDirectories(outputDirectory);
            Path output = outputDirectory.resolve("ppt-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".pptx");
            try (XMLSlideShow slideShow = new XMLSlideShow(); OutputStream stream = Files.newOutputStream(output)) {
                slideShow.setPageSize(new Dimension(960, 540));
                for (SlideSpec slideSpec : spec.getSlides()) {
                    XSLFSlide slide = slideShow.createSlide();
                    addTitle(slide, slideSpec.getTitle());
                    addSubtitle(slide, slideSpec.getSubtitle());
                    addBullets(slide, slideSpec.getBullets());
                }
                slideShow.write(stream);
            }
            return output;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render PPTX file", ex);
        }
    }

    private void addTitle(XSLFSlide slide, String title) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(50, 35, 620, 60));
        XSLFTextParagraph paragraph = box.addNewTextParagraph();
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(title);
        run.setFontSize(24.0);
        run.setBold(true);
        run.setFontColor(new Color(34, 34, 34));
        run.setFontFamily(resolveFontFamily());
    }

    private void addBullets(XSLFSlide slide, List<String> bullets) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(70, 120, 760, 320));
        for (String bullet : bullets) {
            XSLFTextParagraph paragraph = box.addNewTextParagraph();
            paragraph.setBullet(true);
            paragraph.setLeftMargin(18.0);
            paragraph.setIndent(18.0);
            paragraph.setSpaceAfter(8.0);
            paragraph.setTextAlign(TextParagraph.TextAlign.LEFT);
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText(bullet);
            run.setFontSize(18.0);
            run.setFontColor(new Color(55, 55, 55));
            run.setFontFamily(resolveFontFamily());
        }
    }

    private String resolveFontFamily() {
        return FONT_CANDIDATES.getFirst();
    }

    private void addSubtitle(XSLFSlide slide, String subtitle) {
        if (subtitle == null || subtitle.isBlank()) {
            return;
        }
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(52, 78, 620, 30));
        XSLFTextParagraph paragraph = box.addNewTextParagraph();
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(subtitle);
        run.setFontSize(14.0);
        run.setItalic(true);
        run.setFontColor(new Color(90, 90, 90));
        run.setFontFamily(resolveFontFamily());
    }
}
