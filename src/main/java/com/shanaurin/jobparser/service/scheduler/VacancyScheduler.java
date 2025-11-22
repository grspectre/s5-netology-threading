package com.shanaurin.jobparser.service.scheduler;

import com.shanaurin.jobparser.service.ParseService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

// service/scheduler/VacancyScheduler.java
@Service
public class VacancyScheduler {

    private final ParseService parseService;

    // демо-список урлов (можно хранить в конфиге)
    private final List<String> demoUrls = List.of(
            "http://mock-hh.local/vacancy/1",
            "http://mock-superjob.local/vacancy/2"
    );

    public VacancyScheduler(ParseService parseService) {
        this.parseService = parseService;
    }

    // раз в 1 минуту
    @Scheduled(fixedRate = 60_000)
    public void scheduledParsing() {
        parseService.parseUrls(demoUrls);
    }
}