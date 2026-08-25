import React, { useEffect, useState } from 'react';
import { cloudOpsApi } from '../../api';
import { LateralMovementResult } from '../../types/api';
import { Network } from 'lucide-react';

interface LateralMovementPanelProps {
  onError: (err: string) => void;
}

export const LateralMovementPanel: React.FC<LateralMovementPanelProps> = ({ onError }) => {
  const [results, setResults] = useState<LateralMovementResult[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    cloudOpsApi
      .getLateralMovement(3)
      .then((data) => {
        setResults(data);
        setLoading(false);
      })
      .catch((err: Error) => {
        onError(err.message);
        setLoading(false);
      });
  }, [onError]);

  if (loading) {
    return (
      <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/40 text-xs text-slate-400">
        Scanning lateral movement propagation vectors...
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 space-y-3">
      <div className="flex items-center space-x-2 text-sm font-semibold text-slate-200">
        <Network className="w-4 h-4 text-purple-400" />
        <span>Verified Lateral Movement Propagation Paths ({results.length})</span>
      </div>

      {results.length === 0 ? (
        <p className="text-xs text-slate-500 font-mono">
          No lateral movement propagation chains detected across the discovered topology.
        </p>
      ) : (
        <div className="space-y-2 max-h-48 overflow-y-auto font-mono text-xs">
          {results.map((res, idx) => (
            <div key={idx} className="p-2.5 rounded bg-slate-950 border border-slate-800/80 flex items-center justify-between">
              <div>
                <span className="text-slate-300 block font-semibold">{res.sourceNodeId}</span>
                <span className="text-purple-400 text-[11px]">âž” Reachable Target: {res.targetNodeId}</span>
              </div>
              <span className="px-2 py-0.5 rounded bg-purple-950 text-purple-300 border border-purple-800 text-[10px]">
                {res.status}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};