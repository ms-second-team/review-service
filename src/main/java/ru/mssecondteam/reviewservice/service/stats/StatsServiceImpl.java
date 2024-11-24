package ru.mssecondteam.reviewservice.service.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.mssecondteam.reviewservice.dto.EventReviewStats;
import ru.mssecondteam.reviewservice.dto.UserReviewStats;
import ru.mssecondteam.reviewservice.repository.stats.StatsRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Value("${app.min-number-of-likes}")
    private Integer minNumberOfLikes;

    @Value("${app.min-positive-mark}")
    private Integer minPositiveMark;

    @Override
    public EventReviewStats getEventReviewsStats(Long eventId) {
        final List<Long> reviewsIds = statsRepository.getReviewsIdsForStats(minNumberOfLikes);
        EventReviewStats eventStats = statsRepository.getReviewStatsForEvent(eventId, minPositiveMark, reviewsIds);
        log.info("Acquired review stats for event with id '{}'", eventId);
        return eventStats;
    }

    @Override
    public UserReviewStats getUserReviewsStats(Long userId) {
        final List<Long> reviewsIds = statsRepository.getReviewsIdsForStats(minNumberOfLikes);
        UserReviewStats userStats = statsRepository.getReviewStatsForUser(userId, minPositiveMark, reviewsIds);
        log.info("Acquired review stats for user with id '{}'", userId);
        return userStats;
    }
}
