package com.cloudops.manager.aws.forensics.controller;

import com.cloudops.manager.aws.forensics.model.ForensicExportResult;
import com.cloudops.manager.aws.forensics.service.ForensicAuditService;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/aws/forensics")
public class ForensicAuditController {

    private final ForensicAuditService auditService;

    public ForensicAuditController(ForensicAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportForensics(
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(required = false) String region) {

        ForensicExportResult result = auditService.exportForensics(format, region);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .header("X-Forensic-SHA256-Digest", result.sha256Digest())
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }

    @GetMapping("/accounts/{accountId}/export")
    public ResponseEntity<String> exportCrossAccountForensics(
            @PathVariable String accountId,
            @RequestParam String roleArn,
            @RequestParam(required = false) String roleSessionName,
            @RequestParam(required = false) String externalId,
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(required = false) String region) {

        AwsAccountTarget target = new AwsAccountTarget(accountId, roleArn, roleSessionName, externalId, region);
        ForensicExportResult result = auditService.exportCrossAccountForensics(target, format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .header("X-Forensic-SHA256-Digest", result.sha256Digest())
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }
}