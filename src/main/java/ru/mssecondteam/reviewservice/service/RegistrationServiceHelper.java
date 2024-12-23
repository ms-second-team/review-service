package ru.mssecondteam.reviewservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.mssecondteam.reviewservice.client.registration.RegistrationClient;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationResponseDto;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationStatus;
import ru.mssecondteam.reviewservice.exception.ValidationException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationServiceHelper {
    private final RegistrationClient registrationClient;

    public void checkUserApprovedForEvent(Long eventId, String userName) {
        ResponseEntity<List<RegistrationResponseDto>> response = registrationClient.searchRegistrations(
                List.of(RegistrationStatus.APPROVED), eventId);
        List<RegistrationResponseDto> registrations = response.getBody();
        if (registrations.isEmpty()) {
            throw new ValidationException(String.format("No registrations found for event with id = %d", eventId));
        }
        registrations.stream()
                .filter(registration -> registration.username().equals(userName))
                .findFirst()
                .orElseThrow(() -> new ValidationException(String.format(
                        "User %s not approved by event with id = %d", userName, eventId
                )));
    }

}
