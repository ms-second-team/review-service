package ru.mssecondteam.reviewservice.mapper;

import ru.mssecondteam.reviewservice.LikeDto;
import ru.mssecondteam.reviewservice.dto.ReviewDto;
import ru.mssecondteam.reviewservice.model.Review;

import java.util.ArrayList;
import java.util.List;

public class CustomReviewMapper {
    public static ReviewDto toDtoWithLikes(Review review, LikeDto likeDto) {
        return new ReviewDto(
                review.getId(),
                review.getTitle(),
                review.getContent(),
                review.getUsername(),
                review.getMark(),
                review.getEventId(),
                review.getCreatedDateTime(),
                review.getUpdatedDateTime(),
                likeDto.numbersOfLikes(),
                likeDto.numbersOfDislikes()
        );
    }

    public static List<ReviewDto> toDtoListWithLikes(List<Review> eventReviews, List<LikeDto> likes) {
        List<ReviewDto> reviewsDto = new ArrayList<>();
        Review review;
        LikeDto likeDto;
        ReviewDto reviewDto;

        for (int i = 0; i < eventReviews.size(); i++) {
            review = eventReviews.get(i);
            likeDto = likes.get(i);
            reviewDto = toDtoWithLikes(review, likeDto);
            reviewsDto.add(reviewDto);
        }
        return reviewsDto;
    }
}