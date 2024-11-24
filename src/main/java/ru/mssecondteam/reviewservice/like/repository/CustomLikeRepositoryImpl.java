package ru.mssecondteam.reviewservice.like.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.mssecondteam.reviewservice.like.dto.LikeDto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class CustomLikeRepositoryImpl implements CustomLikeRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<LikeDto> getLikesAndDislikesByReviewId(Long reviewId) {
        String sql =
                "select review_id, " +
                        "COUNT(CASE WHEN is_positive = TRUE THEN 1 END) AS likes, " +
                        "COUNT(CASE WHEN is_positive = FALSE THEN 1 END) AS dislikes " +
                        "from likes " +
                        "where review_id = ? " +
                        "group by review_id";

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) -> makeLikeDto(rs), reviewId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }

    }

    @Override
    public Map<Long, LikeDto> getLikesAndDislikesByReviewsIds(List<Long> reviewsIds) {
        String inSql = String.join(",", Collections.nCopies(reviewsIds.size(), "?"));

        String sql =
                "select review_id, " +
                        "COUNT(CASE WHEN is_positive = TRUE THEN 1 END) AS likes, " +
                        "COUNT(CASE WHEN is_positive = FALSE THEN 1 END) AS dislikes " +
                        "from likes " +
                        "where review_id IN (%s) " +
                        "group by review_id";

        return jdbcTemplate.query(String.format(sql, inSql), this::mapToReviewIdToLike,
                reviewsIds.toArray());

    }

    private LikeDto makeLikeDto(ResultSet resultSet) throws SQLException {
        System.out.println(resultSet);
        return new LikeDto(
                resultSet.getLong("review_id"),
                resultSet.getLong("likes"),
                resultSet.getLong("dislikes")
        );
    }

    private Map<Long, LikeDto> mapToReviewIdToLike(ResultSet resultSet) throws SQLException {
        Map<Long, LikeDto> result = new HashMap<>();
        while (resultSet.next()) {
            LikeDto likeDto = LikeDto.builder()
                    .reviewId(resultSet.getLong("review_id"))
                    .numbersOfLikes(resultSet.getLong("likes"))
                    .numbersOfDislikes(resultSet.getLong("dislikes"))
                    .build();
            result.put(likeDto.reviewId(), likeDto);
        }

        return result;
    }

}