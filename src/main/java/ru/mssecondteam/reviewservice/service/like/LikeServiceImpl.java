package ru.mssecondteam.reviewservice.service.like;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.mssecondteam.reviewservice.dto.LikeDto;
import ru.mssecondteam.reviewservice.exception.NotFoundException;
import ru.mssecondteam.reviewservice.model.Like;
import ru.mssecondteam.reviewservice.model.Review;
import ru.mssecondteam.reviewservice.repository.like.LikeRepository;

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
            log.info("User with id '{}' add '{}' like to review with id '{}'", userId, isPositive, review.getId());
        } else if (like.getIsPositive() != isPositive) {
            repository.deleteById(like.getId());
            log.info("User with id '{}' delete '{}' like to review with id '{}'", userId, isPositive, review.getId());
        }
    }

    @Override
    public void deleteLikeOrDislike(Long reviewId, Long userId, Boolean isPositive) {
        Like like = getLikeByUserIdAndReviewId(userId, reviewId);

        if (like == null) {
            throw new NotFoundException(String.format("Like with userId '%s' and reviewId '%s' was not found",
                    userId, reviewId));
        }
        if (like.getIsPositive() == isPositive) {
            repository.deleteById(like.getId());
            log.info("User with id '{}' delete '{}' like to review with id '{}'", userId, isPositive, reviewId);
        }

    }

    @Override
    public LikeDto getNumberOfLikesAndDislikesByReviewId(Long reviewId) {
        log.info("Received likes and dislikes for review with id '{}'", reviewId);
        return repository.getLikesAndDislikesByReviewId(reviewId).orElse(null);
    }

    @Override
    public Map<Long, LikeDto> getNumberOfLikesAndDislikesByListReviewsId(List<Long> reviewsIds) {
        log.info("Received likes and dislikes for reviews with ids '{}'", reviewsIds);
        return repository.getLikesAndDislikesByReviewsIds(reviewsIds);
    }

    private Like getLikeByUserIdAndReviewId(Long userId, Long reviewId) {
        return repository.getLikeByUserIdAndReviewId(userId, reviewId).orElse(null);
    }

}