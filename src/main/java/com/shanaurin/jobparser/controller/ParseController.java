package com.shanaurin.jobparser.controller;

import com.shanaurin.jobparser.model.dto.ParseRequest;
import com.shanaurin.jobparser.service.UrlQueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ParseController {

    private final UrlQueueService urlQueueService;

    public ParseController(UrlQueueService urlQueueService) {
        this.urlQueueService = urlQueueService;
    }

    @PostMapping("/parse")
    public ResponseEntity<String> parse(@RequestBody ParseRequest request) {
        urlQueueService.addAll(request.getUrls());
        return ResponseEntity.accepted().body("URLs added to queue");
    }
}