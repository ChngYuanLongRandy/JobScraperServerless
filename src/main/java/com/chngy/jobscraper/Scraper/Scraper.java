package com.chngy.jobscraper.Scraper;

import com.chngy.jobscraper.Common.ListingDTO;

import java.io.IOException;
import java.util.List;

public interface Scraper {
    List<ListingDTO> Search() throws IOException, InterruptedException;
}