package ru.mssecondteam.reviewservice.client.registration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.mssecondteam.reviewservice.config.RegistrationClientConfig;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationResponseDto;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationStatus;

import java.util.List;

@FeignClient(name = "registrationClient", url = "${app.registration-service.url}", configuration = RegistrationClientConfig.class)
public interface RegistrationClient {
    @GetMapping("/registrations/search")
    ResponseEntity<List<RegistrationResponseDto>> searchRegistrations(
            @RequestParam List<RegistrationStatus> statuses,
            @RequestParam Long eventId);
}
