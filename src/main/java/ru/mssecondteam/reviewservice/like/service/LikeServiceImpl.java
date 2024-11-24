package ru.mssecondteam.reviewservice.like.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.mssecondteam.reviewservice.like.model.Like;
import ru.mssecondteam.reviewservice.like.dto.LikeDto;
import ru.mssecondteam.reviewservice.like.repository.LikeRepository;
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

        if (like == null) {
            throw new NotFoundException(String.format("Like with userId '%s'  and reviewId was not found", userId, reviewId));
        }
        if (like.getIsPositive() == isPositive) {
            repository.deleteById(like.getId());
            log.info("User with id '%s' delete '%s' like to review with id '%s'", userId, isPositive, reviewId);
        }

    }

    @Override
    public LikeDto getNumberOfLikesAndDislikesByReviewId(Long reviewId) {
        log.info("Received likes and dislikes for review with id '%s'", reviewId);
        return repository.getLikesAndDislikesByReviewId(reviewId).orElse(null);
    }

    @Override
    public Map<Long, LikeDto> getNumberOfLikesAndDislikesByListReviewsId(List<Long> reviewsIds) {
        log.info("Received likes and dislikes for reviews with ids '%s'", reviewsIds);
        return repository.getLikesAndDislikesByReviewsIds(reviewsIds);
    }

    private Like getLikeByUserIdAndReviewId(Long userId, Long reviewId) {
        return repository.getLikeByUserIdAndReviewId(userId, reviewId).orElse(null);
    }

}