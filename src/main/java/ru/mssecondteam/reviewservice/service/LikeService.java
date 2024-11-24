package ru.mssecondteam.reviewservice.service;

import ru.mssecondteam.reviewservice.dto.LikeDto;
import ru.mssecondteam.reviewservice.model.Review;

import java.util.List;
import java.util.Map;

public interface LikeService {
    void addLikeOrDislike(Review review, Long userId, Boolean isPositive);

    void deleteLikeOrDislike(Long reviewId, Long userId, Boolean isPositive);

    LikeDto getNumberOfLikesAndDislikesByReviewId(Long reviewId);

    Map<Long, LikeDto> getNumberOfLikesAndDislikesByListReviewsId(List<Long> reviewsIds);
}