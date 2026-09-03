import React, { useEffect, useState } from 'react';
import { cloudOpsApi } from '../api';
import { TopologyGraph, TopologyNode, TopologyEdge } from '../types/api';
import { useRegion } from '../context/RegionContext';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorBanner } from '../components/feedback/ErrorBanner';
import { TopologyGraphView } from '../components/graph/TopologyGraphView';

export const TopologyPage: React.FC = () => {
  const { currentRegion } = useRegion();
  const [graph, setGraph] = useState<TopologyGraph | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedNode, setSelectedNode] = useState<TopologyNode | null>(null);
  const [highlightedNeighbors, setHighlightedNeighbors] = useState<string[]>([]);

  useEffect(() => {
    setLoading(true);
    setError(null);
    cloudOpsApi
      .getTopology(currentRegion)
      .then((data) => {
        setGraph(data);
        setLoading(false);
      })
      .catch((err: Error) => {
        setError(err.message);
        setLoading(false);
      });
  }, [currentRegion]);

  const handleSelectNode = (node: TopologyNode) => {
    setSelectedNode(node);
    if (graph) {
      const neighborIds = graph.edges
        .filter((e) => e.sourceNodeId === node.nodeId || e.targetNodeId === node.nodeId)
        .map((e) => (e.sourceNodeId === node.nodeId ? e.targetNodeId : e.sourceNodeId));
      setHighlightedNeighbors([node.nodeId, ...neighborIds]);
    }
  };

  const getConnectedEdges = (nodeId: string): TopologyEdge[] => {
    if (!graph) return [];
    return graph.edges.filter((e) => e.sourceNodeId === nodeId || e.targetNodeId === nodeId);
  };

  const connectedEdges = selectedNode ? getConnectedEdges(selectedNode.nodeId) : [];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold tracking-tight text-slate-100">Infrastructure Topology Graph Explorer</h2>
        <p className="text-xs text-slate-400 mt-1">
          Deterministic directed relationship graph connecting EC2, RDS, VPC, Subnets, and Security Groups.
        </p>
      </div>

      {loading && <LoadingSpinner message="Constructing live topology graph from AWS evidence..." />}
      {error && <ErrorBanner message={error} errorCode="TOPOLOGY_EVIDENCE_STATUS" />}

      {!loading && !error && graph && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <TopologyGraphView
              graph={graph}
              onSelectNode={handleSelectNode}
              highlightedNodeIds={highlightedNeighbors}
            />
          </div>

          <div className="space-y-4">
            <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/60 space-y-3">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-300 flex items-center justify-between">
                <span>Node Inspector</span>
                {selectedNode && (
                  <span className="text-[10px] text-sky-400 font-mono">
                    {connectedEdges.length} Neighbors
                  </span>
                )}
              </h3>

              {selectedNode ? (
                <div className="space-y-3 text-xs font-mono">
                  <div>
                    <span className="text-slate-500 block text-[11px]">Node ID:</span>
                    <span className="text-slate-200 break-all select-all">{selectedNode.nodeId}</span>
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <span className="text-slate-500 block text-[11px]">Type:</span>
                      <span className="text-sky-400 font-semibold">{selectedNode.resourceType}</span>
                    </div>
                    <div>
                      <span className="text-slate-500 block text-[11px]">Region:</span>
                      <span className="text-slate-300">{selectedNode.region}</span>
                    </div>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[11px]">Resource ID:</span>
                    <span className="text-slate-300">{selectedNode.resourceId}</span>
                  </div>

                  {/* Connected Relationships */}
                  {connectedEdges.length > 0 && (
                    <div className="space-y-1.5 pt-2 border-t border-slate-800">
                      <span className="text-slate-400 block text-[11px] font-semibold">
                        Connected Relationships ({connectedEdges.length}):
                      </span>
                      <div className="space-y-1 max-h-36 overflow-y-auto">
                        {connectedEdges.map((e) => (
                          <div key={e.edgeId} className="p-1.5 rounded bg-slate-950 border border-slate-800/80 text-[10px]">
                            <span className="text-purple-400 font-semibold">{e.relationshipType}</span>
                            <span className="text-slate-500 block truncate">
                              {e.sourceNodeId === selectedNode.nodeId ? `âž” ${e.targetNodeId}` : `â¬… from ${e.sourceNodeId}`}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Attributes */}
                  {Object.keys(selectedNode.attributes).length > 0 && (
                    <div className="p-2.5 rounded bg-slate-950 border border-slate-800 space-y-1">
                      <span className="text-slate-500 block text-[10px] font-sans font-semibold uppercase">Observed Attributes</span>
                      {Object.entries(selectedNode.attributes).map(([k, v]) => (
                        <div key={k} className="flex justify-between py-0.5 text-[11px]">
                          <span className="text-slate-500">{k}:</span>
                          <span className="text-slate-300 truncate max-w-[140px]">{String(v)}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ) : (
                <p className="text-xs text-slate-500">Select any graph node on the map to inspect its relationships and facts.</p>
              )}
            </div>

            <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/40 text-xs text-slate-400 space-y-2">
              <span className="font-semibold text-slate-300 block">Graph Legend</span>
              <div className="grid grid-cols-2 gap-2 text-[11px]">
                <span className="flex items-center space-x-1.5"><span className="w-2.5 h-2.5 rounded-full bg-sky-400"></span><span>VPC</span></span>
                <span className="flex items-center space-x-1.5"><span className="w-2.5 h-2.5 rounded-full bg-emerald-400"></span><span>Subnet</span></span>
                <span className="flex items-center space-x-1.5"><span className="w-2.5 h-2.5 rounded-full bg-amber-400"></span><span>EC2</span></span>
                <span className="flex items-center space-x-1.5"><span className="w-2.5 h-2.5 rounded-full bg-purple-400"></span><span>RDS</span></span>
                <span className="flex items-center space-x-1.5"><span className="w-2.5 h-2.5 rounded-full bg-rose-400"></span><span>Security Group</span></span>
                <span className="flex items-center space-x-1.5"><span className="w-2.5 h-2.5 rounded-full bg-pink-400"></span><span>IAM Role</span></span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};