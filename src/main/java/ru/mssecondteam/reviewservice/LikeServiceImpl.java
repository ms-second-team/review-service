package ru.mssecondteam.reviewservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.mssecondteam.reviewservice.model.Review;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeServiceImpl implements LikeService {
    private final LikeRepository repository;

    @Override
    public void addLike(Review review, Long userId, Boolean isPositive) {
        Like like = getLikeByUserIdAndReviewId(userId, review.getId());
        String name = checkLikeOrDislike(isPositive);

        if (like == null) {
            Like newLike = new Like(null, userId, review, isPositive);
            repository.save(newLike);
            log.info("User with id '%s' add '%s' to review with id '%s'", userId, name, review.getId());
            System.out.println("rrr");
        } else if (like.getIsPositive() != isPositive) {
            repository.deleteById(like.getId());
            log.info("User with id '%s' delete '%s' to review with id '%s'", userId, name, review.getId());
        }

    }

    @Override
    public void deleteLike(Long reviewId, Long userId, Boolean isPositive) {
        Like like = getLikeByUserIdAndReviewId(userId, reviewId);
        String name = checkLikeOrDislike(isPositive);

        if (like != null && like.getIsPositive() == isPositive) {
            repository.deleteById(like.getId());
            log.info("User with id '%s' delete '%s' to review with id '%s'", userId, name, reviewId);
        }

    }

    @Override
    public Like getLikeByUserIdAndReviewId(Long userId, Long reviewId) {
        return repository.getLikeByUserIdAndReviewId(userId, reviewId).orElse(null);
    }

    @Override
    public LikeDto getLikesByReviewId(Long reviewId) {
        return repository.getLikesAndDislikesByReviewId(reviewId);
    }

    @Override
    public List<LikeDto> getLikesByListReviewsId(List <Long> reviewsIds) {
        return repository.getLikesAndDislikesByReviewsIds(reviewsIds);
    }

    private String checkLikeOrDislike(Boolean isPositive) {
        if (isPositive) {
            return "like";
        } else {
            return "dislike";
        }
    }

}