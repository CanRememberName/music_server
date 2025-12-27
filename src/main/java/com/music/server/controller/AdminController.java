package com.music.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin")
    public String adminPage() {
        return "upload";
    }

    @GetMapping("/admin/login")
    public String loginPage() {
        return "login";
    }
}
