package ru.mssecondteam.reviewservice.repository;

import org.springframework.data.domain.Sort;
import ru.mssecondteam.reviewservice.model.Review;

import java.util.List;

public interface JdbcReviewRepository {

    List<Review> getTopReviewsForEvent(Long eventId, int limit, Sort.Direction sortDirection);
}
