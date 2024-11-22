package ru.mssecondteam.reviewservice;

import java.util.List;

public interface CustomLikeRepository {
    LikeDto getLikesAndDislikesByReviewId(Long reviewsId);

    List<LikeDto> getLikesAndDislikesByReviewsIds(List <Long> reviewsIds);
}
