package ru.mssecondteam.reviewservice.client.registration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.mssecondteam.reviewservice.config.RegistrationClientConfig;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationResponseDto;

import java.util.List;

@FeignClient(name = "registrationClient", url = "${registration-service.url}", configuration = RegistrationClientConfig.class)
public interface RegistrationClient {
    @GetMapping("/registrations")
    ResponseEntity<List<RegistrationResponseDto>> findAllByEventId(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Positive int size,
            @RequestParam @Positive Long eventId);
}
