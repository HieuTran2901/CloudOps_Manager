package com.cloudops.manager.aws.audit.provider;

import com.cloudops.manager.aws.audit.model.CloudTrailEventLookupRequest;
import com.cloudops.manager.aws.audit.model.CloudTrailEventResult;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;

public interface CloudTrailProvider {
    CloudTrailEventResult lookupEvents(CloudTrailEventLookupRequest request, CloudTrailClient client);
}