package com.shanaurin.jobparser.controller;

import com.shanaurin.jobparser.model.Vacancy;
import com.shanaurin.jobparser.service.VacancyParsingService;
import com.shanaurin.jobparser.service.ThreadManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vacancies")
public class VacancyController {

    @Autowired
    private VacancyParsingService parsingService;

    @Autowired
    private ThreadManagementService threadService;

    @PostMapping("/parse")
    public Vacancy parseVacancy(@RequestParam String url) {
        return parsingService.parseVacancy(url);
    }

    @PostMapping("/demo-threads")
    public String demoThreads() {
        threadService.demonstrateThreads();
        return "Потоки запущены. Проверьте консоль.";
    }
}