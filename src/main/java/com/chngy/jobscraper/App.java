package com.chngy.jobscraper;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.List;
import java.util.Map;

import com.chngy.jobscraper.DTO.ListingDTO;
import com.chngy.jobscraper.Config.Config;
import com.chngy.jobscraper.Orchestrator.Orchestrator;
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
    private final Orchestrator orchestrator;
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
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class)) {
            orchestrator = ctx.getBean(Orchestrator.class);
        }
    }

    @Override
    public String handleRequest(final Map<String, String> input, final Context context) {
        log.info("Start to handle request");
        List<ListingDTO> results = orchestrator.runJobs();
        log.info("End of scraping");

        if(!isDev) {
//      This sends it to the SNS topic
            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(TOPIC_NAME)
                    .subject(SUBJECT)
                    .message(results.toString())
                    .build();

            snsClient.publish(publishRequest);
        }
        return "";
    }
}
