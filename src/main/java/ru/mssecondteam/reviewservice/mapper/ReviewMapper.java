package ru.mssecondteam.reviewservice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.mssecondteam.reviewservice.LikeDto;
import ru.mssecondteam.reviewservice.dto.NewReviewRequest;
import ru.mssecondteam.reviewservice.dto.ReviewDto;
import ru.mssecondteam.reviewservice.dto.ReviewUpdateRequest;
import ru.mssecondteam.reviewservice.model.Review;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    Review toModel(NewReviewRequest newReview);

    ReviewDto toDto(Review createdReview);

    List<ReviewDto> toDtoList(List<Review> eventReviews);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateReview(ReviewUpdateRequest updateRequest, @MappingTarget Review review);

    static ReviewDto toDtoWithLikes(Review review, LikeDto likeDto) {
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

    static List<ReviewDto> toDtoListWithLikes(List<Review> eventReviews, Map<Long,LikeDto> likes) {
        List<ReviewDto> reviewsDto = new ArrayList<>();
        Review review;
        LikeDto likeDto;
        ReviewDto reviewDto;

        for (int i = 0; i < eventReviews.size(); i++) {
            review = eventReviews.get(i);
            likeDto = likes.get(review.getId());
            reviewDto = toDtoWithLikes(review, likeDto);
            reviewsDto.add(reviewDto);
        }
        return reviewsDto;
    }
}