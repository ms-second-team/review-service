package ru.mssecondteam.reviewservice.like.dto;

import lombok.Builder;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Builder
public record LikeDto(
        long reviewId,

        long numbersOfLikes,

        long numbersOfDislikes
) {
}