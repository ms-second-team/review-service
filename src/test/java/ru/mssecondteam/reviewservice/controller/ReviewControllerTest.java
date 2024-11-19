package ru.mssecondteam.reviewservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import ru.mssecondteam.reviewservice.dto.NewReviewRequest;
import ru.mssecondteam.reviewservice.dto.ReviewDto;
import ru.mssecondteam.reviewservice.dto.ReviewUpdateRequest;
import ru.mssecondteam.reviewservice.exception.NotAuthorizedException;
import ru.mssecondteam.reviewservice.exception.NotFoundException;
import ru.mssecondteam.reviewservice.mapper.ReviewMapper;
import ru.mssecondteam.reviewservice.model.Review;
import ru.mssecondteam.reviewservice.service.ReviewService;

import java.time.LocalDateTime;
import java.util.Collections;

import static java.time.format.DateTimeFormatter.ofPattern;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private ReviewMapper reviewMapper;

    private NewReviewRequest newReview;

    private ReviewDto reviewDto;

    private Review review;

    private Long userId;

    private Long reviewId;

    @Value("${spring.jackson.date-format}")
    private String dateTimeFormat;

    @BeforeEach
    void init() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("new content")
                .username("new_username")
                .eventId(444L)
                .mark(6)
                .build();
        reviewDto = ReviewDto.builder()
                .title("dto title")
                .content("dto content")
                .username("dto_username")
                .eventId(333L)
                .mark(7)
                .createdDateTime(LocalDateTime.of(2025, 10, 10, 12, 34, 33))
                .updatedDateTime(LocalDateTime.of(2025, 11, 10, 12, 34, 33))
                .build();
        review = Review.builder()
                .title("title")
                .content("content")
                .username("username")
                .eventId(222L)
                .mark(8)
                .build();
        userId = 1L;
        reviewId = 4L;
    }

    @Test
    @DisplayName("Create review")
    @SneakyThrows
    void createReview_whenAllFieldsValid_shouldReturn201Status() {
        when(reviewMapper.toModel(newReview))
                .thenReturn(review);
        when(reviewService.createReview(review, userId))
                .thenReturn(review);
        when(reviewMapper.toDto(review))
                .thenReturn(reviewDto);

        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(reviewDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(reviewDto.title())))
                .andExpect(jsonPath("$.content", is(reviewDto.content())))
                .andExpect(jsonPath("$.createdDateTime", is(reviewDto.createdDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.updatedDateTime", is(reviewDto.updatedDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.mark", is(reviewDto.mark())));

        verify(reviewMapper, times(1)).toModel(newReview);
        verify(reviewService, times(1)).createReview(review, userId);
        verify(reviewMapper, times(1)).toDto(review);
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review, request without header")
    void createReview_whenUserIdHeaderIsMissing_shouldReturn400Status() {
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MissingRequestHeaderException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with empty title")
    void createReview_whenTitleIsEmpty_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("")
                .content("new content")
                .username("new_username")
                .eventId(444L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Title can not be blank and must contain between 2 " +
                        "and 100 symbols.")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with empty title")
    void createReview_whenTitleIsNull_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title(null)
                .content("new content")
                .username("new_username")
                .eventId(444L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Title can not be blank and must contain between 2 " +
                        "and 100 symbols.")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with blank title")
    void createReview_whenTitleIsBlank_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("  ")
                .content("new content")
                .username("new_username")
                .eventId(444L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Title can not be blank and must contain between 2 " +
                        "and 100 symbols.")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with short title")
    void createReview_whenTitleIsShort_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("a")
                .content("new content")
                .username("new_username")
                .eventId(444L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Title can not be blank and must contain between 2 " +
                        "and 100 symbols.")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with empty content")
    void createReview_whenContentIsEmpty_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("")
                .username("new_username")
                .eventId(444L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Content can not be blank and must contain between 2 " +
                        "and 500 symbols.")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with empty content")
    void createReview_whenContentIsNull_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content(null)
                .username("new_username")
                .eventId(444L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Content can not be blank and must contain between 2 " +
                        "and 500 symbols.")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with blank content")
    void createReview_whenContentIsBlank_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("   ")
                .username("new_username")
                .eventId(444L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Content can not be blank and must contain between 2 " +
                        "and 500 symbols.")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with short content")
    void createReview_whenContentIsShort_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("a")
                .username("new_username")
                .eventId(444L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Content can not be blank and must contain between 2 " +
                        "and 500 symbols.")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with empty username")
    void createReview_whenUsernameIsEmpty_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("new content")
                .username("")
                .eventId(444L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Username can not be blank and must contain between 2 " +
                        "and 30 symbols.")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with empty username")
    void createReview_whenUsernameIsNull_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("new content")
                .username(null)
                .eventId(444L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Username can not be blank and must contain between 2 " +
                        "and 30 symbols.")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with blank username")
    void createReview_whenUsernameIsBlank_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("new content")
                .username("   ")
                .eventId(444L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Username can not be blank and must contain between 2 " +
                        "and 30 symbols.")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with short username")
    void createReview_whenUsernameIsShort_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("new content")
                .username("a")
                .eventId(444L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Username can not be blank and must contain between 2 " +
                        "and 30 symbols.")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with negative mark")
    void createReview_whenMarkIsNegative_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("new content")
                .username("new_username")
                .eventId(444L)
                .mark(-6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Mark must be between '1' and '10'")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with zero mark")
    void createReview_whenMarkIsZero_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("new content")
                .username("new_username")
                .eventId(444L)
                .mark(0)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Mark must be between '1' and '10'")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with null mark")
    void createReview_whenMarkIsNull_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("new content")
                .username("new_username")
                .eventId(444L)
                .mark(null)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Mark must be between '1' and '10'")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with 11 mark")
    void createReview_whenMarkIs11_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("new content")
                .username("new_username")
                .eventId(444L)
                .mark(11)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Mark must be between '1' and '10'")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with null event id")
    void createReview_whenEventIdIsNull_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("new content")
                .username("new_username")
                .eventId(null)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Event id must be positive")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with negative event id")
    void createReview_whenEventIdIsNegative_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("new content")
                .username("new_username")
                .eventId(-1L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Event id must be positive")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Create review with zero event id")
    void createReview_whenEventIdIsZero_shouldReturn400Status() {
        newReview = NewReviewRequest.builder()
                .title("new title")
                .content("new content")
                .username("new_username")
                .eventId(0L)
                .mark(6)
                .build();
        mvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReview))
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.errors", hasValue("Event id must be positive")));

        verify(reviewMapper, never()).toModel(any());
        verify(reviewService, never()).createReview(any(), any());
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update review, all valid fields")
    void updateReview_whenAllFieldsValid_shouldReturn200Status() {
        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                .title("updated title")
                .content("updated content")
                .username("updated_username")
                .mark(1)
                .build();

        when(reviewService.updateReview(reviewId, updateRequest, userId))
                .thenReturn(review);
        when(reviewMapper.toDto(review))
                .thenReturn(reviewDto);
        mvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reviewDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(reviewDto.title())))
                .andExpect(jsonPath("$.content", is(reviewDto.content())))
                .andExpect(jsonPath("$.createdDateTime", is(reviewDto.createdDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.updatedDateTime", is(reviewDto.updatedDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.mark", is(reviewDto.mark())));

        verify(reviewService, times(1)).updateReview(reviewId, updateRequest, userId);
        verify(reviewMapper, times(1)).toDto(review);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update review, optional title")
    void updateReview_whenNoTitle_shouldReturn200Status() {
        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                .content("updated content")
                .username("updated_username")
                .mark(1)
                .build();

        when(reviewService.updateReview(reviewId, updateRequest, userId))
                .thenReturn(review);
        when(reviewMapper.toDto(review))
                .thenReturn(reviewDto);
        mvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reviewDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(reviewDto.title())))
                .andExpect(jsonPath("$.content", is(reviewDto.content())))
                .andExpect(jsonPath("$.createdDateTime", is(reviewDto.createdDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.updatedDateTime", is(reviewDto.updatedDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.mark", is(reviewDto.mark())));

        verify(reviewService, times(1)).updateReview(reviewId, updateRequest, userId);
        verify(reviewMapper, times(1)).toDto(review);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update review, optional content")
    void updateReview_whenNoContent_shouldReturn200Status() {
        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                .username("updated_username")
                .mark(1)
                .build();

        when(reviewService.updateReview(reviewId, updateRequest, userId))
                .thenReturn(review);
        when(reviewMapper.toDto(review))
                .thenReturn(reviewDto);
        mvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reviewDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(reviewDto.title())))
                .andExpect(jsonPath("$.content", is(reviewDto.content())))
                .andExpect(jsonPath("$.createdDateTime", is(reviewDto.createdDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.updatedDateTime", is(reviewDto.updatedDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.mark", is(reviewDto.mark())));

        verify(reviewService, times(1)).updateReview(reviewId, updateRequest, userId);
        verify(reviewMapper, times(1)).toDto(review);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update review, optional username")
    void updateReview_whenNoUsername_shouldReturn200Status() {
        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                .mark(1)
                .build();

        when(reviewService.updateReview(reviewId, updateRequest, userId))
                .thenReturn(review);
        when(reviewMapper.toDto(review))
                .thenReturn(reviewDto);
        mvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reviewDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(reviewDto.title())))
                .andExpect(jsonPath("$.content", is(reviewDto.content())))
                .andExpect(jsonPath("$.createdDateTime", is(reviewDto.createdDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.updatedDateTime", is(reviewDto.updatedDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.mark", is(reviewDto.mark())));

        verify(reviewService, times(1)).updateReview(reviewId, updateRequest, userId);
        verify(reviewMapper, times(1)).toDto(review);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update review, optional all fields")
    void updateReview_whenNoFields_shouldReturn200Status() {
        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                .build();

        when(reviewService.updateReview(reviewId, updateRequest, userId))
                .thenReturn(review);
        when(reviewMapper.toDto(review))
                .thenReturn(reviewDto);
        mvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reviewDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(reviewDto.title())))
                .andExpect(jsonPath("$.content", is(reviewDto.content())))
                .andExpect(jsonPath("$.createdDateTime", is(reviewDto.createdDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.updatedDateTime", is(reviewDto.updatedDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.mark", is(reviewDto.mark())));

        verify(reviewService, times(1)).updateReview(reviewId, updateRequest, userId);
        verify(reviewMapper, times(1)).toDto(review);
    }

    @Test
    @SneakyThrows
    @DisplayName("Update review, review not found")
    void updateReview_whenReviewNotFound_shouldReturn404Status() {
        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                .build();

        when(reviewService.updateReview(reviewId, updateRequest, userId))
                .thenThrow(new NotFoundException("Review was not found"));

        mvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof NotFoundException))
                .andExpect(jsonPath("$.errors", hasValue("Review was not found")));


        verify(reviewService, times(1)).updateReview(reviewId, updateRequest, userId);
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Update review,  user not authorized to modify review")
    void updateReview_whenNotAuthorizedToModifyReview_shouldReturn403Status() {
        ReviewUpdateRequest updateRequest = ReviewUpdateRequest.builder()
                .build();

        when(reviewService.updateReview(reviewId, updateRequest, userId))
                .thenThrow(new NotAuthorizedException("Not authorized"));

        mvc.perform(patch("/reviews/{reviewId}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("X-User-Id", userId))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof NotAuthorizedException))
                .andExpect(jsonPath("$.errors", hasValue("Not authorized")));


        verify(reviewService, times(1)).updateReview(reviewId, updateRequest, userId);
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Find review by id")
    void findReviewById_whenReviewFound_shouldReturnReview() {
        when(reviewService.findReviewById(reviewId, userId))
                .thenReturn(review);
        when(reviewMapper.toDto(review))
                .thenReturn(reviewDto);

        mvc.perform(get("/reviews/{reviewId}", reviewId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(reviewDto.id()), Long.class))
                .andExpect(jsonPath("$.title", is(reviewDto.title())))
                .andExpect(jsonPath("$.content", is(reviewDto.content())))
                .andExpect(jsonPath("$.createdDateTime", is(reviewDto.createdDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.updatedDateTime", is(reviewDto.updatedDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.mark", is(reviewDto.mark())));

        verify(reviewService, times(1)).findReviewById(reviewId, userId);
        verify(reviewMapper, times(1)).toDto(review);
    }

    @Test
    @SneakyThrows
    @DisplayName("Find review by id, review not found")
    void findReviewById_whenReviewNotFound_shouldReturnReview() {
        when(reviewService.findReviewById(reviewId, userId))
                .thenThrow(new NotFoundException("Review was not found"));

        mvc.perform(get("/reviews/{reviewId}", reviewId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof NotFoundException))
                .andExpect(jsonPath("$.errors", hasValue("Review was not found")));

        verify(reviewService, times(1)).findReviewById(reviewId, userId);
        verify(reviewMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Find reviews by eventId")
    @SneakyThrows
    void findReviewsByEventId_shouldReturnReviewList() {
        Long eventId = 34L;
        when(reviewService.findReviewsByEventId(eventId, 0, 10, userId))
                .thenReturn(Collections.singletonList(review));
        when(reviewMapper.toDtoList(Collections.singletonList(review)))
                .thenReturn(Collections.singletonList(reviewDto));

        mvc.perform(get("/reviews")
                        .param("eventId", String.valueOf(eventId))
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$.[0].id", is(reviewDto.id()), Long.class))
                .andExpect(jsonPath("$.[0].title", is(reviewDto.title())))
                .andExpect(jsonPath("$.[0].content", is(reviewDto.content())))
                .andExpect(jsonPath("$.[0].createdDateTime", is(reviewDto.createdDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.[0].updatedDateTime", is(reviewDto.updatedDateTime()
                        .format(ofPattern(dateTimeFormat)))))
                .andExpect(jsonPath("$.[0].mark", is(reviewDto.mark())));

        verify(reviewService, times(1)).findReviewsByEventId(eventId, 0, 10, userId);
        verify(reviewMapper, times(1)).toDtoList(Collections.singletonList(review));
    }

    @Test
    @DisplayName("Delete review by id")
    @SneakyThrows
    void deleteReviewById_whenReviewExists_shouldDeleteReview() {

        mvc.perform(delete("/reviews/{reviewId}", reviewId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());

        verify(reviewService, times(1)).deleteReviewById(reviewId, userId);

    }

    @Test
    @DisplayName("Delete review by id, review not found")
    @SneakyThrows
    void deleteReviewById_whenReviewNotFound_shouldReturn404Status() {
        doThrow(new NotFoundException("Review was not found"))
                .when(reviewService).deleteReviewById(reviewId, userId);

        mvc.perform(delete("/reviews/{reviewId}", reviewId)
                        .header("X-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors", hasValue("Review was not found")));

        verify(reviewService, times(1)).deleteReviewById(reviewId, userId);
    }

    @Test
    @DisplayName("Delete review by id, user not authorized to delete")
    @SneakyThrows
    void deleteReviewById_whenRNotAuthorized_shouldReturn403Status() {
        doThrow(new NotAuthorizedException("Not authorized"))
                .when(reviewService).deleteReviewById(reviewId, userId);

        mvc.perform(delete("/reviews/{reviewId}", reviewId)
                        .header("X-User-Id", userId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors", hasValue("Not authorized")));

        verify(reviewService, times(1)).deleteReviewById(reviewId, userId);
    }
}