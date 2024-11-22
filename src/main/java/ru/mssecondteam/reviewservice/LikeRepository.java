package ru.mssecondteam.reviewservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long>, CustomLikeRepository {

    @Query("SELECT l " +
            "FROM Like l " +
            "WHERE l.userId = ?1 " +
            "AND l.review.id = ?2 ")
    Optional<Like> getLikeByUserIdAndReviewId(Long userId, Long reviewId);

}