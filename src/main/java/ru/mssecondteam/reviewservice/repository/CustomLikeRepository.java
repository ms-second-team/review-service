package ru.mssecondteam.reviewservice.repository;

import ru.mssecondteam.reviewservice.dto.LikeDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CustomLikeRepository {
    Optional<LikeDto> getLikesAndDislikesByReviewId(Long reviewsId);

    Map<Long, LikeDto> getLikesAndDislikesByReviewsIds(List<Long> reviewsIds);
}
