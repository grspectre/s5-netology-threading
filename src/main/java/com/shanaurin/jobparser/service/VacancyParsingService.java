package com.shanaurin.jobparser.service;

import com.shanaurin.jobparser.model.Vacancy;
import com.shanaurin.jobparser.parsing.VacancyParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VacancyParsingService {

    @Autowired
    private List<VacancyParser> parsers;

    public Vacancy parseVacancy(String url) {
        for (VacancyParser parser : parsers) {
            if (parser.supports(url)) {
                return parser.parse(url);
            }
        }
        throw new IllegalArgumentException("No parser found for URL: " + url);
    }
}