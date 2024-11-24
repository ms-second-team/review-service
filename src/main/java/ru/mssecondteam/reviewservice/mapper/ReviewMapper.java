package ru.mssecondteam.reviewservice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.mssecondteam.reviewservice.dto.NewReviewRequest;
import ru.mssecondteam.reviewservice.dto.ReviewDto;
import ru.mssecondteam.reviewservice.dto.ReviewUpdateRequest;
import ru.mssecondteam.reviewservice.like.dto.LikeDto;
import ru.mssecondteam.reviewservice.model.Review;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    Review toModel(NewReviewRequest newReview);

    ReviewDto toDto(Review createdReview);

    List<ReviewDto> toDtoList(List<Review> eventReviews);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateReview(ReviewUpdateRequest updateRequest, @MappingTarget Review review);

    @Mapping(target = "id", source = "review.id")
    @Mapping(target = "title", source = "review.title")
    @Mapping(target = "content", source = "review.content")
    @Mapping(target = "username", source = "review.username")
    @Mapping(target = "mark", source = "review.mark")
    @Mapping(target = "eventId", source = "review.eventId")
    @Mapping(target = "createdDateTime", source = "review.createdDateTime")
    @Mapping(target = "updatedDateTime", source = "review.updatedDateTime")
    @Mapping(target = "numberOfLikes", expression = "java(likeDto == null ? 0 : likeDto.numbersOfLikes())")
    @Mapping(target = "numberOfDislikes", expression = "java(likeDto == null ? 0 : likeDto.numbersOfDislikes())")
    ReviewDto toDtoWithLikes(Review review, LikeDto likeDto);

    default List<ReviewDto> toDtoListWithLikes(List<Review> eventReviews, Map<Long, LikeDto> reviewIdToLikeDto) {
        final Map<Long, Review> reviewIdToReview = new HashMap<>();
        eventReviews.forEach(review -> reviewIdToReview.put(review.getId(), review));
        final List<ReviewDto> result = new ArrayList<>();
        for (Long reviewId : reviewIdToReview.keySet()) {
            final Review review = reviewIdToReview.get(reviewId);
            final LikeDto likeDto = reviewIdToLikeDto.get(reviewId);
            result.add(toDtoWithLikes(review, likeDto));
        }
        return result;
    }

    static List<Long> getReviewsIds(List<Review> reviews) {
        return reviews.stream()
                .map(Review::getId)
                .collect(Collectors.toList());
    }

}