package com.assignment.buyogo_backend_assignment.repository;

import com.assignment.buyogo_backend_assignment.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByEventId(String eventId);
    List<Event> findByEventIdIn(List<String> eventIds);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.machineId = :machineId " +
            "AND e.eventTime >= :start AND e.eventTime < :end")
    long countByMachineIdAndEventTimeBetween(
            @Param("machineId") String machineId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(e.defectCount), 0) FROM Event e WHERE e.machineId = :machineId " +
            "AND e.eventTime >= :start AND e.eventTime < :end AND e.defectCount >= 0")
    long sumDefectsByMachineIdAndEventTimeBetween(
            @Param("machineId") String machineId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT e.lineId as lineId, COALESCE(SUM(e.defectCount), 0) as totalDefects, " +
            "COUNT(e) as eventCount FROM Event e " +
            "WHERE e.factoryId = :factoryId AND e.eventTime >= :from AND e.eventTime < :to " +
            "AND e.defectCount >= 0 AND e.lineId IS NOT NULL " +
            "GROUP BY e.lineId ORDER BY totalDefects DESC")
    List<Object[]> findTopDefectLinesByFactoryIdAndEventTimeBetween(
            @Param("factoryId") String factoryId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

}
