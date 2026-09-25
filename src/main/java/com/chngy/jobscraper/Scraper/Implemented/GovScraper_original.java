package com.chngy.jobscraper.Scraper.Implemented;

import com.chngy.jobscraper.DTO.Request.AlgoliaSearchRequest;
import com.chngy.jobscraper.DTO.Response.AlgoliaSearchResponse;
import com.chngy.jobscraper.Enums.GovEmploymentType;
import com.chngy.jobscraper.DTO.ListingDTO;
import com.chngy.jobscraper.Enums.GovSortBy;
import com.chngy.jobscraper.Enums.GovYoe;
import com.chngy.jobscraper.Scraper.Scraper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GovScraper_original implements Scraper {

    @Value("${CAREER_GOV_URL:https://jobs.careers.gov.sg/}")
    private String baseUrl;
    @Value("${SEARCH_TERM:software+engineer}")
    private String searchTerm;
    @Value("${YEARS_OF_EXPERIENCE:oneToThree}")
    private List<String> yearsOfExperience;
    @Value("${EMPLOYMENT_TERM:fullTime}")
    private List<String> employmentTerm;
    @Value("${CAREER_GOV_SORT_BY:relevance}")
    private String sortCriteria;

    private final String SEARCH_TERM_PREFIX = "s=";
    private final String SORT_PREFIX = "o=";
    private final String YOE_PREFIX = "e=";
    private final String EMPLOYMENT_TERM_PREFIX = "t=";
    private final String FIRST_ORDER_PREFIX = "?";
    private final String SUBSEQUENT_PREFIX = "&";

    private String ALGOLIA_APP_ID = "3OW7D8B4IZ";
    private String ALGOLIA_API_KEY = "32fa71d8b0bc06be1e6395bf8c430107";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<ListingDTO> search() throws IOException, InterruptedException {
        log.info("Entering Career Gov Scrapper");
//        HttpRequest request = HttpRequest.newBuilder(URI.create(buildURL())).POST().build();
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://3ow7d8b4iz-dsn.algolia.net/1/indexes/job_index/query"))
                .header("X-Algolia-Application-Id", ALGOLIA_APP_ID)
                .header("X-Algolia-API-Key", ALGOLIA_API_KEY)
                .header("Content-Type", "application/json")
                .header("Referer", baseUrl)
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody()))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Printing out the response : {}", response);
        // the document has only one body inside the html response which we want
        Document doc = Jsoup.parse(response.body());
        String content = doc.body().html();

        log.info("Printing out the response body: {}", doc);
        log.info("Printing out the body: {}", content);
        AlgoliaSearchResponse algoliaSearchResponse = new ObjectMapper().readValue(content, AlgoliaSearchResponse.class);
        log.info("Printing out all the hits: {}", algoliaSearchResponse);
        log.info("Total count: {}", algoliaSearchResponse.hits().size());
        ListingDTO listingDTO = ListingDTO.builder().jobTitle("SoftwareDev").build();
        return List.of(listingDTO);
    }

    // url would be like
    // https://jobs.careers.gov.sg/?s=software+engineer&t=Full-time;Internship&e=1+-+3+years;4+-+6+years;0+-+1+year;7+-+9+years;%3E+10+years
    // the first param would be appended with ? then the rest will follow &
    String buildURL() {
        ArrayList<String> searchFilter = new ArrayList<>();
        String searchTerm = getSearchTerm();
        String filters = getFilter();
        String sort = getSort();
        if (StringUtils.isNotBlank(searchTerm)) {
            searchFilter.add(searchTerm);
        }
        if (StringUtils.isNotBlank(filters)) {
            searchFilter.add(filters);
        }
        if (StringUtils.isNotBlank(sort)) {
            searchFilter.add(sort);
        }
        String returnString = searchFilter.isEmpty() ? "" : FIRST_ORDER_PREFIX + String.join(SUBSEQUENT_PREFIX, searchFilter);
        return baseUrl + returnString;
    }

    private String getSort() {
        if (EnumUtils.isValidEnumIgnoreCase(GovSortBy.class, sortCriteria)){
            return SORT_PREFIX + GovSortBy.valueOf(sortCriteria).getCode();
        }
        log.warn("Invalid Sort Criteria, returning empty");
        return "";
    }

    // every space will be replaced with a +
    private String getSearchTerm(){
        if (StringUtils.isBlank(searchTerm)) {
            return "";
        }
        return SEARCH_TERM_PREFIX + searchTerm.replace(" ","+");
    }

    // Take the appropriate filters, paste the value of the enums in it or return empty if
    // incorrect
    private String getFilter() {
        List<String> validEmploymentTerm = employmentTerm == null ? List.of() : employmentTerm.stream()
                .filter(empTerm -> EnumUtils.isValidEnumIgnoreCase(GovEmploymentType.class, empTerm))
                .map(empTerm -> GovEmploymentType.valueOf(empTerm).getCode())
                .toList();;
        List<String> validYearsOfExperience = yearsOfExperience == null? List.of(): yearsOfExperience.stream()
                .filter(yoe -> EnumUtils.isValidEnumIgnoreCase(GovYoe.class, yoe))
                .map(yoe -> GovYoe.valueOf(yoe).getCode())
                .toList();;


        if (validEmploymentTerm.isEmpty() && validYearsOfExperience.isEmpty()) {
            log.warn("Both Employment Term and YOE is empty");
            return "";
        }
        if (validEmploymentTerm.isEmpty()) {
            return YOE_PREFIX + String.join(";",validYearsOfExperience);
        }

        if (validYearsOfExperience.isEmpty()){
            return EMPLOYMENT_TERM_PREFIX + String.join(";",validEmploymentTerm);
        }

        return EMPLOYMENT_TERM_PREFIX
                + String.join(";",validEmploymentTerm)
                + SUBSEQUENT_PREFIX + YOE_PREFIX
                + String.join(";",validYearsOfExperience);
    }

    private String buildRequestBody() throws JsonProcessingException {
        AlgoliaSearchRequest payload = AlgoliaSearchRequest.builder()
                .query(searchTerm)
                .typoTolerance(true)
                .hitsPerPage(1000)
                .attributesToHighlight(List.of())
                .attributesToRetrieve(List.of("objectID","jobSource"))
                .queryLanguages(List.of("en"))
                .queryType("prefixLast")
                .build();
        return new ObjectMapper().writeValueAsString(payload);
    }
}
