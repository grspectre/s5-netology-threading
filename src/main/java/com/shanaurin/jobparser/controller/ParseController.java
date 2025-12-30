package com.shanaurin.jobparser.controller;

import com.shanaurin.jobparser.model.dto.ParseRequest;
import com.shanaurin.jobparser.service.UrlQueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.shanaurin.jobparser.service.ParseService;

@RestController
@RequestMapping("/api")
public class ParseController {

    private final UrlQueueService urlQueueService;
    private final ParseService parseService;

    public ParseController(UrlQueueService urlQueueService, ParseService parseService) {
        this.urlQueueService = urlQueueService;
        this.parseService = parseService;
    }

    @PostMapping("/parse")
    public ResponseEntity<String> parse(@RequestBody ParseRequest request) {
        urlQueueService.addAll(request.getUrls());
        return ResponseEntity.accepted().body("URLs added to queue");
    }

    @PostMapping("/parse/force")
    public ResponseEntity<String> forceParse(@RequestBody ParseRequest request) {
        parseService.parseUrls(request.getUrls());
        return ResponseEntity.accepted().body("URLs parsed");
    }
}