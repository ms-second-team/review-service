package ru.mssecondteam.reviewservice.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.mssecondteam.reviewservice.client.event.EventClientErrorDecoder;

@Configuration
public class EventClientConfig {

    @Bean
    public ErrorDecoder eventClientErrorDecoder() {
        return new EventClientErrorDecoder();
    }
}
