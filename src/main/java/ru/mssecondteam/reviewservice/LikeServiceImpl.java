package ru.mssecondteam.reviewservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.mssecondteam.reviewservice.exception.NotFoundException;
import ru.mssecondteam.reviewservice.model.Review;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeServiceImpl implements LikeService {
    private final LikeRepository repository;

    @Override
    public void addLikeOrDislike(Review review, Long userId, Boolean isPositive) {
        Like like = getLikeByUserIdAndReviewId(userId, review.getId());

        if (like == null) {
            Like newLike = new Like(null, userId, review, isPositive);
            repository.save(newLike);
            log.info("User with id '%s' add '%s' like to review with id '%s'", userId, isPositive, review.getId());
        } else if (like.getIsPositive() != isPositive) {
            repository.deleteById(like.getId());
            log.info("User with id '%s' delete '%s' like to review with id '%s'", userId, isPositive, review.getId());
        }
    }

    @Override
    public void deleteLikeOrDislike(Long reviewId, Long userId, Boolean isPositive) {
        Like like = getLikeByUserIdAndReviewId(userId, reviewId);

        if (like.getIsPositive() == isPositive) {
            repository.deleteById(like.getId());
            log.info("User with id '%s' delete '%s' like to review with id '%s'", userId, isPositive, reviewId);
        }

    }

    @Override
    public LikeDto getNumberOfLikeByReviewId(Long reviewId) {
        return repository.getLikesAndDislikesByReviewId(reviewId);
    }

    @Override
    public Map<Long, LikeDto> getNumbersOfLikeByListReviewsId(List<Long> reviewsIds) {
        return repository.getLikesAndDislikesByReviewsIds(reviewsIds);
    }

    private Like getLikeByUserIdAndReviewId(Long userId, Long reviewId) {
        return repository.getLikeByUserIdAndReviewId(userId, reviewId)
                .orElseThrow(() -> new NotFoundException(String.format("Like with user_id '%s' and review_id was not found", userId, reviewId)));
    }

}