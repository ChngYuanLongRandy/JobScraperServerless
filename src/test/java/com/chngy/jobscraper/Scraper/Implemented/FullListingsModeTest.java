package com.chngy.jobscraper.Scraper.Implemented;

import com.chngy.jobscraper.Config.Config;
import com.chngy.jobscraper.DTO.ListingDTO;
import com.chngy.jobscraper.Service.SyncService;
import com.chngy.jobscraper.Utils.S3ClientService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FullListingsModeTest {

    private static ListingDTO listing(String id) {
        return ListingDTO.builder().id(id).jobTitle("t" + id).url("http://x/" + id).build();
    }

    // ---- job description fetching: failures and the time budget ----

    @Test
    public void failedJdFetchIsSkippedSoItStaysUnseenAndIsRetriedNextRun() throws Exception {
        GovScraper scraper = new GovScraper(null, null);
        List<ListingDTO> out = scraper.populateJobDescription(
                List.of(listing("1"), listing("2"), listing("3")),
                url -> {
                    if (url.endsWith("/2")) throw new IOException("HTTP 429");
                    return "jd for " + url;
                },
                60);
        assertEquals(List.of("1", "3"), out.stream().map(ListingDTO::id).toList());
        assertEquals("jd for http://x/1", out.getFirst().jobDescription());
    }

    @Test
    public void exhaustedBudgetStopsFetchingAndReturnsOnlyWhatWasFetched() throws Exception {
        GovScraper scraper = new GovScraper(null, null);
        List<ListingDTO> out = scraper.populateJobDescription(
                List.of(listing("1"), listing("2")), url -> "jd", 0);
        assertTrue(out.isEmpty(), "a zero-second budget fetches nothing; nothing is marked seen");
    }

    // ---- sync: unreadable closing times must not crash the run ----

    @Test
    public void syncKeepsListingsWithMissingClosingTimeAndDropsClosedOnes() {
        S3ClientService s3 = mock(S3ClientService.class);
        SyncService sync = new SyncService(s3);
        long now = System.currentTimeMillis();
        List<ListingDTO> fresh = List.of(
                listing("blank").toBuilder().closingTimestamp("").build(),
                listing("null"),
                listing("open").toBuilder().closingTimestamp(String.valueOf(now + 86_400_000L)).build(),
                listing("closed").toBuilder().closingTimestamp(String.valueOf(now - 86_400_000L)).build(),
                listing("y9999").toBuilder().closingTimestamp("253370764800000").build());
        sync.syncData(fresh, List.of());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ListingDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(s3).putData(captor.capture());
        assertEquals(List.of("blank", "null", "open", "y9999"), captor.getValue().stream().map(ListingDTO::id).toList());
    }

    // ---- run file: key, format and round trip ----

    @Test
    public void runFileIsWrittenUnderThePrefixAsAJsonArrayThatRoundTrips() throws Exception {
        S3AsyncClient s3Client = mock(S3AsyncClient.class);
        when(s3Client.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(CompletableFuture.completedFuture(PutObjectResponse.builder().build()));
        ObjectMapper mapper = new Config().objectMapper();
        S3ClientService service = new S3ClientService(s3Client, mapper);
        ReflectionTestUtils.setField(service, "S3_BUCKET_NAME", "bucket");
        ReflectionTestUtils.setField(service, "S3_RUN_PREFIX", "data/runs/");

        ListingDTO withRanges = listing("1").toBuilder()
                .jobDescription("desc").experienceRange(Pair.of(3, 5)).salaryRange(Pair.of(null, null)).build();
        String key = service.putRun(List.of(withRanges));

        ArgumentCaptor<PutObjectRequest> req = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<AsyncRequestBody> body = ArgumentCaptor.forClass(AsyncRequestBody.class);
        verify(s3Client).putObject(req.capture(), body.capture());
        assertTrue(key.matches("data/runs/\\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d{2}Z\\.json"), key);
        assertEquals("bucket", req.getValue().bucket());
        assertEquals(key, req.getValue().key());

        byte[] bytes = body.getValue().contentLength().isPresent() ? readAll(body.getValue()) : new byte[0];
        String json = new String(bytes);
        System.out.println("RUN FILE SAMPLE:\n" + json);
        List<ListingDTO> back = mapper.readValue(bytes, new TypeReference<>() {});
        assertEquals("1", back.getFirst().id());
        assertEquals("desc", back.getFirst().jobDescription());
    }

    @Test
    public void emptyRunWritesNothing() {
        S3AsyncClient s3Client = mock(S3AsyncClient.class);
        S3ClientService service = new S3ClientService(s3Client, new ObjectMapper());
        assertNull(service.putRun(List.of()));
        org.mockito.Mockito.verifyNoInteractions(s3Client);
    }

    private static byte[] readAll(AsyncRequestBody body) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        body.subscribe(new org.reactivestreams.Subscriber<>() {
            public void onSubscribe(org.reactivestreams.Subscription s) { s.request(Long.MAX_VALUE); }
            public void onNext(java.nio.ByteBuffer b) { byte[] a = new byte[b.remaining()]; b.get(a); out.writeBytes(a); }
            public void onError(Throwable t) { throw new RuntimeException(t); }
            public void onComplete() { }
        });
        return out.toByteArray();
    }
}
