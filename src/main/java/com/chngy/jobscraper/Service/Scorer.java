package com.chngy.jobscraper.Service;

import com.chngy.jobscraper.DTO.ListingDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Scorer {

    @Value("${NUMBER_OF_RESULTS:5}")
    private int NUMBER_OF_RESULTS;

    public List<ListingDTO> score(List<ListingDTO> data) {
        return data;
    }
}
