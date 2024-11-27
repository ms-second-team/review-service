package ru.mssecondteam.reviewservice.service.stats;

import ru.mssecondteam.reviewservice.dto.EventReviewStats;
import ru.mssecondteam.reviewservice.dto.UserReviewStats;

public interface StatsService {

    EventReviewStats getEventReviewsStats(Long eventId);

    UserReviewStats getUserReviewsStats(Long userId);
}
