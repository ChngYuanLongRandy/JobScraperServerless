package com.chngy.jobscraper.Common;

public record ListingDTO (
        String title,
        String company,
        String description,
        int minSalary,
        int maxSalary,
        int yearsOfExperience
) {}
