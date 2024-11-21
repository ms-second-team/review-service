package ru.mssecondteam.reviewservice;

import java.util.Optional;

public interface LikeService {
    void addLike(Long reviewId, Long userId);

    void deleteLike(Long reviewId, Long userId);

    void addDislike(Long reviewId, Long userId);

    void deleteDislike(Long reviewId, Long userId);

    Like getLikeByUserIdAndReviewId(Long userId, Long reviewId);

   // Like getDislikeByUserIdAndReviewId(Long userId, Long reviewId);

    Integer getLikes(Long reviewId);

    Integer getDislikes(Long reviewId);
}
