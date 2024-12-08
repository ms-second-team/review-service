package ru.mssecondteam.reviewservice.service.stats;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.mssecondteam.reviewservice.dto.EventReviewStats;
import ru.mssecondteam.reviewservice.dto.UserReviewStats;
import ru.mssecondteam.reviewservice.model.Review;
import ru.mssecondteam.reviewservice.service.ReviewService;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@Transactional
class StatsServiceImplIT {

    @Autowired
    private StatsService statsService;

    @Autowired
    private ReviewService reviewService;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static WireMockServer registrationServer;
    static WireMockServer eventServer;

    @BeforeAll
    static void setupWireMock() {
        registrationServer = new WireMockServer(WireMockConfiguration.options().port(8090));
        eventServer = new WireMockServer(WireMockConfiguration.options().port(8070));
        registrationServer.start();
        eventServer.start();
        // Setup WireMock for RegistrationClient
        configureFor("localhost", 8090);
        stubFor(get(urlPathMatching("/registrations"))
                .withQueryParam("page", matching("\\d+"))
                .withQueryParam("size", matching("\\d+"))
                .withQueryParam("eventId", matching("\\d+"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody("[{\"id\": 1, \"eventId\": 4, \"username\": \"username\", \"status\": \"APPROVED\"}]")));

        // Setup WireMock for EventClient
        configureFor("localhost", 8070);
        stubFor(get(urlPathMatching("/events/\\d+"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody("{\"id\": 4, \"ownerId\": 2, \"endDateTime\": \"2024-12-31 23:59:59\"}")));

        stubFor(get(urlPathMatching("/events/teams/\\d+"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody("[{\"userId\": 1}, {\"userId\": 13}]")));
    }

    @AfterAll
    static void tearDownWireMock() {
        if (registrationServer != null) registrationServer.stop();
        if (eventServer != null) eventServer.stop();
    }

    @Test
    @DisplayName("Get event stats, no reviews")
    void getEventReviewsStats_whenNoReviewsForEvent_shouldReturnNull() {
        Long eventId = 1L;

        EventReviewStats reviewsStats = statsService.getEventReviewsStats(eventId);

        assertThat(reviewsStats, nullValue());
    }

    @Test
    @DisplayName("Get event stats, one positive review")
    void getEventReviewsStats_whenOnlyOnePositiveReview_shouldReturnStats() {
        Long userId = 13L;
        Review review = createReview(6);
        reviewService.createReview(review, userId);

        EventReviewStats reviewsStats = statsService.getEventReviewsStats(review.getEventId());

        assertThat(reviewsStats, notNullValue());
        assertThat(reviewsStats.eventId(), is(review.getEventId()));
        assertThat(reviewsStats.totalNumberOfReviews(), is(Long.valueOf(1)));
        assertThat(reviewsStats.avgMark(), is(Float.valueOf(6)));
        assertThat(reviewsStats.positiveReviewsPercentage(), is(Float.valueOf(100)));
        assertThat(reviewsStats.negativeReviewsPercentage(), is(Float.valueOf(0)));
    }

    @Test
    @DisplayName("Get event stats, one negative review")
    void getEventReviewsStats_whenOnlyOneNegativeReview_shouldReturnStats() {
        Long userId = 13L;
        Review review = createReview(5);
        reviewService.createReview(review, userId);

        EventReviewStats reviewsStats = statsService.getEventReviewsStats(review.getEventId());

        assertThat(reviewsStats, notNullValue());
        assertThat(reviewsStats.eventId(), is(review.getEventId()));
        assertThat(reviewsStats.totalNumberOfReviews(), is(Long.valueOf(1)));
        assertThat(reviewsStats.avgMark(), is(Float.valueOf(5)));
        assertThat(reviewsStats.positiveReviewsPercentage(), is(Float.valueOf(0)));
        assertThat(reviewsStats.negativeReviewsPercentage(), is(Float.valueOf(100)));
    }

    @Test
    @DisplayName("Get event stats, one negative and one positive review")
    void getEventReviewsStats_whenOneNegativeAndOnePositiveReview_shouldReturnStats() {
        Long userId = 13L;
        Review review1 = createReview(5);
        reviewService.createReview(review1, userId);
        Review review2 = createReview(8);
        reviewService.createReview(review2, userId);

        EventReviewStats reviewsStats = statsService.getEventReviewsStats(review1.getEventId());

        assertThat(reviewsStats, notNullValue());
        assertThat(reviewsStats.eventId(), is(review1.getEventId()));
        assertThat(reviewsStats.totalNumberOfReviews(), is(Long.valueOf(2)));
        assertThat(reviewsStats.avgMark(), is(6.5F));
        assertThat(reviewsStats.positiveReviewsPercentage(), is(Float.valueOf(50)));
        assertThat(reviewsStats.negativeReviewsPercentage(), is(Float.valueOf(50)));
    }

    @Test
    @DisplayName("Get event stats, one review with negative rating should not count in avg rating")
    void getEventReviewsStats_whenMultipleReview_shouldReturnStats() {
        Long userId = 13L;
        Review review1 = createReview(5);
        reviewService.createReview(review1, userId);
        Review review2 = createReview(8);
        reviewService.createReview(review2, userId);
        reviewService.addLikeOrDislike(review1.getId(), userId + 1, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 2, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 3, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 4, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 5, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 6, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 7, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 8, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 9, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 10, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 11, true);

        EventReviewStats reviewsStats = statsService.getEventReviewsStats(review1.getEventId());

        assertThat(reviewsStats, notNullValue());
        assertThat(reviewsStats.eventId(), is(review1.getEventId()));
        assertThat(reviewsStats.totalNumberOfReviews(), is(Long.valueOf(2)));
        assertThat(reviewsStats.avgMark(), is(8F));
        assertThat(reviewsStats.positiveReviewsPercentage(), is(Float.valueOf(50)));
        assertThat(reviewsStats.negativeReviewsPercentage(), is(Float.valueOf(50)));
    }

    @Test
    @DisplayName("Get event stats, one review with negative rating should not count in avg rating")
    void getEventReviewsStats_whenOneReviewWithNegativeRating_shouldReturnStats() {
        Long userId = 13L;
        Review review1 = createReview(5);
        reviewService.createReview(review1, userId);
        reviewService.addLikeOrDislike(review1.getId(), userId + 1, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 2, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 3, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 4, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 5, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 6, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 7, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 8, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 9, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 10, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 11, true);

        EventReviewStats reviewsStats = statsService.getEventReviewsStats(review1.getEventId());

        assertThat(reviewsStats, notNullValue());
        assertThat(reviewsStats.eventId(), is(review1.getEventId()));
        assertThat(reviewsStats.totalNumberOfReviews(), is(Long.valueOf(1)));
        assertThat(reviewsStats.avgMark(), is(0F));
        assertThat(reviewsStats.positiveReviewsPercentage(), is(Float.valueOf(0)));
        assertThat(reviewsStats.negativeReviewsPercentage(), is(Float.valueOf(100)));
    }

    @Test
    @DisplayName("Get user stats, no reviews")
    void getUserReviewsStats_whenNoReviewsForEvent_shouldReturnNull() {
        Long userId = 1L;

        UserReviewStats reviewsStats = statsService.getUserReviewsStats(userId);

        assertThat(reviewsStats, nullValue());
    }

    @Test
    @DisplayName("Get user stats, one positive review")
    void getUserReviewsStats_whenOnlyOnePositiveReview_shouldReturnStats() {
        Long userId = 13L;
        Review review = createReview(6);
        reviewService.createReview(review, userId);

        UserReviewStats reviewsStats = statsService.getUserReviewsStats(userId);

        assertThat(reviewsStats, notNullValue());
        assertThat(reviewsStats.userId(), is(userId));
        assertThat(reviewsStats.totalNumberOfReviews(), is(Long.valueOf(1)));
        assertThat(reviewsStats.avgMark(), is(Float.valueOf(6)));
        assertThat(reviewsStats.positiveReviewsPercentage(), is(Float.valueOf(100)));
        assertThat(reviewsStats.negativeReviewsPercentage(), is(Float.valueOf(0)));
    }

    @Test
    @DisplayName("Get user stats, one negative review")
    void getUserReviewsStats_whenOnlyOneNegativeReview_shouldReturnStats() {
        Long userId = 13L;
        Review review = createReview(2);
        reviewService.createReview(review, userId);

        UserReviewStats reviewsStats = statsService.getUserReviewsStats(userId);

        assertThat(reviewsStats, notNullValue());
        assertThat(reviewsStats.userId(), is(userId));
        assertThat(reviewsStats.totalNumberOfReviews(), is(Long.valueOf(1)));
        assertThat(reviewsStats.avgMark(), is(Float.valueOf(2)));
        assertThat(reviewsStats.positiveReviewsPercentage(), is(Float.valueOf(0)));
        assertThat(reviewsStats.negativeReviewsPercentage(), is(Float.valueOf(100)));
    }

    @Test
    @DisplayName("Get user stats, one negative and one positive review")
    void getUserReviewsStats_whenOneNegativeAndOnePositiveReview_shouldReturnStats() {
        Long userId = 13L;
        Review review1 = createReview(5);
        reviewService.createReview(review1, userId);
        Review review2 = createReview(8);
        reviewService.createReview(review2, userId);

        UserReviewStats reviewsStats = statsService.getUserReviewsStats(userId);

        assertThat(reviewsStats, notNullValue());
        assertThat(reviewsStats.userId(), is(userId));
        assertThat(reviewsStats.totalNumberOfReviews(), is(Long.valueOf(2)));
        assertThat(reviewsStats.avgMark(), is(6.5F));
        assertThat(reviewsStats.positiveReviewsPercentage(), is(Float.valueOf(50)));
        assertThat(reviewsStats.negativeReviewsPercentage(), is(Float.valueOf(50)));
    }

    @Test
    @DisplayName("Get user stats, one review with negative rating should not count in avg rating")
    void getUserReviewsStats_whenMultipleReview_shouldReturnStats() {
        Long userId = 13L;
        Review review1 = createReview(5);
        reviewService.createReview(review1, userId);
        Review review2 = createReview(8);
        reviewService.createReview(review2, userId);
        reviewService.addLikeOrDislike(review1.getId(), userId + 1, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 2, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 3, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 4, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 5, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 6, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 7, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 8, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 9, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 10, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 11, true);

        UserReviewStats reviewsStats = statsService.getUserReviewsStats(userId);

        assertThat(reviewsStats, notNullValue());
        assertThat(reviewsStats.userId(), is(userId));
        assertThat(reviewsStats.totalNumberOfReviews(), is(Long.valueOf(2)));
        assertThat(reviewsStats.avgMark(), is(8F));
        assertThat(reviewsStats.positiveReviewsPercentage(), is(Float.valueOf(50)));
        assertThat(reviewsStats.negativeReviewsPercentage(), is(Float.valueOf(50)));
    }

    @Test
    @DisplayName("Get user stats, one review with negative rating should not count in avg rating")
    void getUserReviewsStats_whenOneReviewWithNegativeRating_shouldReturnStats() {
        Long userId = 13L;
        Review review1 = createReview(5);
        reviewService.createReview(review1, userId);
        reviewService.addLikeOrDislike(review1.getId(), userId + 1, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 2, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 3, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 4, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 5, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 6, false);
        reviewService.addLikeOrDislike(review1.getId(), userId + 7, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 8, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 9, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 10, true);
        reviewService.addLikeOrDislike(review1.getId(), userId + 11, true);

        UserReviewStats reviewsStats = statsService.getUserReviewsStats(userId);

        assertThat(reviewsStats, notNullValue());
        assertThat(reviewsStats.userId(), is(userId));
        assertThat(reviewsStats.totalNumberOfReviews(), is(Long.valueOf(1)));
        assertThat(reviewsStats.avgMark(), is(0F));
        assertThat(reviewsStats.positiveReviewsPercentage(), is(Float.valueOf(0)));
        assertThat(reviewsStats.negativeReviewsPercentage(), is(Float.valueOf(100)));
    }

    private Review createReview(int id) {
        return Review.builder()
                .title("review title " + id)
                .content("review content " + id)
                .username("username")
                .eventId(4L)
                .mark(id)
                .build();
    }
}