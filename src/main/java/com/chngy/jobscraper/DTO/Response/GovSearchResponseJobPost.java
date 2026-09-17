package com.chngy.jobscraper.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record GovSearchResponseJobPost(
        @JsonProperty(required = true)
        String id,
        String name,
        String agency,
        String department,
        String activityTimestamp,
        String closingTimestamp,
        @JsonProperty(required = true)
        String jobSource,
        String employmentType,
        List<String> experienceLevels,
        String salaryRange,
        Boolean isAvailable
) {}
