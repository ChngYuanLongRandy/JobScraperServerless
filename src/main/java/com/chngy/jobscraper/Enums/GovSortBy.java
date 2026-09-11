package com.chngy.jobscraper.Enums;

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
