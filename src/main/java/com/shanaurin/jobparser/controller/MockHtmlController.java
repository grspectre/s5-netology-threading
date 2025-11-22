package com.shanaurin.jobparser.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// controller/MockHtmlController.java
@RestController
@RequestMapping("/mock")
public class MockHtmlController {

    @GetMapping("/vacancy/{type}/{id}")
    public String getMockVacancy(
            @PathVariable("type") String templateType,
            @PathVariable("id") Long id
    ) {
        // Здесь вы позже подставите реальные HTML-шаблоны
        return """
                <html>
                  <body>
                    <h1 class="title">Java Developer %d</h1>
                    <div class="company">Awesome Company</div>
                    <div class="city">Moscow</div>
                    <div class="salary">200000 RUB</div>
                    <div class="requirements">Spring, Java, SQL</div>
                    <div class="published-at">2024-10-01</div>
                  </body>
                </html>
                """.formatted(id);
    }
}