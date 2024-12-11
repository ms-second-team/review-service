package ru.mssecondteam.reviewservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
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
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "app.event-service.url=http://localhost:${wiremock.server.port}",
        "app.registration-service.url=http://localhost:${wiremock.server.port}"
})
class LikeServiceImplTest {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private LikeService likeService;

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
    void addLike() {
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();

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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
        Review newReview = createReview(1);
        Long userId = 1L;

        Review review = reviewService.createReview(newReview, userId);

        LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());

        assertNull(likeDto);
    }

    @Test
    void getNumberOfLikesAndDislikesByListReviewsId() {
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
        setupWireMockForRegistrationClientPositiveAnswer();
        setupWireMockForEventClientPositiveAnswer();
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
                new TeamMemberDto(4L, 1L, TeamMemberRole.MEMBER));

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