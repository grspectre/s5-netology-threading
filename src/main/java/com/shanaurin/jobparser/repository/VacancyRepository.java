package com.shanaurin.jobparser.repository;

import com.shanaurin.jobparser.model.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// repository/VacancyRepository.java
public interface VacancyRepository extends JpaRepository<Vacancy, Long> {

    List<Vacancy> findByCityIgnoreCase(String city);

    List<Vacancy> findByCompanyIgnoreCase(String company);
}