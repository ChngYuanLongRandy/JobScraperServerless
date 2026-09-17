package com.chngy.jobscraper.Mapper;

import com.chngy.jobscraper.DTO.ListingDTO;
import com.chngy.jobscraper.DTO.Response.GovSearchResponseJobPost;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
public class CareerGovJobPostingMapper {

    private static final String sourceOfPosting = "careers@gov";

    // Maps all the GovSearchResponseJobPost to ListingDtos
    public static List<ListingDTO> toListingDTOs (List<GovSearchResponseJobPost> jobListings, String baseUrl) {
        List<ListingDTO> listingDTOsToBeReturned = new ArrayList<>();
        for (GovSearchResponseJobPost jobPost : jobListings) {
            List<Integer> parsedExperienceLevels = Pattern.compile("\\d+")
                    .matcher(jobPost.experienceLevels() == null || jobPost.experienceLevels().isEmpty() ? "" : jobPost.experienceLevels().toString())
                    .results()
                    .map(matchResult -> Integer.parseInt(matchResult.group()))
                    .toList();
            List<Integer> parsedSalaryRange = Pattern.compile("\\d+")
                    .matcher(StringUtils.isNotEmpty(jobPost.salaryRange()) ? jobPost.salaryRange().replace(",","") : "")
                    .results()
                    .map(matchResult ->  Integer.parseInt(matchResult.group()))
                    .toList();

            ListingDTO listingDTO = ListingDTO.builder()
                    .jobTitle(Objects.toString(jobPost.name(),""))
                    .company(Objects.toString(jobPost.agency(), ""))
                    .experienceRange(parsedExperienceLevels.isEmpty() ? Pair.of(null, null)
                            : parsedExperienceLevels.size() == 1 ? Pair.of(parsedExperienceLevels.getFirst(), 99)
                            : Pair.of(parsedExperienceLevels.getFirst(),parsedExperienceLevels.getLast()))
                    .salaryRange(parsedSalaryRange.size() < 2 ? Pair.of(null, null) : Pair.of(parsedSalaryRange.getFirst(),parsedSalaryRange.getLast()))
                    .sourceOfPosting(sourceOfPosting)
                    .id(jobPost.id())
                    .employmentType(Objects.toString(jobPost.employmentType(), ""))
                    .openingTimestamp(Objects.toString(jobPost.activityTimestamp(),""))
                    .closingTimestamp(Objects.toString(jobPost.closingTimestamp(), ""))
                    .url(baseUrl + "jobs/" + jobPost.jobSource() + "/" + jobPost.id())
                    .build();
            listingDTOsToBeReturned.add(listingDTO);
        }

        return listingDTOsToBeReturned;
    }
}
