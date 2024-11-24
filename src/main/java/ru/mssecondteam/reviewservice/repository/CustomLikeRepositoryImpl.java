package ru.mssecondteam.reviewservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.mssecondteam.reviewservice.dto.LikeDto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        return jdbcTemplate.query(sql, this::makeLikeDto, reviewId);
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

    private Optional<LikeDto> makeLikeDto(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return Optional.empty();
        }
        return Optional.of(LikeDto.builder()
                .reviewId(resultSet.getLong("review_id"))
                .numbersOfLikes(resultSet.getLong("likes"))
                .numbersOfDislikes(resultSet.getLong("dislikes"))
                .build());
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