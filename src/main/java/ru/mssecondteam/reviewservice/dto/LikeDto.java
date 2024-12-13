package ru.mssecondteam.reviewservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Representing likes and dislikes for a review")
public record LikeDto(

        @Schema(description = "The ID of the review", example = "10")
        long reviewId,

        @Schema(description = "The number of likes", example = "35")
        long numbersOfLikes,

        @Schema(description = "The number of dislikes", example = "5")
        long numbersOfDislikes
) {
}