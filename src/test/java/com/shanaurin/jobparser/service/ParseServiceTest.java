package com.shanaurin.jobparser.service;

import com.shanaurin.jobparser.logging.LoggingDaemon;
import com.shanaurin.jobparser.model.Vacancy;
import com.shanaurin.jobparser.repository.VacancyRepository;
import com.shanaurin.jobparser.service.client.WebFluxMockHtmlClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ParseServiceTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void parseUrls_shouldSubmitTasksToExecutorAndSaveVacancies() throws Exception {
        WebFluxMockHtmlClient mockClient = mock(WebFluxMockHtmlClient.class);
        VacancyParser parser = mock(VacancyParser.class);
        VacancyRepository repository = mock(VacancyRepository.class);
        LoggingDaemon loggingDaemon = mock(LoggingDaemon.class);

        String html = "<html><body>test</body></html>";
        when(mockClient.fetchHtml(anyString())).thenReturn(html);
        when(parser.parse(eq(html), anyString())).thenAnswer(inv -> {
            String url = inv.getArgument(1, String.class);
            Vacancy v = new Vacancy();
            v.setUrl(url);
            return v;
        });

        ParseService parseService = new ParseService(
                executor,
                mockClient,
                parser,
                repository,
                loggingDaemon
        );

        List<String> urls = List.of("http://localhost/mock/1", "http://localhost/mock/2");

        parseService.parseUrls(urls);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        ArgumentCaptor<Vacancy> captor = ArgumentCaptor.forClass(Vacancy.class);
        verify(repository, times(2)).save(captor.capture());

        List<Vacancy> saved = captor.getAllValues();
        assertThat(saved)
                .extracting(Vacancy::getUrl)
                .containsExactlyInAnyOrder(
                        "http://localhost/mock/1",
                        "http://localhost/mock/2"
                );

        verify(loggingDaemon, atLeastOnce()).log(contains("Saved vacancy from:"));
    }

    @Test
    void parseUrls_shouldLogErrorOnException() throws Exception {
        WebFluxMockHtmlClient mockClient = mock(WebFluxMockHtmlClient.class);
        VacancyParser parser = mock(VacancyParser.class);
        VacancyRepository repository = mock(VacancyRepository.class);
        LoggingDaemon loggingDaemon = mock(LoggingDaemon.class);

        when(mockClient.fetchHtml(anyString())).thenThrow(new RuntimeException("boom"));

        ParseService parseService = new ParseService(
                executor, mockClient, parser, repository, loggingDaemon
        );

        parseService.parseUrls(List.of("http://bad-url"));

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        verify(loggingDaemon).log(contains("Error processing url"));
        verifyNoInteractions(repository);
    }
}