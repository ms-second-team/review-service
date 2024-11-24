package ru.mssecondteam.reviewservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mssecondteam.reviewservice.dto.EventReviewStats;
import ru.mssecondteam.reviewservice.dto.UserReviewStats;
import ru.mssecondteam.reviewservice.service.stats.StatsService;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/events/{eventId}")
    public EventReviewStats getEventReviewsStats(@PathVariable Long eventId) {
        log.info("Requesting reviews stats for event with id '{}'", eventId);
        return statsService.getEventReviewsStats(eventId);
    }

    @GetMapping("/users/{authorId}")
    public UserReviewStats getUserReviewsStats(@PathVariable Long authorId) {
        log.info("Requesting reviews stats for user with id '{}'", authorId);
        return statsService.getUserReviewsStats(authorId);
    }
}
