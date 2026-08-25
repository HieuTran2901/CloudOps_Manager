import React, { useState } from 'react';
import { cloudOpsApi } from '../../api';
import { SecurityReachabilityResult } from '../../types/api';
import { StatusBadge } from '../ui/StatusBadge';
import { Search } from 'lucide-react';

interface ReachabilityPanelProps {
  onError: (err: string) => void;
}

export const ReachabilityPanel: React.FC<ReachabilityPanelProps> = ({ onError }) => {
  const [reachFrom, setReachFrom] = useState('');
  const [reachTo, setReachTo] = useState('');
  const [reachResult, setReachResult] = useState<SecurityReachabilityResult | null>(null);
  const [reachLoading, setReachLoading] = useState(false);

  const handleReachSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!reachFrom || !reachTo) return;
    setReachLoading(true);
    cloudOpsApi
      .getReachability(reachFrom, reachTo)
      .then((res) => {
        setReachResult(res);
        setReachLoading(false);
      })
      .catch((err: Error) => {
        onError(err.message);
        setReachLoading(false);
      });
  };

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 space-y-4">
      <div className="flex items-center space-x-2 text-sm font-semibold text-slate-200">
        <Search className="w-4 h-4 text-emerald-400" />
        <span>Path Reachability Analysis</span>
      </div>

      <form onSubmit={handleReachSubmit} className="space-y-3 text-xs">
        <div>
          <label className="text-slate-400 block mb-1">Source Node</label>
          <input
            type="text"
            value={reachFrom}
            onChange={(e) => setReachFrom(e.target.value)}
            placeholder="Source Node ID"
            className="w-full rounded bg-slate-950 border border-slate-800 px-3 py-2 text-slate-200 font-mono focus:outline-none focus:border-emerald-500"
          />
        </div>
        <div>
          <label className="text-slate-400 block mb-1">Target Node</label>
          <input
            type="text"
            value={reachTo}
            onChange={(e) => setReachTo(e.target.value)}
            placeholder="Target Node ID"
            className="w-full rounded bg-slate-950 border border-slate-800 px-3 py-2 text-slate-200 font-mono focus:outline-none focus:border-emerald-500"
          />
        </div>
        <div className="flex justify-end">
          <button
            type="submit"
            disabled={reachLoading}
            className="px-4 py-2 rounded bg-emerald-600 hover:bg-emerald-500 text-white font-medium"
          >
            {reachLoading ? 'Evaluating...' : 'Find Reachability Path'}
          </button>
        </div>
      </form>

      {reachResult && (
        <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 space-y-2 text-xs">
          <div className="flex justify-between items-center">
            <span className="text-slate-400">Path Status:</span>
            <StatusBadge status={reachResult.status} />
          </div>
          {reachResult.path && (
            <div className="space-y-1 font-mono text-[11px]">
              <span className="text-slate-500 block">Shortest Traversal ({reachResult.path.length} hops):</span>
              <div className="p-2 rounded bg-slate-900 border border-slate-800 text-slate-300">
                {reachResult.path.nodeIds.join('  ➔  ')}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};