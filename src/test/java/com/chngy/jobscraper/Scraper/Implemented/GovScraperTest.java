package com.chngy.jobscraper.Scraper.Implemented;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class GovScraperTest {

    @Mock
    private HttpClient httpClient;
    @InjectMocks
    private GovScraper govScraper;

    @BeforeEach
    public void setup(){
        ReflectionTestUtils.setField(govScraper, "baseUrl", "https://jobs.careers.gov.sg/");
    }

    @Test
    public void govScraperWhenBuildingUrlIfAllComponentsArePresentAndCorrectShouldReturnFullURL () {
        ReflectionTestUtils.setField(govScraper, "yearsOfExperience", List.of("oneToThree","zeroToOne"));
        ReflectionTestUtils.setField(govScraper, "employmentTerm", List.of("fullTime","contract"));
        ReflectionTestUtils.setField(govScraper, "sortCriteria", "relevance");
        ReflectionTestUtils.setField(govScraper, "searchTerm", "software engineer");

        String builtUrl = govScraper.buildURL();
        String expectedResult = "https://jobs.careers.gov.sg/?s=software+engineer&t=Full-time;Contract&e=1+-+3+years;0+-+1+year&o=relevance";
        assertNotNull(builtUrl);
        assertEquals(expectedResult,builtUrl);
    }

    @Test
    public void govScraperWhenBuildingUrlIfSearchTermIsMissingShouldReturnWithoutSearchTerm () {
        ReflectionTestUtils.setField(govScraper, "yearsOfExperience", List.of("oneToThree","zeroToOne"));
        ReflectionTestUtils.setField(govScraper, "employmentTerm", List.of("fullTime","contract"));
        ReflectionTestUtils.setField(govScraper, "sortCriteria", "relevance");

        String builtUrl = govScraper.buildURL();
        String expectedResult = "https://jobs.careers.gov.sg/?t=Full-time;Contract&e=1+-+3+years;0+-+1+year&o=relevance";
        assertNotNull(builtUrl);
        assertEquals(expectedResult,builtUrl);
    }

    @Test
    public void govScraperWhenBuildingUrlIfYoeMissingShouldReturnWithoutYoe() {
        ReflectionTestUtils.setField(govScraper, "employmentTerm", List.of("fullTime","contract"));
        ReflectionTestUtils.setField(govScraper, "sortCriteria", "relevance");
        ReflectionTestUtils.setField(govScraper, "searchTerm", "software engineer");

        String builtUrl = govScraper.buildURL();
        String expectedResult = "https://jobs.careers.gov.sg/?s=software+engineer&t=Full-time;Contract&o=relevance";
        assertNotNull(builtUrl);
        assertEquals(expectedResult,builtUrl);
    }

    @Test
    public void govScraperWhenBuildingUrlIfEmploymentTermMissingShouldReturnWithoutEmploymentTerm() {
        ReflectionTestUtils.setField(govScraper, "yearsOfExperience", List.of("oneToThree","zeroToOne"));
        ReflectionTestUtils.setField(govScraper, "sortCriteria", "relevance");
        ReflectionTestUtils.setField(govScraper, "searchTerm", "software engineer");

        String builtUrl = govScraper.buildURL();
        String expectedResult = "https://jobs.careers.gov.sg/?s=software+engineer&e=1+-+3+years;0+-+1+year&o=relevance";
        assertNotNull(builtUrl);
        assertEquals(expectedResult,builtUrl);
    }

    @Test
    public void govScraperWhenBuildingUrlIfSortCriteriaMissingShouldReturnWithoutSortCriteria() {
        ReflectionTestUtils.setField(govScraper, "yearsOfExperience", List.of("oneToThree","zeroToOne"));
        ReflectionTestUtils.setField(govScraper, "employmentTerm", List.of("fullTime","contract"));
        ReflectionTestUtils.setField(govScraper, "searchTerm", "software engineer");

        String builtUrl = govScraper.buildURL();
        String expectedResult = "https://jobs.careers.gov.sg/?s=software+engineer&t=Full-time;Contract&e=1+-+3+years;0+-+1+year";
        assertNotNull(builtUrl);
        assertEquals(expectedResult,builtUrl);
    }

    @Test
    public void govScraperWhenBuildingUrlIfOnlyOneFilterCriteriaShouldReturnWithOnlyOneFilterCriteria() {
        ReflectionTestUtils.setField(govScraper, "yearsOfExperience", List.of("oneToThree"));
        ReflectionTestUtils.setField(govScraper, "employmentTerm", List.of("fullTime","contract"));
        ReflectionTestUtils.setField(govScraper, "searchTerm", "software engineer");

        String builtUrl = govScraper.buildURL();
        String expectedResult = "https://jobs.careers.gov.sg/?s=software+engineer&t=Full-time;Contract&e=1+-+3+years";
        assertNotNull(builtUrl);
        assertEquals(expectedResult,builtUrl);
    }

    @Test
    public void govScraperWhenBuildingUrlIfOnlySearchTermShouldReturnWithOnlySearchTerm() {
        ReflectionTestUtils.setField(govScraper, "searchTerm", "software engineer");

        String builtUrl = govScraper.buildURL();
        String expectedResult = "https://jobs.careers.gov.sg/?s=software+engineer";
        assertNotNull(builtUrl);
        assertEquals(expectedResult,builtUrl);
    }
}
