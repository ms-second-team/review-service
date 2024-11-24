package ru.mssecondteam.reviewservice.dto;

import lombok.Builder;

@Builder
public record EventReviewStats(

        Long eventId,

        Float avgMark,

        long totalNumberOfReviews,

        Float positiveReviewsPercentage,

        Float negativeReviewsPercentage
) {
}
