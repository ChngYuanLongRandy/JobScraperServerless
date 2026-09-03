package com.chngy.jobscraper.Common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GovSortBy {
    relevance("relevance"),
    closingDate("closing+date"),
    latest("latest");

    private final String code;
}
