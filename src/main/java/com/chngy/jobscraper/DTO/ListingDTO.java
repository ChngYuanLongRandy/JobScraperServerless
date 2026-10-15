package com.chngy.jobscraper.DTO;

import lombok.Builder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;

@Builder(toBuilder = true)
public record ListingDTO (
        String jobTitle,
        String id,
        String url,
        String company,
        String employmentType,
        String sourceOfPosting,
        String closingTimestamp,
        String openingTimestamp,
        Pair<Integer, Integer>salaryRange,
        Pair<Integer, Integer> experienceRange,
        String jobDescription,
        // filled by Scorer; 0 / null until scored
        int score,
        List<String> hits
) implements Comparable<ListingDTO> {
    // natural order = best score first, so Collections.sort() gives the Top N
    @Override
    public int compareTo(ListingDTO other) {
        return Integer.compare(other.score, this.score);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof ListingDTO other)) return false;
        return Objects.equals(id, other.id) && Objects.equals(jobTitle, other.jobTitle);
    }

    @Override
    public int hashCode(){
        return Objects.hash(id, jobTitle);
    }
}
