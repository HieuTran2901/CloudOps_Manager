package com.cloudops.manager.operations.service;

import com.cloudops.manager.operations.model.OperationalEvent;
import com.cloudops.manager.operations.model.OperationalSeverity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OperationalEventBufferTest {

    @Test
    @DisplayName("Verify event buffer records events and limits capacity to max 50 items")
    void testEventBufferCapacityAndOrdering() {
        OperationalEventBuffer buffer = new OperationalEventBuffer();

        for (int i = 1; i <= 60; i++) {
            buffer.recordEvent("TEST_EVT_" + i, OperationalSeverity.INFO, "Message " + i, "TEST", Map.of("index", String.valueOf(i)));
        }

        List<OperationalEvent> events = buffer.getRecentEvents();
        assertNotNull(events);
        assertTrue(events.size() <= 50, "Buffer must not exceed 50 items");
        // Most recent event must be first
        assertEquals("TEST_EVT_60", events.get(0).eventType());
    }
}