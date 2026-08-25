package com.cloudops.manager.aws.discovery;

import com.cloudops.manager.aws.discovery.model.InventorySummary;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("integration")
class AwsResourceDiscoveryIntegrationTest {

    @Autowired
    private AwsResourceDiscoveryService discoveryService;

    @Test
    @DisplayName("Integration: Should perform live read-only AWS resource discovery across all services")
    void shouldPerformLiveResourceDiscovery() {
        InventorySummary summary = discoveryService.discoverAll(null);

        assertThat(summary).isNotNull();
        assertThat(summary.accountId()).matches("^[0-9]{12}$");
        assertThat(summary.region()).isNotBlank();
        assertThat(summary.countByType()).isNotEmpty();
    }
}