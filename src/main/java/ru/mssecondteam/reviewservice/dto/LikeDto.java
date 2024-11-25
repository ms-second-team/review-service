package ru.mssecondteam.reviewservice.dto;

import lombok.Builder;

@Builder
public record LikeDto(

        long reviewId,

        long numbersOfLikes,

        long numbersOfDislikes
) {
}