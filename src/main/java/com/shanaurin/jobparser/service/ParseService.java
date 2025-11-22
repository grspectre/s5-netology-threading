package com.shanaurin.jobparser.service;

import com.shanaurin.jobparser.logging.LoggingDaemon;
import com.shanaurin.jobparser.model.Vacancy;
import com.shanaurin.jobparser.repository.VacancyRepository;
import com.shanaurin.jobparser.service.client.WebFluxMockHtmlClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
public class ParseService {

    private final ExecutorService vacancyExecutor;
    private final WebFluxMockHtmlClient mockHtmlClient;
    private final VacancyParser vacancyParser;
    private final VacancyRepository vacancyRepository;
    private final LoggingDaemon loggingDaemon;

    public ParseService(ExecutorService vacancyExecutor,
                        WebFluxMockHtmlClient mockHtmlClient,
                        VacancyParser vacancyParser,
                        VacancyRepository vacancyRepository,
                        LoggingDaemon loggingDaemon) {
        this.vacancyExecutor = vacancyExecutor;
        this.mockHtmlClient = mockHtmlClient;
        this.vacancyParser = vacancyParser;
        this.vacancyRepository = vacancyRepository;
        this.loggingDaemon = loggingDaemon;
    }

    public void parseUrls(List<String> urls) {
        for (String url : urls) {
            vacancyExecutor.submit(() -> processUrl(url));
        }
    }

    private void processUrl(String url) {
        try {
            String html = mockHtmlClient.fetchHtml(url);
            Vacancy vacancy = vacancyParser.parse(html, url);
            vacancyRepository.save(vacancy);
            loggingDaemon.log("Saved vacancy from: " + url);
        } catch (Exception e) {
            loggingDaemon.log("Error processing url " + url + ": " + e.getMessage());
        }
    }
}