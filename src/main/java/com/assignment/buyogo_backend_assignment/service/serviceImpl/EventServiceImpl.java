package com.assignment.buyogo_backend_assignment.service.serviceImpl;

import com.assignment.buyogo_backend_assignment.entity.Event;
import com.assignment.buyogo_backend_assignment.exception.ValidationException;
import com.assignment.buyogo_backend_assignment.repository.EventRepository;
import com.assignment.buyogo_backend_assignment.request.EventRequest;
import com.assignment.buyogo_backend_assignment.request.StatsRequest;
import com.assignment.buyogo_backend_assignment.response.BatchResponse;
import com.assignment.buyogo_backend_assignment.response.RejectionDetail;
import com.assignment.buyogo_backend_assignment.response.StatsResponse;
import com.assignment.buyogo_backend_assignment.response.Status;
import com.assignment.buyogo_backend_assignment.service.EventService;

import com.assignment.buyogo_backend_assignment.util.EventPayloadHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor

public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private static final long MAX_DURATION_MS = 21_600_000L; // 6 hours
    private static final long MAX_FUTURE_MINUTES = 15;
    private static final double HEALTHY_DEFECT_RATE_THRESHOLD = 2.0;

    @Override
    @Transactional
    public BatchResponse processBatchEvents(List<EventRequest> eventRequests) {
        int accepted = 0;
        int deduped = 0;
        int updated = 0;
        int rejected = 0;
        List<RejectionDetail> rejections = new ArrayList<>();

        for( EventRequest eventRequest : eventRequests  ){
            try{
                validateEvent(eventRequest);
                Instant receivedTime = Instant.now();
                String payloadHash= EventPayloadHashUtil.computeHash(eventRequest);

                Optional<Event> existingEventCheck = eventRepository.findByEventId(eventRequest.eventId());
                if(existingEventCheck.isPresent()){
                     Event existingEvent = existingEventCheck.get();
                    if(existingEvent.getPayloadHash().equals(payloadHash)){
                        // same payload i.e duplicate
                        deduped++;
                    }
                    else{
                        // different payload of same event
                        if(receivedTime.isAfter(existingEvent.getReceivedTime())){
                            updateEvent(existingEvent, eventRequest, receivedTime, payloadHash);
                            updated++;
                        }
                        else{
                            // older event -- so ignore and count deduped
                            deduped++;
                        }
                    }
                }
                else{
                    // new event
                    Event event=  Event.builder()
                            .eventId(eventRequest.eventId())
                            .eventTime(eventRequest.eventTime())
                            .payloadHash(payloadHash)
                            .durationMs(eventRequest.durationMs())
                            .receivedTime(receivedTime)
                            .machineId(eventRequest.machineId())
                            .defectCount(eventRequest.defectCount())
                            .lineId(eventRequest.lineId())
                            .factoryId(eventRequest.factoryId())
                            .build();

                    eventRepository.save(event);
                    accepted++;


                }


            }
            catch (ValidationException e){
                rejected++;
                rejections.add(RejectionDetail
                        .builder()
                                .eventId(eventRequest.eventId())
                                .reason(e.getMessage())
                        .build()
                );
            }
            catch (DataIntegrityViolationException e){
                // Handle race condition where another thread inserted the same eventId
                deduped++;
            }
            catch( Exception e ){
                rejected++;
                rejections.add(RejectionDetail
                        .builder()
                                .eventId(eventRequest.eventId())
                                .reason(e.getMessage())
                        .build()
                );
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

    @Override
    public StatsResponse getStats(StatsRequest statsRequest){
        long eventsCount = eventRepository.countByMachineIdAndEventTimeBetween(
                statsRequest.machineId(),
                statsRequest.start(),
                statsRequest.end()
        );

        long defectsCount = eventRepository.sumDefectsByMachineIdAndEventTimeBetween(
                statsRequest.machineId(),
                statsRequest.start(),
                statsRequest.end()
        );

        double windowHours = Duration.between(statsRequest.start(), statsRequest.end()).toSeconds() / 3600.0;
        double avgDefectRate= windowHours >0 ? defectsCount/windowHours : 0.0;

        Status status =  avgDefectRate < HEALTHY_DEFECT_RATE_THRESHOLD ? Status.Healthy :Status.Warning;


        return StatsResponse.builder()
                .eventsCount(eventsCount)
                .defectsCount(defectsCount)
                .avgDefectRate(avgDefectRate)
                .end(statsRequest.end())
                .start(statsRequest.start())
                .machineId(statsRequest.machineId())
                .status(status)
                .build();
    }




    private void validateEvent(EventRequest eventRequest){

        // reject if eventTime is > 15 minutes in the future
        if(eventRequest.eventTime().isAfter(Instant.now().plus(Duration.ofMinutes(MAX_DURATION_MS)))){
            throw new ValidationException(
                    String.format("eventTime is more than %d minutes in the future", MAX_FUTURE_MINUTES)
            );
        }

        // reject if durationMs < 0 or durationMs > 6 hours
        if(eventRequest.durationMs() > MAX_DURATION_MS  || eventRequest.durationMs() <0){
            throw new ValidationException(
                    String.format("Invalid durationMs: must be between 0 and %d", MAX_DURATION_MS)
            );
        }
    }

    private void updateEvent(Event existingEvent, EventRequest eventRequest, Instant receivedTime, String payloadHash){
        existingEvent.setReceivedTime(receivedTime);
        existingEvent.setPayloadHash(payloadHash);
        existingEvent.setDurationMs(eventRequest.durationMs());
        existingEvent.setEventTime(eventRequest.eventTime());
        existingEvent.setDefectCount(eventRequest.defectCount());
        existingEvent.setMachineId(eventRequest.machineId());
        existingEvent.setLineId(eventRequest.lineId());
        existingEvent.setFactoryId(eventRequest.factoryId());
        eventRepository.save(existingEvent);

    }
}


