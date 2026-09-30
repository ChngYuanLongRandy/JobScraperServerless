package com.chngy.jobscraper.Config;

import com.chngy.jobscraper.App;
import com.chngy.jobscraper.DependencyFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;

// This componentScan is needed since its not running on spring boot, just spring context
@Configuration
@ComponentScan(basePackageClasses = App.class)
public class Config {

    @Bean
    @Profile("!dev")
    public S3AsyncClient s3Client() {
        return DependencyFactory.s3Client();
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.getSerializerProvider().setNullKeySerializer(new StdSerializer<Object>(Object.class) {
            @Override
            public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeFieldName("null");
            }
        });

        return objectMapper;
    }
}
