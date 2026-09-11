package com.chngy.jobscraper.DTO.Request;

import lombok.Builder;

import java.util.List;

@Builder
public record AlgoliaSearchRequest(
        String query,
        int hitsPerPage,
        boolean typoTolerance,
        List<String> attributesToRetrieve,
        List<String> attributesToHighlight,
        List<String> queryLanguages,
        String queryType
) {}
