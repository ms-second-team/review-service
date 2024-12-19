package ru.mssecondteam.reviewservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import ru.mssecondteam.reviewservice.dto.NewReviewRequest;
import ru.mssecondteam.reviewservice.dto.ReviewDto;
import ru.mssecondteam.reviewservice.dto.ReviewUpdateRequest;
import ru.mssecondteam.reviewservice.dto.TopReviewsDto;
import ru.mssecondteam.reviewservice.dto.UserReviewStats;
import ru.mssecondteam.reviewservice.mapper.ReviewMapper;
import ru.mssecondteam.reviewservice.model.Review;
import ru.mssecondteam.reviewservice.model.TopReviews;
import ru.mssecondteam.reviewservice.service.helper.ReviewLikeService;
import ru.mssecondteam.reviewservice.service.review.ReviewService;
import ru.mssecondteam.reviewservice.service.stats.StatsService;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reviews API", description = "API for review management")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;
    private final ReviewLikeService reviewLikeService;
    private final StatsService statsService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new review", description = "Allows the user to create a new review for a past event they participated in")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review successfully created",
                    content = @Content(schema = @Schema(implementation = ReviewDto.class))),
            @ApiResponse(responseCode = "400", description = "Incorrect data"),
            @ApiResponse(responseCode = "500", description = "Unknown error")
    })
    public ReviewDto createReview(
            @RequestBody @Valid @Parameter(description = "New review") NewReviewRequest newReview,
            @RequestHeader("X-User-Id") @Parameter(description = "ID of the user creating the review") Long userId) {
        log.info("User with id '{}' publishing review for event with id '{}", userId, newReview.eventId());
        final Review review = reviewMapper.toModel(newReview);
        final Review createdReview = reviewService.createReview(review, userId);
        return reviewMapper.toDto(createdReview);
    }

    @PatchMapping("/{reviewId}")
    @Operation(summary = "Update Review", description = "Updates an existing review by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The review has been successfully updated",
                    content = @Content(schema = @Schema(implementation = ReviewDto.class))),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @ApiResponse(responseCode = "403", description = "User is not authorized to modify review"),
            @ApiResponse(responseCode = "500", description = "Unknown error")
    })
    public ReviewDto updateReview(
            @PathVariable @Parameter(description = "Review ID to update") Long reviewId,
            @RequestBody @Valid @Parameter(description = "New review") ReviewUpdateRequest updateRequest,
            @RequestHeader("X-User-Id") @Parameter(description = "User ID of the user updating the review") Long userId) {
        log.info("User with id '{}' updating review with id '{}'", userId, reviewId);
        final Review updatedReview = reviewService.updateReview(reviewId, updateRequest, userId);
        return reviewLikeService.getReviewWithLikes(updatedReview);
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "Get review by ID", description = "Returns a review with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review found",
                    content = @Content(schema = @Schema(implementation = ReviewDto.class))),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "500", description = "Unknown error")
    })
    public ReviewDto findReviewById(
            @PathVariable @Parameter(description = "Review ID") Long reviewId,
            @RequestHeader("X-User-Id") @Parameter(description = "ID of the user requesting review") Long userId) {
        log.debug("User with id '{}' requesting review with id '{}'", userId, reviewId);
        final Review review = reviewService.findReviewById(reviewId, userId);
        return reviewLikeService.getReviewWithLikes(review);
    }

    @GetMapping
    @Operation(summary = "Get reviews for an event", description = "Returns a list of reviews for the specified event with pagination support")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reviews successfully received",
                    content = @Content(schema = @Schema(implementation = ReviewDto.class))),
            @ApiResponse(responseCode = "400", description = "Incorrect data"),
            @ApiResponse(responseCode = "500", description = "Unknown error")
    })
    public List<ReviewDto> findReviewsByEventId(
            @RequestParam @Parameter(description = "Review ID") Long eventId,
            @RequestParam(defaultValue = "0") @PositiveOrZero @Parameter(description = "Page number") Integer page,
            @RequestParam(defaultValue = "10") @Positive @Parameter(description = "Page size") Integer size,
            @RequestHeader("X-User-Id") @Parameter(description = "User ID") Long userId) {
        log.debug("Requesting reviews for event with id '{}", eventId);
        final List<Review> eventReviews = reviewService.findReviewsByEventId(eventId, page, size, userId);
        return reviewLikeService.getReviewsWithLikes(eventReviews);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete review", description = "Deletes a review by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Review successfully deleted"),
            @ApiResponse(responseCode = "403", description = "User is not authorized to delete review"),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "500", description = "Unknown error")
    })
    public void deleteReviewById(
            @PathVariable @Parameter(description = "Review ID for deletion") Long reviewId,
            @RequestHeader("X-User-Id") @Parameter(description = "ID of the user deleting the review") Long userId) {
        log.info("User with id '{}' deleting review with id '{}'", userId, reviewId);
        reviewService.deleteReviewById(reviewId, userId);
    }

    @PostMapping("/{reviewId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a like", description = "Adds a like to a review by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Like was added",
                    content = @Content(schema = @Schema(implementation = ReviewDto.class))),
            @ApiResponse(responseCode = "403", description = "User is not authorized to add like to review"),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "500", description = "Unknown error")
    })
    public ReviewDto addLike(
            @PathVariable @Parameter(description = "Review ID") Long reviewId,
            @RequestHeader("X-User-Id") @Parameter(description = "ID of the user adding the like") Long userId) {
        log.info("User with id '{}' add like to review with id '{}'", userId, reviewId);
        final Review review = reviewService.addLikeOrDislike(reviewId, userId, true);
        return reviewLikeService.getReviewWithLikes(review);
    }

    @DeleteMapping("/{reviewId}/like")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Delete like", description = "Removes likes from a user from a review by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Like was deleted",
                    content = @Content(schema = @Schema(implementation = ReviewDto.class))),
            @ApiResponse(responseCode = "403", description = "User is not authorized to delete like to review"),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "500", description = "Unknown error")
    })
    public ReviewDto deleteLike(
            @PathVariable @Parameter(description = "Review ID") Long reviewId,
            @RequestHeader("X-User-Id") @Parameter(description = "User ID") Long userId) {
        log.info("User with id '{}' delete like to review with id '{}'", userId, reviewId);
        final Review review = reviewService.deleteLikeOrDislike(reviewId, userId, true);
        return reviewLikeService.getReviewWithLikes(review);
    }

    @PostMapping("/{reviewId}/dislike")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a dislike", description = "Adds a dislike to a review by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dislike was added",
                    content = @Content(schema = @Schema(implementation = ReviewDto.class))),
            @ApiResponse(responseCode = "403", description = "User is not authorized to add dislike to review"),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "500", description = "Unknown error")
    })
    public ReviewDto addDislike(
            @PathVariable @Parameter(description = "Review ID") Long reviewId,
            @RequestHeader("X-User-Id") @Parameter(description = "ID of the user adding the dislike") Long userId) {
        log.info("User with id '{}' add dislike to review with id '{}'", userId, reviewId);
        final Review review = reviewService.addLikeOrDislike(reviewId, userId, false);
        return reviewLikeService.getReviewWithLikes(review);
    }

    @DeleteMapping("/{reviewId}/dislike")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Delete dislike", description = "Removes dislikes from a user from a review by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dislike was deleted",
                    content = @Content(schema = @Schema(implementation = ReviewDto.class))),
            @ApiResponse(responseCode = "403", description = "User is not authorized to delete dislike to review"),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "500", description = "Unknown error")
    })
    public ReviewDto deleteDislike(
            @PathVariable @Parameter(description = "Review ID") Long reviewId,
            @RequestHeader("X-User-Id") @Parameter(description = "User ID") Long userId) {
        log.info("User with id '{}' delete dislike to review with id '{}'", userId, reviewId);
        final Review review = reviewService.deleteLikeOrDislike(reviewId, userId, false);
        return reviewLikeService.getReviewWithLikes(review);
    }

    @GetMapping("/top")
    @Operation(summary = "Get top reviews", description = "Returns the best and worst reviews for the event with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Top reviews have been successfully received",
                    content = @Content(schema = @Schema(implementation = TopReviewsDto.class))),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "500", description = "Unknown error")
    })
    public TopReviewsDto getTopReviewsForEvent(
            @RequestParam @Parameter(description = "Review ID") Long eventId) {
        log.info("Requesting top reviews for event with id '{}'", eventId);
        final TopReviews topReviews = reviewService.getTopReviews(eventId);
        return reviewLikeService.getLikesAndMapToDto(topReviews);
    }

    @GetMapping("/stats/events/{eventId}")
    @Operation(summary = "Get event review statistics", description = "Returns review statistics for the event by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics successfully received",
                    content = @Content(schema = @Schema(implementation = EventReviewStats.class))),
            @ApiResponse(responseCode = "404", description = "Review not found"),
            @ApiResponse(responseCode = "500", description = "Unknown error")
    })
    public EventReviewStats getEventReviewsStats(
            @PathVariable @Parameter(description = "Review ID") Long eventId) {
        log.info("Requesting reviews stats for event with id '{}'", eventId);
        return statsService.getEventReviewsStats(eventId);
    }

    @GetMapping("/stats/users/{authorId}")
    @Operation(summary = "Get user review statistics", description = "Returns review statistics for the user with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics successfully received",
                    content = @Content(schema = @Schema(implementation = UserReviewStats.class))),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Unknown error")
    })
    public UserReviewStats getUserReviewsStats(
            @PathVariable @Parameter(description = "User ID") Long authorId) {
        log.info("Requesting reviews stats for user with id '{}'", authorId);
        return statsService.getUserReviewsStats(authorId);
    }
}