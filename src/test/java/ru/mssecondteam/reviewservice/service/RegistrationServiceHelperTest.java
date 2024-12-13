package ru.mssecondteam.reviewservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import ru.mssecondteam.reviewservice.client.registration.RegistrationClient;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationResponseDto;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationStatus;
import ru.mssecondteam.reviewservice.exception.NotFoundException;
import ru.mssecondteam.reviewservice.exception.ValidationException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceHelperTest {

    @Mock
    private RegistrationClient registrationClient;

    private RegistrationServiceHelper registrationServiceHelper;

    @BeforeEach
    void setUp() {
        registrationServiceHelper = new RegistrationServiceHelper(registrationClient);
    }

    @Test
    @DisplayName("Should successfully validate when user is approved for the event")
    void checkUserApprovedForEvent_whenUserApprovedToEvent_shouldThrowValidationException() {
        Long eventId = 1L;
        String userName = "approvedUser";

        List<RegistrationResponseDto> registrations = List.of(
                new RegistrationResponseDto(userName, "email@example.com", "1234567890", eventId, RegistrationStatus.APPROVED)
        );

        when(registrationClient.searchRegistrations(List.of(RegistrationStatus.APPROVED), eventId))
                .thenReturn(ResponseEntity.ok(registrations));

        assertDoesNotThrow(() -> registrationServiceHelper.checkUserApprovedForEvent(eventId, userName));

        verify(registrationClient).searchRegistrations(List.of(RegistrationStatus.APPROVED), eventId);
    }

    @Test
    @DisplayName("Should throw ValidationException when no registrations are found for the event")
    void checkUserApprovedForEvent_whenNoRegistrationsFound_shouldThrowValidationException() {
        Long eventId = 1L;
        String userName = "nonExistentUser";

        when(registrationClient.searchRegistrations(List.of(RegistrationStatus.APPROVED), eventId))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));
        ValidationException exception = assertThrows(ValidationException.class, () ->
                registrationServiceHelper.checkUserApprovedForEvent(eventId, userName));
        assertEquals(String.format("No registrations found for event with id = %d", eventId), exception.getMessage());

        verify(registrationClient).searchRegistrations(List.of(RegistrationStatus.APPROVED), eventId);
    }

    @Test
    @DisplayName("Should throw ValidationException when user is not in the approved registrations")
    void checkUserApprovedForEvent_whenUserNotApproved_shouldThrowValidationException() {
        Long eventId = 1L;
        String userName = "nonApprovedUser";

        List<RegistrationResponseDto> registrations = List.of(
                new RegistrationResponseDto("otherUser", "email@example.com", "1234567890", eventId, RegistrationStatus.APPROVED)
        );

        when(registrationClient.searchRegistrations(List.of(RegistrationStatus.APPROVED), eventId))
                .thenReturn(ResponseEntity.ok(registrations));
        ValidationException exception = assertThrows(ValidationException.class, () ->
                registrationServiceHelper.checkUserApprovedForEvent(eventId, userName));
        assertEquals(String.format("User %s not approved by event with id = %d", userName, eventId), exception.getMessage());

        verify(registrationClient).searchRegistrations(List.of(RegistrationStatus.APPROVED), eventId);
    }

    @Test
    @DisplayName("Should handle empty response body and throw ValidationException")
    void checkUserApprovedForEvent_whenReturnEmptyList_shouldThrowValidationException() {
        Long eventId = 1L;
        String userName = "user";

        when(registrationClient.searchRegistrations(List.of(RegistrationStatus.APPROVED), eventId))
                .thenReturn(ResponseEntity.ok(List.of()));
        ValidationException exception = assertThrows(ValidationException.class, () ->
                registrationServiceHelper.checkUserApprovedForEvent(eventId, userName));
        assertEquals(String.format("No registrations found for event with id = %d", eventId), exception.getMessage());

        verify(registrationClient).searchRegistrations(List.of(RegistrationStatus.APPROVED), eventId);
    }

    @Test
    @DisplayName("Should throw exception when RegistrationClient returns 404")
    void checkUserApprovedForEvent_whenRegistrationClientReturn404_shouldThrowNotFoundException() {
        Long eventId = 1L;
        String userName = "user";

        when(registrationClient.searchRegistrations(List.of(RegistrationStatus.APPROVED), eventId))
                .thenThrow(new NotFoundException("Registration was not found"));
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                registrationServiceHelper.checkUserApprovedForEvent(eventId, userName));
        assertEquals("Registration was not found", exception.getMessage());

        verify(registrationClient).searchRegistrations(List.of(RegistrationStatus.APPROVED), eventId);
    }

    @Test
    @DisplayName("Should throw exception for unexpected errors from RegistrationClient")
    void checkUserApprovedForEvent_whenUnexpectedErrors_shouldThrowException() {
        Long eventId = 1L;
        String userName = "user";

        when(registrationClient.searchRegistrations(List.of(RegistrationStatus.APPROVED), eventId))
                .thenThrow(new RuntimeException("Unexpected error"));
        Exception exception = assertThrows(Exception.class, () ->
                registrationServiceHelper.checkUserApprovedForEvent(eventId, userName));
        assertEquals("Unexpected error", exception.getMessage());

        verify(registrationClient).searchRegistrations(List.of(RegistrationStatus.APPROVED), eventId);
    }
}
