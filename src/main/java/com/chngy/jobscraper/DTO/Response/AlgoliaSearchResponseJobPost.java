package com.chngy.jobscraper.DTO.Response;

/**
 *
 * @param jobSource 1 to 1 with the jobSource in GovSearchResponseJobPost
 * @param objectID
 */
public record AlgoliaSearchResponseJobPost(
        String jobSource,
        String objectID
) {}
