package ru.mssecondteam.reviewservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

import static ru.mssecondteam.reviewservice.Constants.DATA_PATTERN;

@Builder
@Schema(description = "DTO representing a review entity")
public record ReviewDto(

        @Schema(description = "Unique identifier of the review", example = "1")
        Long id,

        @Schema(description = "Title of the review", example = "Amazing Event!")
        String title,

        @Schema(description = "Content of the review", example = "The event was well-organized and enjoyable.")
        String content,

        @Schema(description = "Username of the reviewer", example = "user_123")
        String username,

        @Schema(description = "Rating given to the event, between 1 and 10", example = "8")
        Integer mark,

        @Schema(description = "ID of the associated event", example = "456")
        Long eventId,

        @JsonFormat(pattern = DATA_PATTERN)
        @Schema(description = "Timestamp when the review was created", example = "2024-11-27 10:15:30")
        LocalDateTime createdDateTime,

        @JsonFormat(pattern = DATA_PATTERN)
        @Schema(description = "Timestamp when the review was last updated", example = "2024-11-27 12:45:00")
        LocalDateTime updatedDateTime,

        @Schema(description = "Number of likes the review has received", example = "120")
        Long numberOfLikes,

        @Schema(description = "Number of dislikes the review has received", example = "3")
        Long numberOfDislikes
) {
}