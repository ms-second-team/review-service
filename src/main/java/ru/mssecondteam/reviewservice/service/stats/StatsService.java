package ru.mssecondteam.reviewservice.service.stats;

import ru.mssecondteam.reviewservice.dto.stats.EventReviewStats;
import ru.mssecondteam.reviewservice.dto.stats.UserReviewStats;

public interface StatsService {

    EventReviewStats getEventReviewsStats(Long eventId);

    UserReviewStats getUserReviewsStats(Long userId);
}
