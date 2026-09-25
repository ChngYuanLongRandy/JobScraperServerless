package com.chngy.jobscraper.Service;

import com.chngy.jobscraper.DTO.ListingDTO;
import com.chngy.jobscraper.Utils.S3ClientService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {
    private final S3ClientService s3ClientService;
    private final ObjectMapper objectMapper;

    @Value("${FILTER_OUT_RESULTS_DAYS_AGO:14}")
    private int DAYS_FILTER;

    // composite function to process the listings including
    // retrieve, dedupe and sync
    public List<ListingDTO> process(List<ListingDTO> data) throws JsonProcessingException {
        log.info("Processing");
        List<ListingDTO> retrievedDataFromS3 = retrieveData();
        List<ListingDTO> dedupedData = dedupeData(data, retrievedDataFromS3);
        syncData(dedupedData, retrievedDataFromS3);
        return dedupedData;
    }

    // retrieve the data from s3
    List<ListingDTO> retrieveData() {
        log.info("Retrieving Data");
        return s3ClientService.getData();
    }

    // compare against what is present in s3
    // remove them and from fresh results
    List<ListingDTO> dedupeData (List<ListingDTO> freshData, List<ListingDTO> retrievedDataFromS3) throws JsonProcessingException {
        log.info("Deduping data, fresh data size: {}, retrievedDataFromS3 size: {}", freshData.size(), retrievedDataFromS3.size());
        Set<ListingDTO> freshDataSet = new HashSet<>(freshData);
        log.info("FreshSet : {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(freshDataSet));
        Set<ListingDTO> retrievedDataSetFromS3 = new HashSet<>(retrievedDataFromS3);
        log.info("retrievedDataSetFromS3 : {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(retrievedDataSetFromS3));
        freshDataSet.removeAll(retrievedDataSetFromS3);
        log.info("FreshSet after removing: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(freshDataSet));
        return freshDataSet.stream().toList();
    }

    // use deduped results and add to the back to s3
    void syncData(List<ListingDTO> dedupedData, List<ListingDTO> retrievedDataFromS3){
        retrievedDataFromS3.addAll(dedupedData);
        List<ListingDTO> newerData = retrievedDataFromS3.stream()
                .filter(data -> {
                    long closingTimeStamp = Long.parseLong(data.closingTimestamp());
                    long openingTimeStamp = Long.parseLong(data.openingTimestamp());
                    long currentTimeStamp = System.currentTimeMillis();
                    long pastTimeStamp = Instant.now().minus(DAYS_FILTER, ChronoUnit.DAYS).toEpochMilli();

                    if (closingTimeStamp > currentTimeStamp) return false;
                    // I see sometimes closing TimeStamp is invalid (year 9999)
                    if (openingTimeStamp > pastTimeStamp) return false;
                    return true;
                }).toList();
        s3ClientService.putData(newerData);
    }
}
