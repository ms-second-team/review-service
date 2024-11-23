package ru.mssecondteam.reviewservice;

import java.util.List;
import java.util.Map;

public interface CustomLikeRepository {
    LikeDto getLikesAndDislikesByReviewId(Long reviewsId);

    Map<Long,LikeDto> getLikesAndDislikesByReviewsIds(List <Long> reviewsIds);
}
