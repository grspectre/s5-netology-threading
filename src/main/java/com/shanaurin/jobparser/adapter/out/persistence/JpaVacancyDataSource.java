package com.shanaurin.jobparser.adapter.out.persistence;

import com.shanaurin.jobparser.domain.model.VacancyDomain;
import com.shanaurin.jobparser.domain.port.out.VacancyDataSource;
import com.shanaurin.jobparser.model.Vacancy;
import com.shanaurin.jobparser.repository.VacancyRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Адаптер для получения вакансий из базы данных через JPA.
 * Конвертирует JPA-сущности в доменные объекты.
 */
public class JpaVacancyDataSource implements VacancyDataSource {

    private static final String SOURCE_NAME = "JPA_DATABASE";

    private final VacancyRepository vacancyRepository;

    public JpaVacancyDataSource(VacancyRepository vacancyRepository) {
        this.vacancyRepository = vacancyRepository;
    }

    @Override
    public List<VacancyDomain> fetchAll() {
        return vacancyRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<VacancyDomain> fetchByCity(String city) {
        return vacancyRepository.findByCityIgnoreCase(city).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<VacancyDomain> fetchById(Long id) {
        return vacancyRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public boolean isAvailable() {
        try {
            vacancyRepository.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    /**
     * Конвертация JPA-сущности в доменную модель
     */
    private VacancyDomain toDomain(Vacancy entity) {
        return new VacancyDomain(
                entity.getId(),
                entity.getSource(),
                entity.getUrl(),
                entity.getTitle(),
                entity.getCompany(),
                entity.getCity(),
                entity.getSalary(),
                entity.getRequirements(),
                entity.getPublishedAt(),
                entity.getCreatedAt()
        );
    }
}