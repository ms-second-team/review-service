package ru.mssecondteam.reviewservice;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CustomLikeRepositoryImpl implements CustomLikeRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public LikeDto getLikesAndDislikesByReviewId(Long reviewId) {
        LikeDto likeDto;

        String sql =
                "select review_id, " +
                        "(select count(user_id) from likes where is_positive = true and review_id = ?) likes, " +
                        "(select count(user_id) from likes where is_positive = false and review_id = ?) dislikes " +
                        "from likes " +
                        "group by review_id";

        likeDto = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> makeLikeDto(rs), reviewId, reviewId);

        return likeDto;
    }

    @Override
    public Map<Long, LikeDto> getLikesAndDislikesByReviewsIds(List<Long> reviewsIds) {
        String inSql = String.join(",", Collections.nCopies(reviewsIds.size(), "?"));

        String sql =
                "select review_id, " +
                        "(select count(user_id) from likes where review_id IN (?) and is_positive = true) likes, " +
                        "(select count(user_id) from likes where review_id IN (?) and is_positive = false) dislikes " +
                        "from likes " +
                        "where review_id IN (?) " +
                        "group by review_id";

        List<LikeDto> dtoList = jdbcTemplate.query(String.format(sql, inSql, inSql), (rs, rowNum) -> makeLikeDto(rs),
                reviewsIds.toArray(), reviewsIds.toArray());

        Map<Long, LikeDto> likeDtoMap = new HashMap<>();

        for (LikeDto likeDto : dtoList) {
            likeDtoMap.put(likeDto.reviewId(), likeDto);
        }

        return likeDtoMap;
    }

    private LikeDto makeLikeDto(ResultSet resultSet) throws SQLException {
        return new LikeDto(
                resultSet.getLong("review_id"),
                resultSet.getLong("likes"),
                resultSet.getLong("dislikes")
        );
    }

}