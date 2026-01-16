package com.assignment.buyogo_backend_assignment.service.serviceImpl;

import com.assignment.buyogo_backend_assignment.repository.EventRepository;
import com.assignment.buyogo_backend_assignment.request.StatsRequest;
import com.assignment.buyogo_backend_assignment.response.StatsResponse;
import com.assignment.buyogo_backend_assignment.response.Status;
import com.assignment.buyogo_backend_assignment.service.StatsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@AllArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final EventRepository eventRepository;
    private static final double HEALTHY_DEFECT_RATE_THRESHOLD = 2.0;

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

}
