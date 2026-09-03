package com.chngy.jobscraper.Scraper.Implemented;

import com.chngy.jobscraper.Common.GovSortBy;
import com.chngy.jobscraper.Common.ListingDTO;
import com.chngy.jobscraper.Scraper.Scraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
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
public class GovScraper implements Scraper {

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

    private String SEARCH_TERM_PREFIX = "?s=";
    private String SORT_PREFIX = "&o=";
    private String YOE_PREFIX = "&e=";
    private String EMPLOYMENT_TERM_PREFIX = "&t=";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<ListingDTO> Search() throws IOException, InterruptedException {
        log.info("Entering Career Gov Scrapper");
        HttpRequest request = HttpRequest.newBuilder(URI.create(buildURL())).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Printing out the response : {}", response);
        ListingDTO listingDTO = ListingDTO.builder().title("SoftwareDev").build();
        return List.of(listingDTO);
    }

    // url would be like
    // https://jobs.careers.gov.sg/?s=software+engineer&t=Full-time;Internship&e=1+-+3+years;4+-+6+years;0+-+1+year;7+-+9+years;%3E+10+years
    private String buildURL() {
        return baseUrl + getSearchTerm() + getFilter() + getSort();
    }

    private String getSort() {
        if (EnumUtils.isValidEnumIgnoreCase(GovSortBy.class, sortCriteria)){
            return GovSortBy.valueOf(sortCriteria).getCode();
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

    //
    private String getFilter() {
        List<String> validEmploymentTerm = new ArrayList<>();
        List<String> validYearsOfExperience = new ArrayList<>();
        if (yearsOfExperience.isEmpty() && employmentTerm.isEmpty()) {
            return "";
        }
        if (yearsOfExperience.isEmpty()) {
            employmentTerm.stream().filter(empTerm -> )
            return EMPLOYMENT_TERM_PREFIX +
        }
        String employmentTerms = EMPLOYMENT_TERM_PREFIX +
        return
    }
}
