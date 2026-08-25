import React, { useState } from 'react';
import { cloudOpsApi } from '../../api';
import { BlastRadiusResult } from '../../types/api';
import { Compass } from 'lucide-react';

interface BlastRadiusPanelProps {
  defaultNodeId: string;
  onError: (err: string) => void;
}

export const BlastRadiusPanel: React.FC<BlastRadiusPanelProps> = ({ defaultNodeId, onError }) => {
  const [blastNodeId, setBlastNodeId] = useState(defaultNodeId);
  const [blastDepth, setBlastDepth] = useState(3);
  const [blastResult, setBlastResult] = useState<BlastRadiusResult | null>(null);
  const [blastLoading, setBlastLoading] = useState(false);

  const handleBlastSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!blastNodeId) return;
    setBlastLoading(true);
    cloudOpsApi
      .getBlastRadius(blastNodeId, blastDepth)
      .then((res) => {
        setBlastResult(res);
        setBlastLoading(false);
      })
      .catch((err: Error) => {
        onError(err.message);
        setBlastLoading(false);
      });
  };

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 space-y-4">
      <div className="flex items-center space-x-2 text-sm font-semibold text-slate-200">
        <Compass className="w-4 h-4 text-sky-400" />
        <span>Blast-Radius Engine</span>
      </div>

      <form onSubmit={handleBlastSubmit} className="space-y-3 text-xs">
        <div>
          <label className="text-slate-400 block mb-1">Source Node ID</label>
          <input
            type="text"
            value={blastNodeId}
            onChange={(e) => setBlastNodeId(e.target.value)}
            placeholder="123456789012:us-east-1:EC2_INSTANCE:i-xxx"
            className="w-full rounded bg-slate-950 border border-slate-800 px-3 py-2 text-slate-200 font-mono focus:outline-none focus:border-sky-500"
          />
        </div>
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <span className="text-slate-400">Max Depth:</span>
            <select
              value={blastDepth}
              onChange={(e) => setBlastDepth(Number(e.target.value))}
              className="bg-slate-950 border border-slate-800 rounded px-2 py-1 text-slate-200 font-mono"
            >
              {[1, 2, 3, 4, 5].map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </select>
          </div>
          <button
            type="submit"
            disabled={blastLoading}
            className="px-4 py-2 rounded bg-sky-600 hover:bg-sky-500 text-white font-medium"
          >
            {blastLoading ? 'Analyzing...' : 'Calculate Blast Radius'}
          </button>
        </div>
      </form>

      {blastResult && (
        <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 space-y-2 text-xs">
          <div className="flex justify-between font-mono text-[11px] text-slate-400">
            <span>Reachable Nodes: {blastResult.traversedNodeCount}</span>
            <span>Traversed Edges: {blastResult.traversedEdgeCount}</span>
          </div>
          <div className="space-y-1 font-mono text-[11px] max-h-36 overflow-y-auto">
            {blastResult.reachableNodes.map((n) => (
              <div key={n.nodeId} className="p-1.5 rounded bg-slate-900/80 flex justify-between">
                <span className="text-slate-300">{n.resourceId}</span>
                <span className="text-sky-400">{n.resourceType}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};