package ru.mssecondteam.reviewservice.repository.like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mssecondteam.reviewservice.dto.like.LikeDto;
import ru.mssecondteam.reviewservice.model.Like;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    @Query("SELECT l " +
            "FROM Like l " +
            "WHERE l.userId = ?1 " +
            "AND l.review.id = ?2 ")
    Optional<Like> getLikeByUserIdAndReviewId(Long userId, Long reviewId);

    @Query(name = "like_dto", nativeQuery = true)
    Optional<LikeDto> getLikesAndDislikesByReviewId(Long reviewId);

    @Query(name = "like_dto_list", nativeQuery = true)
    List<LikeDto> getLikesAndDislikesByReviewsIds(List<Long> reviewsIds);
}