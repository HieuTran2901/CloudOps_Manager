package com.cloudops.manager.operations.impact.controller;

import com.cloudops.manager.common.api.ApiResponse;
import com.cloudops.manager.operations.impact.model.ImpactAnalysisResult;
import com.cloudops.manager.operations.impact.service.BlastRadiusAnalysisEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/impact")
public class ImpactAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(ImpactAnalysisController.class);

    private final BlastRadiusAnalysisEngine blastRadiusAnalysisEngine;

    public ImpactAnalysisController(@Qualifier("changeImpactBlastRadiusAnalysisEngine") BlastRadiusAnalysisEngine blastRadiusAnalysisEngine) {
        this.blastRadiusAnalysisEngine = blastRadiusAnalysisEngine;
    }

    @GetMapping("/blast-radius")
    public ApiResponse<ImpactAnalysisResult> getBlastRadius(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false, defaultValue = "3") Integer maxDepth
    ) {
        log.info("REST request for blast-radius analysis - resource: {} ({}), region: {}, maxDepth: {}",
                resourceId, resourceType, region, maxDepth);

        ImpactAnalysisResult result = blastRadiusAnalysisEngine.analyzeBlastRadius(
                resourceType, resourceId, region, accountId, maxDepth
        );

        return ApiResponse.success(result, "Blast radius impact analysis completed successfully.");
    }
}
