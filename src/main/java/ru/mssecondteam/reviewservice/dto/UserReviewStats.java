package ru.mssecondteam.reviewservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Representing statistics for a user's reviews.")
public record UserReviewStats(

        @Schema(description = "Unique identifier of the user.", example = "12345")
        Long userId,

        @Schema(description = "Average mark given by the user for reviews.", example = "4.5")
        Float avgMark,

        @Schema(description = "Total number of reviews written by the user.", example = "150")
        long totalNumberOfReviews,

        @Schema(description = "Percentage of positive reviews written by the user.", example = "80.0")
        Float positiveReviewsPercentage,

        @Schema(description = "Percentage of negative reviews written by the user.", example = "20.0")
        Float negativeReviewsPercentage
) {
}