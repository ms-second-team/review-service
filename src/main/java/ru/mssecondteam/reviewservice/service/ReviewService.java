package ru.mssecondteam.reviewservice.service;

import ru.mssecondteam.reviewservice.dto.ReviewUpdateRequest;
import ru.mssecondteam.reviewservice.model.Review;
import ru.mssecondteam.reviewservice.model.TopReviews;

import java.util.List;

public interface ReviewService {

    Review createReview(Review review, Long userId);

    Review updateReview(Long reviewId, ReviewUpdateRequest updateRequest, Long userId);

    Review findReviewById(Long reviewId, Long userId);

    List<Review> findReviewsByEventId(Long eventId, Integer page, Integer size, Long userId);

    void deleteReviewById(Long reviewId, Long userId);

    Review addLikeOrDislike(Long reviewId, Long userId, Boolean isPositive);

    Review deleteLikeOrDislike(Long reviewId, Long userId, Boolean isPositive);

    TopReviews getTopReviews(Long eventId);
}