package com.assignment.buyogo_backend_assignment.service.serviceImpl;

import com.assignment.buyogo_backend_assignment.repository.EventRepository;
import com.assignment.buyogo_backend_assignment.request.StatsRequest;
import com.assignment.buyogo_backend_assignment.response.DefectLineResponse;
import com.assignment.buyogo_backend_assignment.response.StatsResponse;
import com.assignment.buyogo_backend_assignment.response.Status;
import com.assignment.buyogo_backend_assignment.service.StatsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public List<DefectLineResponse> getDefectsLine(String factoryId, Instant from, Instant to, int limit){
        List<Object []>  results= eventRepository
                .findTopDefectLinesByFactoryIdAndEventTimeBetween(factoryId, from, to);

        return results
                .stream()
                .limit(limit)
                .map(row ->{
                    String lineId = row[0].toString();
                    long totalDefects= ((Number)row[1]).longValue();
                    long eventsCount = ((Number)row[2]).longValue();

                    double defectPercentage= eventsCount >0
                            ? BigDecimal.valueOf(totalDefects)
                                .divide(BigDecimal.valueOf(eventsCount),4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2,RoundingMode.HALF_UP)
                                .doubleValue()

                            : 0.0;

                     return DefectLineResponse.builder()
                            .defectsPercent(defectPercentage)
                            .totalDefects(totalDefects)
                            .eventCount(eventsCount)
                            .lineId(lineId)
                            .build();

                })
                .collect(Collectors.toList());
    }

}
