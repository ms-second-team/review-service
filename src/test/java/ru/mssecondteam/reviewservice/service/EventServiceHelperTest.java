package ru.mssecondteam.reviewservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import ru.mssecondteam.reviewservice.client.event.EventClient;
import ru.mssecondteam.reviewservice.dto.event.EventDto;
import ru.mssecondteam.reviewservice.dto.event.TeamMemberDto;
import ru.mssecondteam.reviewservice.dto.event.TeamMemberRole;
import ru.mssecondteam.reviewservice.exception.NotAuthorizedException;
import ru.mssecondteam.reviewservice.exception.NotFoundException;
import ru.mssecondteam.reviewservice.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventServiceHelperTest {
    private EventClient eventClient;
    private EventServiceHelper eventServiceHelper;

    @BeforeEach
    void setUp() {
        eventClient = Mockito.mock(EventClient.class);
        eventServiceHelper = new EventServiceHelper(eventClient);
    }

    @Test
    @DisplayName("Should successfully validate when event has passed and user is a team member")
    void shouldValidateEventAndUserSuccessfully() {
        Long userId = 1L;
        Long eventId = 100L;

        EventDto eventDto = new EventDto(
                eventId, "Event Name", "Description",
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(1),
                "Location", 2L
        );

        List<TeamMemberDto> teamMembers = List.of(
                new TeamMemberDto(eventId, userId, TeamMemberRole.MEMBER),
                new TeamMemberDto(eventId, 3L, TeamMemberRole.MANAGER)
        );

        when(eventClient.getEventById(any(), any())).thenReturn(ResponseEntity.ok(eventDto));
        when(eventClient.getTeamsByEventId(any(), any())).thenReturn(ResponseEntity.ok(teamMembers));

        eventServiceHelper.checkThatEventHasPassedAndUserIsEventTeamMembers(userId, eventId);

        verify(eventClient).getEventById(userId, eventId);
        verify(eventClient).getTeamsByEventId(userId, eventId);
    }

    @Test
    @DisplayName("Should throw ValidationException when event has not yet passed")
    void shouldThrowValidationExceptionWhenEventNotPassed() {
        Long userId = 1L;
        Long eventId = 100L;

        EventDto eventDto = new EventDto(
                eventId, "Event Name", "Description",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                "Location", 2L
        );

        when(eventClient.getEventById(any(), any())).thenReturn(ResponseEntity.ok(eventDto));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> eventServiceHelper.checkThatEventHasPassedAndUserIsEventTeamMembers(userId, eventId)
        );

        verify(eventClient).getEventById(userId, eventId);
        verify(eventClient, never()).getTeamsByEventId(any(), any());
    }

    @Test
    @DisplayName("Should throw NotAuthorizedException when user is not a team member")
    void shouldThrowNotAuthorizedExceptionWhenUserNotTeamMember() {
        Long userId = 1L;
        Long eventId = 100L;

        EventDto eventDto = new EventDto(
                eventId, "Event Name", "Description",
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(1),
                "Location", 2L
        );

        List<TeamMemberDto> teamMembers = List.of(
                new TeamMemberDto(eventId, 3L, TeamMemberRole.MEMBER),
                new TeamMemberDto(eventId, 4L, TeamMemberRole.MANAGER)
        );

        when(eventClient.getEventById(any(), any())).thenReturn(ResponseEntity.ok(eventDto));
        when(eventClient.getTeamsByEventId(any(), any())).thenReturn(ResponseEntity.ok(teamMembers));

        NotAuthorizedException exception = assertThrows(
                NotAuthorizedException.class,
                () -> eventServiceHelper.checkThatEventHasPassedAndUserIsEventTeamMembers(userId, eventId)
        );

        verify(eventClient).getEventById(userId, eventId);
        verify(eventClient).getTeamsByEventId(userId, eventId);
    }

    @Test
    @DisplayName("Should throw NotFoundException when event is not found")
    void shouldThrowNotFoundExceptionWhenEventNotFound() {
        Long userId = 1L;
        Long eventId = 100L;

        when(eventClient.getEventById(any(), any())).thenThrow(new NotFoundException("Event was not found"));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> eventServiceHelper.checkThatEventHasPassedAndUserIsEventTeamMembers(userId, eventId)
        );

        verify(eventClient).getEventById(userId, eventId);
        verify(eventClient, never()).getTeamsByEventId(any(), any());
    }
}