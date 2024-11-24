package ru.mssecondteam.reviewservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.mssecondteam.reviewservice.model.Review;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcReviewRepositoryImpl implements JdbcReviewRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Review> getTopReviewsForEvent(Long eventId, int limit, Sort.Direction sortDirection) {
        String sql = "SELECT r.* " +
                "FROM reviews r " +
                "LEFT JOIN likes l " +
                "ON r.review_id  = l.review_id " +
                "WHERE r.event_id = ? " +
                "GROUP BY r.review_id " +
                "ORDER BY (SUM(CASE WHEN l.is_positive  = TRUE THEN 1 ELSE 0 END) - SUM(CASE WHEN l.is_positive = FALSE THEN 1 ELSE 0 END)) ";

        switch (sortDirection) {
            case ASC -> sql += "ASC limit ?";
            case DESC -> sql += "DESC limit ?";
        }
        return jdbcTemplate.query(sql, this::mapToReviewList, eventId, limit);
    }

    private List<Review> mapToReviewList(ResultSet rs) throws SQLException {
        final List<Review> reviews = new ArrayList<>();
        while (rs.next()) {
            reviews.add(Review.builder()
                    .id(rs.getLong("review_id"))
                    .title(rs.getString("title"))
                    .content(rs.getString("content"))
                    .mark(rs.getInt("mark"))
                    .eventId(rs.getLong("event_id"))
                    .username(rs.getString("username"))
                    .createdDateTime(rs.getTimestamp("created_at").toLocalDateTime())
                    .createdDateTime(rs.getTimestamp("updated_at").toLocalDateTime())
                    .build());
        }
        return reviews;
    }
}
