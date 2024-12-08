package ru.mssecondteam.reviewservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mssecondteam.reviewservice.client.event.EventClient;
import ru.mssecondteam.reviewservice.dto.event.EventDto;
import ru.mssecondteam.reviewservice.dto.event.TeamMemberDto;
import ru.mssecondteam.reviewservice.exception.NotAuthorizedException;
import ru.mssecondteam.reviewservice.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceHelper {

    private final EventClient eventClient;

    public void checkThatEventHasPassedAndUserIsEventTeamMembers(Long userId, Long eventId) {
        final EventDto event = eventClient.getEventById(userId, eventId).getBody();
        if (event.endDateTime().isBefore(LocalDateTime.now())) {
            throw new ValidationException(String.format("The event with id = %d has not yet passed", eventId));
        }
        final Set<Long> eventTeamMembersId = eventClient.getTeamsByEventId(userId, eventId).getBody().stream()
                .map(TeamMemberDto::userId)
                .collect(Collectors.toSet());
        eventTeamMembersId.add(event.ownerId());

        checkIfUserIsATeamMember(eventTeamMembersId, userId, eventId);
    }

    private void checkIfUserIsATeamMember(Set<Long> teamMembersIds, Long userId, Long eventId) {
        if (!teamMembersIds.contains(userId)) {
            throw new NotAuthorizedException(String.format("User is with id '%s' not a team member for event with id '%s'",
                    userId, eventId));
        }
    }
}
