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

    @Modifying
    @Query(value = """
        INSERT INTO events(event_id, event_time, received_time, machine_id, duration_ms, defect_count, factory_id, line_id, payload_hash)
        VALUES (:eventId, :eventTime, :receivedTime, :machineId, :durationMs, :defectCount, :factoryId, :lineId, :payloadHash)
        ON CONFLICT (event_id)
        DO UPDATE SET
          event_time = EXCLUDED.event_time,
          received_time = EXCLUDED.received_time,
          machine_id = EXCLUDED.machine_id,
          duration_ms = EXCLUDED.duration_ms,
          defect_count = EXCLUDED.defect_count,
          factory_id = EXCLUDED.factory_id,
          line_id = EXCLUDED.line_id,
          payload_hash = EXCLUDED.payload_hash
        WHERE
          events.payload_hash <> EXCLUDED.payload_hash
          AND EXCLUDED.received_time > events.received_time
        """, nativeQuery = true)
    int upsertEvent(String eventId,
                    Instant eventTime,
                    Instant receivedTime,
                    String machineId,
                    Long durationMs,
                    Integer defectCount,
                    String factoryId,
                    String lineId,
                    String payloadHash);

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
