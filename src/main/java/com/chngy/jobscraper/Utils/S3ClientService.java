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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletionException;

@Slf4j
@RequiredArgsConstructor
@Service
@Profile("!dev")
public class S3ClientService {
    private static final DateTimeFormatter RUN_KEY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss'Z'").withZone(ZoneOffset.UTC);

    private final S3AsyncClient s3AsyncClient;
    private final ObjectMapper objectMapper;

    @Value("${S3_BUCKET_NAME:jobscraper}")
    private String S3_BUCKET_NAME;
    @Value("${S3_KEY_NAME:seen}")
    private String S3_KEY_NAME;
    // One file per run holding only the NEW listings of that run (the "seen" file above is cumulative).
    @Value("${S3_RUN_PREFIX:data/runs/}")
    private String S3_RUN_PREFIX;

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
            // Do NOT return an empty list here: that would make dedupe treat every listing as new and the next sync
            // would overwrite the cumulative seen file with only this run's listings. Fail the run instead.
            throw new IllegalStateException("Unable to read seen data s3://" + S3_BUCKET_NAME + "/" + S3_KEY_NAME
                    + ": " + exception.getMessage(), exception);
        }
    }

    // Writes this run's new listings as a JSON array, same serialisation as the "seen" file. Keys sort chronologically.
    // Throws on failure so the caller can stop before marking the listings as seen.
    public String putRun(List<ListingDTO> data) {
        if (data == null || data.isEmpty()) {
            log.info("No new listings this run, not writing a run file");
            return null;
        }
        String key = S3_RUN_PREFIX + RUN_KEY_FORMAT.format(Instant.now()) + ".json";
        byte[] body;
        try {
            body = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialise run file", exception);
        }
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(S3_BUCKET_NAME)
                .key(key)
                .contentType("application/json")
                .build();
        s3AsyncClient.putObject(putObjectRequest, AsyncRequestBody.fromBytes(body)).join();
        log.info("Wrote {} listings to s3://{}/{}", data.size(), S3_BUCKET_NAME, key);
        return key;
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
