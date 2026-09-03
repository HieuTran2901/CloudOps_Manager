package com.cloudops.manager.aws.quota.controller;

import com.cloudops.manager.aws.quota.model.QuotaUtilizationReport;
import com.cloudops.manager.aws.quota.model.ServiceQuotaItem;
import com.cloudops.manager.aws.quota.service.ServiceQuotasService;
import com.cloudops.manager.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quotas")
public class ServiceQuotasController {

    private static final Logger log = LoggerFactory.getLogger(ServiceQuotasController.class);

    private final ServiceQuotasService serviceQuotasService;

    public ServiceQuotasController(ServiceQuotasService serviceQuotasService) {
        this.serviceQuotasService = serviceQuotasService;
    }

    @GetMapping
    public ApiResponse<QuotaUtilizationReport> getQuotaReport(
            @RequestParam(required = false) String region
    ) {
        log.info("REST request to get AWS Service Quotas utilization report for region: {}", region);
        QuotaUtilizationReport report = serviceQuotasService.getQuotaUtilizationReport(region);
        return ApiResponse.success(report, "AWS Service Quotas report retrieved successfully.");
    }

    @GetMapping("/{serviceCode}")
    public ApiResponse<List<ServiceQuotaItem>> getServiceQuotas(
            @PathVariable String serviceCode,
            @RequestParam(required = false) String region
    ) {
        log.info("REST request to get AWS Service Quotas for service: {}, region: {}", serviceCode, region);
        List<ServiceQuotaItem> items = serviceQuotasService.getQuotasForService(serviceCode, region);
        return ApiResponse.success(items, "Service quotas retrieved successfully for " + serviceCode);
    }
}
