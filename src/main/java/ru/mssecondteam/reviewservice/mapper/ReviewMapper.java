package ru.mssecondteam.reviewservice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.mssecondteam.reviewservice.dto.NewReviewRequest;
import ru.mssecondteam.reviewservice.dto.ReviewDto;
import ru.mssecondteam.reviewservice.dto.ReviewUpdateRequest;
import ru.mssecondteam.reviewservice.model.Review;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    Review toModel(NewReviewRequest newReview);

    ReviewDto toDto(Review createdReview);

    List<ReviewDto> toDtoList(List<Review> eventReviews);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateReview(ReviewUpdateRequest updateRequest, @MappingTarget Review review);

}
