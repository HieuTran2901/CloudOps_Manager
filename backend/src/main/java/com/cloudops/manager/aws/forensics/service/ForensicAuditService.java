package com.cloudops.manager.aws.forensics.service;

import com.cloudops.manager.aws.forensics.aggregator.ForensicEvidenceAggregator;
import com.cloudops.manager.aws.forensics.export.ForensicCsvExporter;
import com.cloudops.manager.aws.forensics.export.ForensicJsonExporter;
import com.cloudops.manager.aws.forensics.model.ForensicEvidenceItem;
import com.cloudops.manager.aws.forensics.model.ForensicExportResult;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForensicAuditService {

    private final ForensicEvidenceAggregator aggregator;
    private final ForensicJsonExporter jsonExporter;
    private final ForensicCsvExporter csvExporter;
    private final AwsIdentityService identityService;

    @Value("${cloudops.aws.region:us-east-1}")
    private String defaultRegion;

    public ForensicAuditService(
            ForensicEvidenceAggregator aggregator,
            ForensicJsonExporter jsonExporter,
            ForensicCsvExporter csvExporter,
            AwsIdentityService identityService) {
        this.aggregator = aggregator;
        this.jsonExporter = jsonExporter;
        this.csvExporter = csvExporter;
        this.identityService = identityService;
    }

    public ForensicExportResult exportForensics(String format, String optionalRegion) {
        String region = resolveRegion(optionalRegion);
        String accountId = identityService.getCurrentIdentity().accountId();

        List<ForensicEvidenceItem> items = aggregator.aggregate(accountId, region);
        return formatExport(items, accountId, region, format);
    }

    public ForensicExportResult exportCrossAccountForensics(AwsAccountTarget target, String format) {
        String region = resolveRegion(target.region());
        String accountId = target.accountId();

        List<ForensicEvidenceItem> items = aggregator.aggregate(accountId, region);
        return formatExport(items, accountId, region, format);
    }

    private ForensicExportResult formatExport(
            List<ForensicEvidenceItem> items, String accountId, String region, String format) {
        if ("csv".equalsIgnoreCase(format)) {
            return csvExporter.export(items, accountId, region);
        }
        return jsonExporter.export(items, accountId, region);
    }

    private String resolveRegion(String optionalRegion) {
        return (optionalRegion != null && !optionalRegion.isBlank()) ? optionalRegion.trim() : defaultRegion;
    }
}