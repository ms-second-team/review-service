package ru.mssecondteam.reviewservice.dto;

import java.util.List;

public record TopReviewsDto(

        List<ReviewDto> bestReviews,

        List<ReviewDto> worstReviews
) {
}
