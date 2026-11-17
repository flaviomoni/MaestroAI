package com.corp.ai.agents.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageNavigationController {

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "forward:/login.html";
    }

    @GetMapping("/portal")
    public String portalPage() {
        return "forward:/portal.html";
    }

    @GetMapping("/monitor")
    public String monitorPage() {
        return "forward:/monitor.html";
    }
}
