package com.cloudops.manager.operations.service;

import com.cloudops.manager.operations.model.OperationalEvent;
import com.cloudops.manager.operations.model.OperationalSeverity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class OperationalEventBuffer {

    private static final int MAX_BUFFER_SIZE = 50;
    private final ConcurrentLinkedDeque<OperationalEvent> buffer = new ConcurrentLinkedDeque<>();

    public OperationalEventBuffer() {
        recordEvent("APPLICATION_STARTUP", OperationalSeverity.INFO, "CloudOps Manager operational event buffer initialized.", "SYSTEM", Map.of());
    }

    public OperationalEvent recordEvent(
            String eventType,
            OperationalSeverity severity,
            String message,
            String sourceSubsystem,
            Map<String, String> details) {

        OperationalEvent event = new OperationalEvent(
                "evt-" + UUID.randomUUID().toString().substring(0, 8),
                Instant.now(),
                eventType != null ? eventType : "GENERAL_EVENT",
                severity != null ? severity : OperationalSeverity.INFO,
                message != null ? message : "",
                sourceSubsystem != null ? sourceSubsystem : "UNKNOWN",
                details != null ? Map.copyOf(details) : Map.of()
        );

        buffer.addFirst(event);
        while (buffer.size() > MAX_BUFFER_SIZE) {
            buffer.pollLast();
        }

        return event;
    }

    public List<OperationalEvent> getRecentEvents() {
        return Collections.unmodifiableList(new ArrayList<>(buffer));
    }
}