package com.assignment.buyogo_backend_assignment.service.serviceImpl;

import com.assignment.buyogo_backend_assignment.entity.Event;
import com.assignment.buyogo_backend_assignment.exception.ValidationException;
import com.assignment.buyogo_backend_assignment.repository.EventBulkRepository;
import com.assignment.buyogo_backend_assignment.repository.EventRepository;
import com.assignment.buyogo_backend_assignment.request.EventRequest;
import com.assignment.buyogo_backend_assignment.response.BatchResponse;
import com.assignment.buyogo_backend_assignment.response.RejectionDetail;
import com.assignment.buyogo_backend_assignment.service.EventService;
import com.assignment.buyogo_backend_assignment.util.EventPayloadHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventBulkRepository eventBulkRepository;

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

        // 1) Collect valid events
        List<Event> validEvents = new ArrayList<>();
        Set<String> eventIds = new HashSet<>();

        for (EventRequest req : eventRequests) {
            try {
                validateEvent(req);

                Instant receivedTime = Instant.now(); // backend sets
                String payloadHash = EventPayloadHashUtil.computeHash(req);

                Event e = Event.builder()
                        .eventId(req.eventId())
                        .eventTime(req.eventTime())
                        .receivedTime(receivedTime)
                        .machineId(req.machineId())
                        .durationMs(req.durationMs())
                        .defectCount(req.defectCount())
                        .factoryId(req.factoryId())
                        .lineId(req.lineId())
                        .payloadHash(payloadHash)
                        .build();

                validEvents.add(e);
                eventIds.add(req.eventId());

            } catch (ValidationException ve) {
                rejected++;
                rejections.add(RejectionDetail.builder()
                        .eventId(req != null ? req.eventId() : null)
                        .reason(ve.getMessage())
                        .build());
            } catch (Exception e) {
                rejected++;
                rejections.add(RejectionDetail.builder()
                        .eventId(req != null ? req.eventId() : null)
                        .reason("Unexpected error: " + e.getMessage())
                        .build());
            }
        }

        if (validEvents.isEmpty()) {
            return BatchResponse.builder()
                    .accepted(accepted)
                    .deduped(deduped)
                    .updated(updated)
                    .rejected(rejected)
                    .rejections(rejections)
                    .build();
        }

        // 2) Prefetch existing events for classification (only needed rows)
        Map<String, Event> existingMap = new HashMap<>();
        List<Event> existingEvents = eventRepository.findByEventIdIn(new ArrayList<>(eventIds));
        for (Event ex : existingEvents) {
            existingMap.put(ex.getEventId(), ex);
        }

        // 3) Bulk upsert (FAST)
        int[] results = eventBulkRepository.bulkUpsert(validEvents);

        // 4) Count accepted / updated / deduped
        // NOTE: results[i] is usually 1 if insert/update happened, 0 if no-op
        for (int i = 0; i < validEvents.size(); i++) {
            Event incoming = validEvents.get(i);
            Event existing = existingMap.get(incoming.getEventId());

            if (existing == null) {
                // was not in DB at prefetch time => likely insert
                accepted++;
            } else {
                // existed already
                if (Objects.equals(existing.getPayloadHash(), incoming.getPayloadHash())) {
                    deduped++;
                } else {
                    // payload differs -> update may happen only if incoming receivedTime newer
                    if (incoming.getReceivedTime().isAfter(existing.getReceivedTime()) && results[i] > 0) {
                        updated++;
                    } else {
                        deduped++;
                    }
                }
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
