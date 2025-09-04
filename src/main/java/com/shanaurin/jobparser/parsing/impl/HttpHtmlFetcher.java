package com.shanaurin.jobparser.parsing.impl;

import com.shanaurin.jobparser.parsing.HtmlFetcher;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class HttpHtmlFetcher implements HtmlFetcher {
    private final HttpClient httpClient;

    public HttpHtmlFetcher() {
        this.httpClient = HttpClient.newBuilder().build();
    }

    @Override
    public String fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch HTML from: " + url, e);
        }
    }
}