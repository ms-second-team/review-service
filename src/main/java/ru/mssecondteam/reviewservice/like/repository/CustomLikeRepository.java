package ru.mssecondteam.reviewservice.like.repository;

import ru.mssecondteam.reviewservice.like.dto.LikeDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CustomLikeRepository {
    Optional<LikeDto> getLikesAndDislikesByReviewId(Long reviewsId);

    Map<Long, LikeDto> getLikesAndDislikesByReviewsIds(List<Long> reviewsIds);
}
