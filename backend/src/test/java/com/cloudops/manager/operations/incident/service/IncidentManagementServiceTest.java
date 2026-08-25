package com.cloudops.manager.operations.incident.service;

import com.cloudops.manager.operations.incident.model.IncidentRecord;
import com.cloudops.manager.operations.incident.model.IncidentSeverity;
import com.cloudops.manager.operations.incident.model.IncidentStatus;
import com.cloudops.manager.operations.incident.model.IncidentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IncidentManagementServiceTest {

    private IncidentManagementService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentManagementService();
    }

    @Test
    @DisplayName("Correlates repeated identical operational failures into a single incident with incremented occurrences")
    void testIncidentCorrelation() {
        IncidentRecord first = incidentService.recordOrUpdateIncident(
                IncidentType.AWS_ACCESS_DENIED,
                IncidentSeverity.CRITICAL,
                "351405419700",
                "ap-southeast-2",
                "ECR DescribeRepositories denied",
                "ECR",
                "BLOCKED",
                Map.of("code", "AccessDenied")
        );

        assertNotNull(first);
        assertEquals(1, first.occurrenceCount());
        assertEquals(IncidentStatus.OPEN, first.status());

        IncidentRecord second = incidentService.recordOrUpdateIncident(
                IncidentType.AWS_ACCESS_DENIED,
                IncidentSeverity.CRITICAL,
                "351405419700",
                "ap-southeast-2",
                "ECR DescribeRepositories denied",
                "ECR",
                "BLOCKED",
                Map.of("code", "AccessDenied")
        );

        assertEquals(first.incidentId(), second.incidentId(), "Incident ID must correlate for identical operational failure");
        assertEquals(2, second.occurrenceCount());

        List<IncidentRecord> active = incidentService.getActiveIncidents();
        assertEquals(1, active.size());
    }

    @Test
    @DisplayName("Transitions incident state to RECOVERING and RESOLVED upon verified recovery")
    void testIncidentRecoveryTransition() {
        IncidentRecord inc = incidentService.recordOrUpdateIncident(
                IncidentType.AWS_TIMEOUT,
                IncidentSeverity.WARNING,
                "351405419700",
                "ap-southeast-2",
                "CloudWatch timeout",
                "CloudWatch",
                "DEGRADED",
                Map.of()
        );

        assertTrue(incidentService.markRecovering(inc.incidentId()).isPresent());
        assertEquals(IncidentStatus.RECOVERING, incidentService.getAllIncidents().get(0).status());

        assertTrue(incidentService.resolveIncident(inc.incidentId()).isPresent());
        assertEquals(IncidentStatus.RESOLVED, incidentService.getAllIncidents().get(0).status());
        assertTrue(incidentService.getActiveIncidents().isEmpty(), "Resolved incident should not appear in active list");
    }
}