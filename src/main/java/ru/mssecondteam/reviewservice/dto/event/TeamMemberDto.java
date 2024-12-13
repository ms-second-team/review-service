package ru.mssecondteam.reviewservice.dto.event;

import lombok.Builder;

@Builder
public record TeamMemberDto(
        Long eventId,
        Long userId,
        TeamMemberRole role
) {
}
