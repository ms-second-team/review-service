package ru.mssecondteam.reviewservice.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mssecondteam.reviewservice.model.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findReviewsByEventId(Long eventId, Pageable pageable);
}
