package ru.mssecondteam.reviewservice;

import lombok.Builder;

@Builder
public record LikeDto(
        Long reviewId,

        Long numbersOfLikes,

        Long numbersOfDislikes
) {
}