package com.chngy.jobscraper.Mapper;

import com.chngy.jobscraper.DTO.ListingDTO;
import com.chngy.jobscraper.DTO.Response.GovSearchResponseJobPost;
import com.chngy.jobscraper.Enums.GovEmploymentType;
import com.chngy.jobscraper.Enums.GovYoe;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CareerGovJobPostingMapperTest {

    String sourceOfPosting = "careers@gov";
    String jobTitleSoftwareEngineer = "Software Engineer";
    String agencySoftwareEngineer = "Agency";
    String departmentSoftwareEngineer = "Dept";
    String baseURL = "https://jobs.careers.gov.sg/";
    String randomUUIDSoftwareEngineer = UUID.randomUUID().toString();

    @Test
    public void careerGovPostMappingWhenPayloadAllValidReturnsValid () {

        long randomActivityTimestamp = ThreadLocalRandom.current().nextLong();
        long randomClosingTimestamp = ThreadLocalRandom.current().nextLong();

        GovSearchResponseJobPost validListing = GovSearchResponseJobPost.builder()
                .agency(agencySoftwareEngineer)
                .department(departmentSoftwareEngineer)
                .employmentType(GovEmploymentType.contract.getCode())
                .experienceLevels(List.of(GovYoe.fourToSix.getCode(), GovYoe.sevenToNine.getCode()))
                .id(randomUUIDSoftwareEngineer)
                .jobSource("HRP")
                .salaryRange("5,000 to 7,000")
                .isAvailable(true)
                .name(jobTitleSoftwareEngineer)
                .activityTimestamp(Long.toString(randomActivityTimestamp))
                .closingTimestamp(Long.toString(randomClosingTimestamp))
                .build();

        ListingDTO listingDTO = ListingDTO.builder()
                .salaryRange(Pair.of(5000,7000))
                .experienceRange(Pair.of(4,9))
                .sourceOfPosting(sourceOfPosting)
                .jobTitle(jobTitleSoftwareEngineer)
                .id(randomUUIDSoftwareEngineer)
                .url(baseURL + "jobs/HRP/"+ randomUUIDSoftwareEngineer)
                .openingTimestamp(Long.toString(randomActivityTimestamp))
                .closingTimestamp(Long.toString(randomClosingTimestamp))
                .build();

        List<ListingDTO> result = CareerGovJobPostingMapper.toListingDTOs(List.of(validListing), baseURL);

        assertEquals(List.of(listingDTO), result);

    }

    @Test
    public void careerGovPostMappingWhenPayloadIsMissingExperienceLevelReturnsValid () {

        long randomActivityTimestamp = ThreadLocalRandom.current().nextLong();
        long randomClosingTimestamp = ThreadLocalRandom.current().nextLong();

        GovSearchResponseJobPost validListing = GovSearchResponseJobPost.builder()
                .agency(agencySoftwareEngineer)
                .department(departmentSoftwareEngineer)
                .employmentType(GovEmploymentType.contract.getCode())
                .experienceLevels(List.of())
                .id(randomUUIDSoftwareEngineer)
                .jobSource("HRP")
                .salaryRange("5,000 to 7,000")
                .isAvailable(true)
                .name(jobTitleSoftwareEngineer)
                .activityTimestamp(Long.toString(randomActivityTimestamp))
                .closingTimestamp(Long.toString(randomClosingTimestamp))
                .build();

        ListingDTO listingDTO = ListingDTO.builder()
                .salaryRange(Pair.of(5000,7000))
                .experienceRange(Pair.of(null,null))
                .sourceOfPosting(sourceOfPosting)
                .jobTitle(jobTitleSoftwareEngineer)
                .id(randomUUIDSoftwareEngineer)
                .url(baseURL + "jobs/HRP/"+ randomUUIDSoftwareEngineer)
                .openingTimestamp(Long.toString(randomActivityTimestamp))
                .closingTimestamp(Long.toString(randomClosingTimestamp))
                .build();

        List<ListingDTO> result = CareerGovJobPostingMapper.toListingDTOs(List.of(validListing), baseURL);

        assertEquals(List.of(listingDTO), result);

    }

    @Test
    public void careerGovPostMappingWhenPayloadIsMissingSalaryReturnsValid () {

        long randomActivityTimestamp = ThreadLocalRandom.current().nextLong();
        long randomClosingTimestamp = ThreadLocalRandom.current().nextLong();

        GovSearchResponseJobPost validListing = GovSearchResponseJobPost.builder()
                .agency(agencySoftwareEngineer)
                .department(departmentSoftwareEngineer)
                .employmentType(GovEmploymentType.contract.getCode())
                .experienceLevels(List.of(GovYoe.fourToSix.getCode(), GovYoe.sevenToNine.getCode()))
                .id(randomUUIDSoftwareEngineer)
                .jobSource("HRP")
                .salaryRange(null)
                .isAvailable(true)
                .name(jobTitleSoftwareEngineer)
                .activityTimestamp(Long.toString(randomActivityTimestamp))
                .closingTimestamp(Long.toString(randomClosingTimestamp))
                .build();

        ListingDTO listingDTO = ListingDTO.builder()
                .salaryRange(Pair.of(null,null))
                .experienceRange(Pair.of(4,9))
                .sourceOfPosting(sourceOfPosting)
                .jobTitle(jobTitleSoftwareEngineer)
                .id(randomUUIDSoftwareEngineer)
                .url(baseURL + "jobs/HRP/"+ randomUUIDSoftwareEngineer)
                .openingTimestamp(Long.toString(randomActivityTimestamp))
                .closingTimestamp(Long.toString(randomClosingTimestamp))
                .build();

        List<ListingDTO> result = CareerGovJobPostingMapper.toListingDTOs(List.of(validListing), baseURL);

        assertEquals(List.of(listingDTO), result);

    }

    @Test
    public void careerGovPostMappingWhenPayloadHasNoExperienceLevelSalaryValidReturnsValid () {

        long randomActivityTimestamp = ThreadLocalRandom.current().nextLong();
        long randomClosingTimestamp = ThreadLocalRandom.current().nextLong();

        GovSearchResponseJobPost validListing = GovSearchResponseJobPost.builder()
                .agency(agencySoftwareEngineer)
                .department(departmentSoftwareEngineer)
                .employmentType(GovEmploymentType.contract.getCode())
                .experienceLevels(List.of())
                .id(randomUUIDSoftwareEngineer)
                .jobSource("HRP")
                .salaryRange(null)
                .isAvailable(true)
                .name(jobTitleSoftwareEngineer)
                .activityTimestamp(Long.toString(randomActivityTimestamp))
                .closingTimestamp(Long.toString(randomClosingTimestamp))
                .build();

        ListingDTO listingDTO = ListingDTO.builder()
                .salaryRange(Pair.of(null,null))
                .experienceRange(Pair.of(null,null))
                .sourceOfPosting(sourceOfPosting)
                .jobTitle(jobTitleSoftwareEngineer)
                .id(randomUUIDSoftwareEngineer)
                .url(baseURL + "jobs/HRP/"+ randomUUIDSoftwareEngineer)
                .openingTimestamp(Long.toString(randomActivityTimestamp))
                .closingTimestamp(Long.toString(randomClosingTimestamp))
                .build();

        List<ListingDTO> result = CareerGovJobPostingMapper.toListingDTOs(List.of(validListing), baseURL);

        assertEquals(List.of(listingDTO), result);

    }
}
