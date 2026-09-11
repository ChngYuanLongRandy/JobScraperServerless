package com.chngy.jobscraper.Enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum GovEmploymentType {
    fullTime("Full-time"),
    traineeship("Traineeship"),
    contract("Contract"),
    others("Others"),
    internship("Internship");

    private final String code;

    public static Optional<GovEmploymentType> fromCode(String value){
        return Arrays.stream(values())
                .filter(employmentType-> employmentType.code.equalsIgnoreCase(value))
                .findFirst();
    }
}
