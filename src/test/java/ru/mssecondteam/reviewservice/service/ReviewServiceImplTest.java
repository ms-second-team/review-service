package ru.mssecondteam.reviewservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
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
import ru.mssecondteam.reviewservice.dto.ReviewUpdateRequest;
import ru.mssecondteam.reviewservice.dto.event.EventDto;
import ru.mssecondteam.reviewservice.dto.event.TeamMemberDto;
import ru.mssecondteam.reviewservice.dto.event.TeamMemberRole;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationResponseDto;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationStatus;
import ru.mssecondteam.reviewservice.exception.NotAuthorizedException;
import ru.mssecondteam.reviewservice.exception.NotFoundException;
import ru.mssecondteam.reviewservice.model.Review;
import ru.mssecondteam.reviewservice.model.TopReviews;

import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@Transactional
class ReviewServiceImplTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ReviewService reviewService;

    static WireMockServer registrationServer;
    static WireMockServer eventServer;

    @BeforeAll
    static void setupWireMock() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        registrationServer = new WireMockServer(wireMockConfig().dynamicPort());
        eventServer = new WireMockServer(wireMockConfig().dynamicPort());

        registrationServer.start();
        eventServer.start();

        int registrationPort = registrationServer.port();
        int eventPort = eventServer.port();

        System.setProperty("app.registration-service.url", "http://localhost:" + registrationPort);
        System.setProperty("app.event-service.url", "http://localhost:" + eventPort);

        // Setup WireMock for RegistrationClient
        List<RegistrationResponseDto> searchRegistrationsResponse = List.of(
                new RegistrationResponseDto(
                        "username",
                        "user@name.mail",
                        "7777777777",
                        4L,
                        RegistrationStatus.APPROVED)
        );

        String searchRegistrationsResponseBody = objectMapper.writeValueAsString(searchRegistrationsResponse);

        configureFor("localhost", registrationPort);
        stubFor(get(urlPathMatching("/registrations/search"))
                .withQueryParam("statuses", equalTo("APPROVED"))
                .withQueryParam("eventId", matching("\\d+"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(searchRegistrationsResponseBody)));

        // Setup WireMock for EventClient
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
                new TeamMemberDto(4L, 123L, TeamMemberRole.MEMBER));

        String getEventByIdResponseBody = objectMapper.writeValueAsString(getEventByIdResponse);
        String getTeamsByEventIdResponseBody = objectMapper.writeValueAsString(getTeamsByEventIdResponse);

        configureFor("localhost", eventPort);
        stubFor(get(urlPathMatching("/events/\\d+"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(getEventByIdResponseBody)));

        stubFor(get(urlPathMatching("/events/teams/\\d+"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(getTeamsByEventIdResponseBody)));
    }

    @AfterAll
    static void tearDownWireMock() {
        if (registrationServer != null) registrationServer.stop();
        if (eventServer != null) eventServer.stop();
    }

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
        assertThat(updatedReview.getUpdatedDateTime(), greaterThanOrEqualTo(updatedReview.getCreatedDateTime()));
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
        assertThat(updatedReview.getUpdatedDateTime(), greaterThanOrEqualTo(updatedReview.getCreatedDateTime()));
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

    @Test
    @DisplayName("Add like")
    void addLike_whenReviewExists_shouldReturnReview() {
        Review newReview = createReview(6);
        Long userId = 2L;
        Long otherId = 999L;

        Review savedReview = reviewService.createReview(newReview, userId);
        reviewService.addLikeOrDislike(newReview.getId(), otherId, true);

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
    @DisplayName("Add dislike")
    void addDislike_whenReviewExists_shouldReturnReview() {
        Review newReview = createReview(7);
        Long userId = 2L;
        Long otherId = 999L;

        Review savedReview = reviewService.createReview(newReview, userId);
        reviewService.addLikeOrDislike(newReview.getId(), otherId, false);

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
    @DisplayName("Add like, review not found")
    void addLike_whenReviewDoesNotExist_shouldThrowNotFoundException() {
        Long userId = 2L;
        Long unknownId = 999L;

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.addLikeOrDislike(unknownId, userId, true));

        assertThat(ex.getMessage(), is("Review with id '" + unknownId + "' was not found"));
    }

    @Test
    @DisplayName("Add dislike, review not found")
    void addDislike_whenReviewDoesNotExist_shouldThrowNotFoundException() {
        Long userId = 2L;
        Long unknownId = 999L;

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.addLikeOrDislike(unknownId, userId, false));

        assertThat(ex.getMessage(), is("Review with id '" + unknownId + "' was not found"));
    }

    @Test
    @DisplayName("Add like, unauthorized")
    void ddLike_whenUnauthorized_shouldThrowNotAuthorizedException() {
        Review newReview = createReview(8);
        Long userId = 2L;
        Review savedReview = reviewService.createReview(newReview, userId);

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> reviewService.addLikeOrDislike(savedReview.getId(), userId, true));

        assertThat(ex.getMessage(), is("User with id '" + userId + "' is not authorized to add like/dislike review " +
                "with id '" + savedReview.getId() + "'"));
    }

    @Test
    @DisplayName("Add dislike, unauthorized")
    void addDislike_whenUnauthorized_shouldThrowNotAuthorizedException() {
        Review newReview = createReview(9);
        Long userId = 2L;
        Review savedReview = reviewService.createReview(newReview, userId);

        NotAuthorizedException ex = assertThrows(NotAuthorizedException.class,
                () -> reviewService.addLikeOrDislike(savedReview.getId(), userId, false));

        assertThat(ex.getMessage(), is("User with id '" + userId + "' is not authorized to add like/dislike review " +
                "with id '" + savedReview.getId() + "'"));
    }

    @Test
    @DisplayName("Delete like")
    void deleteLike_whenReviewExists_shouldReturnReview() {
        Review newReview = createReview(10);
        Long userId = 2L;
        Long otherId = 999L;

        Review savedReview = reviewService.createReview(newReview, userId);
        reviewService.addLikeOrDislike(newReview.getId(), otherId, true);
        reviewService.deleteLikeOrDislike(newReview.getId(), otherId, true);

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
    @DisplayName("Delete dislike")
    void deleteDislike_whenReviewExists_shouldReturnReview() {
        Review newReview = createReview(11);
        Long userId = 2L;
        Long otherId = 999L;

        Review savedReview = reviewService.createReview(newReview, userId);
        reviewService.addLikeOrDislike(newReview.getId(), otherId, false);
        reviewService.deleteLikeOrDislike(newReview.getId(), otherId, false);

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
    @DisplayName("Delete like, review not found")
    void deleteLike_whenReviewDoesNotExist_shouldThrowNotFoundException() {
        Long userId = 2L;
        Long unknownId = 999L;

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.deleteLikeOrDislike(unknownId, userId, true));

        assertThat(ex.getMessage(), is("Review with id '" + unknownId + "' was not found"));
    }

    @Test
    @DisplayName("Delete dislike, review not found")
    void deleteDislike_whenReviewDoesNotExist_shouldThrowNotFoundException() {
        Long userId = 2L;
        Long unknownId = 999L;

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.deleteLikeOrDislike(unknownId, userId, false));

        assertThat(ex.getMessage(), is("Review with id '" + unknownId + "' was not found"));
    }

    @Test
    @DisplayName("Get top reviews, no reviews exists")
    void getTopReviews_whenNoReviews_shouldReturnEmptyLists() {
        Long eventId = 11L;

        TopReviews topReviews = reviewService.getTopReviews(eventId);

        assertThat(topReviews, notNullValue());
        assertThat(topReviews.bestReviews(), emptyIterable());
        assertThat(topReviews.worstReviews(), emptyIterable());
    }

    @Test
    @DisplayName("Get top reviews")
    void getTopReviews_whenReviewsExists_shouldReturnLists() {
        Long userId = 123L;

        Review review1 = createReview(11);
        Review savedReview1 = reviewService.createReview(review1, userId);
        reviewService.addLikeOrDislike(savedReview1.getId(), userId + 2, true);
        reviewService.addLikeOrDislike(savedReview1.getId(), userId + 1, true);

        Review review2 = createReview(12);
        Review savedReview2 = reviewService.createReview(review2, userId);
        reviewService.addLikeOrDislike(savedReview2.getId(), userId + 1, false);

        Review review3 = createReview(13);
        Review savedReview3 = reviewService.createReview(review3, userId);
        reviewService.addLikeOrDislike(savedReview3.getId(), userId + 2, true);


        Review review4 = createReview(13);
        Review savedReview4 = reviewService.createReview(review4, userId);
        reviewService.addLikeOrDislike(savedReview4.getId(), userId + 2, false);
        reviewService.addLikeOrDislike(savedReview4.getId(), userId + 1, false);

        TopReviews topReviews = reviewService.getTopReviews(review1.getEventId());

        assertThat(topReviews, notNullValue());
        assertThat(topReviews.bestReviews().size(), is(3));
        assertThat(topReviews.bestReviews().get(0).getId(), is(savedReview1.getId()));
        assertThat(topReviews.bestReviews().get(1).getId(), is(savedReview3.getId()));
        assertThat(topReviews.bestReviews().get(2).getId(), is(savedReview2.getId()));
        assertThat(topReviews.worstReviews().size(), is(3));
        assertThat(topReviews.worstReviews().get(0).getId(), is(savedReview4.getId()));
        assertThat(topReviews.worstReviews().get(1).getId(), is(savedReview2.getId()));
        assertThat(topReviews.worstReviews().get(2).getId(), is(savedReview3.getId()));
    }

    @Test
    @DisplayName("Get top reviews, when less than minimum")
    void getTopReviews_whenReviewsExistsLessThanMinimum_shouldLists() {
        Long userId = 123L;

        Review review1 = createReview(11);
        Review savedReview1 = reviewService.createReview(review1, userId);
        reviewService.addLikeOrDislike(savedReview1.getId(), userId + 2, true);
        reviewService.addLikeOrDislike(savedReview1.getId(), userId + 1, true);

        Review review2 = createReview(12);
        Review savedReview2 = reviewService.createReview(review2, userId);
        reviewService.addLikeOrDislike(savedReview2.getId(), userId + 1, false);


        TopReviews topReviews = reviewService.getTopReviews(review1.getEventId());

        assertThat(topReviews, notNullValue());
        assertThat(topReviews.bestReviews().size(), is(2));
        assertThat(topReviews.bestReviews().get(0).getId(), is(savedReview1.getId()));
        assertThat(topReviews.bestReviews().get(1).getId(), is(savedReview2.getId()));
        assertThat(topReviews.worstReviews().size(), is(2));
        assertThat(topReviews.worstReviews().get(0).getId(), is(savedReview2.getId()));
        assertThat(topReviews.worstReviews().get(1).getId(), is(savedReview1.getId()));
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