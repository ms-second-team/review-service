package ru.mssecondteam.reviewservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

import static ru.mssecondteam.reviewservice.Constants.DATA_PATTERN;

@Builder
public record ReviewDto(

        Long id,

        String title,

        String content,

        String username,

        Integer mark,

        Long eventId,

        @JsonFormat(pattern = DATA_PATTERN)
        LocalDateTime createdDateTime,

        @JsonFormat(pattern = DATA_PATTERN)
        LocalDateTime updatedDateTime,

        Long numberOfLikes,

        Long numberOfDislikes
) {
}