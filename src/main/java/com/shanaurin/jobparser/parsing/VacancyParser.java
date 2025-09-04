package com.shanaurin.jobparser.parsing;

import com.shanaurin.jobparser.model.Vacancy;

public interface VacancyParser {
    boolean supports(String url);
    Vacancy parse(String url);
}