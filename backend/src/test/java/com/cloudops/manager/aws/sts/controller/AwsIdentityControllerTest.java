package com.cloudops.manager.aws.sts.controller;

import com.cloudops.manager.aws.sts.exception.AwsAuthenticationException;
import com.cloudops.manager.aws.sts.exception.AwsIdentityException;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AwsIdentityController.class)
@Import(GlobalExceptionHandler.class)
class AwsIdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AwsIdentityService awsIdentityService;

    @Test
    @DisplayName("GET /api/v1/aws/identity should return 200 OK with caller identity")
    void shouldReturnCallerIdentity() throws Exception {
        CallerIdentity identity = new CallerIdentity(
                "998877665544",
                "arn:aws:iam::998877665544:user/developer",
                "AIDAIEXAMPLE"
        );

        when(awsIdentityService.getCurrentIdentity()).thenReturn(identity);

        mockMvc.perform(get("/api/v1/aws/identity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("998877665544"))
                .andExpect(jsonPath("$.data.arn").value("arn:aws:iam::998877665544:user/developer"))
                .andExpect(jsonPath("$.data.userId").value("AIDAIEXAMPLE"))
                .andExpect(jsonPath("$.data.accessKeyId").doesNotExist())
                .andExpect(jsonPath("$.data.secretAccessKey").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/aws/identity should return 401 UNAUTHORIZED on authentication failure with sanitized message")
    void shouldReturn401OnAuthFailure() throws Exception {
        when(awsIdentityService.getCurrentIdentity())
                .thenThrow(new AwsAuthenticationException("Token expired"));

        mockMvc.perform(get("/api/v1/aws/identity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AWS_AUTH_FAILED"))
                .andExpect(jsonPath("$.message").value("AWS credentials unavailable, expired, or invalid."));
    }

    @Test
    @DisplayName("GET /api/v1/aws/identity should return 503 SERVICE_UNAVAILABLE on AWS STS service error with sanitized message")
    void shouldReturn503OnIdentityError() throws Exception {
        when(awsIdentityService.getCurrentIdentity())
                .thenThrow(new AwsIdentityException("STS service temporarily unreachable"));

        mockMvc.perform(get("/api/v1/aws/identity")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AWS_IDENTITY_ERROR"))
                .andExpect(jsonPath("$.message").value("Failed to resolve AWS identity or connect to AWS STS."));
    }
}