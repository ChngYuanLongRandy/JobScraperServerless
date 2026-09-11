package com.chngy.jobscraper.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlgoliaSearchResponse(
        List<AlgoliaSearchResponseJobPost> hits
) {}
