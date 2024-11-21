package ru.mssecondteam.reviewservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReviewDto(
        Long id,

        String title,

        String content,

        String username,

        Integer mark,

        Long eventId,

        @JsonFormat(pattern = "dd.MM.yyyy HH:mm")
        LocalDateTime createdDateTime,

        @JsonFormat(pattern = "dd.MM.yyyy HH:mm")
        LocalDateTime updatedDateTime,

        Integer likes,

        Integer dislikes
) {
}