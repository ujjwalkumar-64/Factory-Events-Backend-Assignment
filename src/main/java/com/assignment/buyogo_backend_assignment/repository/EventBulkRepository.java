package com.assignment.buyogo_backend_assignment.repository;

import com.assignment.buyogo_backend_assignment.entity.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    // returns array of update counts
    public int[] bulkUpsert(List<Event> events) {
        String sql = """
            INSERT INTO events(event_id, event_time, received_time, machine_id, duration_ms, defect_count, factory_id, line_id, payload_hash)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
        """;

        return jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Event e = events.get(i);
                ps.setString(1, e.getEventId());
                ps.setTimestamp(2, Timestamp.from(e.getEventTime()));
                ps.setTimestamp(3, Timestamp.from(e.getReceivedTime()));
                ps.setString(4, e.getMachineId());
                ps.setLong(5, e.getDurationMs());
                ps.setInt(6, e.getDefectCount());
                ps.setString(7, e.getFactoryId());
                ps.setString(8, e.getLineId());
                ps.setString(9, e.getPayloadHash());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        });

    }
}