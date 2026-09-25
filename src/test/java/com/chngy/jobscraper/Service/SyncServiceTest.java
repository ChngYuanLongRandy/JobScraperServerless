package com.chngy.jobscraper.Service;

import com.chngy.jobscraper.DTO.ListingDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SyncServiceTest {

    @Test
    public void SyncServiceHappyFlow() {

    }

    @Test
    public void SyncServiceWhenGivenDataWithDupIdsShouldReturnDeDupedData() {

    }

    @Test
    public void SyncServiceWhenGivenDataIsEmptyDedupeServiceShouldReturnRetrievedData() {

    }

    @Test
    public void SyncServiceWhenWhenDataIsSyncedDataShouldNotBeAltered() {

    }
    @Test
    public void SyncServiceWhenWhenDataIsSyncedS3ClientShouldBeCalled() {
        List<ListingDTO> dedupedData = List.of(
                ListingDTO.builder()
                    .employmentType("perm")
                    .company("Starbucks")
                    .closingTimestamp()
                    .build(),
                ListingDTO.builder().build()
        );
    }
}
