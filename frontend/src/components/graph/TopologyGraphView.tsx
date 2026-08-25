import React, { useState } from 'react';
import { TopologyGraph, TopologyNode } from '../../types/api';
import { ZoomIn, ZoomOut, Maximize2, Shield } from 'lucide-react';

interface TopologyGraphViewProps {
  graph: TopologyGraph;
  onSelectNode?: (node: TopologyNode) => void;
  highlightedNodeIds?: string[];
}

export const TopologyGraphView: React.FC<TopologyGraphViewProps> = ({
  graph,
  onSelectNode,
  highlightedNodeIds = [],
}) => {
  const [zoom, setZoom] = useState(1);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);

  // Deterministic 2D layered coordinates based on resourceType
  const getNodeCoordinates = (node: TopologyNode, index: number, total: number) => {
    let layerY = 250;
    if (node.resourceType === 'VPC') layerY = 70;
    else if (node.resourceType === 'SUBNET') layerY = 160;
    else if (node.resourceType === 'EC2_INSTANCE' || node.resourceType === 'RDS_INSTANCE') layerY = 270;
    else if (node.resourceType === 'SECURITY_GROUP') layerY = 360;
    else if (node.resourceType === 'IAM_ROLE') layerY = 440;

    const spacing = 750 / (total + 1);
    const posX = 60 + (index + 1) * spacing;
    return { x: posX, y: layerY };
  };

  const nodePositions = new Map<string, { x: number; y: number }>();
  graph.nodes.forEach((n, idx) => {
    nodePositions.set(n.nodeId, getNodeCoordinates(n, idx, graph.nodes.length));
  });

  const getNodeColor = (type: string) => {
    switch (type) {
      case 'VPC': return '#38bdf8'; // sky
      case 'SUBNET': return '#34d399'; // emerald
      case 'EC2_INSTANCE': return '#fbbf24'; // amber
      case 'RDS_INSTANCE': return '#a78bfa'; // purple
      case 'SECURITY_GROUP': return '#f87171'; // red
      case 'IAM_ROLE': return '#ec4899'; // pink
      default: return '#94a3b8';
    }
  };

  return (
    <div className="relative rounded-xl border border-slate-800 bg-slate-950 overflow-hidden shadow-inner flex flex-col h-[520px]">
      <div className="p-3 border-b border-slate-800/80 bg-slate-900/60 flex items-center justify-between text-xs">
        <div className="flex items-center space-x-2">
          <Shield className="w-4 h-4 text-sky-400" />
          <span className="font-semibold text-slate-200">Interactive Directed Topology Explorer</span>
          <span className="text-slate-500 font-mono">({graph.nodeCount} nodes, {graph.edgeCount} relationships)</span>
        </div>
        <div className="flex items-center space-x-1 bg-slate-950 border border-slate-800 rounded p-1">
          <button
            onClick={() => setZoom((z) => Math.min(z + 0.15, 2))}
            className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200"
            title="Zoom In"
          >
            <ZoomIn className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => setZoom((z) => Math.max(z - 0.15, 0.5))}
            className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200"
            title="Zoom Out"
          >
            <ZoomOut className="w-3.5 h-3.5" />
          </button>
          <button
            onClick={() => setZoom(1)}
            className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200"
            title="Reset View"
          >
            <Maximize2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-auto p-4 flex items-center justify-center bg-radial-grid">
        <svg
          viewBox="0 0 900 500"
          className="w-full h-full max-w-[850px] transition-transform duration-200 ease-out"
          style={{ transform: `scale(${zoom})` }}
        >
          <defs>
            <marker id="arrow" viewBox="0 0 10 10" refX="22" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
              <path d="M 0 0 L 10 5 L 0 10 z" fill="#64748b" />
            </marker>
          </defs>

          {/* Render Edges */}
          {graph.edges.map((edge) => {
            const src = nodePositions.get(edge.sourceNodeId);
            const dst = nodePositions.get(edge.targetNodeId);
            if (!src || !dst) return null;

            return (
              <g key={edge.edgeId}>
                <line
                  x1={src.x}
                  y1={src.y}
                  x2={dst.x}
                  y2={dst.y}
                  stroke="#475569"
                  strokeWidth="1.5"
                  strokeDasharray="4 2"
                  markerEnd="url(#arrow)"
                />
              </g>
            );
          })}

          {/* Render Nodes */}
          {graph.nodes.map((node) => {
            const pos = nodePositions.get(node.nodeId);
            if (!pos) return null;

            const isSelected = selectedNodeId === node.nodeId;
            const isHighlighted = highlightedNodeIds.includes(node.nodeId);
            const color = getNodeColor(node.resourceType);

            return (
              <g
                key={node.nodeId}
                transform={`translate(${pos.x}, ${pos.y})`}
                onClick={() => {
                  setSelectedNodeId(node.nodeId);
                  if (onSelectNode) onSelectNode(node);
                }}
                className="cursor-pointer group"
              >
                <circle
                  r={isSelected || isHighlighted ? 18 : 14}
                  fill="#0f172a"
                  stroke={isSelected || isHighlighted ? '#38bdf8' : color}
                  strokeWidth={isSelected || isHighlighted ? 3 : 2}
                  className="transition-all duration-200"
                />
                <circle r={6} fill={color} />
                <text
                  y={26}
                  textAnchor="middle"
                  fill="#cbd5e1"
                  fontSize="9"
                  fontFamily="monospace"
                  className="select-none font-semibold group-hover:fill-sky-400"
                >
                  {node.resourceId}
                </text>
              </g>
            );
          })}
        </svg>
      </div>
    </div>
  );
};