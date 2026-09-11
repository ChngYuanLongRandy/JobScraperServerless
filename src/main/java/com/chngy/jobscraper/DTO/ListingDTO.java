package com.chngy.jobscraper.DTO;

import lombok.Builder;
import org.apache.commons.lang3.tuple.Pair;

@Builder
public record ListingDTO (
        String jobTitle,
        String id,
        String url,
        String company,
        String sourceOfPosting,
        String closingTimestamp,
        String openingTimestamp,
        Pair<Integer, Integer>salaryRange,
        Pair<Integer, Integer> experienceRange,
        String jobDescription
) {}
