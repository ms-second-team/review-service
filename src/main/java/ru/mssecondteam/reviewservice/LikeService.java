package ru.mssecondteam.reviewservice;

import ru.mssecondteam.reviewservice.model.Review;

import java.util.List;

public interface LikeService {
    void addLike(Review review, Long userId, Boolean isPositive);

    void deleteLike(Long reviewId, Long userId, Boolean isPositive);

    Like getLikeByUserIdAndReviewId(Long userId, Long reviewId);

    LikeDto getLikesByReviewId(Long reviewId);

    List<LikeDto> getLikesByListReviewsId(List <Long> reviewsIds);

}
