package ru.mssecondteam.reviewservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.mssecondteam.reviewservice.dto.LikeDto;
import ru.mssecondteam.reviewservice.dto.event.EventDto;
import ru.mssecondteam.reviewservice.dto.event.TeamMemberDto;
import ru.mssecondteam.reviewservice.dto.event.TeamMemberRole;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationResponseDto;
import ru.mssecondteam.reviewservice.dto.registration.RegistrationStatus;
import ru.mssecondteam.reviewservice.exception.NotFoundException;
import ru.mssecondteam.reviewservice.model.Review;
import ru.mssecondteam.reviewservice.service.like.LikeService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@WireMockTest
class LikeServiceImplTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private LikeService likeService;

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
                new TeamMemberDto(4L, 1L, TeamMemberRole.MEMBER));

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
    void addLike() {
        Review newReview = createReview(1);
        Long userId = 1L;
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
        Long userId = 1L;
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
        Long userId = 1L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(review, 7L, false);
        likeService.addLikeOrDislike(review, otherUserId, false);
        likeService.addLikeOrDislike(review, otherUserId, true);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfLikes(), is(0L));
        assertThat(likeDto.numbersOfDislikes(), is(1L));
    }

    @Test
    void addDislike() {
        Review newReview = createReview(1);
        Long userId = 1L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(review, otherUserId, false);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfDislikes(), is(1L));
    }

    @Test
    void addDoubleDislike() {
        Review newReview = createReview(1);
        Long userId = 1L;
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
        Long userId = 1L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);

        likeService.addLikeOrDislike(review, otherUserId, true);
        likeService.addLikeOrDislike(review, otherUserId, false);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertNull(likeDto);
    }

    @Test
    void deleteLike() {
        Review newReview = createReview(1);
        Long userId = 1L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(newReview, otherUserId, true);
        likeService.deleteLikeOrDislike(newReview.getId(), otherUserId, true);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertNull(likeDto);
    }

    @Test
    void deleteDislike() {
        Review newReview = createReview(1);
        Long userId = 1L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(newReview, otherUserId, true);
        likeService.deleteLikeOrDislike(newReview.getId(), otherUserId, true);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertNull(likeDto);
    }

    @Test
    void deleteNotExistLike() {
        Review newReview = createReview(1);
        Long userId = 1L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.deleteLikeOrDislike(review.getId(), otherUserId, true));

        assertThat(ex.getMessage(), is("Like with userId '" + otherUserId + "' and reviewId '" + review.getId() + "' was not found"));
    }

    @Test
    void deleteNotExistDislike() {
        Review newReview = createReview(1);
        Long userId = 1L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> reviewService.deleteLikeOrDislike(review.getId(), otherUserId, false));

        assertThat(ex.getMessage(), is("Like with userId '" + otherUserId + "' and reviewId '" + review.getId() + "' was not found"));
    }

    @Test
    void deleteLikeIfExistDislike() {
        Review newReview = createReview(1);
        Long userId = 1L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(newReview, otherUserId, false);
        likeService.deleteLikeOrDislike(newReview.getId(), otherUserId, true);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfLikes(), is(0L));
        assertThat(likeDto.numbersOfDislikes(), is(1L));
    }

    @Test
    void deleteDislikeIfExistLike() {
        Review newReview = createReview(1);
        Long userId = 1L;
        Long otherUserId = 2L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(newReview, otherUserId, true);
        likeService.deleteLikeOrDislike(newReview.getId(), otherUserId, false);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfLikes(), is(1L));
        assertThat(likeDto.numbersOfDislikes(), is(0L));
    }

    @Test
    void getNumberOfLikesAndDislikesByReviewId() {
        Review newReview = createReview(1);
        Long userId = 1L;
        Long otherUserIdNumberOne = 2L;
        Long otherUserIdNumberTwo = 3L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(review, otherUserIdNumberOne, true);
        likeService.addLikeOrDislike(review, otherUserIdNumberTwo, false);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfLikes(), is(1L));
        assertThat(likeDto.numbersOfDislikes(), is(1L));
    }

    @Test
    void getNumberOfLikesAndDislikesByReviewIdIfLikesDoNotExist() {
        Review newReview = createReview(1);
        Long userId = 1L;
        Long otherUserIdNumberOne = 2L;
        Long otherUserIdNumberTwo = 3L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(review, otherUserIdNumberOne, false);
        likeService.addLikeOrDislike(review, otherUserIdNumberTwo, false);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfLikes(), is(0L));
        assertThat(likeDto.numbersOfDislikes(), is(2L));
    }

    @Test
    void getNumberOfLikesAndDislikesByReviewIdIfDislikesDoNotExist() {
        Review newReview = createReview(1);
        Long userId = 1L;
        Long otherUserIdNumberOne = 2L;
        Long otherUserIdNumberTwo = 3L;

        Review review = reviewService.createReview(newReview, userId);
        likeService.addLikeOrDislike(review, otherUserIdNumberOne, true);
        likeService.addLikeOrDislike(review, otherUserIdNumberTwo, true);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertThat(likeDto, notNullValue());
        assertThat(likeDto.reviewId(), is(review.getId()));
        assertThat(likeDto.numbersOfLikes(), is(2L));
        assertThat(likeDto.numbersOfDislikes(), is(0L));
    }

    @Test
    void getNumberOfLikesAndDislikesByReviewIdIfLikesAndDislikesDoNotExist() {
        Review newReview = createReview(1);
        Long userId = 1L;

        Review review = reviewService.createReview(newReview, userId);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertNull(likeDto);
    }

    @Test
    void getNumberOfLikesAndDislikesByListReviewsId() {
        Review newReviewNumberOne = createReview(1);
        Review newReviewNumberTwo = createReview(2);
        Long userId = 1L;
        Long otherUserIdNumberOne = 2L;
        Long otherUserIdNumberTwo = 3L;

        Review reviewNumberOne = reviewService.createReview(newReviewNumberOne, userId);
        Review reviewNumberTwo = reviewService.createReview(newReviewNumberTwo, userId);
        likeService.addLikeOrDislike(reviewNumberOne, otherUserIdNumberOne, true);
        likeService.addLikeOrDislike(reviewNumberOne, otherUserIdNumberTwo, false);
        likeService.addLikeOrDislike(reviewNumberTwo, otherUserIdNumberTwo, true);

        ArrayList<Long> ids = new ArrayList<>();
        ids.add(reviewNumberOne.getId());
        ids.add(reviewNumberTwo.getId());

        Map<Long, LikeDto> likeDto = likeService.getNumberOfLikesAndDislikesByListReviewsId(ids);

        assertThat(likeDto.size(), is(2));
        assertThat(likeDto.get(reviewNumberOne.getId()).numbersOfLikes(), is(1L));
        assertThat(likeDto.get(reviewNumberOne.getId()).numbersOfDislikes(), is(1L));
        assertThat(likeDto.get(reviewNumberTwo.getId()).numbersOfLikes(), is(1L));
        assertThat(likeDto.get(reviewNumberTwo.getId()).numbersOfDislikes(), is(0L));
    }

    @Test
    void getNumberOfLikesAndDislikesByListReviewsIdIfOneReviewHasNoLikesOrDislikes() {
        Review newReviewNumberOne = createReview(1);
        Review newReviewNumberTwo = createReview(2);
        Review newReviewNumberThree = createReview(3);
        Long userId = 1L;
        Long otherUserIdNumberOne = 2L;
        Long otherUserIdNumberTwo = 3L;

        Review reviewNumberOne = reviewService.createReview(newReviewNumberOne, userId);
        Review reviewNumberTwo = reviewService.createReview(newReviewNumberTwo, userId);
        likeService.addLikeOrDislike(reviewNumberOne, otherUserIdNumberOne, true);
        likeService.addLikeOrDislike(reviewNumberOne, otherUserIdNumberTwo, false);
        likeService.addLikeOrDislike(reviewNumberTwo, otherUserIdNumberTwo, true);

        ArrayList<Long> ids = new ArrayList<>();
        ids.add(reviewNumberOne.getId());
        ids.add(reviewNumberTwo.getId());
        ids.add(newReviewNumberThree.getId());

        Map<Long, LikeDto> likeDto = likeService.getNumberOfLikesAndDislikesByListReviewsId(ids);

        assertThat(likeDto.size(), is(2));
        assertNull(likeDto.get(newReviewNumberThree.getId()));
        assertThat(likeDto.get(reviewNumberOne.getId()).numbersOfLikes(), is(1L));
        assertThat(likeDto.get(reviewNumberOne.getId()).numbersOfDislikes(), is(1L));
        assertThat(likeDto.get(reviewNumberTwo.getId()).numbersOfLikes(), is(1L));
        assertThat(likeDto.get(reviewNumberTwo.getId()).numbersOfDislikes(), is(0L));
    }

    @Test
    void getNumberOfLikesAndDislikesByListReviewsIdIfLikesAndDislikesDoNotExist() {
        Review newReviewNumberOne = createReview(1);
        Review newReviewNumberTwo = createReview(2);
        Long userId = 1L;

        Review reviewNumberOne = reviewService.createReview(newReviewNumberOne, userId);
        Review reviewNumberTwo = reviewService.createReview(newReviewNumberTwo, userId);

        ArrayList<Long> ids = new ArrayList<>();
        ids.add(reviewNumberOne.getId());
        ids.add(reviewNumberTwo.getId());

        Map<Long, LikeDto> likeDto = likeService.getNumberOfLikesAndDislikesByListReviewsId(ids);

        assertTrue(likeDto.isEmpty());
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