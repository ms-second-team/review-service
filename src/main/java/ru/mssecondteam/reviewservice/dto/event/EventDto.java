package ru.mssecondteam.reviewservice.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

import static ru.mssecondteam.reviewservice.Constants.DATA_PATTERN;

@Builder
public record EventDto(
        Long id,
        String name,
        String description,
        @JsonFormat(pattern = DATA_PATTERN)
        LocalDateTime createdDateTime,
        @JsonFormat(pattern = DATA_PATTERN)
        LocalDateTime startDateTime,
        @JsonFormat(pattern = DATA_PATTERN)
        LocalDateTime endDateTime,
        String location,
        Long ownerId
) {
}
