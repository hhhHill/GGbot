package org.example.ggbot.adapter.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebAppController {

    @GetMapping({"/", "/chat/new", "/chat/{sessionId}"})
    public String index() {
        return "forward:/index.html";
    }
}
