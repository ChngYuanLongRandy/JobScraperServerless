package com.chngy.jobscraper.Config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

@Configuration
public class JacksonConfig {

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
