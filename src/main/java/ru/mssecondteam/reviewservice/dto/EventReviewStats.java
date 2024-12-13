package ru.mssecondteam.reviewservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Representing statistics for reviews of an event.")
public record EventReviewStats(

        @Schema(description = "Unique identifier of the event.", example = "67890")
        Long eventId,

        @Schema(description = "Average mark given for the event reviews.", example = "4.2")
        Float avgMark,

        @Schema(description = "Total number of reviews for the event.", example = "320")
        long totalNumberOfReviews,

        @Schema(description = "Percentage of positive reviews for the event.", example = "75.0")
        Float positiveReviewsPercentage,

        @Schema(description = "Percentage of negative reviews for the event.", example = "25.0")
        Float negativeReviewsPercentage
) {
}
