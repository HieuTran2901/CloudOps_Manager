package com.cloudops.manager.operations.risk.controller;

import com.cloudops.manager.common.api.ApiResponse;
import com.cloudops.manager.operations.risk.model.RiskAssessmentReport;
import com.cloudops.manager.operations.risk.service.RiskAssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risks")
public class RiskAssessmentController {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentController.class);

    private final RiskAssessmentService riskAssessmentService;

    public RiskAssessmentController(RiskAssessmentService riskAssessmentService) {
        this.riskAssessmentService = riskAssessmentService;
    }

    @GetMapping
    public ApiResponse<RiskAssessmentReport> getOperationalRisks(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity
    ) {
        log.info("REST request to get operational risks - region: {}, category: {}, severity: {}", region, category, severity);
        RiskAssessmentReport report = riskAssessmentService.getRiskAssessment(region, category, severity);
        return ApiResponse.success(report, "Operational risk assessment retrieved successfully.");
    }
}
