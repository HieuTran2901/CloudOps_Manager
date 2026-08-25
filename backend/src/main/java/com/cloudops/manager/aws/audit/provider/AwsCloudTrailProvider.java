package com.cloudops.manager.aws.audit.provider;

import com.cloudops.manager.aws.audit.model.*;
import com.cloudops.manager.common.exception.AwsErrorTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.*;

import java.time.Instant;
import java.util.*;

@Component
public class AwsCloudTrailProvider implements CloudTrailProvider {

    private static final Logger log = LoggerFactory.getLogger(AwsCloudTrailProvider.class);
    private final CloudTrailClient defaultCloudTrailClient;

    public AwsCloudTrailProvider(CloudTrailClient defaultCloudTrailClient) {
        this.defaultCloudTrailClient = defaultCloudTrailClient;
    }

    @Override
    public CloudTrailEventResult lookupEvents(CloudTrailEventLookupRequest request, CloudTrailClient targetClient) {
        log.info("Executing CloudTrail LookupEvents for account: {}, region: {}, eventName: {}",
                request.accountId(), request.region(), request.eventName());

        CloudTrailClient client = targetClient != null ? targetClient : defaultCloudTrailClient;
        int limit = request.maxResults() != null ? request.maxResults() : 50;

        try {
            List<LookupAttribute> attributes = new ArrayList<>();
            if (request.eventName() != null && !request.eventName().isBlank()) {
                attributes.add(LookupAttribute.builder().attributeKey(LookupAttributeKey.EVENT_NAME).attributeValue(request.eventName().trim()).build());
            } else if (request.username() != null && !request.username().isBlank()) {
                attributes.add(LookupAttribute.builder().attributeKey(LookupAttributeKey.USERNAME).attributeValue(request.username().trim()).build());
            } else if (request.resourceName() != null && !request.resourceName().isBlank()) {
                attributes.add(LookupAttribute.builder().attributeKey(LookupAttributeKey.RESOURCE_NAME).attributeValue(request.resourceName().trim()).build());
            } else if (request.resourceType() != null && !request.resourceType().isBlank()) {
                attributes.add(LookupAttribute.builder().attributeKey(LookupAttributeKey.RESOURCE_TYPE).attributeValue(request.resourceType().trim()).build());
            }

            List<CloudTrailEventResource> events = new ArrayList<>();
            String nextToken = null;

            do {
                LookupEventsRequest.Builder reqBuilder = LookupEventsRequest.builder()
                        .startTime(request.startTime())
                        .endTime(request.endTime())
                        .maxResults(Math.min(limit - events.size(), 50))
                        .nextToken(nextToken);

                if (!attributes.isEmpty()) {
                    reqBuilder.lookupAttributes(attributes);
                }

                LookupEventsResponse response = client.lookupEvents(reqBuilder.build());

                for (Event ev : response.events()) {
                    List<CloudTrailEventResourceReference> refs = new ArrayList<>();
                    if (ev.hasResources() && ev.resources() != null) {
                        for (Resource res : ev.resources()) {
                            refs.add(new CloudTrailEventResourceReference(res.resourceType(), res.resourceName()));
                        }
                    }

                    CloudTrailEventIdentity identity = new CloudTrailEventIdentity(
                            ev.username(), ev.username(), request.accountId(), "IAMUser"
                    );

                    Boolean readOnly = ev.readOnly() != null ? ev.readOnly().equalsIgnoreCase("true") : null;

                    events.add(new CloudTrailEventResource(
                            ev.eventId(),
                            ev.eventName(),
                            ev.eventSource(),
                            ev.eventTime(),
                            request.region(),
                            identity,
                            null,
                            null,
                            refs,
                            readOnly,
                            ev.accessKeyId(),
                            "Management"
                    ));

                    if (events.size() >= limit) break;
                }

                nextToken = response.nextToken();
            } while (nextToken != null && !nextToken.isBlank() && events.size() < limit);

            events.sort(Comparator.comparing(CloudTrailEventResource::eventTime, Comparator.nullsLast(Comparator.reverseOrder())));

            return new CloudTrailEventResult(
                    request.accountId(),
                    request.region(),
                    request.startTime(),
                    request.endTime(),
                    events.size(),
                    events,
                    Instant.now()
            );
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("CloudTrail:LookupEvents", e, log);
        }
    }
}