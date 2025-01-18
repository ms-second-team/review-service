package ru.mssecondteam.reviewservice.repository.stats;

import ru.mssecondteam.reviewservice.dto.stats.EventReviewStats;
import ru.mssecondteam.reviewservice.dto.stats.UserReviewStats;

import java.util.List;

public interface StatsRepository {

    EventReviewStats getReviewStatsForEvent(Long eventId, int minPositiveMark, List<Long> reviewIds);

    UserReviewStats getReviewStatsForUser(Long authorId, int minPositiveMark, List<Long> reviewIds);

    List<Long> getReviewsIdsForEventStats(int minNumberOfLikes, Long eventId);

    List<Long> getReviewsIdsForUserStats(int minNumberOfLikes, Long authorId);
}
