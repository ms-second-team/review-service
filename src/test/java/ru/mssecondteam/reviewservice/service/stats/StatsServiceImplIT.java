package ru.mssecondteam.reviewservice.service.stats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.mssecondteam.reviewservice.dto.EventReviewStats;
import ru.mssecondteam.reviewservice.dto.UserReviewStats;
import ru.mssecondteam.reviewservice.dto.event.EventDto;
import ru.mssecondteam.reviewservice.dto.event.TeamMemberDto;
import ru.mssecondteam.reviewservice.dto.event.TeamMemberRole;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationResponseDto;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationStatus;
import ru.mssecondteam.reviewservice.model.Review;
import ru.mssecondteam.reviewservice.service.review.ReviewService;

import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
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
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "app.event-service.url=http://localhost:${wiremock.server.port}",
        "app.registration-service.url=http://localhost:${wiremock.server.port}"
})
class StatsServiceImplIT {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private StatsService statsService;

    @Autowired
    private ReviewService reviewService;

    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void beforeAll() {
        POSTGRES.start();
    }

    @AfterAll
    static void afterAll() {
        POSTGRES.stop();
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Get event stats, no reviews")
    void getEventReviewsStats_whenNoReviewsForEvent_shouldReturnNull() {
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

        Long eventId = 1L;

        EventReviewStats reviewsStats = statsService.getEventReviewsStats(eventId);

        assertThat(reviewsStats, nullValue());
    }

    @Test
    @DisplayName("Get event stats, one positive review")
    void getEventReviewsStats_whenOnlyOnePositiveReview_shouldReturnStats() {
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

        Long userId = 1L;

        UserReviewStats reviewsStats = statsService.getUserReviewsStats(userId);

        assertThat(reviewsStats, nullValue());
    }

    @Test
    @DisplayName("Get user stats, one positive review")
    void getUserReviewsStats_whenOnlyOnePositiveReview_shouldReturnStats() {
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

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

    @SneakyThrows
    private void setupWireMockForRegistrationClientPositiveAnswer() {
        List<RegistrationResponseDto> searchRegistrationsResponse = List.of(
                new RegistrationResponseDto(
                        "username",
                        "user@name.mail",
                        "7777777777",
                        4L,
                        RegistrationStatus.APPROVED)
        );
        stubFor(get(urlPathMatching("/registrations/search"))
                .withQueryParam("statuses", equalTo("APPROVED"))
                .withQueryParam("eventId", matching("\\d+"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(searchRegistrationsResponse))));
    }

    @SneakyThrows
    private void setupWireMockForEventClientPositiveAnswer() {
        EventDto getEventByIdResponse = new EventDto(
                4L,
                "username",
                "description",
                LocalDateTime.now().minusDays(20),
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(5),
                "stadium",
                2L);

        List<TeamMemberDto> getTeamsByEventIdResponse = List.of(
                new TeamMemberDto(4L, 13L, TeamMemberRole.MEMBER));

        stubFor(get(urlPathMatching("/events/\\d+"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(getEventByIdResponse))));

        stubFor(get(urlPathMatching("/events/teams/\\d+"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(getTeamsByEventIdResponse))));
    }
}