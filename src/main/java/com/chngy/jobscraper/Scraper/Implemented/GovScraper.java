package com.chngy.jobscraper.Scraper.Implemented;

import com.chngy.jobscraper.DTO.*;
import com.chngy.jobscraper.DTO.Request.AlgoliaSearchRequest;
import com.chngy.jobscraper.DTO.Response.AlgoliaSearchResponseJobPost;
import com.chngy.jobscraper.DTO.Response.AlgoliaSearchResponse;
import com.chngy.jobscraper.DTO.Response.GovSearchResponseJobPost;
import com.chngy.jobscraper.Enums.GovEmploymentType;
import com.chngy.jobscraper.Enums.GovSortBy;
import com.chngy.jobscraper.Enums.GovYoe;
import com.chngy.jobscraper.Mapper.CareerGovJobPostingMapper;
import com.chngy.jobscraper.Parser.Parser;
import com.chngy.jobscraper.Scraper.Scraper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final String SEARCH_TERM_PREFIX = "s=";
    private final String SORT_PREFIX = "o=";
    private final String YOE_PREFIX = "e=";
    private final String EMPLOYMENT_TERM_PREFIX = "t=";
    private final String FIRST_ORDER_PREFIX = "?";
    private final String SUBSEQUENT_PREFIX = "&";

    private String ALGOLIA_APP_ID = "3OW7D8B4IZ";
    private String ALGOLIA_API_KEY = "32fa71d8b0bc06be1e6395bf8c430107";


    // Job detail pages (/jobs/<source>/<id>) sit behind a WAF that 403s
    // requests without a browser-like User-Agent — Java's HttpClient default
    // is rejected outright, confirmed via curl (200 with this UA, 403 without).
    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Parser parser;
    private final ObjectMapper objectMapper;

    public List<ListingDTO> Search() throws IOException, InterruptedException {
        log.info("Entering Career Gov Scrapper");

        List<AlgoliaSearchResponseJobPost> hits = fetchAlgoliaHits();
        log.info("Algolia returned {} ranked hits for '{}'", hits.size(), getPlainSearchTerm());

        List<GovSearchResponseJobPost> jobsCatalog = fetchFullCatalogById();
        log.info("Full catalog fetch catalog : {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jobsCatalog));
        log.info("Full catalog fetch returned {} postings", jobsCatalog.size());

        List<GovSearchResponseJobPost> listingsFilteredBySearchTerm = mergeHitsWithCatalog(hits, jobsCatalog);

        List<ListingDTO> listingsWithoutJD = CareerGovJobPostingMapper.toListingDTOs(listingsFilteredBySearchTerm, baseUrl);

        log.info("listingsWithoutJD : {}",objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listingsWithoutJD));
        log.info("listingsWithoutJD count: {}", listingsWithoutJD.size());
        
//        List<ListingDTO> filteredListingsWithoutJD = filterPostings(listingsWithoutJD);
        
        List<ListingDTO> finalPostings = populateJobDescription(listingsWithoutJD);

        log.info("finalPostings : {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalPostings));
        log.info("finalPostings count : {}", finalPostings.size());

        return finalPostings;
    }

    // reduce the number of postings by applying filters
//    List<ListingDTO> filterPostings(List<ListingDTO> listingDTOS) {
//        return
//    }
    
    // ---------------------------------------------------------------
    // Algolia call — ranked object IDs for the free-text search only.
    // No filters are (or should be) sent here; t=/e= are applied locally
    // against the full catalog below, since Algolia's index only carries
    // objectID + jobSource, not employmentType/experienceLevels.
    // ---------------------------------------------------------------
    private List<AlgoliaSearchResponseJobPost> fetchAlgoliaHits() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://3ow7d8b4iz-dsn.algolia.net/1/indexes/job_index/query"))
                .header("X-Algolia-Application-Id", ALGOLIA_APP_ID)
                .header("X-Algolia-API-Key", ALGOLIA_API_KEY)
                .header("Content-Type", "application/json")
                .header("Referer", baseUrl)
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody()))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Algolia response status: {}", response.statusCode());

        // NOTE: previously this went through Jsoup.parse(...).body().html() to strip
        // an <html><body> wrapper — but that wrapper was Jsoup's OWN artifact from
        // parsing raw JSON as if it were HTML (see the toString() of the Document in
        // earlier logs). Round-tripping through .html() also HTML-entity-encodes '&'
        // inside string values (e.g. the "params" field's query string), corrupting
        // the JSON. response.body() is already plain JSON — parse it directly.
        AlgoliaSearchResponse algoliaSearchResponse = objectMapper.readValue(response.body(), AlgoliaSearchResponse.class);
        return algoliaSearchResponse.hits();
    }

    // ---------------------------------------------------------------
    // Full catalog fetch — plain GET to the site itself, then reconstruct
    // the RSC ("Flight") payload Next.js streams via self.__next_f.push()
    // calls, and extract the embedded "jobs":[...] array. This array holds
    // every current posting (site-wide, filter-independent) with full
    // detail — employmentType, experienceLevels, agency, closeAt, etc. —
    // none of which Algolia's response carries.
    // ---------------------------------------------------------------
    private List<GovSearchResponseJobPost> fetchFullCatalogById() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(buildURL()))
                .header("Accept", "text/html")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Catalog page fetch status: {}", response.statusCode());

        return parser.extractJobsFromRsc(response.body());
    }

    // Match the listings from the full catagloue vs what is retrieved from the search term
    private List<GovSearchResponseJobPost> mergeHitsWithCatalog(List<AlgoliaSearchResponseJobPost> hits, List<GovSearchResponseJobPost> jobCatalogue) {

        Set<String> hitIds = hits.stream()
                .map(hit->stripSourcePrefix((hit.objectID())))
                .collect(Collectors.toSet());

        return jobCatalogue.stream()
                .filter(job -> hitIds.contains(job.id()))
                .filter(job -> job.isAvailable() == true)
                .collect(Collectors.toList());
    }

    private String stripSourcePrefix(String objectID) {
        int colonIdx = objectID.indexOf(':');
        return colonIdx == -1 ? objectID : objectID.substring(colonIdx + 1);
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

    // every space will be replaced with a + (for building the URL only —
    // see getPlainSearchTerm() for the form Algolia's JSON body needs)
    private String getSearchTerm(){
        if (StringUtils.isBlank(searchTerm)) {
            return "";
        }
        return SEARCH_TERM_PREFIX + searchTerm.replace(" ","+");
    }

    // Algolia's "query" field is plain JSON text, not a URL — it wants real
    // spaces, not the "+"-joined form used for the browser URL. Previously
    // buildRequestBody() passed the raw searchTerm straight through, so a
    // default of "software+engineer" was sent to Algolia containing a
    // literal '+' character instead of a space between the words.
    private String getPlainSearchTerm() {
        if (StringUtils.isBlank(searchTerm)) {
            return "";
        }
        return searchTerm.replace("+", " ").trim();
    }

    // Take the appropriate filters, paste the value of the enums in it or return empty if
    // incorrect
    private String getFilter() {
        List<String> validEmploymentTerm = getValidEmploymentTermCodes();
        List<String> validYearsOfExperience = getValidYoeCodes();

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

    // Extracted from getFilter() so the same source-of-truth code list can be
    // reused for local (post-catalog-fetch) filtering in matchesFilters().
    private List<String> getValidEmploymentTermCodes() {
        return employmentTerm == null ? List.of() : employmentTerm.stream()
                .filter(empTerm -> EnumUtils.isValidEnumIgnoreCase(GovEmploymentType.class, empTerm))
                .map(empTerm -> GovEmploymentType.valueOf(empTerm).getCode())
                .toList();
    }

    private List<String> getValidYoeCodes() {
        return yearsOfExperience == null ? List.of() : yearsOfExperience.stream()
                .filter(yoe -> EnumUtils.isValidEnumIgnoreCase(GovYoe.class, yoe))
                .map(yoe -> GovYoe.valueOf(yoe).getCode())
                .toList();
    }

    private String buildRequestBody() throws JsonProcessingException {
        AlgoliaSearchRequest payload = AlgoliaSearchRequest.builder()
                .query(getPlainSearchTerm())
                .typoTolerance(true)
                .hitsPerPage(1000)
                .attributesToHighlight(List.of())
                .attributesToRetrieve(List.of("objectID","jobSource"))
                .queryLanguages(List.of("en"))
                .queryType("prefixLast")
                .build();
        return objectMapper.writeValueAsString(payload);
    }

    // API calls are intentionally made blocking in order to not overwhelm server
    private List<ListingDTO> populateJobDescription(List<ListingDTO> jobCatalogue) throws IOException, InterruptedException {
        List<ListingDTO> jobsWithJDs = new ArrayList<>();
        for (ListingDTO job : jobCatalogue) {
            String jd = returnJobDescription(job.url());
            ListingDTO jobWithJD = job.toBuilder()
                    .jobDescription(jd)
                    .build();
            jobsWithJDs.add(jobWithJD);
        }
        return jobsWithJDs;
    }

    private String returnJobDescription(String url) throws IOException, InterruptedException {
        if (url.isBlank()) return "";
        log.info("Attempting to return JD with url : {}", url);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "text/html")
                .header("User-Agent", BROWSER_USER_AGENT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Reading response statusCode: {}", response.statusCode());
//        log.info("Reading response body: {}", response.body());
//        Document document = Jsoup.parse(response.body());
//        log.info("Reading response body after parsing: {}", document.body());
        // test out function first
        return extractJDFromJson(response.body());
    }

    private String extractJDFromJson(String html) {
        Element script = Jsoup.parse(html).selectFirst("script[type=application/ld+json]");
        if (script == null) {
            log.warn("No JSON-LD job posting block found in job details page");
            return "";
        }
        try{
            JsonNode jobPosting = objectMapper.readTree(script.data());
            String descriptionHtml = jobPosting.path("description").asText("");
            String jdText = Jsoup.parse(descriptionHtml).text();
            log.info("jd text: {}", jdText);
            return jdText;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON-LD JobPosting block", e);
        }
    }

}
