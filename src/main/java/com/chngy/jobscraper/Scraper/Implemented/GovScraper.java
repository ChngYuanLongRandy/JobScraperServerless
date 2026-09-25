package com.chngy.jobscraper.Scraper.Implemented;

import com.chngy.jobscraper.DTO.*;
import com.chngy.jobscraper.DTO.Request.AlgoliaSearchRequest;
import com.chngy.jobscraper.DTO.Response.AlgoliaSearchResponseJobPost;
import com.chngy.jobscraper.DTO.Response.AlgoliaSearchResponse;
import com.chngy.jobscraper.DTO.Response.GovSearchResponseJobPost;
import com.chngy.jobscraper.Mapper.CareerGovJobPostingMapper;
import com.chngy.jobscraper.Parser.Parser;
import com.chngy.jobscraper.Scraper.Scraper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GovScraper implements Scraper {

    @Value("${CAREER_GOV_URL:https://jobs.careers.gov.sg/}")
    private String baseUrl;
    @Value("${SEARCH_TERM:software+engineer}")
    private String searchTerm;
    @Value("${YEARS_OF_EXPERIENCE:4}")
    private int yearsOfExperience;
    @Value("${YEARS_OF_EXPERIENCE_BUFFER:1}")
    private int yearsOfExperienceBuffer;
    @Value("${EMPLOYMENT_TERM:perm}")
    private List<String> employmentTerm;
    @Value("${CAREER_GOV_SORT_BY:relevance}")
    private String sortCriteria;

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

    public List<ListingDTO> search() throws IOException, InterruptedException {
        log.info("Entering Career Gov Scrapper");

        List<AlgoliaSearchResponseJobPost> hits = fetchAlgoliaHits();
        log.info("*******************************************");
        log.info("Algolia returned {} ranked hits for '{}'", hits.size(), getPlainSearchTerm());

        List<GovSearchResponseJobPost> jobsCatalog = fetchFullCatalogById();
        log.info("*******************************************");
        log.info("Full catalog fetch catalog : {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jobsCatalog));
        log.info("Full catalog fetch returned {} postings", jobsCatalog.size());

        List<GovSearchResponseJobPost> listingsFilteredBySearchTerm = mergeHitsWithCatalog(hits, jobsCatalog);
        log.info("*******************************************");
        log.info("listingsFilteredBySearchTerm : {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listingsFilteredBySearchTerm));
        log.info("listingsFilteredBySearchTerm count: {}", listingsFilteredBySearchTerm.size());

        List<ListingDTO> listingsWithoutJD = CareerGovJobPostingMapper.toListingDTOs(listingsFilteredBySearchTerm, baseUrl);
        log.info("*******************************************");
        log.info("listingsWithoutJD : {}",objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listingsWithoutJD));
        log.info("listingsWithoutJD count: {}", listingsWithoutJD.size());
        
        List<ListingDTO> filteredListingsWithoutJD = filterPostings(listingsWithoutJD);
        
        List<ListingDTO> finalPostings = populateJobDescription(filteredListingsWithoutJD);
        log.info("*******************************************");
        log.info("finalPostings : {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalPostings));
        log.info("finalPostings count : {}", finalPostings.size());

        return finalPostings;
    }

    // reduce the number of postings by applying filters
    List<ListingDTO> filterPostings(List<ListingDTO> listingDTOS) {
        Pair<Integer, Integer> yoeRange = Pair.of(
                Math.max(0, yearsOfExperience - yearsOfExperienceBuffer),
                yearsOfExperience + yearsOfExperienceBuffer
        );
        log.info("yoeRange : {}", yoeRange);
        return listingDTOS.stream()
                .filter(listing -> {
                    log.info("Filtering postings: posting id : {}", listing.id());
                    if (listing.experienceRange() == null ||
                            listing.experienceRange().getLeft() == null && listing.experienceRange().getRight() == null){
                        log.info("returned");
                        return true;
                    }
                    IntPredicate yoeMatch = desiredYoe -> listing.experienceRange().getLeft() <= desiredYoe && listing.experienceRange().getRight() >= desiredYoe;
                    log.info("range provided");
                    log.info("listing.experienceRange().getLeft() : {}", listing.experienceRange().getLeft());
                    log.info("listing.experienceRange().getRight() : {}", listing.experienceRange().getRight());
                    log.info("yoeMatch.test(yoeRange.getLeft()) : {}", yoeMatch.test(yoeRange.getLeft()));
                    log.info("yoeMatch.test(yoeRange.getRight()) : {}", yoeMatch.test(yoeRange.getRight()));
                    return yoeMatch.test(yoeRange.getLeft()) || yoeMatch.test(yoeRange.getRight());
                })
                .collect(Collectors.toList());
    }
    
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
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                .header("Accept", "text/html")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Catalog page fetch status: {}", response.statusCode());

        return parser.extractJobsFromRsc(response.body());
    }

    // Match the listings from the full catalogue vs what is retrieved from the search term
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
//        log.info("Attempting to return JD with url : {}", url);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "text/html")
                .header("User-Agent", BROWSER_USER_AGENT)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//        log.info("Reading response statusCode: {}", response.statusCode());
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
