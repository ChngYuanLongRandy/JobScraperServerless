package com.chngy.jobscraper.Enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

@Getter
@AllArgsConstructor
public enum GovEmploymentType {
    fullTime("Full-time"),
    traineeship("Traineeship"),
    contract("Contract"),
    others("Others"),
    internship("Internship");

    private final String code;

    // Given a code returns the enum if found
    public static Optional<GovEmploymentType> fromCode(String value){
        return Arrays.stream(values())
                .filter(employmentType-> employmentType.code.equalsIgnoreCase(value))
                .findFirst();
    }

    // Returns Strings with enums constants back to code
    public static List<String> fromEnumConstantToCode (List<String> potentialEmployTypes) {
        if (potentialEmployTypes.isEmpty()) return List.of("");
        Set<String> dedupPotentialEmployTypes = new HashSet<>(potentialEmployTypes);
        return dedupPotentialEmployTypes.stream()
                .filter(potentialEmployType -> Arrays.stream(values())
                        .anyMatch(value -> value.name().equalsIgnoreCase(potentialEmployType)))
                .toList();
    }
}
