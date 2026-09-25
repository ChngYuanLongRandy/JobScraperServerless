package com.chngy.jobscraper.Scraper;

import com.chngy.jobscraper.DTO.ListingDTO;

import java.io.IOException;
import java.util.List;

public interface Scraper {
    List<ListingDTO> search() throws IOException, InterruptedException;
}