
package com.chngy.jobscraper.Service;

import com.chngy.jobscraper.DTO.ListingDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class Scorer {

    @Value("${NUMBER_OF_RESULTS:5}")
    private int NUMBER_OF_RESULTS;

    @Value("#{'${POSITIVE_SCORER_ITEMS:java}'.split(',')}")
    private List<String> POSITIVE_SCORER_ITEMS;

    @Value("#{'${NEGATIVE_SCORER_ITEMS:react native}'.split(',')}")
    private List<String> NEGATIVE_SCORER_ITEMS;

    public List<ListingDTO> score(List<ListingDTO> data) {
        List<ListingDTO> resultsWithScore = populateScore(data);
        Collections.sort(resultsWithScore);
        return resultsWithScore.stream().limit(NUMBER_OF_RESULTS).toList();
    }

    private record ScoringRule(String keyword, int weight) {}

    private record Match(int start, int end, String keyword, int weight) {}

    public List<ListingDTO> populateScore(List<ListingDTO> data) {
        List<ListingDTO> scoredListings = new ArrayList<>();

        List<ScoringRule> rules = new ArrayList<>();

        POSITIVE_SCORER_ITEMS.forEach(item ->
                addRule(rules, item, 1));

        NEGATIVE_SCORER_ITEMS.forEach(item ->
                addRule(rules, item, -1));

        for (ListingDTO listing : data) {
            String description = listing.jobDescription()
                    .toLowerCase(Locale.ROOT);

            List<Match> matches = new ArrayList<>();

            // Find every whole-word/phrase occurrence.
            for (ScoringRule rule : rules) {
                Pattern pattern = Pattern.compile(
                        "(?<![\\p{L}\\p{N}_])"
                                + Pattern.quote(rule.keyword())
                                + "(?![\\p{L}\\p{N}_])"
                );

                Matcher matcher = pattern.matcher(description);

                while (matcher.find()) {
                    matches.add(new Match(
                            matcher.start(),
                            matcher.end(),
                            rule.keyword(),
                            rule.weight()
                    ));
                }
            }

            // Prefer longer matches when occurrences overlap.
            matches.sort(
                    Comparator.comparingInt(
                                    (Match m) -> m.end() - m.start())
                            .reversed()
                            .thenComparingInt(Match::start)
            );

            List<Match> accepted = new ArrayList<>();

            for (Match candidate : matches) {
                boolean overlaps = accepted.stream().anyMatch(existing ->
                        candidate.start() < existing.end()
                                && candidate.end() > existing.start()
                );

                if (!overlaps) {
                    accepted.add(candidate);
                }
            }

            int totalScore = accepted.stream()
                    .mapToInt(Match::weight)
                    .sum();

            List<String> hits = accepted.stream()
                    .sorted(Comparator.comparingInt(Match::start))
                    .map(Match::keyword).distinct().collect(Collectors.toList());

            ListingDTO scoredListing = listing.toBuilder()
                    .score(totalScore)
                    .hits(hits)
                    .build();

            scoredListings.add(scoredListing);
        }

//        log.info("scoredListings : {}", scoredListings);
        return scoredListings;
    }

    private void addRule(
            List<ScoringRule> rules,
            String keyword,
            int weight
    ) {
        String normalized = keyword.toLowerCase(Locale.ROOT).trim();

        if (!normalized.isBlank()) {
            rules.add(new ScoringRule(normalized, weight));
        }
    }
}
