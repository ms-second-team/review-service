package ru.mssecondteam.reviewservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.mssecondteam.reviewservice.dto.ReviewUpdateRequest;
import ru.mssecondteam.reviewservice.exception.NotAuthorizedException;
import ru.mssecondteam.reviewservice.exception.NotFoundException;
import ru.mssecondteam.reviewservice.mapper.ReviewMapper;
import ru.mssecondteam.reviewservice.model.Review;
import ru.mssecondteam.reviewservice.model.TopReviews;
import ru.mssecondteam.reviewservice.repository.ReviewRepository;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    private final ReviewMapper reviewMapper;

    private final LikeService likeService;

    @Value("${app.top-reviews-limit}")
    private int topReviewsLimit;

    @Override
    public Review createReview(Review review, Long userId) {
        review.setAuthorId(userId);
        final Review savedReview = reviewRepository.save(review);
        log.info("Review with id '{}' was created", savedReview.getId());
        return savedReview;
    }

    @Override
    public Review updateReview(Long reviewId, ReviewUpdateRequest updateRequest, Long userId) {
        final Review reviewToUpdate = getReviewById(reviewId);
        checkIfUserIsAuthor(reviewToUpdate, userId);
        reviewMapper.updateReview(updateRequest, reviewToUpdate);
        final Review updatedReview = reviewRepository.save(reviewToUpdate);
        log.info("Review with id '{}' was updated", updatedReview.getId());
        return updatedReview;
    }

    @Override
    public Review findReviewById(Long reviewId, Long userId) {
        return getReviewById(reviewId);
    }

    @Override
    public List<Review> findReviewsByEventId(Long eventId, Integer page, Integer size, Long userId) {
        final Pageable pageable = PageRequest.of(page, size);
        final List<Review> eventReviews = reviewRepository.findReviewsByEventId(eventId, pageable);
        log.info("Found '{}' reviews for event with id '{}", eventReviews.size(), eventId);
        return eventReviews;
    }

    @Override
    public void deleteReviewById(Long reviewId, Long userId) {
        final Review reviewToDelete = getReviewById(reviewId);
        checkIfUserIsAuthor(reviewToDelete, userId);
        reviewRepository.deleteById(reviewId);
        log.info("Review with id '{}' was deleted", reviewId);
    }

    @Override
    public Review addLikeOrDislike(Long reviewId, Long userId, Boolean isPositive) {
        final Review review = getReviewById(reviewId);
        checkIfUserIsNotAuthor(review, userId);
        likeService.addLikeOrDislike(review, userId, isPositive);
        log.info("User with id '%s' add like to review with id '%s'", userId, review.getId());
        return review;
    }

    @Override
    public Review deleteLikeOrDislike(Long reviewId, Long userId, Boolean isPositive) {
        final Review review = getReviewById(reviewId);
        likeService.deleteLikeOrDislike(reviewId, userId, isPositive);
        log.info("User with id '%s' delete like to review with id '%s'", userId, review.getId());
        return review;
    }

    @Override
    public TopReviews getTopReviews(Long eventId) {
        final List<Review> worstReviews = reviewRepository.getTopReviewsForEvent(eventId, topReviewsLimit, ASC);
        final List<Review> bestReviews = reviewRepository.getTopReviewsForEvent(eventId, topReviewsLimit, DESC);
        return new TopReviews(bestReviews, worstReviews);
    }

    private Review getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException(String.format("Review with id '%s' was not found", reviewId)));
    }

    private void checkIfUserIsAuthor(Review review, Long userId) {
        if (!review.getAuthorId().equals(userId)) {
            throw new NotAuthorizedException(String.format("User with id '%s' is not authorized to modify review " +
                    "with id '%s'", userId, review.getId()));
        }
    }

    private void checkIfUserIsNotAuthor(Review review, Long userId) {
        if (review.getAuthorId().equals(userId)) {
            throw new NotAuthorizedException(String.format("User with id '%s' is not authorized to add like/dislike review " +
                    "with id '%s'", userId, review.getId()));
        }
    }
}