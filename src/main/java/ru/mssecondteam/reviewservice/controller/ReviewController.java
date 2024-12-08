package ru.mssecondteam.reviewservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.mssecondteam.reviewservice.dto.EventReviewStats;
import ru.mssecondteam.reviewservice.dto.LikeDto;
import ru.mssecondteam.reviewservice.dto.NewReviewRequest;
import ru.mssecondteam.reviewservice.dto.ReviewDto;
import ru.mssecondteam.reviewservice.dto.ReviewUpdateRequest;
import ru.mssecondteam.reviewservice.dto.TopReviewsDto;
import ru.mssecondteam.reviewservice.dto.UserReviewStats;
import ru.mssecondteam.reviewservice.mapper.ReviewMapper;
import ru.mssecondteam.reviewservice.model.Review;
import ru.mssecondteam.reviewservice.model.TopReviews;
import ru.mssecondteam.reviewservice.service.ReviewService;
import ru.mssecondteam.reviewservice.service.like.LikeService;
import ru.mssecondteam.reviewservice.service.stats.StatsService;

import java.util.List;
import java.util.Map;

import static ru.mssecondteam.reviewservice.mapper.ReviewMapper.getReviewsIds;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    private final LikeService likeService;

    private final StatsService statsService;

    private final ReviewMapper reviewMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto createReview(@RequestBody @Valid NewReviewRequest newReview,
                                  @RequestHeader("X-User-Id") Long userId) {
        log.info("User with id '{}' publishing review for event with id '{}", userId, newReview.eventId());
        final Review review = reviewMapper.toModel(newReview);
        final Review createdReview = reviewService.createReview(review, userId);
        return reviewMapper.toDto(createdReview);
    }

    @PatchMapping("/{reviewId}")
    public ReviewDto updateReview(@PathVariable Long reviewId,
                                  @RequestBody @Valid ReviewUpdateRequest updateRequest,
                                  @RequestHeader("X-User-Id") Long userId) {
        log.info("User with id '{}' updating review with id '{}'", userId, reviewId);
        final Review updatedReview = reviewService.updateReview(reviewId, updateRequest, userId);
        final LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(updatedReview.getId());
        return reviewMapper.toDtoWithLikes(updatedReview, likeDto);
    }

    @GetMapping("/{reviewId}")
    public ReviewDto findReviewById(@PathVariable Long reviewId,
                                    @RequestHeader("X-User-Id") Long userId) {
        log.debug("User with id '{}' requesting review with id '{}'", userId, reviewId);
        final Review review = reviewService.findReviewById(reviewId, userId);
        final LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());
        return reviewMapper.toDtoWithLikes(review, likeDto);
    }

    @GetMapping
    public List<ReviewDto> findReviewsByEventId(@RequestParam Long eventId,
                                                @RequestParam(defaultValue = "0") @PositiveOrZero Integer page,
                                                @RequestParam(defaultValue = "10") @Positive Integer size,
                                                @RequestHeader("X-User-Id") Long userId) {
        log.debug("Requesting reviews for event with id '{}", eventId);
        final List<Review> eventReviews = reviewService.findReviewsByEventId(eventId, page, size, userId);
        final List<Long> reviewsIds = getReviewsIds(eventReviews);
        final Map<Long, LikeDto> likesDto = likeService.getNumberOfLikesAndDislikesByListReviewsId(reviewsIds);
        return reviewMapper.toDtoListWithLikes(eventReviews, likesDto);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReviewById(@PathVariable Long reviewId,
                                 @RequestHeader("X-User-Id") Long userId) {
        log.info("User with id '{}' deleting review with id '{}'", userId, reviewId);
        reviewService.deleteReviewById(reviewId, userId);
    }

    @PostMapping("/{reviewId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto addLike(@PathVariable Long reviewId,
                             @RequestHeader("X-User-Id") Long userId) {
        log.info("User with id '{}' add like to review with id '{}'", userId, reviewId);
        final Review review = reviewService.addLikeOrDislike(reviewId, userId, true);
        final LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());
        return reviewMapper.toDtoWithLikes(review, likeDto);
    }

    @DeleteMapping("/{reviewId}/like")
    @ResponseStatus(HttpStatus.OK)
    public ReviewDto deleteLike(@PathVariable Long reviewId,
                                @RequestHeader("X-User-Id") Long userId) {
        log.info("User with id '{}' delete like to review with id '{}'", userId, reviewId);
        final Review review = reviewService.deleteLikeOrDislike(reviewId, userId, true);
        final LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());
        return reviewMapper.toDtoWithLikes(review, likeDto);
    }

    @PostMapping("/{reviewId}/dislike")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto addDislike(@PathVariable Long reviewId,
                                @RequestHeader("X-User-Id") Long userId) {
        log.info("User with id '{}' add dislike to review with id '{}'", userId, reviewId);
        final Review review = reviewService.addLikeOrDislike(reviewId, userId, false);
        final LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());
        return reviewMapper.toDtoWithLikes(review, likeDto);
    }

    @DeleteMapping("/{reviewId}/dislike")
    @ResponseStatus(HttpStatus.OK)
    public ReviewDto deleteDislike(@PathVariable Long reviewId,
                                   @RequestHeader("X-User-Id") Long userId) {
        log.info("User with id '{}' delete dislike to review with id '{}'", userId, reviewId);
        final Review review = reviewService.deleteLikeOrDislike(reviewId, userId, false);
        final LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());
        return reviewMapper.toDtoWithLikes(review, likeDto);
    }

    @GetMapping("/top")
    public TopReviewsDto getTopReviewsForEvent(@RequestParam Long eventId) {
        log.info("Requesting top reviews for event with id '{}'", eventId);
        final TopReviews topReviews = reviewService.getTopReviews(eventId);
        return getLikesAndMapToDto(topReviews);
    }

    @GetMapping("/stats/events/{eventId}")
    public EventReviewStats getEventReviewsStats(@PathVariable Long eventId) {
        log.info("Requesting reviews stats for event with id '{}'", eventId);
        return statsService.getEventReviewsStats(eventId);
    }

    @GetMapping("/stats/users/{authorId}")
    public UserReviewStats getUserReviewsStats(@PathVariable Long authorId) {
        log.info("Requesting reviews stats for user with id '{}'", authorId);
        return statsService.getUserReviewsStats(authorId);
    }

    private TopReviewsDto getLikesAndMapToDto(TopReviews topReviews) {
        List<Long> bestReviewsIds = topReviews.bestReviews()
                .stream()
                .map(Review::getId)
                .toList();
        Map<Long, LikeDto> bestLikesDto = likeService.getNumberOfLikesAndDislikesByListReviewsId(bestReviewsIds);
        List<ReviewDto> bestReviewsDto = reviewMapper.toDtoListWithLikes(topReviews.bestReviews(), bestLikesDto);

        List<Long> worstReviewsIds = topReviews.worstReviews()
                .stream()
                .map(Review::getId)
                .toList();
        Map<Long, LikeDto> worstLikesDto = likeService.getNumberOfLikesAndDislikesByListReviewsId(worstReviewsIds);
        List<ReviewDto> worstReviewsDto = reviewMapper.toDtoListWithLikes(topReviews.worstReviews(), worstLikesDto);
        return new TopReviewsDto(bestReviewsDto, worstReviewsDto);
    }
}