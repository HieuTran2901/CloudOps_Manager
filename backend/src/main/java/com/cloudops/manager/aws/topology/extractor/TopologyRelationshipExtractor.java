package com.cloudops.manager.aws.topology.extractor;

import com.cloudops.manager.aws.topology.model.TopologyContext;
import com.cloudops.manager.aws.topology.model.TopologyEdge;

import java.util.List;

public interface TopologyRelationshipExtractor {
    List<TopologyEdge> extract(TopologyContext context);
}