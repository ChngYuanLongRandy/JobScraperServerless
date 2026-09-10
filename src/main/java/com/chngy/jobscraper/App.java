package com.chngy.jobscraper;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.chngy.jobscraper.Common.ListingDTO;
import com.chngy.jobscraper.Config.Config;
import com.chngy.jobscraper.Scraper.Scraper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import static com.chngy.jobscraper.Common.Constants.*;

/**
 * Lambda function entry point. You can change to use other pojo type or implement
 * a different RequestHandler.
 *
 * @see <a href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda Java Handler</a> for more information
 */
@Slf4j
public class App implements RequestHandler<Map<String, String>, String> {
    private S3AsyncClient s3Client;
    private SnsClient snsClient;
    private final List<Scraper> scrapers;
    final String PROFILE = System.getenv("PROFILE");
    boolean isDev = Strings.isNotEmpty(PROFILE) && PROFILE.equalsIgnoreCase("dev");

    public App() {
        // Initialize the SDK client outside of the handler method so that it can be reused for subsequent invocations.
        // It is initialized when the class is loaded.
        log.info("Profile is: {}", PROFILE);
        if (!isDev){
            s3Client = DependencyFactory.s3Client();
            snsClient = SnsClient.builder().build();
        }
        // Consider invoking a simple api here to pre-warm up the application, eg: dynamodb#listTables
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class)) {
            scrapers = List.copyOf(ctx.getBeansOfType(Scraper.class).values());
        }
    }

    @Override
    public String handleRequest(final Map<String, String> input, final Context context) {
        log.info("Start to handle request");
        log.info("Test logging with SLF4j");
        // TODO: invoking the api call using s3Client.

//        Calls scraper to read website
        List<ListingDTO> listings = new ArrayList<>();
        for (Scraper scraper : scrapers) {
            try {
                listings.addAll(scraper.Search());
            } catch (IOException | InterruptedException e) {
                log.info("Scraper failed: {} - {}", scraper.getClass().getSimpleName(), e.getMessage());
            }
        }

        log.info("End of scraping");

        if(!isDev) {
//      This sends it to the SNS topic
            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(TOPIC_NAME)
                    .subject(SUBJECT)
                    .message(MESSAGE)
                    .build();

            snsClient.publish(publishRequest);
        }
        return "";
    }
}
