package ru.mssecondteam.reviewservice.dto;

import lombok.Builder;

@Builder
public record UserReviewStats(

        Long userId,

        Float avgMark,

        long totalNumberOfReviews,

        Float positiveReviewsPercentage,

        Float negativeReviewsPercentage
) {
}
