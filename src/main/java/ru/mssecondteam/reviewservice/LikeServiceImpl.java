package ru.mssecondteam.reviewservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeServiceImpl implements LikeService {
    private final LikeRepository repository;

    @Override
    public void addLike(Long reviewId, Long userId) {
        Like like = getLikeByUserIdAndReviewId(userId, reviewId);
        //Like dislike = getDislikeByUserIdAndReviewId(userId, reviewId);

        if (like == null) {
            Like newLike = new Like(null, userId, reviewId, true);
            repository.save(newLike);
            log.info("User with id '%s' add like to review with id '%s'", userId, reviewId);
        } else if (like.getIsPositive() == false) {
            repository.deleteById(like.getId());
            log.info("User with id '%s' delete dislike to review with id '%s'", userId, reviewId);
        }

    }

    @Override
    public void deleteLike(Long reviewId, Long userId) {
        Like like = getLikeByUserIdAndReviewId(userId, reviewId);

        if (like != null) {
            repository.deleteById(like.getId());
            log.info("User with id '%s' delete like to review with id '%s'", userId, reviewId);
        }

    }

    @Override
    public void addDislike(Long reviewId, Long userId) {
        Like dislike = getLikeByUserIdAndReviewId(userId, reviewId);
        //Like dislike = getDislikeByUserIdAndReviewId(userId, reviewId);

        if (dislike == null) {
            Like newLike = new Like(null, userId, reviewId, false);
            repository.save(newLike);
            log.info("User with id '%s' add dislike to review with id '%s'", userId, reviewId);
        } else if (dislike.getIsPositive() == true) {
            repository.deleteById(dislike.getId());
            log.info("User with id '%s' delete like to review with id '%s'", userId, reviewId);
        }
    }

    @Override
    public void deleteDislike(Long reviewId, Long userId) {
        Like dislike = getLikeByUserIdAndReviewId(userId, reviewId);

        if (dislike != null) {
            repository.deleteById(dislike.getId());
            log.info("User with id '%s' delete dislike to review with id '%s'", userId, reviewId);
        }
    }

    @Override
    public Like getLikeByUserIdAndReviewId(Long userId, Long reviewId) {
        return repository.getLikeByUserIdAndReviewId(userId, reviewId);
    }

    //@Override
    //public Like getDislikeByUserIdAndReviewId(Long userId, Long reviewId) {
    //    return repository.getLikeByUserIdAndReviewId(userId, reviewId);
    //}

    @Override
    public Integer getLikes(Long reviewId) {
        return repository.getLikesCountByReviewId(reviewId, true);
       // if ()
    }

    @Override
    public Integer getDislikes(Long reviewId) {
        return repository.getLikesCountByReviewId(reviewId, false);
    }
}
