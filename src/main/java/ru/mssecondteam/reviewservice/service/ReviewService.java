package ru.mssecondteam.reviewservice.service;

import ru.mssecondteam.reviewservice.dto.ReviewUpdateRequest;
import ru.mssecondteam.reviewservice.model.Review;

import java.util.List;

public interface ReviewService {

    Review createReview(Review review, Long userId);

    Review updateReview(Long reviewId, ReviewUpdateRequest updateRequest, Long userId);

    Review findReviewById(Long reviewId, Long userId);

    List<Review> findReviewsByEventId(Long eventId, Integer page, Integer size, Long userId);

    void deleteReviewById(Long reviewId, Long userId);

    Review addLike(Long reviewId, Long userId, Boolean isPositive);

    Review deleteLike(Long reviewId, Long userId, Boolean isPositive);
}