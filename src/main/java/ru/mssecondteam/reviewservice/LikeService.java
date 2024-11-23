package ru.mssecondteam.reviewservice;

import ru.mssecondteam.reviewservice.model.Review;

import java.util.List;
import java.util.Map;

public interface LikeService {
    void addLikeOrDislike(Review review, Long userId, Boolean isPositive);

    void deleteLikeOrDislike(Long reviewId, Long userId, Boolean isPositive);

    LikeDto getNumberOfLikeByReviewId(Long reviewId);

    Map<Long,LikeDto> getNumbersOfLikeByListReviewsId(List <Long> reviewsIds);
}
