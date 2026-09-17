package com.chngy.jobscraper.Utils;

import com.chngy.jobscraper.DTO.ListingDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3ClientService {
    private final S3AsyncClient s3AsyncClient;

    @Value("${BUCKET_NAME:randomBucketName}")
    private String BUCKET_NAME;

    List<ListingDTO> GetData() {

    }

    void putData(List<ListingDTO> data) {

    }
}
