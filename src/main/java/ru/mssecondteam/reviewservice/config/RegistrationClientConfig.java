package ru.mssecondteam.reviewservice.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.mssecondteam.reviewservice.client.registration.RegistrationClientErrorDecoder;

@Configuration
public class RegistrationClientConfig {

    @Bean
    public ErrorDecoder registrationClientErrorDecoder() {
        return new RegistrationClientErrorDecoder();
    }
}
