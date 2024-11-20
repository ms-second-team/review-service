package ru.mssecondteam.reviewservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.mssecondteam.reviewservice.dto.ReviewUpdateRequest;
import ru.mssecondteam.reviewservice.exception.NotAuthorizedException;
import ru.mssecondteam.reviewservice.exception.NotFoundException;
import ru.mssecondteam.reviewservice.model.Review;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class ReviewServiceImplTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ReviewService reviewService;

    @Test
    @DisplayName("Create review")
    void createReview_shouldReturnReviewWithNotNullAuthorIdCreationDateAndId() {
        Review newReview = createReview(1);
        Long userId = 2L;

        Review savedReview = reviewService.createReview(newReview, userId);

        assertThat(savedReview, notNullValue());
        assertThat(savedReview.getId(), greaterThan(0L));
        assertThat(savedReview.getAuthorId(), is(userId));
        assertThat(savedReview.getCreatedDateTime(), notNullValue());
        assertThat(savedReview.getUpdatedDateTime(), notNullValue());
        assertThat(savedReview.getContent(), is(newReview.getContent()));
        assertThat(savedReview.getTitle(), is(newReview.getTitle()));
        assertThat(savedReview.getUsername(), is(newReview.getUsername()));
    }

    @Test
    @DisplayName("Update review")
    void updateReview_whenUpdateByAuthor_shouldUpdateAllFields() {
        Review review = createReview(1);
        Long userId = 2L;
        Review savedReview = reviewService.createReview(review, userId);

        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                .title("new title")
                .content("new content")
                .mark(1)
                .build();

        Review updatedReview = reviewService.updateReview(savedReview.getId(), updateRequest, userId);

        assertThat(updatedReview, notNullValue());
        assertThat(updatedReview.getId(), is(savedReview.getId()));
        assertThat(updatedReview.getMark(), is(updateRequest.mark()));
        assertThat(updatedReview.getContent(), is(updateRequest.content()));
        assertThat(updatedReview.getTitle(), is(updateRequest.title()));
        assertThat(updatedReview.getCreatedDateTime(), is(savedReview.getCreatedDateTime()));
        assertThat(updatedReview.getUpdatedDateTime(), greaterThan(updatedReview.getCreatedDateTime()));
    }

    @Test
    @DisplayName("Update title review")
    void updateReview_whenUpdateByAuthorOnlyTitle_shouldUpdateOnlyTitle() {
        Review review = createReview(1);
        Long userId = 2L;
        Review savedReview = reviewService.createReview(review, userId);

        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                .title("new title")
                .build();

        Review updatedReview = reviewService.updateReview(savedReview.getId(), updateRequest, userId);

        assertThat(updatedReview, notNullValue());
        assertThat(updatedReview.getId(), is(savedReview.getId()));
        assertThat(updatedReview.getMark(), is(savedReview.getMark()));
        assertThat(updatedReview.getContent(), is(savedReview.getContent()));
        assertThat(updatedReview.getTitle(), is(updateRequest.title()));
        assertThat(updatedReview.getCreatedDateTime(), is(savedReview.getCreatedDateTime()));
        assertThat(updatedReview.getUpdatedDateTime(), greaterThan(updatedReview.getCreatedDateTime()));
    }

    @Test
    @DisplayName("Update review, unauthorized")
    void updateReview_whenUnauthorizedUpdate_shouldThrowNotAuthorizedException() {
        Review review = createReview(1);
        Long userId = 2L;
        Review savedReview = reviewService.createReview(review, userId);

        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                .title("new title")
                .build();
        Long unauthorizedId = 999L;


        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> reviewService.updateReview(savedReview.getId(), updateRequest, unauthorizedId));

        assertThat(ex.getMessage(), is("User with id '" + unauthorizedId + "' is not authorized to modify review " +
                "with id '" + savedReview.getId() + "'"));
    }

    @Test
    @DisplayName("Update review, not found")
    void updateReview_whenReviewNotFound_shouldThrowNotFoundException() {
        Long userId = 2L;
        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                .title("new title")
                .build();
        Long unknownId = 999L;


        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.updateReview(unknownId, updateRequest, userId));

        assertThat(ex.getMessage(), is("Review with id '" + unknownId + "' was not found"));
    }

    @Test
    @DisplayName("Find review by id")
    void findReviewById_whenReviewExists_shouldReturnReview() {
        Review newReview = createReview(1);
        Long userId = 2L;
        Long unknownId = 999L;

        Review savedReview = reviewService.createReview(newReview, userId);

        Review foundReview = reviewService.findReviewById(savedReview.getId(), userId);

        assertThat(foundReview, notNullValue());
        assertThat(foundReview.getId(), is(savedReview.getId()));
        assertThat(foundReview.getAuthorId(), is(savedReview.getAuthorId()));
        assertThat(foundReview.getCreatedDateTime(), is(savedReview.getCreatedDateTime()));
        assertThat(foundReview.getUpdatedDateTime(), is(savedReview.getUpdatedDateTime()));
        assertThat(foundReview.getContent(), is(savedReview.getContent()));
        assertThat(foundReview.getTitle(), is(savedReview.getTitle()));
        assertThat(foundReview.getUsername(), is(savedReview.getUsername()));
    }

    @Test
    @DisplayName("Find review by id, review not found")
    void findReviewById_whenReviewDoesNotExist_shouldThrowNotFoundException() {
        Long userId = 2L;
        Long unknownId = 999L;

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.findReviewById(unknownId, userId));

        assertThat(ex.getMessage(), is("Review with id '" + unknownId + "' was not found"));
    }

    @Test
    @DisplayName("Find reviews by event id, no reviews exist")
    void findReviewsByEventId_whenNoReviewsExists_shouldReturnEmptyList() {
        Long userId = 2L;
        Long eventId = 5L;
        Integer page = 0;
        Integer size = 13;

        List<Review> reviews = reviewService.findReviewsByEventId(eventId, page, size, userId);

        assertThat(reviews, notNullValue());
        assertThat(reviews, emptyIterable());
    }

    @Test
    @DisplayName("Find reviews by event id, no reviews exist")
    void findReviewsByEventId_whenTwoReviewsOfEvent_shouldReturnTwoReviews() {
        Long userId = 2L;
        Integer page = 0;
        Integer size = 13;

        Review review1 = createReview(1);
        Review review2 = createReview(2);
        Review review3 = createReview(2);
        review3.setEventId(123L);

        Review savedReview1 = reviewService.createReview(review1, userId);
        Review savedReview2 = reviewService.createReview(review2, userId);
        reviewService.createReview(review3, userId);

        List<Review> reviews = reviewService.findReviewsByEventId(review1.getEventId(), page, size, userId);

        assertThat(reviews, notNullValue());
        assertThat(reviews.size(), is(2));
        assertThat(reviews.get(0).getId(), is(savedReview1.getId()));
        assertThat(reviews.get(1).getId(), is(savedReview2.getId()));
    }

    @Test
    @DisplayName("Delete review by author")
    void deleteReviewById_whenAuthorDeletes_shouldDeleteReview() {
        Review newReview = createReview(1);
        Long userId = 2L;
        Review savedReview = reviewService.createReview(newReview, userId);

        reviewService.deleteReviewById(savedReview.getId(), userId);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.findReviewById(savedReview.getId(), userId));

        assertThat(ex.getMessage(), is("Review with id '" + savedReview.getId() + "' was not found"));
    }

    @Test
    @DisplayName("Delete review, unauthorized")
    void deleteReviewById_whenUnauthorized_shouldThrowNotAuthorizedException() {
        Review newReview = createReview(1);
        Long userId = 2L;
        Review savedReview = reviewService.createReview(newReview, userId);
        Long unauthorizedId = 999L;

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> reviewService.deleteReviewById(savedReview.getId(), unauthorizedId));

        assertThat(ex.getMessage(), is("User with id '" + unauthorizedId + "' is not authorized to modify review " +
                "with id '" + savedReview.getId() + "'"));
    }

    @Test
    @DisplayName("Delete review, review not found")
    void deleteReviewById_whenReviewNotFound_shouldThrowNotFoundException() {
        Long userId = 2L;
        Long unknownId = 999L;

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.deleteReviewById(unknownId, userId));

        assertThat(ex.getMessage(), is("Review with id '" + unknownId + "' was not found"));
    }

    private Review createReview(int id) {
        return Review.builder()
                .title("review title " + id)
                .content("review content " + id)
                .username("username")
                .eventId(4L)
                .mark(5)
                .build();
    }
}