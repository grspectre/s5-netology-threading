package com.shanaurin.jobparser.controller;

import com.shanaurin.jobparser.model.dto.ParseRequest;
import com.shanaurin.jobparser.service.ParseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ParseController {

    private final ParseService parseService;

    public ParseController(ParseService parseService) {
        this.parseService = parseService;
    }

    @PostMapping("/parse")
    public ResponseEntity<String> parse(@RequestBody ParseRequest request) {
        parseService.parseUrls(request.getUrls());
        return ResponseEntity.accepted().body("Parsing started");
    }
}