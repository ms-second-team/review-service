package ru.mssecondteam.reviewservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "DTO representing the top reviews with the best and worst reviews.")
public record TopReviewsDto(

        @Schema(description = "List of the best reviews.", example = "[{\"id\": 1, \"title\": \"Excellent product!\", \"content\": \"I loved it.\", \"username\": \"user123\", \"mark\": 10, \"eventId\": 101}]")
        List<ReviewDto> bestReviews,

        @Schema(description = "List of the worst reviews.", example = "[{\"id\": 2, \"title\": \"Terrible experience\", \"content\": \"Not as expected.\", \"username\": \"user456\", \"mark\": 2, \"eventId\": 102}]")
        List<ReviewDto> worstReviews
) {
}
