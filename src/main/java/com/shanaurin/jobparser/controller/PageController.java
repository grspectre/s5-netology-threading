// src/main/java/com/shanaurin/jobparser/controller/PageController.java
package com.shanaurin.jobparser.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/comparison")
    public String comparison() {
        return "comparison";
    }
}