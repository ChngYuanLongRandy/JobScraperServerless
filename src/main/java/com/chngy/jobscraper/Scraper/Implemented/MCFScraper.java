package com.chngy.jobscraper.Scraper.Implemented;

import com.chngy.jobscraper.Common.ListingDTO;
import com.chngy.jobscraper.Scraper.Scraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MCFScraper implements Scraper {

    @Value("${MY_CAREER_FUTURES_URL:https://www.mycareersfuture.gov.sg/}")
    private String url;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<ListingDTO> Search() throws IOException, InterruptedException {
        log.info("Entering MCF Scrapper");
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Printing out the response : {}", response);
        ListingDTO listingDTO = ListingDTO.builder().title("SoftwareDev").build();
        return List.of(listingDTO);
    }
}
