package com.chngy.jobscraper.Common;

import lombok.Builder;

@Builder
public record ListingDTO (
        String title,
        String company,
        String description,
        int minSalary,
        int maxSalary,
        int yearsOfExperience
) {}
