package com.shanaurin.jobparser.service;

import com.shanaurin.jobparser.logging.LoggingDaemon;
import com.shanaurin.jobparser.metrics.ParserMetrics;
import com.shanaurin.jobparser.model.Vacancy;
import com.shanaurin.jobparser.repository.VacancyRepository;
import com.shanaurin.jobparser.service.client.WebFluxMockHtmlClient;
import io.micrometer.core.instrument.Timer;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Service
public class ParseService {

    private final ExecutorService vacancyExecutor;
    private final WebFluxMockHtmlClient mockHtmlClient;
    private final VacancyParser vacancyParser;
    private final VacancyRepository vacancyRepository;
    private final LoggingDaemon loggingDaemon;
    private final ParserMetrics metrics;

    public ParseService(ExecutorService vacancyExecutor,
                        WebFluxMockHtmlClient mockHtmlClient,
                        VacancyParser vacancyParser,
                        VacancyRepository vacancyRepository,
                        LoggingDaemon loggingDaemon,
                        ParserMetrics metrics) {
        this.vacancyExecutor = vacancyExecutor;
        this.mockHtmlClient = mockHtmlClient;
        this.vacancyParser = vacancyParser;
        this.vacancyRepository = vacancyRepository;
        this.loggingDaemon = loggingDaemon;
        this.metrics = metrics;
    }

    public void parseUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }

        Timer.Sample batchSample = metrics.startBatchTimer();

        // Счётчик "сколько задач из batch осталось"
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(urls.size());

        // Запускаем обработку каждого URL
        for (String url : urls) {
            vacancyExecutor.submit(() -> {
                try {
                    processUrl(url);
                } finally {
                    latch.countDown();
                }
            });
        }

        // НЕ блокируем вызывающий поток: ждём completion в отдельной задаче
        vacancyExecutor.submit(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // interruption трактуем как unknown/прочее — по желанию можно завести отдельный тип
                metrics.incError("unknown");
            } finally {
                metrics.stopBatchTimer(batchSample);
            }
        });
    }

    private void processUrl(String url) {
        metrics.incProcessed();
        Timer.Sample urlSample = metrics.startUrlTimer();

        try {
            Timer.Sample fetchSample = metrics.startFetchTimer();
            String html;
            try {
                html = mockHtmlClient.fetchHtml(url);
            } finally {
                metrics.stopFetchTimer(fetchSample);
            }

            Timer.Sample parseSample = metrics.startParseTimer();
            Vacancy vacancy;
            try {
                vacancy = vacancyParser.parse(html, url);
            } finally {
                metrics.stopParseTimer(parseSample);
            }

            Timer.Sample dbSample = metrics.startDbTimer();
            try {
                vacancyRepository.save(vacancy);
            } finally {
                metrics.stopDbTimer(dbSample);
            }

            metrics.incSaved();
            loggingDaemon.log("Saved vacancy from: " + url);

        } catch (Exception e) {
            metrics.incError(classifyError(e));
            loggingDaemon.log("Error processing url " + url + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
        } finally {
            metrics.stopUrlTimer(urlSample);
        }
    }
    private String classifyError(Exception e) {
        // HTTP/клиентские ошибки WebClient
        if (e instanceof WebClientResponseException || e instanceof WebClientRequestException) {
            return "http";
        }
        // ошибки БД (Spring Data)
        if (e instanceof DataAccessException) {
            return "db";
        }
        // ошибки парсинга (например, NPE из-за структуры HTML) — можно сузить, если введёшь свой ParseException
        if (e instanceof IllegalArgumentException || e instanceof NullPointerException) {
            return "parse";
        }
        return "unknown";
    }
}