package com.chngy.jobscraper.Scraper.Implemented;

import com.chngy.jobscraper.Common.ListingDTO;
import com.chngy.jobscraper.Scraper.Scraper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GovScraper implements Scraper {

    @Value("${CAREER_GOV_URL:https://jobs.careers.gov.sg/}")
    private String url;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<ListingDTO> Search() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
