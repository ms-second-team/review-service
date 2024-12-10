package ru.mssecondteam.reviewservice.dto.registration;

import lombok.Builder;

@Builder
public record RegistrationResponseDto(
        String username,
        String email,
        String phone,
        Long eventId,
        RegistrationStatus status
) {
}
