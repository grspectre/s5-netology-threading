package com.shanaurin.jobparser.model;

import java.time.LocalDate;

public class Vacancy {
    private String title;
    private String company;
    private String salary;
    private String requirements;
    private String city;
    private LocalDate postedDate;
    private String sourceUrl;

    public Vacancy() {}

    public Vacancy(String title, String company, String salary, String requirements, String city, LocalDate postedDate, String sourceUrl) {
        this.title = title;
        this.company = company;
        this.salary = salary;
        this.requirements = requirements;
        this.city = city;
        this.postedDate = postedDate;
        this.sourceUrl = sourceUrl;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocalDate getPostedDate() { return postedDate; }
    public void setPostedDate(LocalDate postedDate) { this.postedDate = postedDate; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    @Override
    public String toString() {
        return "Vacancy{" +
                "title='" + title + '\'' +
                ", company='" + company + '\'' +
                ", salary='" + salary + '\'' +
                ", city='" + city + '\'' +
                ", postedDate=" + postedDate +
                '}';
    }
}