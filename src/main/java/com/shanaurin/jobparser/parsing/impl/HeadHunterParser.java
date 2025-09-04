package com.shanaurin.jobparser.parsing.impl;

import com.shanaurin.jobparser.model.Vacancy;
import com.shanaurin.jobparser.parsing.HtmlFetcher;
import com.shanaurin.jobparser.parsing.VacancyParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class HeadHunterParser implements VacancyParser {

    @Autowired
    private HtmlFetcher htmlFetcher;

    @Override
    public boolean supports(String url) {
        return url.contains("hh.ru");
    }

    @Override
    public Vacancy parse(String url) {
        String html = htmlFetcher.fetch(url);

        // Здесь будет реальная логика парсинга HTML
        // Для демонстрации создаём заглушку
        Vacancy vacancy = new Vacancy();
        vacancy.setTitle("Java Developer (Parsed from HH.ru)");
        vacancy.setCompany("Tech Company");
        vacancy.setSalary("100000-150000 руб.");
        vacancy.setRequirements("Java, Spring, SQL");
        vacancy.setCity("Москва");
        vacancy.setPostedDate(LocalDate.now());
        vacancy.setSourceUrl(url);

        return vacancy;
    }
}