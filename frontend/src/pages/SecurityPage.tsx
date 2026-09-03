import React, { useEffect, useState } from 'react';
import { cloudOpsApi } from '../api';
import { SecurityExposure } from '../types/api';
import { useRegion } from '../context/RegionContext';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';
import { ErrorBanner } from '../components/feedback/ErrorBanner';
import { StatusBadge } from '../components/ui/StatusBadge';
import { BlastRadiusPanel } from '../components/security/BlastRadiusPanel';
import { ReachabilityPanel } from '../components/security/ReachabilityPanel';
import { LateralMovementPanel } from '../components/security/LateralMovementPanel';
import { ShieldAlert } from 'lucide-react';

export const SecurityPage: React.FC = () => {
  const { currentRegion } = useRegion();
  const [exposures, setExposures] = useState<SecurityExposure[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    cloudOpsApi
      .getExposures(currentRegion)
      .then((data) => {
        setExposures(data);
        setLoading(false);
      })
      .catch((err: Error) => {
        setError(err.message);
        setLoading(false);
      });
  }, [currentRegion]);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold tracking-tight text-slate-100">Security Blast-Radius & Exposure Intelligence</h2>
        <p className="text-xs text-slate-400 mt-1">
          Deterministic reachability analysis, public exposure detection, and lateral movement propagation.
        </p>
      </div>

      {loading && <LoadingSpinner message="Scanning security exposures..." />}
      {error && <ErrorBanner message={error} />}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <BlastRadiusPanel defaultNodeId={exposures[0]?.nodeId || ''} onError={setError} />
        <ReachabilityPanel onError={setError} />
      </div>

      {/* Lateral Movement Section */}
      <LateralMovementPanel onError={setError} />

      {/* Exposure Findings Table */}
      <div className="rounded-xl border border-slate-800 bg-slate-900/60 overflow-hidden">
        <div className="p-4 border-b border-slate-800 flex items-center space-x-2 text-sm font-semibold text-slate-200">
          <ShieldAlert className="w-4 h-4 text-amber-400" />
          <span>Observed Public Administrative Exposures (SECURITY-EXPOSURE-001)</span>
        </div>
        <table className="w-full text-left text-xs font-mono">
          <thead className="bg-slate-950/80 text-slate-400 border-b border-slate-800 uppercase font-sans font-semibold">
            <tr>
              <th className="px-4 py-3">Node ID</th>
              <th className="px-4 py-3">Resource ID</th>
              <th className="px-4 py-3">Exposure Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60">
            {exposures.map((exp) => (
              <tr key={exp.nodeId} className="hover:bg-slate-800/30">
                <td className="px-4 py-3 text-slate-300">{exp.nodeId}</td>
                <td className="px-4 py-3 text-sky-400">{exp.resourceId}</td>
                <td className="px-4 py-3">
                  <StatusBadge status={exp.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};