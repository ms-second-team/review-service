package ru.mssecondteam.reviewservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LikeRepository extends JpaRepository<Like, Long> {
    //@Query("INSERT into LIKES " +
    //        "(user_id, review_id, is_positive) " +
    //        "VALUES (?1, ?2, ?3) ")
    //void addLike(Long userId, Long reviewId, Boolean isPositive);

    //@Query("DELETE from LIKES " +
    //        "WHERE user_id = ?1 AND " +
    //        "review_id = ?2 AND " +
     //       "l.isPositive = ?3 ")
    //void deleteLike(Long userId, Long reviewId, Boolean isPositive);

    //@Query("INSERT into LIKES " +
    //        "(user_id, review_id, is_positive) " +
     //       "VALUES (?1, ?2, false) ")
   // void addDislike(Long userId, Long reviewId);

   // @Query("DELETE from LIKES " +
    //        "WHERE user_id = ?1 AND " +
    //        "review_id = ?2 AND " +
    //        "l.isPositive = false")
    //void deleteDislike(Long userId, Long reviewId);

    @Query("SELECT l " +
            "FROM Like l " +
            "WHERE l.userId = ?1 " +
            "AND l.reviewId = ?2 ")//+
          //  "AND l.isPositive = ?3")
    Like getLikeByUserIdAndReviewId(Long userId, Long reviewId);

    //@Query("SELECT l " +
    //        "FROM Like l " +
    //        "WHERE l.userId = ?1 " +
     //       "AND l.reviewId = ?2 " +
     //       "AND l.isPositive = false")
    //Like getDislikeByUserIdAndReviewId(Long userId, Long reviewId);

    @Query("SELECT COUNT(l.userId) " +
            "FROM Like l " +
            "WHERE l.reviewId = ?1 " +
            "AND l.isPositive = ?2 ")
    Integer getLikesCountByReviewId(Long reviewId, Boolean isPositive);

    //@Query("SELECT COUNT(l.user_id) " +
    //        "FROM Like l " +
    //        "AND l.reviewId = ? " +
     //       "AND l.isPositive = false")
    //Integer getDislikesCountByReviewId(Long reviewId);
}