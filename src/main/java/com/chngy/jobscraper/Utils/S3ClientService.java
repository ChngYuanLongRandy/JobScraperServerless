package com.chngy.jobscraper.Utils;

import com.chngy.jobscraper.DTO.ListingDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionException;

@Slf4j
@RequiredArgsConstructor
@Service
@Profile("!dev")
public class S3ClientService {
    private final S3AsyncClient s3AsyncClient;
    private final ObjectMapper objectMapper;

    @Value("${S3_BUCKET_NAME:jobscraper}")
    private String S3_BUCKET_NAME;
    @Value("${S3_KEY_NAME:seen}")
    private String S3_KEY_NAME;

    public List<ListingDTO> getData() {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(S3_BUCKET_NAME)
                .key(S3_KEY_NAME)
                .build();

        byte[] body;

        try {
            body = s3AsyncClient.getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
                    .join().asByteArray();
        }

        catch (CompletionException exception) {
            if (exception.getCause() instanceof NoSuchKeyException) {
                log.info("Object not created yet at s3://{}/{}", S3_BUCKET_NAME, S3_KEY_NAME);
                return List.of();
            }
            else {
                throw exception;
            }
        }

        try {
            return objectMapper.readValue(body, new TypeReference<>() {});
        }

        catch (IOException exception) {
            log.error("Unable to get data with {} due to {} ", S3_BUCKET_NAME, exception.getMessage());
        }

        return List.of();
    }

    public void putData(List<ListingDTO> data) {
        byte[] body = null;
        if (data == null) {
            log.error("Putting data where data is null, abort");
            return;
        }
        if (data.isEmpty()){
            log.error("Putting data where data is empty, abort");
            return;
        }
        try {
            body = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
        }
        catch (JsonProcessingException exception) {
            log.error("Unable to get write data due to {} ", exception.getMessage());
            return;
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(S3_BUCKET_NAME)
                .key(S3_KEY_NAME)
                .contentType("application/json")
                .build();
        s3AsyncClient.putObject(putObjectRequest, AsyncRequestBody.fromBytes(body)).join();
    }
}
