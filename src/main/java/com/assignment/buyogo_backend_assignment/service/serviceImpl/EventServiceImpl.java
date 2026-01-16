package com.assignment.buyogo_backend_assignment.service.serviceImpl;

import com.assignment.buyogo_backend_assignment.entity.Event;
import com.assignment.buyogo_backend_assignment.exception.ValidationException;
import com.assignment.buyogo_backend_assignment.repository.EventRepository;
import com.assignment.buyogo_backend_assignment.request.EventRequest;
import com.assignment.buyogo_backend_assignment.response.BatchResponse;
import com.assignment.buyogo_backend_assignment.response.RejectionDetail;
import com.assignment.buyogo_backend_assignment.service.EventService;

import com.assignment.buyogo_backend_assignment.util.EventPayloadHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor

public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;

    private static final long MAX_DURATION_MS = 21_600_000L; // 6 hours
    private static final long MAX_FUTURE_MINUTES = 15;

    @Override
    @Transactional
    public BatchResponse processBatchEvents(List<EventRequest> eventRequests) {
        int accepted = 0;
        int deduped = 0;
        int updated = 0;
        int rejected = 0;

        List<RejectionDetail> rejections = new ArrayList<>();

        // Optimization: prefetch existing events in one go (reduces DB calls)
        Set<String> eventIds = new HashSet<>();
        for (EventRequest req : eventRequests) {
            if (req != null && req.eventId() != null) {
                eventIds.add(req.eventId());
            }
        }

        Map<String, Event> existingMap = new HashMap<>();
        if (!eventIds.isEmpty()) {
            List<Event> existingEvents = eventRepository.findByEventIdIn(new ArrayList<>(eventIds));

            for (Event e : existingEvents) {
                existingMap.put(e.getEventId(), e);
            }
        }

        for (EventRequest eventRequest : eventRequests) {
            try {
                validateEvent(eventRequest);

                Instant receivedTime = Instant.now(); // set by backend
                String payloadHash = EventPayloadHashUtil.computeHash(eventRequest);

                Event existing = existingMap.get(eventRequest.eventId());

                if (existing == null) {
                    // Try insert via upsert. If some other thread inserted, this will become dedupe/update correctly.
                    int affected = eventRepository.upsertEvent(
                            eventRequest.eventId(),
                            eventRequest.eventTime(),
                            receivedTime,
                            eventRequest.machineId(),
                            eventRequest.durationMs(),
                            eventRequest.defectCount(),
                            eventRequest.factoryId(),
                            eventRequest.lineId(),
                            payloadHash
                    );

                    if (affected == 1) {
                        // Could be insert or update; since we had no existing in map, treat as accepted
                        accepted++;
                    } else {
                        deduped++;
                    }
                } else {
                    // Existing known -> decide dedupe or update using payloadHash + receivedTime
                    if (Objects.equals(existing.getPayloadHash(), payloadHash)) {
                        deduped++;
                    } else {
                        // Use DB atomic upsert update rule
                        int affected = eventRepository.upsertEvent(
                                eventRequest.eventId(),
                                eventRequest.eventTime(),
                                receivedTime,
                                eventRequest.machineId(),
                                eventRequest.durationMs(),
                                eventRequest.defectCount(),
                                eventRequest.factoryId(),
                                eventRequest.lineId(),
                                payloadHash
                        );

                        if (affected == 1 && receivedTime.isAfter(existing.getReceivedTime())) {
                            updated++;
                            // Update local map to avoid wrong classification later in same batch
                            existing.setPayloadHash(payloadHash);
                            existing.setReceivedTime(receivedTime);
                        } else {
                            deduped++;
                        }
                    }
                }

            } catch (ValidationException e) {
                rejected++;
                rejections.add(RejectionDetail.builder()
                        .eventId(eventRequest != null ? eventRequest.eventId() : null)
                        .reason(e.getMessage())
                        .build());
            } catch (Exception e) {
                rejected++;
                rejections.add(RejectionDetail.builder()
                        .eventId(eventRequest != null ? eventRequest.eventId() : null)
                        .reason("Unexpected error: " + e.getMessage())
                        .build());
            }
        }

        return BatchResponse.builder()
                .accepted(accepted)
                .deduped(deduped)
                .updated(updated)
                .rejected(rejected)
                .rejections(rejections)
                .build();
    }

    private void validateEvent(EventRequest eventRequest) {
        if (eventRequest == null) {
            throw new ValidationException("event is null");
        }

        Instant now = Instant.now();

        if (eventRequest.eventTime().isAfter(now.plus(Duration.ofMinutes(MAX_FUTURE_MINUTES)))) {
            throw new ValidationException(
                    String.format("eventTime is more than %d minutes in the future", MAX_FUTURE_MINUTES)
            );
        }

        long duration = eventRequest.durationMs();
        if (duration < 0 || duration > MAX_DURATION_MS) {
            throw new ValidationException(
                    String.format("Invalid durationMs: must be between 0 and %d", MAX_DURATION_MS)
            );
        }
    }
}


