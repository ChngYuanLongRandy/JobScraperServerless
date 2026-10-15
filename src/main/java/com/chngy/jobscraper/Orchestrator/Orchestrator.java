package com.chngy.jobscraper.Orchestrator;

import com.chngy.jobscraper.DTO.ListingDTO;
import com.chngy.jobscraper.Scraper.Implemented.GovScraper;
import com.chngy.jobscraper.Scraper.Implemented.MCFScraper;
import com.chngy.jobscraper.Scraper.Scraper;
import com.chngy.jobscraper.Service.Scorer;
import com.chngy.jobscraper.Service.SyncService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class Orchestrator {
    // to avoid error when constructing orchestrator bean in dev
    private final Optional<SyncService> syncService;
    private final GovScraper govScraper;
    private final MCFScraper mcfScraper;
    private final Scorer scorer;

    @Value("#{'${JOBS:gov}'.split(',')}")
    private List<String> JOBS;

    // false = skip the Scorer and the top-N email; the full listings go to S3 for downstream filtering instead.
    @Value("${PUBLISH_TOP_RESULTS:true}")
    private boolean publishTopResults;

    public boolean isPublishTopResults() {
        return publishTopResults;
    }

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
            log.info("Running job : {} ", job);
            Scraper scraper = getScraper(job);
            if (scraper == null) {
                log.error("Returned scrapper is null for job: {}, it will not be executed", job);
                continue;
            }

            try {
                List<ListingDTO> tempDTOsWithOutJDs = scraper.search();
                List<ListingDTO> dedupedDTOs = List.of();
                List<ListingDTO> seenDTO = List.of();

                // If it is in production I need s3 service to be enabled and functioning
                if (!isDev && syncService.isPresent()){
                    log.info("Prod env, entering s3 services");
                    SyncService concreteSyncService = syncService.get();
                    seenDTO = concreteSyncService.retrieveData();
                    log.info("Prod env, seenDTO size : {}", seenDTO.size());
                    try {
                        dedupedDTOs = concreteSyncService.dedupeData(tempDTOsWithOutJDs, seenDTO);
                        log.info("Prod env, dedupedDTOs size : {}", dedupedDTOs.size());
                    }
                    catch (JsonProcessingException exception) {
                        log.error("JsonProcessingException with error: {}", exception.getMessage());
                    }
                    catch (Exception exception) {
                        log.error("Technical Error occurred: {}", exception.getMessage());
                    }
                }
                else {
                    log.info("in Dev environment, proceeding without s3Services");
                }
                // if not prod it will be populating with empty list
                List<ListingDTO> tempDTOsWithJDs = scraper.populateJobDescription(dedupedDTOs);

                if (!isDev && syncService.isPresent()){
                    SyncService concreteSyncService = syncService.get();
                    // Run file first, then mark as seen: if the write fails nothing is marked and the next run retries.
                    concreteSyncService.writeRun(tempDTOsWithJDs);
                    concreteSyncService.syncData(tempDTOsWithJDs, seenDTO);
                }

                if (publishTopResults) {
                    // Scoring
                    log.info("Scoring Listings");
                    List<ListingDTO> shortlistedDTOs = scorer.score(tempDTOsWithJDs);
                    totalDTOs.addAll(shortlistedDTOs);
                } else {
                    log.info("PUBLISH_TOP_RESULTS off: skipping scoring, {} new listings written for downstream", tempDTOsWithJDs.size());
                    totalDTOs.addAll(tempDTOsWithJDs);
                }
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
