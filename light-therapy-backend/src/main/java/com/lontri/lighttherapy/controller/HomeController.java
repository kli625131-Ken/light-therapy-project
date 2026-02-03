package com.lontri.lighttherapy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/index"})
    public String home() {
        return "redirect:/swagger-ui/index.html";
    }
}