package com.chngy.jobscraper.Enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

@Getter
@AllArgsConstructor
public enum GovYoe {
    zeroToOne("0+-+1+year"),
    oneToThree("1+-+3+years"),
    fourToSix("4+-+6+years"),
    sevenToNine("7+-+9+years"),
    moreThanTen(">+10+years");

    private final String code;

    // Given a code returns the enum if found
    public static Optional<GovYoe> fromCode(String value){
        return Arrays.stream(values())
                .filter(employmentType-> employmentType.code.equalsIgnoreCase(value))
                .findFirst();
    }

    // Returns Strings with enums constants back to code
    public static List<String> fromEnumConstantToCode (List<String> potentialEmployYoe) {
        if (potentialEmployYoe.isEmpty()) return List.of("");
        Set<String> dedupPotentialEmployYoe = new HashSet<>(potentialEmployYoe);
        return dedupPotentialEmployYoe.stream()
                .filter(potentialEmploymentYoe -> Arrays.stream(values())
                        .anyMatch(value -> value.name().equalsIgnoreCase(potentialEmploymentYoe)))
                .toList();
    }
}
