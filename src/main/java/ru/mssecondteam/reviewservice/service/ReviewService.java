package ru.mssecondteam.reviewservice.service;

import ru.mssecondteam.reviewservice.dto.UpdateReviewRequest;
import ru.mssecondteam.reviewservice.model.Review;

import java.util.List;

public interface ReviewService {

    Review createReview(Review review, Long userId);

    Review updateReview(Long reviewId, UpdateReviewRequest updateRequest, Long userId);

    Review findReviewById(Long reviewId, Long userId);

    List<Review> findReviewsByEventId(Long eventId, Integer page, Integer size, Long userId);

    void deleteReviewById(Long reviewId, Long userId);
}
