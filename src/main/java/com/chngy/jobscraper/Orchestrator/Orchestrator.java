package com.chngy.jobscraper.Orchestrator;

import com.chngy.jobscraper.DTO.ListingDTO;
import com.chngy.jobscraper.Scraper.Implemented.GovScraper;
import com.chngy.jobscraper.Scraper.Implemented.MCFScraper;
import com.chngy.jobscraper.Scraper.Scraper;
import com.chngy.jobscraper.Service.Scorer;
import com.chngy.jobscraper.Service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class Orchestrator {
    private final SyncService syncService;
    private final GovScraper govScraper;
    private final MCFScraper mcfScraper;
    private final Scorer scorer;

    @Value("${JOBS:gov}")
    private List<String> JOBS;

    final String PROFILE = System.getenv("PROFILE");
    boolean isDev = Strings.isNotEmpty(PROFILE) && PROFILE.equalsIgnoreCase("dev");

    private Scraper getScraper (String job) {
        return switch (job.toLowerCase()){
            case "mcf" -> mcfScraper;
            case "gov" -> govScraper;
            default -> null;
        };
    }

    public List<ListingDTO> runJobs() {
        List<ListingDTO> totalDTOs = new ArrayList<>();
        for (String job: JOBS) {
            Scraper scraper = getScraper(job);
            if (scraper == null) {
                log.error("Returned scrapper is null for job: {}, it will not be executed", job);
                continue;
            }

            try {
                List<ListingDTO> tempDTOs = scraper.search();
                List<ListingDTO> dedupedDTOs = List.of();
                if (!isDev){
                    dedupedDTOs = syncService.process(tempDTOs);
                }
                List<ListingDTO> shortlistedDTOs = scorer.score(isDev ? tempDTOs: dedupedDTOs);
                totalDTOs.addAll(shortlistedDTOs);
            }

            catch (IOException exception) {
                log.error("Unable to finish job due to IOException");
            }
            catch (InterruptedException exception) {
                log.error("Unable to finish job due to InterruptedException");
            }
        }
        return totalDTOs;
    }
}
