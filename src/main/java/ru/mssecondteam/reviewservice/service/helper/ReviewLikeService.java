package ru.mssecondteam.reviewservice.service.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mssecondteam.reviewservice.dto.LikeDto;
import ru.mssecondteam.reviewservice.dto.ReviewDto;
import ru.mssecondteam.reviewservice.dto.TopReviewsDto;
import ru.mssecondteam.reviewservice.mapper.ReviewMapper;
import ru.mssecondteam.reviewservice.model.Review;
import ru.mssecondteam.reviewservice.model.TopReviews;
import ru.mssecondteam.reviewservice.service.like.LikeService;

import java.util.List;
import java.util.Map;

import static ru.mssecondteam.reviewservice.mapper.ReviewMapper.getReviewsIds;

@Service
@RequiredArgsConstructor
public class ReviewLikeService {

    private final LikeService likeService;
    private final ReviewMapper reviewMapper;

    public ReviewDto getReviewWithLikes(Review review) {
        final LikeDto likeDto = likeService.getNumberOfLikesAndDislikesByReviewId(review.getId());
        return reviewMapper.toDtoWithLikes(review, likeDto);
    }

    public List<ReviewDto> getReviewsWithLikes(List<Review> eventReviews) {
        final List<Long> reviewsIds = getReviewsIds(eventReviews);
        final Map<Long, LikeDto> likesDto = likeService.getNumberOfLikesAndDislikesByListReviewsId(reviewsIds);
        return reviewMapper.toDtoListWithLikes(eventReviews, likesDto);
    }

    public TopReviewsDto getLikesAndMapToDto(TopReviews topReviews) {
        List<Long> bestReviewsIds = topReviews.bestReviews()
                .stream()
                .map(Review::getId)
                .toList();
        Map<Long, LikeDto> bestLikesDto = likeService.getNumberOfLikesAndDislikesByListReviewsId(bestReviewsIds);
        List<ReviewDto> bestReviewsDto = reviewMapper.toDtoListWithLikes(topReviews.bestReviews(), bestLikesDto);

        List<Long> worstReviewsIds = topReviews.worstReviews()
                .stream()
                .map(Review::getId)
                .toList();
        Map<Long, LikeDto> worstLikesDto = likeService.getNumberOfLikesAndDislikesByListReviewsId(worstReviewsIds);
        List<ReviewDto> worstReviewsDto = reviewMapper.toDtoListWithLikes(topReviews.worstReviews(), worstLikesDto);
        return new TopReviewsDto(bestReviewsDto, worstReviewsDto);
    }
}
