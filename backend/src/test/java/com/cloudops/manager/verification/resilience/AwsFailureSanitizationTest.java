package com.cloudops.manager.verification.resilience;

import com.cloudops.manager.aws.discovery.controller.AwsResourceDiscoveryController;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.observability.service.AwsObservabilityService;
import com.cloudops.manager.common.exception.AwsAccessDeniedException;
import com.cloudops.manager.common.exception.AwsThrottlingException;
import com.cloudops.manager.common.exception.AwsTimeoutException;
import com.cloudops.manager.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AwsResourceDiscoveryController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("22C.1 — AWS Error Sanitization & Resilience Verification")
class AwsFailureSanitizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AwsResourceDiscoveryService discoveryService;

    @MockBean
    private AwsObservabilityService observabilityService;

    @Test
    @DisplayName("Verify AccessDenied is sanitized into 403 with AWS_ACCESS_DENIED code and zero credential leaks")
    void testAccessDeniedSanitization() throws Exception {
        when(discoveryService.discoverAll(any()))
                .thenThrow(new AwsAccessDeniedException("User arn:aws:iam::123:user/test is not authorized for ec2:DescribeInstances"));

        mockMvc.perform(get("/api/v1/aws/resources").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AWS_ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("The configured AWS identity does not have permission to perform this operation."))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.secretAccessKey").doesNotExist());
    }

    @Test
    @DisplayName("Verify Throttling is sanitized into 429 with AWS_THROTTLED code")
    void testThrottlingSanitization() throws Exception {
        when(discoveryService.discoverAll(any()))
                .thenThrow(new AwsThrottlingException("Request limit exceeded"));

        mockMvc.perform(get("/api/v1/aws/resources").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AWS_THROTTLED"))
                .andExpect(jsonPath("$.message").value("AWS request rate limit exceeded. Please retry later."));
    }

    @Test
    @DisplayName("Verify Timeout is sanitized into 504 with AWS_TIMEOUT code")
    void testTimeoutSanitization() throws Exception {
        when(discoveryService.discoverAll(any()))
                .thenThrow(new AwsTimeoutException("SocketTimeout connecting to AWS endpoint"));

        mockMvc.perform(get("/api/v1/aws/resources").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AWS_TIMEOUT"))
                .andExpect(jsonPath("$.message").value("AWS operation timed out. Please retry later."));
    }
}