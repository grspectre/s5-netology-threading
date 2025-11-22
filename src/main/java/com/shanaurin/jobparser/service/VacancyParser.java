package com.shanaurin.jobparser.service;

import com.shanaurin.jobparser.model.Vacancy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class VacancyParser {

    public Vacancy parse(String html, String url) {
        Document doc = Jsoup.parse(html);

        String title = doc.selectFirst(".title").text();
        String company = doc.selectFirst(".company").text();
        String city = doc.selectFirst(".city").text();
        String salary = doc.selectFirst(".salary").text();
        String requirements = doc.selectFirst(".requirements").text();
        String dateStr = doc.selectFirst(".published-at").text();
        LocalDateTime publishedAt = LocalDate.parse(dateStr).atStartOfDay();

        Vacancy v = new Vacancy();
        v.setTitle(title);
        v.setCompany(company);
        v.setCity(city);
        v.setSalary(salary);
        v.setRequirements(requirements);
        v.setPublishedAt(publishedAt);
        v.setCreatedAt(LocalDateTime.now());
        v.setUrl(url);
        v.setSource(determineSource(url));
        return v;
    }

    private String determineSource(String url) {
        if (url.contains("hh")) return "hh";
        if (url.contains("superjob")) return "superjob";
        if (url.contains("habr")) return "habr";
        return "unknown";
    }
}