package ru.mssecondteam.reviewservice.like;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.mssecondteam.reviewservice.like.dto.LikeDto;
import ru.mssecondteam.reviewservice.like.service.LikeService;
import ru.mssecondteam.reviewservice.model.Review;
import ru.mssecondteam.reviewservice.service.ReviewService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class LikeServiceImplTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.7-alpine");

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private LikeService likeService;

    @Test
    void addLike() {
        Review newReview = createReview(1);
        Long userId = 2L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(newReview, otherUserId, true);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfLikes(), is(1L));
    }

    @Test
    void addDoubleLike() {
        Review newReview = createReview(1);
        Long userId = 2L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(review, otherUserId, true);
        likeService.addLikeOrDislike(review, otherUserId, true);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfLikes(), is(1L));
    }

    @Test
    void addLikeIfExistDislike() {
        Review newReview = createReview(1);
        Long userId = 2L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(review, 7L, false);
        likeService.addLikeOrDislike(review, otherUserId, false);
        likeService.addLikeOrDislike(review, otherUserId, true);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(1L);

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(1L));
        assertThat(likeDto.numbersOfLikes(), is(0L));
        assertThat(likeDto.numbersOfDislikes(), is(1L));
    }

    @Test
    void addDislike() {
        Review newReview = createReview(1);
        Long userId = 2L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(review, otherUserId, false);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(1L);

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(1L));
        assertThat(likeDto.numbersOfDislikes(), is(1L));
    }

    @Test
    void addDoubleDislike() {
        Review newReview = createReview(1);
        Long userId = 2L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(review, otherUserId, false);
        likeService.addLikeOrDislike(review, otherUserId, false);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfDislikes(), is(1L));
    }

    @Test
    void addDislikeIfExistLike() {
        Review newReview = createReview(1);
        Long userId = 2L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(review, 7L, true);
        likeService.addLikeOrDislike(review, otherUserId, true);
        likeService.addLikeOrDislike(review, otherUserId, false);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfLikes(), is(1L));
        assertThat(likeDto.numbersOfDislikes(), is(0L));
    }

    @Test
    void deleteLike() {
        Review newReview = createReview(1);
        Long userId = 2L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(newReview, otherUserId, true);
        likeService.addLikeOrDislike(newReview, 77L, true);
        likeService.deleteLikeOrDislike(newReview.getId(), otherUserId, false);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfLikes(), is(1L));
        assertThat(likeDto.numbersOfDislikes(), is(0L));
    }

    @Test
    void deleteDislike() {
        Review newReview = createReview(1);
        Long userId = 2L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(newReview, otherUserId, true);
        likeService.addLikeOrDislike(newReview, 77L, true);
        likeService.addLikeOrDislike(newReview, otherUserId, false);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfLikes(), is(1L));
        assertThat(likeDto.numbersOfDislikes(), is(0L));
    }

/*
    @Test
    void deleteLikeOrDislike() {
    }

    @Test
    void getNumberOfLikesAndDislikesByReviewId() {
    }

    @Test
    void getNumberOfLikesAndDislikesByListReviewsId() {
    }
*/

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