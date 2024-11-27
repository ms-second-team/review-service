package ru.mssecondteam.reviewservice.repository.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.mssecondteam.reviewservice.dto.EventReviewStats;
import ru.mssecondteam.reviewservice.dto.UserReviewStats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StatsRepositoryImpl implements StatsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public EventReviewStats getReviewStatsForEvent(Long eventId, int minPositiveMark, List<Long> reviewIds) {
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("minPositiveMark", minPositiveMark)
                .addValues(Collections.singletonMap("reviewIds", reviewIds));

        final String sql = "select r.event_id as event_id, " +
                "(select AVG(r.mark) " +
                "from reviews r where r.event_id = :eventId and r.review_id in (:reviewIds)) AS average_mark, " +
                "(select COUNT(*) from reviews r where r.event_id = :eventId) AS total_marks, " +
                "(100.0 * (select count(*) from reviews r where r.event_id = :eventId and r.mark >= :minPositiveMark) / " +
                "(select COUNT(*) from reviews r where r.event_id = :eventId)) AS positive_mark_percentage," +
                "(100.0 * (select count(*) from reviews r where r.event_id = :eventId and r.mark < :minPositiveMark) / " +
                "(select COUNT(*) from reviews r where r.event_id = :eventId)) AS negative_mark_percentage " +
                "FROM reviews r " +
                "WHERE r.event_id = :eventId " +
                "GROUP BY r.event_id";
        return jdbcTemplate.query(sql, namedParams, this::mapToEventReviewStats);
    }

    @Override
    public UserReviewStats getReviewStatsForUser(Long authorId, int minPositiveMark, List<Long> reviewIds) {
        SqlParameterSource namedParams = new MapSqlParameterSource()
                .addValue("authorId", authorId)
                .addValue("minPositiveMark", minPositiveMark)
                .addValues(Collections.singletonMap("reviewIds", reviewIds));

        final String sql = "select r.author_id as author_id, " +
                "(select AVG(r.mark) " +
                "from reviews r where r.author_id = :authorId and r.review_id in (:reviewIds)) AS average_mark," +
                "(select COUNT(*) from reviews r where r.author_id = :authorId) AS total_marks," +
                "(100.0 * (select count(*) from reviews r where r.author_id = :authorId and r.mark >= :minPositiveMark) / " +
                "(select COUNT(*) from reviews r where r.author_id = :authorId))  AS positive_mark_percentage," +
                "(100.0 * (select count(*) from reviews r where r.author_id = :authorId and r.mark < :minPositiveMark) / " +
                "(select COUNT(*) from reviews r where r.author_id = :authorId))  AS negative_mark_percentage " +
                "FROM reviews r " +
                "where r.author_id = :authorId " +
                "GROUP BY r.author_id";
        return jdbcTemplate.query(sql, namedParams, this::mapToUserReviewStats);
    }

    @Override
    public List<Long> getReviewsIdsForEventStats(int minNumberOfLikes, Long eventId) {
        List<Long> reviewIds = getReviewIds(minNumberOfLikes);
        List<Long> allReviewsIds = getReviewsIdsForEventStatsIfNoLikes(eventId);
        if (allReviewsIds != null) {
            allReviewsIds.removeAll(reviewIds);
            return allReviewsIds.isEmpty() ? null : allReviewsIds;
        }
        return null;
    }

    @Override
    public List<Long> getReviewsIdsForUserStats(int minNumberOfLikes, Long authorId) {
        List<Long> reviewIds = getReviewIds(minNumberOfLikes);
        List<Long> allReviewsIds = getReviewsIdsForUserStatsIfNoLikes(authorId);
        if (allReviewsIds != null) {
            allReviewsIds.removeAll(reviewIds);
            return allReviewsIds.isEmpty() ? null : allReviewsIds;
        }
        return null;
    }

    public List<Long> getReviewsIdsForEventStatsIfNoLikes(Long eventId) {
        SqlParameterSource namedParam = new MapSqlParameterSource()
                .addValue("eventId", eventId);

        final String sql = "SELECT review_id " +
                "FROM reviews " +
                "WHERE event_id = :eventId";
        List<Long> result = jdbcTemplate.queryForList(sql, namedParam, Long.class);
        return result.isEmpty() ? null : result;
    }

    public List<Long> getReviewsIdsForUserStatsIfNoLikes(Long authorId) {
        SqlParameterSource namedParam = new MapSqlParameterSource()
                .addValue("authorId", authorId);

        final String sql = "SELECT review_id " +
                "FROM reviews " +
                "WHERE author_id = :authorId";
        List<Long> result = jdbcTemplate.queryForList(sql, namedParam, Long.class);
        return result.isEmpty() ? null : result;
    }


    private EventReviewStats mapToEventReviewStats(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return EventReviewStats.builder()
                    .eventId(rs.getLong("event_id"))
                    .avgMark(rs.getFloat("average_mark"))
                    .totalNumberOfReviews(rs.getLong("total_marks"))
                    .positiveReviewsPercentage(rs.getFloat("positive_mark_percentage"))
                    .negativeReviewsPercentage(rs.getFloat("negative_mark_percentage"))
                    .build();
        }
        return null;
    }

    private UserReviewStats mapToUserReviewStats(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return UserReviewStats.builder()
                    .userId(rs.getLong("author_id"))
                    .avgMark(rs.getFloat("average_mark"))
                    .totalNumberOfReviews(rs.getLong("total_marks"))
                    .positiveReviewsPercentage(rs.getFloat("positive_mark_percentage"))
                    .negativeReviewsPercentage(rs.getFloat("negative_mark_percentage"))
                    .build();
        }
        return null;
    }

    private List<Long> getReviewIds(int minNumberOfLikes) {
        SqlParameterSource namedParam = new MapSqlParameterSource()
                .addValue("minNumberOfLikes", minNumberOfLikes);

        final String sql = "SELECT review_id " +
                "FROM likes " +
                "GROUP BY review_id " +
                "HAVING (SUM(CASE WHEN is_positive = TRUE THEN 1 ELSE 0 END) < " +
                "SUM(CASE WHEN is_positive = FALSE THEN 1 ELSE 0 END)) " +
                "OR COUNT(*) > :minNumberOfLikes";
        List<Long> result = jdbcTemplate.queryForList(sql, namedParam, Long.class);
        return result;
    }
}
